package com.gastozen.ui.produtos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.ProdutoCompradoComDetalhes
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosCompradosScreen(
    viewModel: ProdutosCompradosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mes by viewModel.mes.collectAsStateWithLifecycle()
    val produtos by viewModel.produtos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos Comprados") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navegação de mês
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::mesAnterior) {
                    Icon(Icons.Default.ChevronLeft, "Mês anterior")
                }
                Text(
                    "${mes.month.getDisplayName(TextStyle.FULL, Locale("pt","BR")).replaceFirstChar { it.uppercase() }} ${mes.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = viewModel::proximoMes,
                    enabled = mes.isBefore(LocalDate.now().withDayOfMonth(1))
                ) {
                    Icon(Icons.Default.ChevronRight, "Próximo mês")
                }
            }

            HorizontalDivider()

            if (produtos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nenhum produto registrado neste mês.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Resumo
                val totalGasto = produtos.sumOf { it.valorTotal }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${produtos.size} itens", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Total: R$ ${"%.2f".format(totalGasto)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(produtos) { produto ->
                        ProdutoCompradoItem(produto)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProdutoCompradoItem(produto: ProdutoCompradoComDetalhes) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bolinha de cor da categoria
            val cor = try {
                Color(AndroidColor.parseColor(produto.categoriaCorHex ?: "#9E9E9E"))
            } catch (_: Exception) {
                Color(0xFF9E9E9E.toInt())
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(cor, CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(produto.nome, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                if (!produto.categoriaNome.isNullOrBlank()) {
                    Text(
                        produto.categoriaNome,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Sem categoria",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "R$ ${"%.2f".format(produto.valorTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (produto.quantidade > 1.0 || produto.valorUnitario > 0.0) {
                    Text(
                        "${produto.quantidade.toBigDecimal().stripTrailingZeros().toPlainString()} × R$ ${"%.2f".format(produto.valorUnitario)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
