package com.gastozen.data.repository

import com.gastozen.data.db.*
import com.gastozen.data.model.*
import kotlinx.coroutines.flow.Flow

class ContaRepository(private val dao: ContaDao) {
    fun getAll(): Flow<List<Conta>> = dao.getAll()
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(conta: Conta) = dao.insert(conta)
    suspend fun update(conta: Conta) = dao.update(conta)
    suspend fun delete(conta: Conta) = dao.delete(conta)
}

class CategoriaRepository(private val dao: CategoriaDao) {
    fun getAll(): Flow<List<Categoria>> = dao.getAll()
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(categoria: Categoria) = dao.insert(categoria)
    suspend fun update(categoria: Categoria) = dao.update(categoria)
    suspend fun delete(categoria: Categoria) = dao.delete(categoria)
    suspend fun countLancamentos(id: Long) = dao.countLancamentos(id)
}

class LancamentoRepository(private val dao: LancamentoDao) {
    fun getLancamentosComDetalhes(inicio: Long, fim: Long) =
        dao.getLancamentosComDetalhes(inicio, fim)

    fun buscar(query: String) = dao.buscar(query)
    fun getUltimos10() = dao.getUltimos10()
    fun getGastosPorCategoria(inicio: Long, fim: Long) = dao.getGastosPorCategoria(inicio, fim)
    fun getResumoMensal(inicio: Long, fim: Long, agora: Long) =
        dao.getResumoMensal(inicio, fim, agora)
    fun getGastoCategoriaMes(categoriaId: Long, inicio: Long, fim: Long) =
        dao.getGastoCategoriaMes(categoriaId, inicio, fim)

    suspend fun insert(lancamento: Lancamento) = dao.insert(lancamento)
    suspend fun insertAll(lancamentos: List<Lancamento>) = dao.insertAll(lancamentos)
    suspend fun update(lancamento: Lancamento) = dao.update(lancamento)
    suspend fun delete(lancamento: Lancamento) = dao.delete(lancamento)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun deletePorPeriodo(inicio: Long, fim: Long) = dao.deletePorPeriodo(inicio, fim)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun getComDetalhesById(id: Long) = dao.getComDetalhesById(id)
}

class RegraCategoriaRepository(private val dao: RegraCategoriaDao) {
    fun getAll(): Flow<List<RegraCategoria>> = dao.getAll()
    suspend fun findByKeyword(keyword: String) = dao.findByKeyword(keyword)
    suspend fun findByNcm(ncm: String) = dao.findByNcm(ncm)
    suspend fun insert(regra: RegraCategoria) = dao.insert(regra)
    suspend fun delete(regra: RegraCategoria) = dao.delete(regra)
}

class RecorrenteRepository(private val dao: RecorrenteDao) {
    fun getAll(): Flow<List<Recorrente>> = dao.getAll()
    suspend fun getAtivos() = dao.getAtivos()
    suspend fun insert(recorrente: Recorrente) = dao.insert(recorrente)
    suspend fun update(recorrente: Recorrente) = dao.update(recorrente)
    suspend fun delete(recorrente: Recorrente) = dao.delete(recorrente)
}
