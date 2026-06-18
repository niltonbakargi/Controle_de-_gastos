package com.gastozen.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.gastozen.R
import com.gastozen.data.db.AppDatabase
import com.gastozen.util.CurrencyUtils
import com.gastozen.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class GastoZenWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val inicio = DateUtils.startOfCurrentMonth()
            val fim = DateUtils.endOfCurrentMonth()
            val agora = System.currentTimeMillis()

            val resumo = db.lancamentoDao()
                .getResumoMensal(inicio, fim, agora)
                .first()

            val views = RemoteViews(context.packageName, R.layout.widget_gastozen)
            views.setTextViewText(R.id.widget_saldo, CurrencyUtils.format(resumo.saldo))
            views.setTextViewText(
                R.id.widget_label,
                "Saldo ${DateUtils.formatMonth(inicio)}"
            )

            withContext(Dispatchers.Main) {
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}
