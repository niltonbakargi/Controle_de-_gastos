package com.gastozen.ui.comprovante

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.Categoria
import com.gastozen.data.model.DespesaFixa
import com.gastozen.data.model.TipoLancamento

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceberComprovanteScreen(
    viewModel: ReceberComprovanteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val despesasFixasPendentes by viewModel.despesasFixasPendentes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ComprovanteUiState.Success -> onBack()
            is ComprovanteUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as ComprovanteUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comprovante recebido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->

        if (uiState is ComprovanteUiState.Processing) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Lendo comprovante…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Entrada / Saída ─────────────────────────────────────────────
            Text(
                "Tipo de lançamento",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = form.tipo == TipoLancamento.RECEITA,
                    onClick = { viewModel.updateTipo(TipoLancamento.RECEITA) },
                    label = { Text("Entrada") },
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = form.tipo == TipoLancamento.DEBITO,
                    onClick = { viewModel.updateTipo(TipoLancamento.DEBITO) },
                    label = { Text("Saída") },
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp)) }
                )
            }

            // ── Vincular a despesa fixa (somente Saída) ─────────────────────
            if (form.tipo == TipoLancamento.DEBITO && despesasFixasPendentes.isNotEmpty()) {
                DespesaFixaDropdown(
                    despesas = despesasFixasPendentes,
                    selectedId = form.despesaFixaId,
                    onSelect = viewModel::updateDespesaFixa
                )
            }

            // ── Categoria (somente Saída sem despesa fixa vinculada) ─────────
            if (form.tipo == TipoLancamento.DEBITO
                && form.despesaFixaId == null
                && categorias.isNotEmpty()
            ) {
                Text(
                    "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = form.categoriaId == null,
                        onClick = { viewModel.updateCategoria(null) },
                        label = { Text("Nenhuma") }
                    )
                    categorias.forEach { cat ->
                        FilterChip(
                            selected = form.categoriaId == cat.id,
                            onClick = { viewModel.updateCategoria(cat.id) },
                            label = { Text(cat.nome) }
                        )
                    }
                }
            }

            // ── Descrição ──────────────────────────────────────────────────
            OutlinedTextField(
                value = form.descricao,
                onValueChange = viewModel::updateDescricao,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )

            // ── Valor ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = form.valor,
                onValueChange = viewModel::updateValor,
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
            )

            Spacer(Modifier.height(4.dp))

            // ── Salvar ─────────────────────────────────────────────────────
            Button(
                onClick = viewModel::salvar,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is ComprovanteUiState.Saving
            ) {
                Text("Salvar lançamento")
            }

            if (uiState is ComprovanteUiState.Saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DespesaFixaDropdown(
    despesas: List<DespesaFixa>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = despesas.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.nome} — R$ ${"%.2f".format(it.valor)}" }
                ?: "Não vincular",
            onValueChange = {},
            label = { Text("Despesa fixa (boleto)") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            leadingIcon = { Icon(Icons.Default.Receipt, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Não vincular") },
                onClick = { onSelect(null); expanded = false }
            )
            despesas.forEach { d ->
                DropdownMenuItem(
                    text = { Text("${d.nome} — R$ ${"%.2f".format(d.valor)} · Vence dia ${d.diaVencimento}") },
                    onClick = { onSelect(d.id); expanded = false }
                )
            }
        }
    }
}
