package com.gastozen.ui.lancamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.Categoria
import com.gastozen.data.model.ProdutoCompradoComDetalhes
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.ProdutoCompradoRepository
import com.gastozen.domain.usecase.ClassificarProdutoUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ClassificarUiState {
    object Idle : ClassificarUiState()
    object Salvando : ClassificarUiState()
    object Concluido : ClassificarUiState()
}

class ClassificarProdutosViewModel(
    private val lancamentoId: Long,
    private val produtoRepo: ProdutoCompradoRepository,
    private val categoriaRepo: CategoriaRepository,
    private val classificarUseCase: ClassificarProdutoUseCase
) : ViewModel() {

    private val _produtos = MutableStateFlow<List<ProdutoCompradoComDetalhes>>(emptyList())
    val produtos: StateFlow<List<ProdutoCompradoComDetalhes>> = _produtos.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // mapeamento produtoId → categoriaId selecionada pelo usuário
    private val _mapeamento = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val mapeamento: StateFlow<Map<Long, Long>> = _mapeamento.asStateFlow()

    private val _uiState = MutableStateFlow<ClassificarUiState>(ClassificarUiState.Idle)
    val uiState: StateFlow<ClassificarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _produtos.value = produtoRepo.getSemCategoria(lancamentoId)
        }
    }

    fun atribuirCategoria(produtoId: Long, categoriaId: Long) {
        _mapeamento.update { it + (produtoId to categoriaId) }
    }

    fun salvar() {
        viewModelScope.launch {
            _uiState.value = ClassificarUiState.Salvando
            val map = _mapeamento.value
            _produtos.value.forEach { produto ->
                val categoriaId = map[produto.id] ?: return@forEach
                classificarUseCase.executar(produto, categoriaId)
            }
            _uiState.value = ClassificarUiState.Concluido
        }
    }
}
