package com.gastozen.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gastozen.R

object NotificationHelper {

    const val CHANNEL_METAS = "channel_metas"
    const val CHANNEL_RECORRENTES = "channel_recorrentes"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                CHANNEL_METAS,
                "Alertas de Metas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações sobre metas mensais de gastos"
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_RECORRENTES,
                "Lançamentos Recorrentes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de lançamentos recorrentes criados"
                manager.createNotificationChannel(this)
            }
        }
    }

    fun notifyMetaAlerta(context: Context, categoriaNome: String, percentual: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val titulo = if (percentual >= 100) "Meta ultrapassada!" else "Atenção: meta em $percentual%"
        val texto = if (percentual >= 100)
            "Você ultrapassou a meta de $categoriaNome este mês."
        else
            "Você já usou $percentual% da meta de $categoriaNome."

        val notification = NotificationCompat.Builder(context, CHANNEL_METAS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(categoriaNome.hashCode(), notification)
    }
}
