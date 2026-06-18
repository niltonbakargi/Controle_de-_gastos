package com.gastozen.domain.usecase

import com.gastozen.data.model.*
import com.gastozen.data.repository.*
import com.gastozen.util.DateUtils
import java.util.UUID

class ObterDashboardUseCase(
    private val lancamentoRepo: LancamentoRepository
) {
    fun resumo(inicio: Long, fim: Long) =
        lancamentoRepo.getResumoMensal(inicio, fim, System.currentTimeMillis())

    fun gastosPorCategoria(inicio: Long, fim: Long) =
        lancamentoRepo.getGastosPorCategoria(inicio, fim)

    fun ultimos10() = lancamentoRepo.getUltimos10()
}

class CriarLancamentoUseCase(
    private val lancamentoRepo: LancamentoRepository
) {
    suspend fun executar(lancamento: Lancamento): Long {
        return if (lancamento.totalParcelas > 1 && lancamento.tipo == TipoLancamento.CREDITO) {
            val grupoId = UUID.randomUUID().toString()
            val parcelas = (1..lancamento.totalParcelas).map { i ->
                lancamento.copy(
                    id = 0,
                    parcelaAtual = i,
                    grupoParcela = grupoId,
                    descricao = "${lancamento.descricao} ($i/${lancamento.totalParcelas})"
                )
            }
            lancamentoRepo.insertAll(parcelas)
            -1L
        } else {
            lancamentoRepo.insert(lancamento)
        }
    }
}

class ExcluirLancamentoUseCase(
    private val lancamentoRepo: LancamentoRepository
) {
    suspend fun executar(id: Long) = lancamentoRepo.deleteById(id)
}

class ObterMetasUseCase(
    private val categoriaRepo: CategoriaRepository,
    private val lancamentoRepo: LancamentoRepository
) {
    fun categorias() = categoriaRepo.getAll()

    fun gastoCategoria(categoriaId: Long, inicio: Long, fim: Long) =
        lancamentoRepo.getGastoCategoriaMes(categoriaId, inicio, fim)
}

class SalvarRegraCategoriaUseCase(
    private val regraRepo: RegraCategoriaRepository
) {
    suspend fun executar(palavraChave: String?, ncm: String?, categoriaId: Long) {
        regraRepo.insert(
            RegraCategoria(
                palavraChave = palavraChave,
                ncm = ncm?.takeIf { it.isNotBlank() },
                categoriaId = categoriaId
            )
        )
    }

    suspend fun findCategoria(produto: String, ncm: String, regraRepo: RegraCategoriaRepository): Long? {
        regraRepo.findByNcm(ncm)?.let { return it.categoriaId }
        produto.split(" ").forEach { palavra ->
            if (palavra.length >= 3) {
                regraRepo.findByKeyword(palavra)?.let { return it.categoriaId }
            }
        }
        return null
    }
}
