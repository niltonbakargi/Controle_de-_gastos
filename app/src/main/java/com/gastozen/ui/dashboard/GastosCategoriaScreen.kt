package com.gastozen.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.data.model.ProdutoCompradoComDetalhes
import com.gastozen.data.model.TipoLancamento
import com.gastozen.util.CurrencyUtils
import com.gastozen.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosCategoriaScreen(
    viewModel: GastosCategoriaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val corCategoria = try {
        Color(AndroidColor.parseColor(state.categoriaCorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.categoriaNome) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = corCategoria.copy(alpha = 0.15f)
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header resumo ─────────────────────────────────────────────────
            item {
                ResumoHeader(state, corCategoria)
            }

            // ── Lançamentos ───────────────────────────────────────────────────
            if (state.lancamentos.isNotEmpty()) {
                item {
                    SectionTitle(Icons.Default.Receipt, "Lançamentos", state.lancamentos.size)
                }
                items(state.lancamentos, key = { "lanc_${it.id}" }) { lanc ->
                    LancamentoItemCard(lanc, corCategoria)
                }
            }

            // ── Produtos comprados ────────────────────────────────────────────
            if (state.produtos.isNotEmpty()) {
                item {
                    SectionTitle(Icons.Default.ShoppingCart, "Produtos (NF-e)", state.produtos.size)
                }
                items(state.produtos, key = { "prod_${it.id}" }) { prod ->
                    ProdutoItemCard(prod, corCategoria)
                }
            }

            if (state.lancamentos.isEmpty() && state.produtos.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nenhum gasto nesta categoria no período.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumoHeader(state: GastosCategoriaState, cor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bolinha colorida grande
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.categoriaNome.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                CurrencyUtils.format(state.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("total no período", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (state.totalLancamentos > 0 && state.totalProdutos > 0) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(CurrencyUtils.format(state.totalLancamentos),
                            fontWeight = FontWeight.SemiBold)
                        Text("lançamentos", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(CurrencyUtils.format(state.totalProdutos),
                            fontWeight = FontWeight.SemiBold)
                        Text("produtos NF-e", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold)
        Text("($count)", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LancamentoItemCard(lanc: LancamentoComDetalhes, cor: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
            Column(Modifier.weight(1f)) {
                Text(lanc.descricao, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        append(DateUtils.format(lanc.data))
                        if (lanc.tipoPagamento.name != "DINHEIRO") {
                            append(" • ")
                            append(lanc.tipoPagamento.label())
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                buildString {
                    if (lanc.tipo == TipoLancamento.RECEITA) append("+") else append("-")
                    append(CurrencyUtils.format(lanc.valor))
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (lanc.tipo == TipoLancamento.RECEITA)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProdutoItemCard(prod: ProdutoCompradoComDetalhes, cor: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
            Column(Modifier.weight(1f)) {
                Text(prod.nome, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 2)
                if (prod.quantidade > 1.0 || prod.valorUnitario > 0.0) {
                    Text(
                        "${prod.quantidade.toBigDecimal().stripTrailingZeros().toPlainString()} × R$ ${"%.2f".format(prod.valorUnitario)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "- ${CurrencyUtils.format(prod.valorTotal)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun com.gastozen.data.model.TipoPagamento.label() = when (this) {
    com.gastozen.data.model.TipoPagamento.DINHEIRO       -> "Dinheiro"
    com.gastozen.data.model.TipoPagamento.CARTAO_DEBITO  -> "Débito"
    com.gastozen.data.model.TipoPagamento.CARTAO_CREDITO -> "Crédito"
    com.gastozen.data.model.TipoPagamento.PIX            -> "PIX"
    com.gastozen.data.model.TipoPagamento.OUTROS         -> "Outros"
}
