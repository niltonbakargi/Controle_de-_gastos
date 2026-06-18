package com.gastozen.ui.comprovante

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.DespesaFixaRepository
import com.gastozen.data.repository.PagamentoDespesaFixaRepository
import com.gastozen.domain.usecase.CriarLancamentoUseCase
import com.gastozen.util.OcrHelper
import com.gastozen.util.PendingComprovante
import com.gastozen.util.PixReceiptParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

sealed class ComprovanteUiState {
    object Processing : ComprovanteUiState()
    object Ready : ComprovanteUiState()
    object Saving : ComprovanteUiState()
    object Success : ComprovanteUiState()
    data class Error(val message: String) : ComprovanteUiState()
}

data class ComprovanteFormState(
    val tipo: TipoLancamento = TipoLancamento.DEBITO,
    val descricao: String = "",
    val valor: String = "",
    val categoriaId: Long? = null,
    val despesaFixaId: Long? = null
)

class ReceberComprovanteViewModel(
    private val application: Application,
    private val categoriaRepo: CategoriaRepository,
    private val criarUseCase: CriarLancamentoUseCase,
    private val despesaFixaRepo: DespesaFixaRepository,
    private val pagamentoRepo: PagamentoDespesaFixaRepository
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Despesas fixas pendentes neste mês — para o usuário vincular o comprovante
    private val _mesAtual = Calendar.getInstance().let { Pair(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1) }
    val despesasFixasPendentes: StateFlow<List<DespesaFixa>> = despesaFixaRepo.getAllAtivas()
        .combine(pagamentoRepo.getDoMes(_mesAtual.second, _mesAtual.first)) { todas, pagas ->
            val idsPagas = pagas.filter { it.pago }.map { it.despesaFixaId }.toSet()
            todas.filter { it.id !in idsPagas }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ComprovanteUiState>(ComprovanteUiState.Processing)
    val uiState: StateFlow<ComprovanteUiState> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(ComprovanteFormState())
    val form: StateFlow<ComprovanteFormState> = _form.asStateFlow()

    // URI original do comprovante compartilhado (para cópia ao vincular despesa fixa)
    private var originalUri: Uri? = null

    init {
        processarComprovante()
    }

    private fun processarComprovante() {
        val content = PendingComprovante.consume() ?: run {
            _uiState.value = ComprovanteUiState.Ready
            return
        }
        originalUri = when (content) {
            is PendingComprovante.Content.Imagem -> content.uri
            is PendingComprovante.Content.Pdf    -> content.uri
        }
        viewModelScope.launch {
            _uiState.value = ComprovanteUiState.Processing
            try {
                val texto = when (content) {
                    is PendingComprovante.Content.Imagem ->
                        OcrHelper.fromUri(application, content.uri)
                    is PendingComprovante.Content.Pdf ->
                        OcrHelper.fromPdf(application, content.uri)
                }
                val recibo = PixReceiptParser.parse(texto)
                if (recibo != null) {
                    _form.update {
                        it.copy(
                            tipo = recibo.tipo,
                            descricao = recibo.descricao,
                            valor = "%.2f".format(recibo.valor).replace(".", ",")
                        )
                    }
                }
            } catch (_: Exception) {
                // OCR falhou — abre o formulário vazio para preenchimento manual
            }
            _uiState.value = ComprovanteUiState.Ready
        }
    }

    fun updateTipo(v: TipoLancamento) = _form.update { it.copy(tipo = v) }
    fun updateDescricao(v: String) = _form.update { it.copy(descricao = v) }
    fun updateValor(v: String) = _form.update { it.copy(valor = v) }
    fun updateCategoria(v: Long?) = _form.update { it.copy(categoriaId = v) }
    fun updateDespesaFixa(id: Long?) = _form.update { it.copy(despesaFixaId = id) }
    fun resetState() { _uiState.value = ComprovanteUiState.Ready }

    fun salvar() {
        viewModelScope.launch {
            val f = _form.value
            val valor = f.valor.replace(",", ".").toDoubleOrNull()
            if (f.descricao.isBlank()) {
                _uiState.value = ComprovanteUiState.Error("Descrição obrigatória")
                return@launch
            }
            if (valor == null || valor <= 0) {
                _uiState.value = ComprovanteUiState.Error("Valor inválido")
                return@launch
            }
            _uiState.value = ComprovanteUiState.Saving
            try {
                val lancamento = Lancamento(
                    descricao = f.descricao,
                    valor = valor,
                    data = System.currentTimeMillis(),
                    tipo = f.tipo,
                    tipoPagamento = TipoPagamento.PIX,
                    categoriaId = f.categoriaId
                )
                val lancamentoId = criarUseCase.executar(lancamento)

                // Vincula à despesa fixa se selecionada
                f.despesaFixaId?.let { despesaId ->
                    val comprovantePath = originalUri?.let { copiarComprovante(it) }
                    pagamentoRepo.insert(
                        PagamentoDespesaFixa(
                            despesaFixaId = despesaId,
                            mes = _mesAtual.second,
                            ano = _mesAtual.first,
                            valorPago = valor,
                            dataPagamento = System.currentTimeMillis(),
                            pago = true,
                            comprovantePath = comprovantePath,
                            lancamentoId = lancamentoId
                        )
                    )
                }

                _uiState.value = ComprovanteUiState.Success
            } catch (e: Exception) {
                _uiState.value = ComprovanteUiState.Error(e.message ?: "Erro ao salvar")
            }
        }
    }

    /** Copia o comprovante para o armazenamento interno do app para persistência. */
    private fun copiarComprovante(uri: Uri): String? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val dir = File(application.filesDir, "comprovantes").also { it.mkdirs() }
            val file = File(dir, "${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> inputStream.use { it.copyTo(out) } }
            file.absolutePath
        } catch (_: Exception) { null }
    }
}
