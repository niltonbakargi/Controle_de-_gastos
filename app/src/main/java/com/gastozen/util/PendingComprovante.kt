package com.gastozen.util

import android.net.Uri

/**
 * Armazena temporariamente um URI de imagem ou PDF compartilhado para ser
 * processado por ReceberComprovanteViewModel (OCR + parsing).
 */
object PendingComprovante {

    sealed class Content {
        data class Imagem(val uri: Uri) : Content()
        data class Pdf(val uri: Uri) : Content()
    }

    @Volatile private var content: Content? = null

    fun set(c: Content) { content = c }

    /** Retorna e limpa o conteúdo pendente (só pode ser consumido uma vez). */
    fun consume(): Content? = content.also { content = null }

    fun hasPending(): Boolean = content != null
}
