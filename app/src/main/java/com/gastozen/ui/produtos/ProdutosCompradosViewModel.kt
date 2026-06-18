package com.gastozen.ui.produtos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.ProdutoCompradoComDetalhes
import com.gastozen.data.repository.ProdutoCompradoRepository
import com.gastozen.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

class ProdutosCompradosViewModel(
    private val produtoRepo: ProdutoCompradoRepository
) : ViewModel() {

    // mês exibido (hoje por padrão)
    private val _mes = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val mes: StateFlow<LocalDate> = _mes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val produtos: StateFlow<List<ProdutoCompradoComDetalhes>> = _mes.flatMapLatest { inicio ->
        val fim = inicio.plusMonths(1).minusDays(1)
        val inicioMs = inicio.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val fimMs = fim.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        produtoRepo.getByPeriodo(inicioMs, fimMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun mesAnterior() { _mes.update { it.minusMonths(1) } }
    fun proximoMes() { _mes.update { it.plusMonths(1) } }
}
