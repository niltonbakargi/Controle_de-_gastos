package com.gastozen.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.data.model.TipoLancamento
import com.gastozen.util.CurrencyUtils
import com.gastozen.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNovoLancamento: () -> Unit,
    onVerLancamento: (Long) -> Unit,
    onQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GastoZen") },
                actions = {
                    IconButton(onClick = onQrCode) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Ler QR Code")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNovoLancamento) {
                Icon(Icons.Default.Add, contentDescription = "Novo lançamento")
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
            // Filtro de mês
            MesSelectorRow(
                filtro = filtro,
                onAnterior = viewModel::mesAnterior,
                onSeguinte = viewModel::mesSeguinte
            )

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is DashboardUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cards de resumo
                        item {
                            ResumoCards(state)
                        }

                        // Gráfico por categoria
                        if (state.gastosPorCategoria.isNotEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            "Gastos por Categoria",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        state.gastosPorCategoria.forEach { gasto ->
                                            val pct = if (state.resumo.totalDespesas > 0)
                                                (gasto.total / state.resumo.totalDespesas).toFloat()
                                            else 0f
                                            CategoriaProgressRow(gasto.categoriaNome, gasto.total, pct, gasto.categoriaCorHex)
                                        }
                                    }
                                }
                            }
                        }

                        // Últimos lançamentos
                        item {
                            Text(
                                "Últimos lançamentos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(state.ultimos10, key = { it.id }) { lancamento ->
                            LancamentoCard(
                                lancamento = lancamento,
                                onDelete = { viewModel.deleteLancamento(lancamento.id) },
                                onClick = { onVerLancamento(lancamento.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MesSelectorRow(
    filtro: FiltroMes,
    onAnterior: () -> Unit,
    onSeguinte: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
        }
        Text(
            text = DateUtils.formatMonth(DateUtils.startOfMonth(filtro.year, filtro.month)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onSeguinte) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mês seguinte")
        }
    }
}

@Composable
private fun ResumoCards(state: DashboardUiState.Success) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Saldo principal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.resumo.saldo >= 0)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Saldo do Mês", style = MaterialTheme.typography.bodyMedium)
                Text(
                    CurrencyUtils.format(state.resumo.saldo),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Receitas
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Receitas", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyUtils.format(state.resumo.totalReceitas),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // Despesas
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Despesas", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyUtils.format(state.resumo.totalDespesas),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (state.resumo.totalAVencer > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("A vencer: ${CurrencyUtils.format(state.resumo.totalAVencer)}")
                }
            }
        }
    }
}

@Composable
private fun CategoriaProgressRow(nome: String, valor: Double, pct: Float, corHex: String) {
    val animPct by animateFloatAsState(targetValue = pct, label = "progress")
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nome, style = MaterialTheme.typography.bodyMedium)
            Text(CurrencyUtils.format(valor), style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { animPct },
            modifier = Modifier.fillMaxWidth(),
            color = try { Color(android.graphics.Color.parseColor(corHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LancamentoCard(
    lancamento: LancamentoComDetalhes,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(lancamento.descricao, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            lancamento.categoriaNome?.let { append(it) }
                            append(" • ")
                            append(DateUtils.format(lancamento.data))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    buildString {
                        if (lancamento.tipo == TipoLancamento.RECEITA) append("+") else append("-")
                        append(CurrencyUtils.format(lancamento.valor))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (lancamento.tipo == TipoLancamento.RECEITA)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
