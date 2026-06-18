package com.gastozen.ui.lancamento

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.Categoria
import com.gastozen.data.model.Conta
import com.gastozen.data.model.TipoLancamento
import com.gastozen.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentoScreen(
    viewModel: LancamentoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contas by viewModel.contas.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is LancamentoUiState.Success -> onBack()
            is LancamentoUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as LancamentoUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Lançamento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::salvar,
                        enabled = uiState !is LancamentoUiState.Saving
                    ) {
                        Text("Salvar")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tipo
            TipoSelector(selected = form.tipo, onSelect = viewModel::updateTipo)

            // Descrição
            OutlinedTextField(
                value = form.descricao,
                onValueChange = viewModel::updateDescricao,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )

            // Valor
            OutlinedTextField(
                value = form.valor,
                onValueChange = viewModel::updateValor,
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
            )

            // Data
            DateField(
                label = "Data",
                millis = form.data,
                context = context,
                onDateSelected = viewModel::updateData
            )

            // Categoria
            CategoriaDropdown(
                categorias = categorias,
                selectedId = form.categoriaId,
                onSelect = { viewModel.updateCategoria(it) }
            )

            // Conta
            ContaDropdown(
                contas = contas,
                selectedId = form.contaId,
                onSelect = { viewModel.updateConta(it) }
            )

            // Parcelas (apenas crédito)
            if (form.tipo == TipoLancamento.CREDITO) {
                OutlinedTextField(
                    value = form.totalParcelas.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateParcelas(it) } },
                    label = { Text("Número de parcelas") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                DateField(
                    label = "Data de vencimento",
                    millis = form.dataVencimento ?: System.currentTimeMillis(),
                    context = context,
                    onDateSelected = { viewModel.updateVencimento(it) }
                )
            }

            // Observação
            OutlinedTextField(
                value = form.observacao,
                onValueChange = viewModel::updateObservacao,
                label = { Text("Observação") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Recorrente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Repetir todo mês", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = form.recorrente,
                    onCheckedChange = viewModel::updateRecorrente
                )
            }

            if (uiState is LancamentoUiState.Saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun TipoSelector(selected: TipoLancamento, onSelect: (TipoLancamento) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TipoLancamento.values().forEach { tipo ->
            FilterChip(
                selected = selected == tipo,
                onClick = { onSelect(tipo) },
                label = {
                    Text(when (tipo) {
                        TipoLancamento.DEBITO -> "Débito"
                        TipoLancamento.CREDITO -> "Crédito"
                        TipoLancamento.RECEITA -> "Receita"
                    })
                }
            )
        }
    }
}

@Composable
fun DateField(label: String, millis: Long, context: Context, onDateSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    OutlinedTextField(
        value = DateUtils.format(millis),
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val selected = Calendar.getInstance().apply {
                            set(year, month, day, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        onDateSelected(selected)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.CalendarMonth, "Selecionar data")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaDropdown(
    categorias: List<Categoria>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categorias.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.nome ?: "Categoria",
            onValueChange = {},
            label = { Text("Categoria") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            categorias.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.nome) }, onClick = { onSelect(cat.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContaDropdown(
    contas: List<Conta>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = contas.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.nome ?: "Conta",
            onValueChange = {},
            label = { Text("Conta") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            contas.forEach { conta ->
                DropdownMenuItem(text = { Text(conta.nome) }, onClick = { onSelect(conta.id); expanded = false })
            }
        }
    }
}
