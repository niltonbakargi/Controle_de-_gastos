package com.gastozen.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.data.model.ProdutoCompradoComDetalhes
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.data.repository.ProdutoCompradoRepository
import com.gastozen.util.DateUtils
import kotlinx.coroutines.flow.*

data class GastosCategoriaState(
    val categoriaNome: String = "",
    val categoriaCorHex: String = "#6200EE",
    val totalLancamentos: Double = 0.0,
    val totalProdutos: Double = 0.0,
    val lancamentos: List<LancamentoComDetalhes> = emptyList(),
    val produtos: List<ProdutoCompradoComDetalhes> = emptyList()
) {
    val total get() = totalLancamentos + totalProdutos
}

class GastosCategoriaViewModel(
    val categoriaId: Long,
    val year: Int,
    val month: Int,
    private val lancamentoRepo: LancamentoRepository,
    private val produtoRepo: ProdutoCompradoRepository,
    private val categoriaRepo: CategoriaRepository
) : ViewModel() {

    private val inicio = DateUtils.startOfMonth(year, month)
    private val fim = DateUtils.endOfMonth(year, month)

    val state: StateFlow<GastosCategoriaState> = combine(
        categoriaRepo.getAll(),
        lancamentoRepo.getLancamentosPorCategoria(categoriaId, inicio, fim),
        produtoRepo.getPorCategoria(categoriaId, inicio, fim)
    ) { categorias, lancamentos, produtos ->
        val cat = categorias.find { it.id == categoriaId }
        GastosCategoriaState(
            categoriaNome   = cat?.nome ?: "Categoria",
            categoriaCorHex = cat?.corHex ?: "#6200EE",
            totalLancamentos = lancamentos.sumOf { it.valor },
            totalProdutos    = produtos.sumOf { it.valorTotal },
            lancamentos = lancamentos,
            produtos    = produtos
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GastosCategoriaState())
}
