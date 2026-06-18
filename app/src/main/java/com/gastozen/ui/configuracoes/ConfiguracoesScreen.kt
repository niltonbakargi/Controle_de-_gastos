package com.gastozen.ui.configuracoes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConfigUiState {
    object Idle : ConfigUiState()
    object Loading : ConfigUiState()
    data class Message(val text: String) : ConfigUiState()
    data class ShareIntent(val intent: Intent) : ConfigUiState()
}

class ConfiguracoesViewModel(
    private val lancamentoRepo: LancamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfigUiState>(ConfigUiState.Idle)
    val uiState: StateFlow<ConfigUiState> = _uiState

    fun exportarBackup(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = ConfigUiState.Loading
            BackupManager.exportDb(context).fold(
                onSuccess = { intent -> _uiState.value = ConfigUiState.ShareIntent(intent) },
                onFailure = { e -> _uiState.value = ConfigUiState.Message(e.message ?: "Erro") }
            )
        }
    }

    fun importarBackup(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ConfigUiState.Loading
            BackupManager.importDb(context, uri).fold(
                onSuccess = { _uiState.value = ConfigUiState.Message("Backup restaurado! Reinicie o app.") },
                onFailure = { e -> _uiState.value = ConfigUiState.Message(e.message ?: "Erro") }
            )
        }
    }

    fun limparTodos(context: android.content.Context) {
        viewModelScope.launch {
            lancamentoRepo.deleteAll()
            _uiState.value = ConfigUiState.Message("Todos os lançamentos foram excluídos.")
        }
    }

    fun resetState() { _uiState.value = ConfigUiState.Idle }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    viewModel: ConfiguracoesViewModel,
    onBack: () -> Unit,
    onCategorias: () -> Unit,
    onRecorrentes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmLimpar by remember { mutableStateOf(false) }
    var confirmacaoTexto by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importarBackup(context, it) } }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ConfigUiState.Message -> {
                snackbarHostState.showSnackbar(state.text)
                viewModel.resetState()
            }
            is ConfigUiState.ShareIntent -> {
                context.startActivity(state.intent)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (uiState is ConfigUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            SectionHeader("Dados")

            ListItem(
                headlineContent = { Text("Categorias") },
                supportingContent = { Text("Criar, editar e excluir categorias") },
                leadingContent = { Icon(Icons.Default.Category, null) },
                modifier = Modifier.clickable { onCategorias() }
            )

            ListItem(
                headlineContent = { Text("Lançamentos Recorrentes") },
                supportingContent = { Text("Gerenciar pagamentos automáticos") },
                leadingContent = { Icon(Icons.Default.Repeat, null) },
                modifier = Modifier.clickable { onRecorrentes() }
            )

            HorizontalDivider()
            SectionHeader("Backup")

            ListItem(
                headlineContent = { Text("Exportar Backup") },
                supportingContent = { Text("Compartilhar banco de dados (.db)") },
                leadingContent = { Icon(Icons.Default.Upload, null) },
                modifier = Modifier.clickable { viewModel.exportarBackup(context) }
            )

            ListItem(
                headlineContent = { Text("Importar Backup") },
                supportingContent = { Text("Restaurar de arquivo .db") },
                leadingContent = { Icon(Icons.Default.Download, null) },
                modifier = Modifier.clickable { importLauncher.launch("application/octet-stream") }
            )

            HorizontalDivider()
            SectionHeader("Perigo")

            ListItem(
                headlineContent = {
                    Text("Limpar todos os lançamentos", color = MaterialTheme.colorScheme.error)
                },
                supportingContent = { Text("Ação irreversível") },
                leadingContent = {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable { showConfirmLimpar = true; confirmacaoTexto = "" }
            )
        }
    }

    if (showConfirmLimpar) {
        AlertDialog(
            onDismissRequest = { showConfirmLimpar = false },
            title = { Text("Confirmar exclusão") },
            text = {
                Column {
                    Text("Esta ação excluirá TODOS os lançamentos permanentemente. Digite CONFIRMAR para prosseguir.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmacaoTexto,
                        onValueChange = { confirmacaoTexto = it },
                        label = { Text("Digite CONFIRMAR") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (confirmacaoTexto == "CONFIRMAR") {
                            viewModel.limparTodos(context)
                            showConfirmLimpar = false
                        }
                    },
                    enabled = confirmacaoTexto == "CONFIRMAR"
                ) { Text("Excluir tudo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmLimpar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
