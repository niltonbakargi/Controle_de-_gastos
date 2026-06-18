package com.gastozen.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))

    fun format(millis: Long): String = sdf.format(Date(millis))
    fun formatFull(millis: Long): String = sdfFull.format(Date(millis))
    fun formatMonth(millis: Long): String = sdfMonth.format(Date(millis)).replaceFirstChar { it.uppercase() }

    fun startOfMonth(year: Int, month: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfMonth(year: Int, month: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return cal.timeInMillis
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH)

    fun startOfCurrentMonth(): Long = startOfMonth(currentYear(), currentMonth())
    fun endOfCurrentMonth(): Long = endOfMonth(currentYear(), currentMonth())
}

object CurrencyUtils {
    private val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    fun format(value: Double): String = format.format(value)
}

object PixParser {
    private val valorRegex = Regex("""(?:valor|R\$|value)[:\s]*([0-9]+[.,][0-9]{2})""", RegexOption.IGNORE_CASE)
    private val descricaoRegex = Regex("""(?:descri[çc][ãa]o|motivo|pagamento)[:\s]*(.+)""", RegexOption.IGNORE_CASE)

    fun extractValor(text: String): Double? {
        return valorRegex.find(text)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
    }

    fun extractDescricao(text: String): String? {
        return descricaoRegex.find(text)?.groupValues?.get(1)?.trim()
    }
}
