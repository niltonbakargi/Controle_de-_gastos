package com.gastozen.ui.despesasfixas

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.Categoria
import com.gastozen.data.model.DespesaFixa
import com.gastozen.util.DateUtils
import java.util.Calendar

private val MESES = listOf(
    "Janeiro","Fevereiro","Março","Abril","Maio","Junho",
    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesasFixasScreen(
    viewModel: DespesasFixasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mes by viewModel.mes.collectAsStateWithLifecycle()
    val despesas by viewModel.despesas.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val dialogEditar by viewModel.dialogEditar.collectAsStateWithLifecycle()
    val dialogMarcarPago by viewModel.dialogMarcarPago.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(erro) {
        erro?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparErro()
        }
    }

    val hoje = remember { Calendar.getInstance() }
    val mesAtual = hoje.get(Calendar.MONTH) + 1
    val anoAtual = hoje.get(Calendar.YEAR)
    val diaAtual = hoje.get(Calendar.DAY_OF_MONTH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Despesas Fixas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirAddDespesa) {
                Icon(Icons.Default.Add, "Adicionar despesa fixa")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Navegação de mês ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::mesAnterior) {
                    Icon(Icons.Default.ChevronLeft, "Mês anterior")
                }
                Text(
                    "${MESES[mes.mes - 1]} ${mes.ano}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = viewModel::proximoMes,
                    enabled = !(mes.mes == mesAtual && mes.ano == anoAtual)
                ) {
                    Icon(Icons.Default.ChevronRight, "Próximo mês")
                }
            }

            // ── Resumo ────────────────────────────────────────────────────
            val pagas = despesas.count { it.pago }
            val totalGeral = despesas.sumOf { it.despesa.valor }
            val totalPago = despesas.filter { it.pago }.sumOf { it.pagamento?.valorPago ?: it.despesa.valor }

            if (despesas.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pagas == despesas.size)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$pagas de ${despesas.size} pagas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "R$ ${"%.2f".format(totalPago)} / R$ ${"%.2f".format(totalGeral)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (totalGeral > 0) (totalPago / totalGeral).toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Lista ─────────────────────────────────────────────────────
            if (despesas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nenhuma despesa fixa cadastrada",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Toque em + para adicionar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(despesas, key = { it.despesa.id }) { status ->
                        val vencido = !status.pago
                            && mes.mes == mesAtual && mes.ano == anoAtual
                            && diaAtual > status.despesa.diaVencimento

                        DespesaFixaCard(
                            status = status,
                            vencido = vencido,
                            onPagar = { viewModel.abrirMarcarPago(status) },
                            onDesmarcar = { viewModel.desmarcarPago(status) },
                            onEditar = { viewModel.abrirEditarDespesa(status.despesa) },
                            onDeletar = { viewModel.deletarDespesa(status.despesa) }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo: Adicionar / Editar ──────────────────────────────────────
    dialogEditar?.let { despesa ->
        EditarDespesaFixaDialog(
            despesa = despesa,
            categorias = categorias,
            onSalvar = viewModel::salvarDespesa,
            onDismiss = viewModel::fecharDialogEditar
        )
    }

    // ── Diálogo: Marcar como pago ────────────────────────────────────────
    dialogMarcarPago?.let { status ->
        MarcarPagoDialog(
            status = status,
            onConfirmar = { valor, data -> viewModel.marcarPago(status.despesa, valor, data) },
            onDismiss = viewModel::fecharDialogMarcarPago
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card de cada despesa
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DespesaFixaCard(
    status: DespesaFixaComStatus,
    vencido: Boolean,
    onPagar: () -> Unit,
    onDesmarcar: () -> Unit,
    onEditar: () -> Unit,
    onDeletar: () -> Unit
) {
    val corBorda = when {
        status.pago -> Color(0xFF4CAF50)
        vencido     -> MaterialTheme.colorScheme.error
        else        -> MaterialTheme.colorScheme.outline
    }

    var menuAberto by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra lateral colorida
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(corBorda)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        status.despesa.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "R$ ${"%.2f".format(status.despesa.valor)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    when {
                        status.pago -> {
                            val data = status.pagamento?.dataPagamento
                            val texto = if (data != null) "Pago em ${DateUtils.format(data)}" else "Pago"
                            Text(
                                texto,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        vencido -> Text(
                            "Venceu dia ${status.despesa.diaVencimento}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> Text(
                            "Vence dia ${status.despesa.diaVencimento}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!status.pago) {
                        IconButton(onClick = onPagar) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Marcar como pago",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Default.MoreVert, "Opções")
                        }
                        DropdownMenu(
                            expanded = menuAberto,
                            onDismissRequest = { menuAberto = false }
                        ) {
                            if (status.pago) {
                                DropdownMenuItem(
                                    text = { Text("Desmarcar pago") },
                                    leadingIcon = { Icon(Icons.Default.Undo, null) },
                                    onClick = { onDesmarcar(); menuAberto = false }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { onEditar(); menuAberto = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { onDeletar(); menuAberto = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diálogo: Adicionar / Editar despesa fixa
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarDespesaFixaDialog(
    despesa: DespesaFixa,
    categorias: List<Categoria>,
    onSalvar: (DespesaFixa) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf(despesa.nome) }
    var valor by remember { mutableStateOf(if (despesa.valor > 0) "%.2f".format(despesa.valor).replace(".", ",") else "") }
    var dia by remember { mutableStateOf(if (despesa.diaVencimento > 0) despesa.diaVencimento.toString() else "") }
    var categoriaId by remember { mutableStateOf(despesa.categoriaId) }
    var ativa by remember { mutableStateOf(despesa.ativa) }
    var categoriaExpandida by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (despesa.id == 0L) "Nova despesa fixa" else "Editar despesa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Label, null) }
                )
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
                )
                OutlinedTextField(
                    value = dia,
                    onValueChange = { v ->
                        val n = v.filter { it.isDigit() }
                        if (n.isEmpty() || (n.toIntOrNull() ?: 0) <= 31) dia = n
                    },
                    label = { Text("Dia de vencimento") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                )

                // Categoria dropdown
                ExposedDropdownMenuBox(
                    expanded = categoriaExpandida,
                    onExpandedChange = { categoriaExpandida = it }
                ) {
                    val catNome = categorias.find { it.id == categoriaId }?.nome ?: "Nenhuma"
                    OutlinedTextField(
                        value = catNome,
                        onValueChange = {},
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoriaExpandida) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoriaExpandida,
                        onDismissRequest = { categoriaExpandida = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhuma") },
                            onClick = { categoriaId = null; categoriaExpandida = false }
                        )
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nome) },
                                onClick = { categoriaId = cat.id; categoriaExpandida = false }
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ativa", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = ativa, onCheckedChange = { ativa = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                val d = dia.toIntOrNull()?.takeIf { it in 1..31 } ?: return@TextButton
                if (nome.isBlank()) return@TextButton
                onSalvar(despesa.copy(nome = nome.trim(), valor = v, diaVencimento = d, categoriaId = categoriaId, ativa = ativa))
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Diálogo: Marcar como pago
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MarcarPagoDialog(
    status: DespesaFixaComStatus,
    onConfirmar: (Double, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val despesa = status.despesa
    var valor by remember {
        mutableStateOf("%.2f".format(despesa.valor).replace(".", ","))
    }
    var dataPagamento by remember { mutableStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar: ${despesa.nome}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor pago (R$)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
                )
                // Data de pagamento
                val cal = Calendar.getInstance().apply { timeInMillis = dataPagamento }
                OutlinedTextField(
                    value = DateUtils.format(dataPagamento),
                    onValueChange = {},
                    label = { Text("Data de pagamento") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    dataPagamento = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) { Icon(Icons.Default.CalendarMonth, null) }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                if (v > 0) onConfirmar(v, dataPagamento)
            }) { Text("Confirmar pagamento") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
