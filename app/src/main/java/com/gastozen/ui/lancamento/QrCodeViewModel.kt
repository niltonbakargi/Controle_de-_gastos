package com.gastozen.ui.lancamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.CartaoCreditoRepository
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.ProdutoCompradoRepository
import com.gastozen.data.repository.RegraCategoriaRepository
import com.gastozen.domain.usecase.CriarLancamentoUseCase
import com.gastozen.domain.usecase.SalvarRegraCategoriaUseCase
import com.gastozen.util.FaturaUtils
import com.gastozen.util.NotaFiscal
import com.gastozen.util.QrCodeParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class QrCodeUiState {
    object Idle : QrCodeUiState()
    object Loading : QrCodeUiState()

    /** NF-e lida com sucesso — aguardando confirmação do usuário (tipoPagamento etc.) */
    data class NfeSumario(val nota: NotaFiscal) : QrCodeUiState()

    /** Lançamento salvo; [semCategoria] lista produtos que precisam de classificação */
    data class NfeCarregada(
        val lancamentoId: Long,
        val semCategoria: List<ProdutoCompradoComDetalhes>
    ) : QrCodeUiState()

    data class Error(val message: String, val url: String = "") : QrCodeUiState()
}

class QrCodeViewModel(
    private val categoriaRepo: CategoriaRepository,
    private val regraRepo: RegraCategoriaRepository,
    private val produtoRepo: ProdutoCompradoRepository,
    private val criarUseCase: CriarLancamentoUseCase,
    private val regraUseCase: SalvarRegraCategoriaUseCase,
    private val cartaoRepo: CartaoCreditoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrCodeUiState>(QrCodeUiState.Idle)
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartoes: StateFlow<List<CartaoCredito>> = cartaoRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processarUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = QrCodeUiState.Loading
            QrCodeParser.fetchNotaFiscal(url).fold(
                onSuccess = { nota ->
                    _uiState.value = QrCodeUiState.NfeSumario(nota)
                },
                onFailure = { e ->
                    _uiState.value = QrCodeUiState.Error(e.message ?: "Erro ao processar nota", url)
                }
            )
        }
    }

    /**
     * Confirmado pelo usuário: salva UM Lancamento com o valor total e
     * persiste cada produto em ProdutoComprado.
     */
    fun confirmarNota(
        nota: NotaFiscal,
        tipoPagamento: TipoPagamento,
        contaId: Long?,
        cartaoId: Long?
    ) {
        viewModelScope.launch {
            _uiState.value = QrCodeUiState.Loading
            try {
                val valorLiquido = maxOf(nota.valorTotal - nota.desconto, 0.0)

                val (faturaAno, faturaMes) = if (cartaoId != null) {
                    val cartao = cartaoRepo.getById(cartaoId)
                    if (cartao != null) FaturaUtils.computarFaturaMes(System.currentTimeMillis(), cartao.diaFechamento)
                    else Pair(0, 0)
                } else Pair(0, 0)

                val lancamento = Lancamento(
                    descricao = "Compra NF-e ${nota.cnpjEmitente.takeLast(8)}",
                    valor = valorLiquido,
                    tipo = TipoLancamento.DEBITO,
                    tipoPagamento = tipoPagamento,
                    contaId = contaId,
                    desconto = nota.desconto,
                    nfeCnpj = nota.cnpjEmitente,
                    cartaoId = cartaoId,
                    faturaAno = if (cartaoId != null) faturaAno else null,
                    faturaMes = if (cartaoId != null) faturaMes else null
                )

                // Resolve categorias pelos produtos
                val produtos = nota.produtos.map { p ->
                    val categoriaId = regraUseCase.findCategoria(p.nome, p.ncm, regraRepo)
                    ProdutoComprado(
                        lancamentoId = 0L, // será preenchido após insert
                        nome = p.nome,
                        ncm = p.ncm,
                        quantidade = p.quantidade,
                        valorUnitario = p.valorUnitario,
                        valorTotal = p.valor,
                        categoriaId = categoriaId
                    )
                }

                val lancamentoId = criarUseCase.executarNfe(lancamento, produtos)

                // Busca produtos sem categoria para redirecionar à classificação
                val semCategoria = produtoRepo.getSemCategoria(lancamentoId)
                _uiState.value = QrCodeUiState.NfeCarregada(lancamentoId, semCategoria)

            } catch (e: Exception) {
                _uiState.value = QrCodeUiState.Error(e.message ?: "Erro ao salvar nota")
            }
        }
    }

    fun resetar() { _uiState.value = QrCodeUiState.Idle }
}
