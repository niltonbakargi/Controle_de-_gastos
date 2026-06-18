package com.gastozen.ui.metas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gastozen.data.model.Categoria
import com.gastozen.data.repository.CategoriaRepository
import com.gastozen.data.repository.LancamentoRepository
import com.gastozen.util.CurrencyUtils
import com.gastozen.util.DateUtils
import com.gastozen.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MetaItem(
    val categoria: Categoria,
    val gasto: Double
) {
    val percentual: Float get() = if ((categoria.metaMensal ?: 0.0) > 0)
        (gasto / categoria.metaMensal!!).toFloat()
    else 0f
}

class MetasViewModel(
    private val categoriaRepo: CategoriaRepository,
    private val lancamentoRepo: LancamentoRepository
) : ViewModel() {

    private val filtro = DateUtils.run {
        Pair(startOfCurrentMonth(), endOfCurrentMonth())
    }

    val metas: StateFlow<List<MetaItem>> = categoriaRepo.getAll()
        .flatMapLatest { categorias ->
            val comMeta = categorias.filter { it.metaMensal != null }
            if (comMeta.isEmpty()) flowOf(emptyList())
            else combine(
                comMeta.map { cat ->
                    lancamentoRepo.getGastoCategoriaMes(cat.id, filtro.first, filtro.second)
                        .map { gasto -> MetaItem(cat, gasto) }
                }
            ) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvarMeta(categoria: Categoria, meta: Double?) {
        viewModelScope.launch {
            categoriaRepo.update(categoria.copy(metaMensal = meta))
        }
    }

    fun verificarAlertas(context: android.content.Context, metas: List<MetaItem>) {
        metas.forEach { item ->
            val pct = (item.percentual * 100).toInt()
            if (pct >= 80) {
                NotificationHelper.notifyMetaAlerta(context, item.categoria.nome, pct)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(
    viewModel: MetasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metas by viewModel.metas.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(metas) {
        viewModel.verificarAlertas(context, metas)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metas Mensais") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (metas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Defina metas nas categorias para monitorar seus gastos.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(metas, key = { it.categoria.id }) { item ->
                    MetaCard(item)
                }
            }
        }
    }
}

@Composable
private fun MetaCard(item: MetaItem) {
    val animPct by animateFloatAsState(
        targetValue = item.percentual.coerceIn(0f, 1f),
        label = "meta_progress"
    )
    val ultrapassou = item.percentual >= 1f
    val quase = item.percentual >= 0.8f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                ultrapassou -> MaterialTheme.colorScheme.errorContainer
                quase -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.categoria.nome, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${CurrencyUtils.format(item.gasto)} / ${CurrencyUtils.format(item.categoria.metaMensal ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animPct },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    ultrapassou -> MaterialTheme.colorScheme.error
                    quase -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            if (ultrapassou) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Meta ultrapassada!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
