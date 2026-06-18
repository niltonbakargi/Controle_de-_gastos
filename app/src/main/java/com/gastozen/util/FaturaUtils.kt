package com.gastozen.util

import java.util.Calendar

object FaturaUtils {

    /**
     * Dado a data de compra e o dia de fechamento do cartão,
     * retorna (ano, mes) da fatura à qual a compra pertence.
     *
     * Regra: se dia_compra <= diaFechamento → fatura do mês atual
     *        se dia_compra >  diaFechamento → fatura do mês seguinte
     */
    fun computarFaturaMes(dataCompra: Long, diaFechamento: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = dataCompra }
        val dia = cal.get(Calendar.DAY_OF_MONTH)
        val mes = cal.get(Calendar.MONTH) + 1
        val ano = cal.get(Calendar.YEAR)
        return if (dia <= diaFechamento) {
            Pair(ano, mes)
        } else {
            if (mes == 12) Pair(ano + 1, 1) else Pair(ano, mes + 1)
        }
    }

    /** Período de exibição da fatura: "6/mai a 5/jun" */
    fun periodoTexto(faturaAno: Int, faturaMes: Int, diaFechamento: Int): String {
        val mesAnterior = if (faturaMes == 1) 12 else faturaMes - 1
        val anoAnterior = if (faturaMes == 1) faturaAno - 1 else faturaAno
        val inicio = "${(diaFechamento % 28) + 1}/${mesAnterior.toString().padStart(2,'0')}/$anoAnterior"
        val fim    = "$diaFechamento/${faturaMes.toString().padStart(2,'0')}/$faturaAno"
        return "$inicio a $fim"
    }

    /** Formata "julho/2025" */
    fun nomeMes(mes: Int, ano: Int): String {
        val nomes = listOf("Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                           "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro")
        return "${nomes[mes - 1]}/$ano"
    }
}
