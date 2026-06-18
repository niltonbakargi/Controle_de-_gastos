package com.gastozen.ui.categorias

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.Categoria
import com.gastozen.data.repository.CategoriaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel
class CategoriasViewModel(private val repo: CategoriaRepository) : ViewModel() {
    val categorias: StateFlow<List<Categoria>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    fun salvar(categoria: Categoria) {
        viewModelScope.launch {
            if (categoria.id == 0L) repo.insert(categoria)
            else repo.update(categoria)
        }
    }

    fun deletar(categoria: Categoria) {
        viewModelScope.launch {
            val count = repo.countLancamentos(categoria.id)
            if (count > 0) {
                _uiEvent.emit("Não é possível excluir: categoria possui $count lançamentos")
            } else {
                repo.delete(categoria)
            }
        }
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    viewModel: CategoriasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Categoria?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editando = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Nova categoria")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(categorias, key = { it.id }) { categoria ->
                CategoriaItem(
                    categoria = categoria,
                    onEdit = { editando = it; showDialog = true },
                    onDelete = { viewModel.deletar(it) }
                )
            }
        }
    }

    if (showDialog) {
        CategoriaDialog(
            categoria = editando,
            onDismiss = { showDialog = false },
            onSalvar = { viewModel.salvar(it); showDialog = false }
        )
    }
}

@Composable
private fun CategoriaItem(
    categoria: Categoria,
    onEdit: (Categoria) -> Unit,
    onDelete: (Categoria) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = try { Color(android.graphics.Color.parseColor(categoria.corHex)) }
                        catch (e: Exception) { MaterialTheme.colorScheme.primary },
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoria.nome, style = MaterialTheme.typography.bodyLarge)
                categoria.metaMensal?.let {
                    Text("Meta: R$ ${"%.2f".format(it)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { onEdit(categoria) }) {
                Icon(Icons.Default.Edit, "Editar")
            }
            IconButton(onClick = { onDelete(categoria) }) {
                Icon(Icons.Default.Delete, "Excluir")
            }
        }
    }
}

@Composable
private fun CategoriaDialog(
    categoria: Categoria?,
    onDismiss: () -> Unit,
    onSalvar: (Categoria) -> Unit
) {
    var nome by remember { mutableStateOf(categoria?.nome ?: "") }
    var corHex by remember { mutableStateOf(categoria?.corHex ?: "#6200EE") }
    var meta by remember { mutableStateOf(categoria?.metaMensal?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria == null) "Nova Categoria" else "Editar Categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = corHex,
                    onValueChange = { corHex = it },
                    label = { Text("Cor (hex, ex: #F44336)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meta,
                    onValueChange = { meta = it },
                    label = { Text("Meta mensal (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nome.isNotBlank()) {
                    onSalvar(
                        (categoria ?: Categoria(nome = "")).copy(
                            nome = nome,
                            corHex = corHex,
                            metaMensal = meta.toDoubleOrNull()
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
