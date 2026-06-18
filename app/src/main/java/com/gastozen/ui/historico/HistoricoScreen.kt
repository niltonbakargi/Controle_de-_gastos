package com.gastozen.ui.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.LancamentoComDetalhes
import com.gastozen.data.model.TipoLancamento
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.util.CurrencyUtils
import com.gastozen.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class MesHistorico(val year: Int, val month: Int) {
    val label: String get() = DateUtils.formatMonth(DateUtils.startOfMonth(year, month))
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricoViewModel(private val lancamentoRepo: LancamentoRepository) : ViewModel() {

    // Last 6 months
    val meses: List<MesHistorico> = (0..5).map { offset ->
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
        MesHistorico(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    private val _mesSelecionado = MutableStateFlow(meses.first())

    val lancamentos: StateFlow<List<LancamentoComDetalhes>> = _mesSelecionado.flatMapLatest { mes ->
        lancamentoRepo.getLancamentosComDetalhes(
            DateUtils.startOfMonth(mes.year, mes.month),
            DateUtils.endOfMonth(mes.year, mes.month)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mesSelecionado: StateFlow<MesHistorico> = _mesSelecionado.asStateFlow()

    fun selecionarMes(mes: MesHistorico) { _mesSelecionado.value = mes }

    fun buscar(query: String): Flow<List<LancamentoComDetalhes>> = lancamentoRepo.buscar(query)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    viewModel: HistoricoViewModel,
    onBack: () -> Unit,
    onVerLancamento: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lancamentos by viewModel.lancamentos.collectAsStateWithLifecycle()
    val mesSelecionado by viewModel.mesSelecionado.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.buscar(searchQuery)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val displayList = if (searchQuery.isBlank()) lancamentos else searchResults

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
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
            // Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar lançamentos") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Filtro de meses
            if (searchQuery.isBlank()) {
                ScrollableTabRow(
                    selectedTabIndex = viewModel.meses.indexOf(mesSelecionado)
                ) {
                    viewModel.meses.forEachIndexed { index, mes ->
                        Tab(
                            selected = mes == mesSelecionado,
                            onClick = { viewModel.selecionarMes(mes) },
                            text = { Text(mes.label, maxLines = 1) }
                        )
                    }
                }
            }

            // Lista
            val totalReceitas = displayList.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valor }
            val totalDespesas = displayList.filter { it.tipo != TipoLancamento.RECEITA }.sumOf { it.valor }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column {
                        Text("Receitas", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyUtils.format(totalReceitas), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Despesas", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyUtils.format(totalDespesas), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Column {
                        Text("Saldo", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyUtils.format(totalReceitas - totalDespesas), fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayList, key = { it.id }) { lancamento ->
                    Card(
                        onClick = { onVerLancamento(lancamento.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(lancamento.descricao, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${lancamento.categoriaNome ?: "Sem categoria"} • ${DateUtils.format(lancamento.data)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                buildString {
                                    if (lancamento.tipo == TipoLancamento.RECEITA) append("+") else append("-")
                                    append(CurrencyUtils.format(lancamento.valor))
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (lancamento.tipo == TipoLancamento.RECEITA)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
