package com.gastozen.util

/**
 * Armazena temporariamente um PIX recibo parseado para ser consumido
 * pela próxima instância de LancamentoViewModel.
 */
object PendingPix {
    @Volatile private var recibo: PixRecibo? = null

    fun set(r: PixRecibo) { recibo = r }

    /** Retorna e limpa o recibo pendente (só pode ser consumido uma vez). */
    fun consume(): PixRecibo? = recibo.also { recibo = null }

    fun hasPending(): Boolean = recibo != null
}
