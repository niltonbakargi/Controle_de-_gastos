package com.gastozen.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.GastoPorCategoria
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.data.model.ResumoMensal
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class FiltroMes(val year: Int, val month: Int) {
    val inicio: Long get() = DateUtils.startOfMonth(year, month)
    val fim: Long get() = DateUtils.endOfMonth(year, month)
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val resumo: ResumoMensal,
        val gastosPorCategoria: List<GastoPorCategoria>,
        val ultimos10: List<LancamentoComDetalhes>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val lancamentoRepo: LancamentoRepository
) : ViewModel() {

    private val _filtro = MutableStateFlow(
        FiltroMes(DateUtils.currentYear(), DateUtils.currentMonth())
    )
    val filtro: StateFlow<FiltroMes> = _filtro.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = _filtro.flatMapLatest { f ->
        combine(
            lancamentoRepo.getResumoMensal(f.inicio, f.fim, System.currentTimeMillis()),
            lancamentoRepo.getGastosPorCategoria(f.inicio, f.fim),
            lancamentoRepo.getUltimos10()
        ) { resumo, gastos, ultimos ->
            DashboardUiState.Success(resumo, gastos, ultimos) as DashboardUiState
        }
    }.catch { e ->
        emit(DashboardUiState.Error(e.message ?: "Erro desconhecido"))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState.Loading
    )

    fun mesAnterior() {
        _filtro.update { f ->
            val cal = Calendar.getInstance().apply {
                set(f.year, f.month, 1)
                add(Calendar.MONTH, -1)
            }
            FiltroMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }

    fun mesSeguinte() {
        _filtro.update { f ->
            val cal = Calendar.getInstance().apply {
                set(f.year, f.month, 1)
                add(Calendar.MONTH, 1)
            }
            FiltroMes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }

    fun setMes(year: Int, month: Int) {
        _filtro.value = FiltroMes(year, month)
    }

    fun deleteLancamento(id: Long) {
        viewModelScope.launch {
            lancamentoRepo.deleteById(id)
        }
    }
}
