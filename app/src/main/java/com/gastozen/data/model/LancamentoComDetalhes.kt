package com.gastozen.data.model

import androidx.room.ColumnInfo

/**
 * Flat projection used by JOIN queries.
 * Column names must exactly match the aliases in DAOs.kt SELECT clauses.
 */
data class LancamentoComDetalhes(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val data: Long,
    val tipo: TipoLancamento,
    val tipoPagamento: TipoPagamento,
    val desconto: Double,
    val nfeCnpj: String?,
    @ColumnInfo(name = "parcelaAtual")    val parcelaAtual: Int,
    @ColumnInfo(name = "totalParcelas")   val totalParcelas: Int,
    @ColumnInfo(name = "dataVencimento")  val dataVencimento: Long?,
    val recorrente: Boolean,
    val observacao: String,
    @ColumnInfo(name = "fotoPath")        val fotoPath: String?,
    @ColumnInfo(name = "grupoParcela")    val grupoParcela: String?,
    @ColumnInfo(name = "contaId")         val contaId: Long?,
    @ColumnInfo(name = "contaNome")       val contaNome: String?,
    @ColumnInfo(name = "categoriaId")     val categoriaId: Long?,
    @ColumnInfo(name = "categoriaNome")   val categoriaNome: String?,
    @ColumnInfo(name = "categoriaIcone")  val categoriaIcone: String?,
    @ColumnInfo(name = "categoriaCorHex") val categoriaCorHex: String?,
    @ColumnInfo(name = "cartaoId")        val cartaoId: Long? = null,
    @ColumnInfo(name = "faturaAno")       val faturaAno: Int? = null,
    @ColumnInfo(name = "faturaMes")       val faturaMes: Int? = null
)
