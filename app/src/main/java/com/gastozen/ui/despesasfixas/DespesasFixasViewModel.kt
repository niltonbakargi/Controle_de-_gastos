package com.gastozen.ui.despesasfixas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DespesaFixaComStatus(
    val despesa: DespesaFixa,
    val pagamento: PagamentoDespesaFixa? = null
) {
    val pago: Boolean get() = pagamento?.pago == true
}

data class MesSelecionado(val ano: Int, val mes: Int)  // mes: 1-12

class DespesasFixasViewModel(
    private val despesaRepo: DespesaFixaRepository,
    private val pagamentoRepo: PagamentoDespesaFixaRepository,
    private val categoriaRepo: CategoriaRepository,
    private val lancamentoRepo: LancamentoRepository
) : ViewModel() {

    private val hoje = Calendar.getInstance()

    private val _mes = MutableStateFlow(
        MesSelecionado(hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH) + 1)
    )
    val mes: StateFlow<MesSelecionado> = _mes.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val despesas: StateFlow<List<DespesaFixaComStatus>> = _mes.flatMapLatest { ym ->
        combine(
            despesaRepo.getAllAtivas(),
            pagamentoRepo.getDoMes(ym.mes, ym.ano)
        ) { lista, pagamentos ->
            lista.map { d ->
                DespesaFixaComStatus(d, pagamentos.find { it.despesaFixaId == d.id })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Dialogs ────────────────────────────────────────────────────────────────
    private val _dialogEditar = MutableStateFlow<DespesaFixa?>(null)
    val dialogEditar: StateFlow<DespesaFixa?> = _dialogEditar.asStateFlow()

    private val _dialogMarcarPago = MutableStateFlow<DespesaFixaComStatus?>(null)
    val dialogMarcarPago: StateFlow<DespesaFixaComStatus?> = _dialogMarcarPago.asStateFlow()

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()

    // ── Navegação de mês ───────────────────────────────────────────────────────
    fun mesAnterior() = _mes.update {
        if (it.mes == 1) MesSelecionado(it.ano - 1, 12) else it.copy(mes = it.mes - 1)
    }

    fun proximoMes() = _mes.update {
        if (it.mes == 12) MesSelecionado(it.ano + 1, 1) else it.copy(mes = it.mes + 1)
    }

    // ── CRUD Despesa Fixa ─────────────────────────────────────────────────────
    fun abrirAddDespesa() {
        _dialogEditar.value = DespesaFixa(nome = "", valor = 0.0, diaVencimento = 10)
    }

    fun abrirEditarDespesa(d: DespesaFixa) { _dialogEditar.value = d }
    fun fecharDialogEditar() { _dialogEditar.value = null }

    fun salvarDespesa(d: DespesaFixa) {
        viewModelScope.launch {
            try {
                if (d.id == 0L) despesaRepo.insert(d) else despesaRepo.update(d)
                _dialogEditar.value = null
            } catch (e: Exception) {
                _erro.value = "Erro ao salvar: ${e.message}"
            }
        }
    }

    fun deletarDespesa(d: DespesaFixa) {
        viewModelScope.launch { despesaRepo.delete(d) }
    }

    // ── Pagamento ─────────────────────────────────────────────────────────────
    fun abrirMarcarPago(status: DespesaFixaComStatus) { _dialogMarcarPago.value = status }
    fun fecharDialogMarcarPago() { _dialogMarcarPago.value = null }

    fun marcarPago(
        despesa: DespesaFixa,
        valorPago: Double,
        dataPagamento: Long,
        comprovantePath: String? = null
    ) {
        val ym = _mes.value
        viewModelScope.launch {
            try {
                val lancamento = Lancamento(
                    descricao = despesa.nome,
                    valor = valorPago,
                    data = dataPagamento,
                    tipo = TipoLancamento.DEBITO,
                    tipoPagamento = TipoPagamento.OUTROS,
                    categoriaId = despesa.categoriaId
                )
                val lancamentoId = lancamentoRepo.insert(lancamento)
                pagamentoRepo.insert(
                    PagamentoDespesaFixa(
                        despesaFixaId = despesa.id,
                        mes = ym.mes,
                        ano = ym.ano,
                        valorPago = valorPago,
                        dataPagamento = dataPagamento,
                        pago = true,
                        comprovantePath = comprovantePath,
                        lancamentoId = lancamentoId
                    )
                )
                _dialogMarcarPago.value = null
            } catch (e: Exception) {
                _erro.value = "Erro ao registrar pagamento: ${e.message}"
            }
        }
    }

    fun desmarcarPago(status: DespesaFixaComStatus) {
        val pagamento = status.pagamento ?: return
        viewModelScope.launch {
            pagamentoRepo.deleteById(pagamento.id)
            // O lançamento gerado permanece no histórico
        }
    }

    fun limparErro() { _erro.value = null }
}
