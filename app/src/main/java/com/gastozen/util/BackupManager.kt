package com.gastozen.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gastozen.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupManager {

    private const val DB_NAME = "gastozen.db"

    suspend fun exportDb(context: Context): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            // Force checkpoint of WAL
            AppDatabase.getInstance(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()

            val dbFile = context.getDatabasePath(DB_NAME)
            val backupDir = File(context.filesDir, "backup").also { it.mkdirs() }
            val backupFile = File(backupDir, "gastozen_backup.db")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup GastoZen")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(intent, "Compartilhar backup"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao exportar backup: ${e.message}"))
        }
    }

    suspend fun importDb(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)

            // Close database
            AppDatabase.getInstance(context).close()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao importar backup: ${e.message}"))
        }
    }
}
