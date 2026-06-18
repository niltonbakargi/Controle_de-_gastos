package com.gastozen.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.GastoPorCategoria
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
    onProdutosComprados: () -> Unit = {},
    onCategoria: (categoriaId: Long, year: Int, month: Int) -> Unit = { _, _, _ -> },
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
                    IconButton(onClick = onProdutosComprados) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Produtos Comprados")
                    }
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
                        item { ResumoCards(state) }

                        // ── Dashboard de Categorias ────────────────────────────
                        if (state.gastosPorCategoria.isNotEmpty()) {
                            item {
                                // usa a soma das categorias como total (inclui produtos NF-e)
                                val totalCategorias = state.gastosPorCategoria.sumOf { it.total }
                                DashboardCategorias(
                                    gastos = state.gastosPorCategoria,
                                    totalDespesas = totalCategorias,
                                    onCategoria = { catId ->
                                        onCategoria(catId, filtro.year, filtro.month)
                                    }
                                )
                            }
                        }

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

// ── Dashboard de categorias ────────────────────────────────────────────────────

@Composable
private fun DashboardCategorias(
    gastos: List<GastoPorCategoria>,
    totalDespesas: Double,
    onCategoria: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gastos por Categoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${gastos.size} categorias",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Barra empilhada de cores (mini gráfico)
            if (totalDespesas > 0) {
                Spacer(Modifier.height(8.dp))
                BarraEmpilhada(gastos, totalDespesas)
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            gastos.forEach { gasto ->
                CategoriaRow(
                    gasto = gasto,
                    totalDespesas = totalDespesas,
                    onClick = { onCategoria(gasto.categoriaId) }
                )
            }
        }
    }
}

@Composable
private fun BarraEmpilhada(gastos: List<GastoPorCategoria>, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        gastos.forEach { gasto ->
            val pct = if (total > 0) (gasto.total / total).toFloat() else 0f
            val cor = try { Color(AndroidColor.parseColor(gasto.categoriaCorHex)) }
                      catch (_: Exception) { Color.Gray }
            Box(modifier = Modifier.weight(pct.coerceAtLeast(0.001f)).fillMaxHeight()
                .background(cor))
        }
    }
}

@Composable
private fun CategoriaRow(
    gasto: GastoPorCategoria,
    totalDespesas: Double,
    onClick: () -> Unit
) {
    val pct = if (totalDespesas > 0) (gasto.total / totalDespesas).toFloat() else 0f
    val animPct by animateFloatAsState(targetValue = pct, label = "cat_prog")
    val cor = try { Color(AndroidColor.parseColor(gasto.categoriaCorHex)) }
              catch (_: Exception) { MaterialTheme.colorScheme.primary }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bolinha colorida
        Box(Modifier.size(12.dp).clip(CircleShape).background(cor))

        // Nome e barra
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(gasto.categoriaNome, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { animPct },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = cor,
                trackColor = cor.copy(alpha = 0.15f)
            )
        }

        // Valor + seta
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                CurrencyUtils.format(gasto.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Componentes existentes ─────────────────────────────────────────────────────

@Composable
private fun MesSelectorRow(filtro: FiltroMes, onAnterior: () -> Unit, onSeguinte: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LancamentoCard(
    lancamento: LancamentoComDetalhes,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
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
                Icon(Icons.Default.Delete, contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(lancamento.descricao, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
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
