package com.gastozen.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object QrCodeParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Given the URL from a NF-e QR Code, fetches the XML from SEFAZ and parses it.
     * Returns null with an error message on failure.
     */
    suspend fun fetchNotaFiscal(url: String): Result<NotaFiscal> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Falha ao consultar SEFAZ: HTTP ${response.code}")
                )
            }
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Resposta vazia da SEFAZ"))

            val nota = if (isHtml(body)) {
                NfeHtmlParser.parse(body)
            } else {
                NfeXmlParser.parse(body)
            }
            Result.success(nota)
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao consultar nota fiscal: ${e.message}"))
        }
    }

    private fun isHtml(body: String): Boolean {
        val start = body.trimStart().take(200).lowercase()
        return start.contains("<!doctype html") ||
               start.contains("<html") ||
               start.contains("<head") ||
               start.contains("<body")
    }
}
