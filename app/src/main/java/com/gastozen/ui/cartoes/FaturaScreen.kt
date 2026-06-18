package com.gastozen.ui.cartoes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.util.DateUtils
import com.gastozen.util.FaturaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaScreen(
    viewModel: FaturaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartao by viewModel.cartao.collectAsStateWithLifecycle()
    val mes by viewModel.mes.collectAsStateWithLifecycle()
    val lancamentos by viewModel.lancamentos.collectAsStateWithLifecycle()
    val totalFatura by viewModel.totalFatura.collectAsStateWithLifecycle()
    val pagamento by viewModel.pagamento.collectAsStateWithLifecycle()
    val dialogPagar by viewModel.dialogPagar.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(erro) {
        erro?.let { snackbarHostState.showSnackbar(it); viewModel.limparErro() }
    }

    val corCartao = cartao?.let {
        try { Color(android.graphics.Color.parseColor(it.corHex)) }
        catch (_: Exception) { null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cartao?.nome ?: "Fatura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = corCartao ?: MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    if (pagamento?.pago == true) {
                        IconButton(onClick = viewModel::despagarFatura) {
                            Icon(Icons.Default.Undo, "Desfazer pagamento")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navegação de mês
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::mesAnterior) {
                        Icon(Icons.Default.ChevronLeft, "Mês anterior")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        mes?.let { (ano, m) ->
                            Text(FaturaUtils.nomeMes(m, ano),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            cartao?.let { c ->
                                Text(
                                    FaturaUtils.periodoTexto(ano, m, c.diaFechamento),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = viewModel::proximoMes) {
                        Icon(Icons.Default.ChevronRight, "Próximo mês")
                    }
                }
            }

            // Card de resumo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pagamento?.pago == true)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Total da fatura",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("R$ ${"%.2f".format(totalFatura)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold)
                            }
                            if (pagamento?.pago == true) {
                                Icon(Icons.Default.CheckCircle, "Pago",
                                    tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp))
                            } else {
                                Button(
                                    onClick = viewModel::abrirDialogPagar,
                                    enabled = totalFatura > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = corCartao ?: MaterialTheme.colorScheme.primary
                                    )
                                ) { Text("Pagar fatura") }
                            }
                        }
                        cartao?.let { c ->
                            Text("Vence dia ${c.diaVencimento}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        pagamento?.let { p ->
                            if (p.pago && p.dataPagamento != null) {
                                Text("Pago em ${DateUtils.format(p.dataPagamento)} — R$ ${"%.2f".format(p.valorPago ?: 0.0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }

            if (lancamentos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma compra nesta fatura",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    Text("Compras (${lancamentos.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(lancamentos, key = { it.id }) { lancamento ->
                    LancamentoFaturaItem(lancamento)
                }
            }
        }
    }

    if (dialogPagar) {
        PagarFaturaDialog(
            totalSugerido = totalFatura,
            onConfirmar = viewModel::pagarFatura,
            onDismiss = viewModel::fecharDialogPagar
        )
    }
}

@Composable
private fun LancamentoFaturaItem(lancamento: LancamentoComDetalhes) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lancamento.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        append(DateUtils.format(lancamento.data))
                        lancamento.categoriaNome?.let { append(" · $it") }
                        if (lancamento.totalParcelas > 1) append(" · ${lancamento.parcelaAtual}/${lancamento.totalParcelas}x")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "R$ ${"%.2f".format(lancamento.valor)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PagarFaturaDialog(
    totalSugerido: Double,
    onConfirmar: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var valor by remember {
        mutableStateOf("%.2f".format(totalSugerido).replace(".", ","))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar fatura") },
        text = {
            OutlinedTextField(
                value = valor, onValueChange = { valor = it },
                label = { Text("Valor pago (R$)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                if (v > 0) onConfirmar(v)
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
