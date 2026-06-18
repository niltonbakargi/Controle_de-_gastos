package com.gastozen.ui.lancamento

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.*
import com.gastozen.domain.usecase.CriarLancamentoUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class LancamentoUiState {
    object Idle : LancamentoUiState()
    object Saving : LancamentoUiState()
    object Success : LancamentoUiState()
    data class Error(val message: String) : LancamentoUiState()
}

data class LancamentoFormState(
    val descricao: String = "",
    val valor: String = "",
    val data: Long = System.currentTimeMillis(),
    val tipo: TipoLancamento = TipoLancamento.DEBITO,
    val tipoPagamento: TipoPagamento = TipoPagamento.DINHEIRO,
    val categoriaId: Long? = null,
    val contaId: Long? = null,
    val observacao: String = "",
    val fotoUri: Uri? = null,
    val totalParcelas: Int = 1,
    val dataVencimento: Long? = null,
    val recorrente: Boolean = false
)

class LancamentoViewModel(
    private val criarUseCase: CriarLancamentoUseCase,
    private val contaRepo: ContaRepository,
    private val categoriaRepo: CategoriaRepository,
    private val recorrenteRepo: RecorrenteRepository
) : ViewModel() {

    val contas: StateFlow<List<Conta>> = contaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(LancamentoFormState())
    val form: StateFlow<LancamentoFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<LancamentoUiState>(LancamentoUiState.Idle)
    val uiState: StateFlow<LancamentoUiState> = _uiState.asStateFlow()

    fun updateDescricao(v: String) = _form.update { it.copy(descricao = v) }
    fun updateValor(v: String) = _form.update { it.copy(valor = v) }
    fun updateData(v: Long) = _form.update { it.copy(data = v) }
    fun updateTipo(v: TipoLancamento) = _form.update { it.copy(tipo = v) }
    fun updateTipoPagamento(v: TipoPagamento) = _form.update { it.copy(tipoPagamento = v) }
    fun updateCategoria(v: Long?) = _form.update { it.copy(categoriaId = v) }
    fun updateConta(v: Long?) = _form.update { it.copy(contaId = v) }
    fun updateObservacao(v: String) = _form.update { it.copy(observacao = v) }
    fun updateFoto(v: Uri?) = _form.update { it.copy(fotoUri = v) }
    fun updateParcelas(v: Int) = _form.update { it.copy(totalParcelas = v) }
    fun updateVencimento(v: Long?) = _form.update { it.copy(dataVencimento = v) }
    fun updateRecorrente(v: Boolean) = _form.update { it.copy(recorrente = v) }

    fun preencherDoPix(valor: Double, descricao: String) {
        _form.update {
            it.copy(
                valor = valor.toString(),
                descricao = descricao,
                tipo = TipoLancamento.DEBITO,
                tipoPagamento = TipoPagamento.PIX
            )
        }
    }

    fun salvar() {
        viewModelScope.launch {
            val f = _form.value
            val valor = f.valor.replace(",", ".").toDoubleOrNull()
            if (f.descricao.isBlank()) {
                _uiState.value = LancamentoUiState.Error("Descrição obrigatória")
                return@launch
            }
            if (valor == null || valor <= 0) {
                _uiState.value = LancamentoUiState.Error("Valor inválido")
                return@launch
            }

            _uiState.value = LancamentoUiState.Saving
            try {
                val lancamento = Lancamento(
                    descricao = f.descricao,
                    valor = valor,
                    data = f.data,
                    tipo = f.tipo,
                    tipoPagamento = f.tipoPagamento,
                    categoriaId = f.categoriaId,
                    contaId = f.contaId,
                    observacao = f.observacao,
                    fotoPath = f.fotoUri?.toString(),
                    totalParcelas = if (f.tipo == TipoLancamento.CREDITO) f.totalParcelas else 1,
                    dataVencimento = f.dataVencimento,
                    recorrente = f.recorrente
                )
                criarUseCase.executar(lancamento)

                if (f.recorrente) {
                    recorrenteRepo.insert(
                        Recorrente(
                            descricao = f.descricao,
                            valor = valor,
                            diaVencimento = Calendar.getInstance().apply { timeInMillis = f.data }
                                .get(Calendar.DAY_OF_MONTH),
                            categoriaId = f.categoriaId,
                            contaId = f.contaId,
                            tipo = f.tipo
                        )
                    )
                }

                _uiState.value = LancamentoUiState.Success
            } catch (e: Exception) {
                _uiState.value = LancamentoUiState.Error(e.message ?: "Erro ao salvar")
            }
        }
    }

    fun resetState() { _uiState.value = LancamentoUiState.Idle }
}
