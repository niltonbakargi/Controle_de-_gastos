package com.gastozen.ui.cartoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastozen.data.model.CartaoCredito
import com.gastozen.util.FaturaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartoesScreen(
    viewModel: CartoesViewModel,
    onCartao: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartoes by viewModel.cartoes.collectAsStateWithLifecycle()
    val dialogEditar by viewModel.dialogEditar.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(erro) {
        erro?.let { snackbarHostState.showSnackbar(it); viewModel.limparErro() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cartões de Crédito") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirAddCartao) {
                Icon(Icons.Default.Add, "Adicionar cartão")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        if (cartoes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CreditCard, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhum cartão cadastrado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Toque em + para adicionar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartoes, key = { it.cartao.id }) { item ->
                    CartaoCard(
                        item = item,
                        onTap = { onCartao(item.cartao.id) },
                        onEditar = { viewModel.abrirEditarCartao(item.cartao) },
                        onDeletar = { viewModel.deletarCartao(item.cartao) }
                    )
                }
            }
        }
    }

    dialogEditar?.let { cartao ->
        EditarCartaoDialog(
            cartao = cartao,
            onSalvar = viewModel::salvarCartao,
            onDismiss = viewModel::fecharDialogEditar
        )
    }
}

@Composable
private fun CartaoCard(
    item: CartaoComFatura,
    onTap: () -> Unit,
    onEditar: () -> Unit,
    onDeletar: () -> Unit
) {
    val cor = try { Color(android.graphics.Color.parseColor(item.cartao.corHex)) }
              catch (_: Exception) { MaterialTheme.colorScheme.primary }
    var menuAberto by remember { mutableStateOf(false) }

    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cor)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Text(item.cartao.nome, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Box {
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(Icons.Default.MoreVert, "Opções", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { onEditar(); menuAberto = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error) },
                            onClick = { onDeletar(); menuAberto = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Fatura em aberto — ${FaturaUtils.nomeMes(item.faturaMes, item.faturaAno)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                "R$ ${"%.2f".format(item.totalFaturaAtual)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fecha dia ${item.cartao.diaFechamento}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f))
                Text("Vence dia ${item.cartao.diaVencimento}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditarCartaoDialog(
    cartao: CartaoCredito,
    onSalvar: (CartaoCredito) -> Unit,
    onDismiss: () -> Unit
) {
    val coresDisponiveis = listOf(
        "#8A05BE" to "Nubank",
        "#222222" to "C6 Bank",
        "#009EE3" to "Mercado Pago",
        "#FFB700" to "Inter",
        "#EC7000" to "Itaú",
        "#CC0000" to "Bradesco",
        "#00A650" to "Caixa",
        "#003087" to "PayPal",
        "#6200EE" to "Outro"
    )

    var nome by remember { mutableStateOf(cartao.nome) }
    var diaFechamento by remember { mutableStateOf(cartao.diaFechamento.toString()) }
    var diaVencimento by remember { mutableStateOf(cartao.diaVencimento.toString()) }
    var limite by remember { mutableStateOf(cartao.limite?.let { "%.2f".format(it).replace(".",",") } ?: "") }
    var corHex by remember { mutableStateOf(cartao.corHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cartao.id == 0L) "Novo cartão" else "Editar cartão") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome do cartão") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CreditCard, null) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diaFechamento,
                        onValueChange = { v -> if (v.length <= 2) diaFechamento = v.filter { it.isDigit() } },
                        label = { Text("Fecha dia") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = diaVencimento,
                        onValueChange = { v -> if (v.length <= 2) diaVencimento = v.filter { it.isDigit() } },
                        label = { Text("Vence dia") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                OutlinedTextField(
                    value = limite, onValueChange = { limite = it },
                    label = { Text("Limite (R$, opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) }
                )
                Text("Cor do cartão", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    coresDisponiveis.forEach { (hex, label) ->
                        val cor = try { Color(android.graphics.Color.parseColor(hex)) }
                                  catch (_: Exception) { Color.Gray }
                        FilterChip(
                            selected = corHex == hex,
                            onClick = { corHex = hex },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(cor))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val f = diaFechamento.toIntOrNull()?.takeIf { it in 1..31 } ?: return@TextButton
                val v = diaVencimento.toIntOrNull()?.takeIf { it in 1..31 } ?: return@TextButton
                if (nome.isBlank()) return@TextButton
                val lim = limite.replace(",", ".").toDoubleOrNull()
                onSalvar(cartao.copy(nome = nome.trim(), diaFechamento = f, diaVencimento = v, limite = lim, corHex = corHex))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
