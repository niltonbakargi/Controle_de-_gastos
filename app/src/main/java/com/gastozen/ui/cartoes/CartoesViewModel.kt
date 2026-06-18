package com.gastozen.ui.cartoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.*
import com.gastozen.data.repository.*
import com.gastozen.util.FaturaUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartaoComFatura(
    val cartao: CartaoCredito,
    val totalFaturaAtual: Double,
    val faturaAno: Int,
    val faturaMes: Int
)

class CartoesViewModel(
    private val cartaoRepo: CartaoCreditoRepository,
    private val lancamentoRepo: LancamentoRepository,
    private val pagamentoRepo: PagamentoFaturaRepository
) : ViewModel() {

    private val _dialogEditar = MutableStateFlow<CartaoCredito?>(null)
    val dialogEditar: StateFlow<CartaoCredito?> = _dialogEditar.asStateFlow()

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()

    val cartoes: StateFlow<List<CartaoComFatura>> = cartaoRepo.getAll()
        .flatMapLatest { lista ->
            if (lista.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val hoje = System.currentTimeMillis()
            combine(lista.map { cartao ->
                val (ano, mes) = FaturaUtils.computarFaturaMes(hoje, cartao.diaFechamento)
                lancamentoRepo.getTotalFatura(cartao.id, ano, mes).map { total ->
                    CartaoComFatura(cartao, total, ano, mes)
                }
            }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun abrirAddCartao() {
        _dialogEditar.value = CartaoCredito(nome = "", diaFechamento = 5, diaVencimento = 15)
    }

    fun abrirEditarCartao(c: CartaoCredito) { _dialogEditar.value = c }
    fun fecharDialogEditar() { _dialogEditar.value = null }

    fun salvarCartao(c: CartaoCredito) {
        viewModelScope.launch {
            try {
                if (c.id == 0L) cartaoRepo.insert(c) else cartaoRepo.update(c)
                _dialogEditar.value = null
            } catch (e: Exception) {
                _erro.value = "Erro ao salvar: ${e.message}"
            }
        }
    }

    fun deletarCartao(c: CartaoCredito) {
        viewModelScope.launch { cartaoRepo.delete(c) }
    }

    fun limparErro() { _erro.value = null }
}
