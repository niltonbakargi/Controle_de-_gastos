package com.gastozen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        RegraCategoria::class,
        Recorrente::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contaDao(): ContaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun lancamentoDao(): LancamentoDao
    abstract fun regraCategoriaDao(): RegraCategoriaDao
    abstract fun recorrenteDao(): RecorrenteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

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
                Categoria(nome = "Alimentação",          icone = "restaurant",        corHex = "#F44336"),
                Categoria(nome = "Gasolina",             icone = "local_gas_station", corHex = "#FF9800"),
                Categoria(nome = "Limpeza",              icone = "cleaning_services", corHex = "#2196F3"),
                Categoria(nome = "Manutenção do carro",  icone = "car_repair",        corHex = "#795548"),
                Categoria(nome = "Saúde",                icone = "health_and_safety", corHex = "#4CAF50"),
                Categoria(nome = "Lazer",                icone = "sports_esports",    corHex = "#9C27B0"),
                Categoria(nome = "Moradia",              icone = "home",              corHex = "#607D8B"),
                Categoria(nome = "Transporte",           icone = "directions_bus",    corHex = "#00BCD4"),
                Categoria(nome = "Educação",             icone = "school",            corHex = "#3F51B5"),
                Categoria(nome = "Outros",               icone = "more_horiz",        corHex = "#9E9E9E")
            )
            categorias.forEach { db.categoriaDao().insert(it) }
        }
    }
}
