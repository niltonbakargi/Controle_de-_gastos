package com.gastozen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gastozen.data.db.AppDatabase
import com.gastozen.data.repository.*
import com.gastozen.domain.usecase.CriarLancamentoUseCase
import com.gastozen.domain.usecase.SalvarRegraCategoriaUseCase
import com.gastozen.ui.categorias.CategoriasViewModel
import com.gastozen.ui.configuracoes.ConfiguracoesViewModel
import com.gastozen.ui.configuracoes.RecorrentesViewModel
import com.gastozen.ui.dashboard.DashboardViewModel
import com.gastozen.ui.historico.HistoricoViewModel
import com.gastozen.ui.lancamento.LancamentoViewModel
import com.gastozen.ui.lancamento.QrCodeViewModel
import com.gastozen.ui.metas.MetasViewModel

class AppViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {

    private val contaRepo by lazy { ContaRepository(db.contaDao()) }
    private val categoriaRepo by lazy { CategoriaRepository(db.categoriaDao()) }
    private val lancamentoRepo by lazy { LancamentoRepository(db.lancamentoDao()) }
    private val regraRepo by lazy { RegraCategoriaRepository(db.regraCategoriaDao()) }
    private val recorrenteRepo by lazy { RecorrenteRepository(db.recorrenteDao()) }
    private val criarUseCase by lazy { CriarLancamentoUseCase(lancamentoRepo) }
    private val regraUseCase by lazy { SalvarRegraCategoriaUseCase(regraRepo) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(lancamentoRepo) as T
            modelClass.isAssignableFrom(LancamentoViewModel::class.java) ->
                LancamentoViewModel(criarUseCase, contaRepo, categoriaRepo, recorrenteRepo) as T
            modelClass.isAssignableFrom(QrCodeViewModel::class.java) ->
                QrCodeViewModel(categoriaRepo, regraRepo, criarUseCase, regraUseCase) as T
            modelClass.isAssignableFrom(CategoriasViewModel::class.java) ->
                CategoriasViewModel(categoriaRepo) as T
            modelClass.isAssignableFrom(MetasViewModel::class.java) ->
                MetasViewModel(categoriaRepo, lancamentoRepo) as T
            modelClass.isAssignableFrom(HistoricoViewModel::class.java) ->
                HistoricoViewModel(lancamentoRepo) as T
            modelClass.isAssignableFrom(ConfiguracoesViewModel::class.java) ->
                ConfiguracoesViewModel(lancamentoRepo) as T
            modelClass.isAssignableFrom(RecorrentesViewModel::class.java) ->
                RecorrentesViewModel(recorrenteRepo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
