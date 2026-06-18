package com.gastozen.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object OcrHelper {

    /** Extrai texto de uma imagem via URI (content:// ou file://). */
    suspend fun fromUri(context: Context, uri: Uri): String = suspendCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    /** Renderiza a primeira página de um PDF e extrai o texto via OCR. */
    suspend fun fromPdf(context: Context, uri: Uri): String {
        val bitmap = renderPdfFirstPage(context, uri) ?: return ""
        return fromBitmap(bitmap)
    }

    private fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        return try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            // Renderiza em 2× para melhor precisão de OCR
            val bmp = Bitmap.createBitmap(
                page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
            )
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fromBitmap(bitmap: Bitmap): String = suspendCoroutine { cont ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}
