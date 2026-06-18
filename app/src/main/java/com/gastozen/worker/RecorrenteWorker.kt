package com.gastozen.worker

import android.content.Context
import androidx.work.*
import com.gastozen.data.db.AppDatabase
import com.gastozen.data.model.Lancamento
import com.gastozen.data.model.TipoLancamento
import java.util.*
import java.util.concurrent.TimeUnit

class RecorrenteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val recorrenteDao = db.recorrenteDao()
            val lancamentoDao = db.lancamentoDao()

            val cal = Calendar.getInstance()
            val diaAtual = cal.get(Calendar.DAY_OF_MONTH)

            val ativos = recorrenteDao.getAtivos()
            ativos.filter { it.diaVencimento == diaAtual }.forEach { recorrente ->
                lancamentoDao.insert(
                    Lancamento(
                        descricao = recorrente.descricao,
                        valor = recorrente.valor,
                        data = System.currentTimeMillis(),
                        tipo = recorrente.tipo,
                        contaId = recorrente.contaId,
                        categoriaId = recorrente.categoriaId,
                        recorrente = true
                    )
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "recorrente_worker"

        fun schedule(context: Context) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val delay = cal.timeInMillis - System.currentTimeMillis()

            val request = PeriodicWorkRequestBuilder<RecorrenteWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
