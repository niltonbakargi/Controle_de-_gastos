package com.gastozen.ui.cartoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.*
import com.gastozen.util.FaturaUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FaturaViewModel(
    val cartaoId: Long,
    private val cartaoRepo: CartaoCreditoRepository,
    private val lancamentoRepo: LancamentoRepository,
    private val pagamentoRepo: PagamentoFaturaRepository
) : ViewModel() {

    private val hoje = Calendar.getInstance()

    val cartao: StateFlow<CartaoCredito?> = cartaoRepo.getAll()
        .map { lista -> lista.find { it.id == cartaoId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Mês selecionado — começa na fatura atual do cartão
    private val _mes = MutableStateFlow<Pair<Int, Int>?>(null)
    val mes: StateFlow<Pair<Int, Int>?> = _mes.asStateFlow()

    init {
        viewModelScope.launch {
            cartao.filterNotNull().first().let { c ->
                val (ano, mes) = FaturaUtils.computarFaturaMes(hoje.timeInMillis, c.diaFechamento)
                _mes.value = Pair(ano, mes)
            }
        }
    }

    val lancamentos: StateFlow<List<LancamentoComDetalhes>> = combine(_mes, cartao) { mesVal, cartaoVal ->
        Pair(mesVal, cartaoVal)
    }.flatMapLatest { (mesVal, cartaoVal) ->
        if (mesVal == null || cartaoVal == null) flowOf(emptyList())
        else lancamentoRepo.getLancamentosFatura(cartaoId, mesVal.first, mesVal.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFatura: StateFlow<Double> = combine(_mes, cartao) { m, c -> Pair(m, c) }
        .flatMapLatest { (m, _) ->
            if (m == null) flowOf(0.0)
            else lancamentoRepo.getTotalFatura(cartaoId, m.first, m.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pagamento: StateFlow<PagamentoFatura?> = _mes.flatMapLatest { m ->
        if (m == null) flowOf<PagamentoFatura?>(null)
        else flow { emit(pagamentoRepo.find(cartaoId, m.first, m.second)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dialogPagar = MutableStateFlow(false)
    val dialogPagar: StateFlow<Boolean> = _dialogPagar.asStateFlow()

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()

    fun mesAnterior() = _mes.update { m ->
        m ?: return@update m
        if (m.second == 1) Pair(m.first - 1, 12) else Pair(m.first, m.second - 1)
    }

    fun proximoMes() {
        val hojeMs = FaturaUtils.computarFaturaMes(
            System.currentTimeMillis(),
            cartao.value?.diaFechamento ?: 5
        )
        val m = _mes.value ?: return
        val proximo = if (m.second == 12) Pair(m.first + 1, 1) else Pair(m.first, m.second + 1)
        if (proximo.first < hojeMs.first || (proximo.first == hojeMs.first && proximo.second <= hojeMs.second)) {
            _mes.value = proximo
        }
    }

    fun abrirDialogPagar() { _dialogPagar.value = true }
    fun fecharDialogPagar() { _dialogPagar.value = false }

    fun pagarFatura(valorPago: Double) {
        val m = _mes.value ?: return
        viewModelScope.launch {
            try {
                pagamentoRepo.insert(
                    PagamentoFatura(
                        cartaoId = cartaoId,
                        faturaAno = m.first,
                        faturaMes = m.second,
                        valorPago = valorPago,
                        dataPagamento = System.currentTimeMillis(),
                        pago = true
                    )
                )
                _dialogPagar.value = false
            } catch (e: Exception) {
                _erro.value = "Erro: ${e.message}"
            }
        }
    }

    fun despagarFatura() {
        val pag = pagamento.value ?: return
        viewModelScope.launch { pagamentoRepo.deleteById(pag.id) }
    }

    fun limparErro() { _erro.value = null }
}
