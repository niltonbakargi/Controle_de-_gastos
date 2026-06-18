package com.gastozen.ui.configuracoes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.Recorrente
import com.gastozen.data.repository.RecorrenteRepository
import com.gastozen.util.CurrencyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecorrentesViewModel(private val repo: RecorrenteRepository) : ViewModel() {
    val recorrentes: StateFlow<List<Recorrente>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAtivo(recorrente: Recorrente) {
        viewModelScope.launch {
            repo.update(recorrente.copy(ativo = !recorrente.ativo))
        }
    }

    fun deletar(recorrente: Recorrente) {
        viewModelScope.launch { repo.delete(recorrente) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorrentesScreen(
    viewModel: RecorrentesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recorrentes by viewModel.recorrentes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lançamentos Recorrentes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (recorrentes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum lançamento recorrente.\nAo criar um lançamento, ative a opção 'Repetir todo mês'.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(recorrentes, key = { it.id }) { recorrente ->
                    RecorrenteItem(
                        recorrente = recorrente,
                        onToggle = { viewModel.toggleAtivo(recorrente) },
                        onDelete = { viewModel.deletar(recorrente) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecorrenteItem(
    recorrente: Recorrente,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(recorrente.descricao, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${CurrencyUtils.format(recorrente.valor)} • Dia ${recorrente.diaVencimento}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = recorrente.ativo,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Excluir")
            }
        }
    }
}
