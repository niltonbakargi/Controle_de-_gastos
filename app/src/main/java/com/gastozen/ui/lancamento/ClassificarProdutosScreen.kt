package com.gastozen.ui.lancamento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Tela de classificação dos produtos sem categoria vindos de uma NF-e.
 * O usuário atribui uma categoria a cada produto; a regra fica salva para
 * que próximas notas do mesmo produto sejam classificadas automaticamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificarProdutosScreen(
    viewModel: ClassificarProdutosViewModel,
    onConcluido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val produtos   by viewModel.produtos.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val mapeamento by viewModel.mapeamento.collectAsStateWithLifecycle()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ClassificarUiState.Concluido) onConcluido()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classificar Produtos") },
                navigationIcon = {
                    IconButton(onClick = onConcluido) {
                        Icon(Icons.Default.ArrowBack, "Pular")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::salvar,
                        enabled = uiState !is ClassificarUiState.Salvando && mapeamento.isNotEmpty()
                    ) {
                        Text("Salvar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (produtos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text(
                    "${produtos.size} produto(s) sem categoria. Atribua uma categoria para classificar automaticamente nas próximas compras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(produtos) { produto ->
                        ProdutoClassificacaoItem(
                            nomeProduto = produto.nome,
                            valorTotal = produto.valorTotal,
                            categorias = categorias,
                            categoriaIdSelecionada = mapeamento[produto.id],
                            onCategoriaSelect = { catId ->
                                viewModel.atribuirCategoria(produto.id, catId)
                            }
                        )
                    }
                }

                if (uiState is ClassificarUiState.Salvando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProdutoClassificacaoItem(
    nomeProduto: String,
    valorTotal: Double,
    categorias: List<com.gastozen.data.model.Categoria>,
    categoriaIdSelecionada: Long?,
    onCategoriaSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categoriaSelecionada = categorias.find { it.id == categoriaIdSelecionada }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(nomeProduto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("R$ ${"%.2f".format(valorTotal)}", style = MaterialTheme.typography.bodyMedium)
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = categoriaSelecionada?.nome ?: "Selecionar categoria",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.nome) },
                            onClick = { onCategoriaSelect(cat.id); expanded = false }
                        )
                    }
                }
            }
        }
    }
}
