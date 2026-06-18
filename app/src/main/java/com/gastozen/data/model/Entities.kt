package com.gastozen.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

// ── Conta ──────────────────────────────────────────────────────────────────────
enum class TipoConta { CARTEIRA, CORRENTE, CARTAO }

@Entity(tableName = "contas")
data class Conta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: TipoConta = TipoConta.CARTEIRA,
    val saldoInicial: Double = 0.0
)

// ── Categoria ──────────────────────────────────────────────────────────────────
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val icone: String = "category",
    val corHex: String = "#6200EE",
    val metaMensal: Double? = null
)

// ── Lançamento ─────────────────────────────────────────────────────────────────
enum class TipoLancamento { DEBITO, CREDITO, RECEITA }

@Entity(
    tableName = "lancamentos",
    foreignKeys = [
        ForeignKey(
            entity = Conta::class,
            parentColumns = ["id"],
            childColumns = ["contaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("contaId"),
        Index("categoriaId"),
        Index("data"),
        Index("grupoParcela")
    ]
)
data class Lancamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val valor: Double,
    val data: Long = System.currentTimeMillis(),
    val tipo: TipoLancamento = TipoLancamento.DEBITO,
    val contaId: Long? = null,
    val categoriaId: Long? = null,
    val parcelaAtual: Int = 1,
    val totalParcelas: Int = 1,
    val dataVencimento: Long? = null,
    val recorrente: Boolean = false,
    val observacao: String = "",
    val fotoPath: String? = null,
    val grupoParcela: String? = null  // UUID to group installments
)

// ── Regra de Categoria ─────────────────────────────────────────────────────────
@Entity(
    tableName = "regras_categoria",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoriaId")]
)
data class RegraCategoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val palavraChave: String? = null,
    val ncm: String? = null,
    val categoriaId: Long
)

// ── Recorrente ─────────────────────────────────────────────────────────────────
@Entity(
    tableName = "recorrentes",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Conta::class,
            parentColumns = ["id"],
            childColumns = ["contaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoriaId"), Index("contaId")]
)
data class Recorrente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val valor: Double,
    val diaVencimento: Int = 1,
    val categoriaId: Long? = null,
    val contaId: Long? = null,
    val ativo: Boolean = true,
    val tipo: TipoLancamento = TipoLancamento.DEBITO
)

// ── Data classes para queries ──────────────────────────────────────────────────

data class GastoPorCategoria(
    val categoriaId: Long,
    val categoriaNome: String,
    val categoriaCorHex: String,
    val categoriaIcone: String,
    val total: Double
)

data class ResumoMensal(
    val totalReceitas: Double,
    val totalDespesas: Double,
    val totalAVencer: Double
) {
    val saldo: Double get() = totalReceitas - totalDespesas
}
