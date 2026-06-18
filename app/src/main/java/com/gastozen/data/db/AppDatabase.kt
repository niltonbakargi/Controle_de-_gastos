package com.gastozen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gastozen.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Conta::class,
        Categoria::class,
        Lancamento::class,
        ProdutoComprado::class,
        RegraCategoria::class,
        Recorrente::class,
        DespesaFixa::class,
        PagamentoDespesaFixa::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contaDao(): ContaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun lancamentoDao(): LancamentoDao
    abstract fun produtoCompradoDao(): ProdutoCompradoDao
    abstract fun regraCategoriaDao(): RegraCategoriaDao
    abstract fun recorrenteDao(): RecorrenteDao
    abstract fun despesaFixaDao(): DespesaFixaDao
    abstract fun pagamentoDespesaFixaDao(): PagamentoDespesaFixaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migração 1→2: novos campos em lancamentos + tabela produtos_comprados + novas categorias
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Novos campos na tabela lancamentos
                db.execSQL("ALTER TABLE lancamentos ADD COLUMN tipoPagamento TEXT NOT NULL DEFAULT 'DINHEIRO'")
                db.execSQL("ALTER TABLE lancamentos ADD COLUMN desconto REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE lancamentos ADD COLUMN nfeCnpj TEXT")

                // Nova tabela produtos_comprados
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `produtos_comprados` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `lancamentoId` INTEGER NOT NULL,
                        `nome` TEXT NOT NULL,
                        `ncm` TEXT NOT NULL DEFAULT '',
                        `quantidade` REAL NOT NULL DEFAULT 1.0,
                        `valorUnitario` REAL NOT NULL DEFAULT 0.0,
                        `valorTotal` REAL NOT NULL,
                        `categoriaId` INTEGER,
                        `data` INTEGER NOT NULL,
                        FOREIGN KEY(`lancamentoId`) REFERENCES `lancamentos`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`categoriaId`) REFERENCES `categorias`(`id`) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_produtos_comprados_lancamentoId` ON `produtos_comprados` (`lancamentoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_produtos_comprados_categoriaId` ON `produtos_comprados` (`categoriaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_produtos_comprados_data` ON `produtos_comprados` (`data`)")

                // Novas categorias padrão
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Higiene', 'soap', '#00ACC1')")
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Combustível', 'local_gas_station', '#FF9800')")
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Remédios', 'medication', '#E91E63')")
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Filhos', 'child_care', '#8BC34A')")
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Energia Elétrica', 'bolt', '#FFC107')")
                db.execSQL("INSERT INTO categorias (nome, icone, corHex) VALUES ('Internet', 'wifi', '#3F51B5')")
            }
        }

        // Migração 2→3: tabelas de despesas fixas e pagamentos
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `despesas_fixas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `valor` REAL NOT NULL,
                        `diaVencimento` INTEGER NOT NULL,
                        `categoriaId` INTEGER,
                        `ativa` INTEGER NOT NULL DEFAULT 1,
                        `observacao` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`categoriaId`) REFERENCES `categorias`(`id`) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_despesas_fixas_categoriaId` ON `despesas_fixas` (`categoriaId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pagamentos_despesa_fixa` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `despesaFixaId` INTEGER NOT NULL,
                        `mes` INTEGER NOT NULL,
                        `ano` INTEGER NOT NULL,
                        `valorPago` REAL,
                        `dataPagamento` INTEGER,
                        `pago` INTEGER NOT NULL DEFAULT 0,
                        `comprovantePath` TEXT,
                        `lancamentoId` INTEGER,
                        FOREIGN KEY(`despesaFixaId`) REFERENCES `despesas_fixas`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pagamentos_despesa_fixa_despesaFixaId` ON `pagamentos_despesa_fixa` (`despesaFixaId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pagamentos_unico` ON `pagamentos_despesa_fixa` (`despesaFixaId`, `mes`, `ano`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gastozen.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { populateInitialData(it) }
                        }
                    }
                })
                .build()
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            db.contaDao().insert(
                Conta(nome = "Carteira", tipo = TipoConta.CARTEIRA, saldoInicial = 0.0)
            )

            val categorias = listOf(
                Categoria(nome = "Alimentação",     icone = "restaurant",        corHex = "#F44336"),
                Categoria(nome = "Higiene",          icone = "soap",              corHex = "#00ACC1"),
                Categoria(nome = "Lazer",            icone = "sports_esports",    corHex = "#9C27B0"),
                Categoria(nome = "Combustível",      icone = "local_gas_station", corHex = "#FF9800"),
                Categoria(nome = "Remédios",         icone = "medication",        corHex = "#E91E63"),
                Categoria(nome = "Filhos",           icone = "child_care",        corHex = "#8BC34A"),
                Categoria(nome = "Energia Elétrica", icone = "bolt",              corHex = "#FFC107"),
                Categoria(nome = "Internet",         icone = "wifi",              corHex = "#3F51B5"),
                Categoria(nome = "Limpeza",          icone = "cleaning_services", corHex = "#2196F3"),
                Categoria(nome = "Saúde",            icone = "health_and_safety", corHex = "#4CAF50"),
                Categoria(nome = "Moradia",          icone = "home",              corHex = "#607D8B"),
                Categoria(nome = "Transporte",       icone = "directions_bus",    corHex = "#00BCD4"),
                Categoria(nome = "Educação",         icone = "school",            corHex = "#3F51B5"),
                Categoria(nome = "Outros",           icone = "more_horiz",        corHex = "#9E9E9E")
            )
            categorias.forEach { db.categoriaDao().insert(it) }
        }
    }
}
