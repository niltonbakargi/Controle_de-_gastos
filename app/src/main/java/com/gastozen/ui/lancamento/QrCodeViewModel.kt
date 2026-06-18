package com.gastozen.ui.lancamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.Categoria
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.data.repository.RegraCategoriaRepository
import com.gastozen.domain.usecase.CriarLancamentoUseCase
import com.gastozen.domain.usecase.SalvarRegraCategoriaUseCase
import com.gastozen.util.NfeXmlParser
import com.gastozen.util.NotaFiscal
import com.gastozen.util.ProdutoNfe
import com.gastozen.util.QrCodeParser
import com.gastozen.data.model.Lancamento
import com.gastozen.data.model.TipoLancamento
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class QrCodeUiState {
    object Idle : QrCodeUiState()
    object Loading : QrCodeUiState()
    data class ProdutosSemCategoria(
        val nota: NotaFiscal,
        val semCategoria: List<ProdutoNfe>
    ) : QrCodeUiState()
    data class NfeCarregada(val nota: NotaFiscal) : QrCodeUiState()
    data class Error(val message: String, val url: String = "") : QrCodeUiState()
}

class QrCodeViewModel(
    private val categoriaRepo: CategoriaRepository,
    private val regraRepo: RegraCategoriaRepository,
    private val criarUseCase: CriarLancamentoUseCase,
    private val regraUseCase: SalvarRegraCategoriaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrCodeUiState>(QrCodeUiState.Idle)
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processarUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = QrCodeUiState.Loading
            QrCodeParser.fetchNotaFiscal(url).fold(
                onSuccess = { nota ->
                    val semCategoria = mutableListOf<ProdutoNfe>()
                    nota.produtos.forEach { produto ->
                        val categoriaId = regraUseCase.findCategoria(produto.nome, produto.ncm, regraRepo)
                        if (categoriaId == null) semCategoria.add(produto)
                    }
                    if (semCategoria.isEmpty()) {
                        lançarNota(nota)
                    } else {
                        _uiState.value = QrCodeUiState.ProdutosSemCategoria(nota, semCategoria)
                    }
                },
                onFailure = { e ->
                    _uiState.value = QrCodeUiState.Error(e.message ?: "Erro ao processar nota", url)
                }
            )
        }
    }

    fun salvarRegrasELançar(nota: NotaFiscal, mapeamentos: Map<String, Long>) {
        viewModelScope.launch {
            mapeamentos.forEach { (nomeProduto, categoriaId) ->
                regraUseCase.executar(
                    palavraChave = nomeProduto.split(" ").firstOrNull { it.length >= 3 },
                    ncm = nota.produtos.find { it.nome == nomeProduto }?.ncm,
                    categoriaId = categoriaId
                )
            }
            lançarNota(nota)
        }
    }

    private suspend fun lançarNota(nota: NotaFiscal) {
        nota.produtos.forEach { produto ->
            val categoriaId = regraUseCase.findCategoria(produto.nome, produto.ncm, regraRepo)
            criarUseCase.executar(
                Lancamento(
                    descricao = produto.nome,
                    valor = produto.valor,
                    tipo = TipoLancamento.DEBITO,
                    categoriaId = categoriaId,
                    observacao = "NF-e: ${nota.cnpjEmitente}"
                )
            )
        }
        _uiState.value = QrCodeUiState.NfeCarregada(nota)
    }

    fun salvarSemCategoria(nota: NotaFiscal) {
        viewModelScope.launch { lançarNota(nota) }
    }

    fun resetar() { _uiState.value = QrCodeUiState.Idle }
}
