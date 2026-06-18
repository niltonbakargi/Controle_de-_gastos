package com.gastozen.data.db

import androidx.room.*
import com.gastozen.data.model.*
import kotlinx.coroutines.flow.Flow

// ── ContaDao ───────────────────────────────────────────────────────────────────
@Dao
interface ContaDao {
    @Query("SELECT * FROM contas ORDER BY nome ASC")
    fun getAll(): Flow<List<Conta>>

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun getById(id: Long): Conta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conta: Conta): Long

    @Update
    suspend fun update(conta: Conta)

    @Delete
    suspend fun delete(conta: Conta)
}

// ── CategoriaDao ───────────────────────────────────────────────────────────────
@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nome ASC")
    fun getAll(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun getById(id: Long): Categoria?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: Categoria): Long

    @Update
    suspend fun update(categoria: Categoria)

    @Delete
    suspend fun delete(categoria: Categoria)

    @Query("SELECT COUNT(*) FROM lancamentos WHERE categoriaId = :id")
    suspend fun countLancamentos(id: Long): Int
}

// ── LancamentoDao ──────────────────────────────────────────────────────────────
@Dao
interface LancamentoDao {

    @Query("""
        SELECT l.*, c.nome as contaNome, cat.nome as categoriaNome,
               cat.icone as categoriaIcone, cat.corHex as categoriaCorHex
        FROM lancamentos l
        LEFT JOIN contas c ON l.contaId = c.id
        LEFT JOIN categorias cat ON l.categoriaId = cat.id
        WHERE l.data BETWEEN :dataInicio AND :dataFim
        ORDER BY l.data DESC
    """)
    fun getLancamentosComDetalhes(dataInicio: Long, dataFim: Long): Flow<List<LancamentoComDetalhes>>

    @Query("""
        SELECT l.*, c.nome as contaNome, cat.nome as categoriaNome,
               cat.icone as categoriaIcone, cat.corHex as categoriaCorHex
        FROM lancamentos l
        LEFT JOIN contas c ON l.contaId = c.id
        LEFT JOIN categorias cat ON l.categoriaId = cat.id
        WHERE (l.descricao LIKE '%' || :query || '%'
               OR cat.nome LIKE '%' || :query || '%')
        ORDER BY l.data DESC
        LIMIT 100
    """)
    fun buscar(query: String): Flow<List<LancamentoComDetalhes>>

    @Query("""
        SELECT l.*, c.nome as contaNome, cat.nome as categoriaNome,
               cat.icone as categoriaIcone, cat.corHex as categoriaCorHex
        FROM lancamentos l
        LEFT JOIN contas c ON l.contaId = c.id
        LEFT JOIN categorias cat ON l.categoriaId = cat.id
        ORDER BY l.data DESC
        LIMIT 10
    """)
    fun getUltimos10(): Flow<List<LancamentoComDetalhes>>

    @Query("""
        SELECT cat.id as categoriaId, cat.nome as categoriaNome,
               cat.corHex as categoriaCorHex, cat.icone as categoriaIcone,
               SUM(gasto) as total
        FROM (
            SELECT l.categoriaId AS cid, l.valor AS gasto
            FROM lancamentos l
            WHERE l.categoriaId IS NOT NULL
              AND l.data BETWEEN :dataInicio AND :dataFim
              AND l.tipo IN ('DEBITO', 'CREDITO')
            UNION ALL
            SELECT pc.categoriaId AS cid, pc.valorTotal AS gasto
            FROM produtos_comprados pc
            WHERE pc.categoriaId IS NOT NULL
              AND pc.data BETWEEN :dataInicio AND :dataFim
        ) gastos
        INNER JOIN categorias cat ON gastos.cid = cat.id
        GROUP BY cat.id
        ORDER BY total DESC
    """)
    fun getGastosPorCategoria(dataInicio: Long, dataFim: Long): Flow<List<GastoPorCategoria>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valor ELSE 0 END), 0) as totalReceitas,
            COALESCE(SUM(CASE WHEN tipo IN ('DEBITO','CREDITO') THEN valor ELSE 0 END), 0) as totalDespesas,
            COALESCE(SUM(CASE WHEN dataVencimento > :agora AND tipo = 'CREDITO' THEN valor ELSE 0 END), 0) as totalAVencer
        FROM lancamentos
        WHERE data BETWEEN :dataInicio AND :dataFim
    """)
    fun getResumoMensal(dataInicio: Long, dataFim: Long, agora: Long): Flow<ResumoMensal>

    @Query("""
        SELECT COALESCE(SUM(valor), 0)
        FROM lancamentos
        WHERE categoriaId = :categoriaId
          AND data BETWEEN :dataInicio AND :dataFim
          AND tipo IN ('DEBITO', 'CREDITO')
    """)
    fun getGastoCategoriaMes(categoriaId: Long, dataInicio: Long, dataFim: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lancamento: Lancamento): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lancamentos: List<Lancamento>)

    @Update
    suspend fun update(lancamento: Lancamento)

    @Delete
    suspend fun delete(lancamento: Lancamento)

    @Query("DELETE FROM lancamentos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM lancamentos WHERE data BETWEEN :dataInicio AND :dataFim")
    suspend fun deletePorPeriodo(dataInicio: Long, dataFim: Long)

    @Query("DELETE FROM lancamentos")
    suspend fun deleteAll()

    @Query("""
        SELECT l.*, c.nome as contaNome, cat.nome as categoriaNome,
               cat.icone as categoriaIcone, cat.corHex as categoriaCorHex
        FROM lancamentos l
        LEFT JOIN contas c ON l.contaId = c.id
        LEFT JOIN categorias cat ON l.categoriaId = cat.id
        WHERE l.categoriaId = :categoriaId
          AND l.data BETWEEN :dataInicio AND :dataFim
        ORDER BY l.data DESC
    """)
    fun getLancamentosPorCategoria(
        categoriaId: Long, dataInicio: Long, dataFim: Long
    ): Flow<List<LancamentoComDetalhes>>

    @Query("SELECT * FROM lancamentos WHERE id = :id")
    suspend fun getById(id: Long): Lancamento?

    @Query("""
        SELECT l.*, c.nome as contaNome, cat.nome as categoriaNome,
               cat.icone as categoriaIcone, cat.corHex as categoriaCorHex
        FROM lancamentos l
        LEFT JOIN contas c ON l.contaId = c.id
        LEFT JOIN categorias cat ON l.categoriaId = cat.id
        WHERE l.id = :id
    """)
    suspend fun getComDetalhesById(id: Long): LancamentoComDetalhes?
}

// ── ProdutoCompradoDao ─────────────────────────────────────────────────────────
@Dao
interface ProdutoCompradoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produtos: List<ProdutoComprado>)

    @Query("""
        SELECT pc.*, cat.nome as categoriaNome, cat.corHex as categoriaCorHex, cat.icone as categoriaIcone
        FROM produtos_comprados pc
        LEFT JOIN categorias cat ON pc.categoriaId = cat.id
        WHERE pc.lancamentoId = :lancamentoId
        ORDER BY pc.nome ASC
    """)
    suspend fun getByLancamento(lancamentoId: Long): List<ProdutoCompradoComDetalhes>

    @Query("""
        SELECT pc.*, cat.nome as categoriaNome, cat.corHex as categoriaCorHex, cat.icone as categoriaIcone
        FROM produtos_comprados pc
        LEFT JOIN categorias cat ON pc.categoriaId = cat.id
        WHERE pc.lancamentoId = :lancamentoId AND pc.categoriaId IS NULL
        ORDER BY pc.nome ASC
    """)
    suspend fun getSemCategoria(lancamentoId: Long): List<ProdutoCompradoComDetalhes>

    @Query("""
        SELECT pc.*, cat.nome as categoriaNome, cat.corHex as categoriaCorHex, cat.icone as categoriaIcone
        FROM produtos_comprados pc
        LEFT JOIN categorias cat ON pc.categoriaId = cat.id
        WHERE pc.data BETWEEN :dataInicio AND :dataFim
        ORDER BY pc.data DESC, pc.nome ASC
    """)
    fun getByPeriodo(dataInicio: Long, dataFim: Long): Flow<List<ProdutoCompradoComDetalhes>>

    @Query("""
        SELECT pc.*, cat.nome as categoriaNome, cat.corHex as categoriaCorHex, cat.icone as categoriaIcone
        FROM produtos_comprados pc
        LEFT JOIN categorias cat ON pc.categoriaId = cat.id
        WHERE pc.categoriaId = :categoriaId
          AND pc.data BETWEEN :dataInicio AND :dataFim
        ORDER BY pc.data DESC, pc.nome ASC
    """)
    fun getPorCategoria(
        categoriaId: Long, dataInicio: Long, dataFim: Long
    ): Flow<List<ProdutoCompradoComDetalhes>>

    @Query("UPDATE produtos_comprados SET categoriaId = :categoriaId WHERE id = :id")
    suspend fun updateCategoria(id: Long, categoriaId: Long)

    @Query("DELETE FROM produtos_comprados WHERE lancamentoId = :lancamentoId")
    suspend fun deleteByLancamento(lancamentoId: Long)
}

// ── RegraCategoriaDao ──────────────────────────────────────────────────────────
@Dao
interface RegraCategoriaDao {
    @Query("SELECT * FROM regras_categoria")
    fun getAll(): Flow<List<RegraCategoria>>

    @Query("SELECT * FROM regras_categoria WHERE palavraChave LIKE '%' || :keyword || '%' LIMIT 1")
    suspend fun findByKeyword(keyword: String): RegraCategoria?

    @Query("SELECT * FROM regras_categoria WHERE ncm = :ncm LIMIT 1")
    suspend fun findByNcm(ncm: String): RegraCategoria?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regra: RegraCategoria): Long

    @Delete
    suspend fun delete(regra: RegraCategoria)

    @Query("DELETE FROM regras_categoria WHERE categoriaId = :categoriaId")
    suspend fun deleteByCategoriaId(categoriaId: Long)
}

// ── RecorrenteDao ──────────────────────────────────────────────────────────────
@Dao
interface RecorrenteDao {
    @Query("SELECT * FROM recorrentes ORDER BY descricao ASC")
    fun getAll(): Flow<List<Recorrente>>

    @Query("SELECT * FROM recorrentes WHERE ativo = 1")
    suspend fun getAtivos(): List<Recorrente>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recorrente: Recorrente): Long

    @Update
    suspend fun update(recorrente: Recorrente)

    @Delete
    suspend fun delete(recorrente: Recorrente)
}
