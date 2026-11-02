package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object BotExportHelper {

    fun sanitizeFileName(rawName: String): String {
        val clean = rawName.replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁ_\\-\\s]"), "_").trim()
        return clean.ifBlank { "bot_character" }
    }

    /**
     * Reads JSON text from a chosen file or content Uri.
     */
    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes character JSON text to the Uri selected via ActivityResultContracts.CreateDocument.
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Direct fallback to public Downloads/UntitledAI folder.
     */
    fun saveJsonToDownloads(context: Context, fileNameWithoutExt: String, content: String): Uri? {
        val safeName = "${sanitizeFileName(fileNameWithoutExt)}.json"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/UntitledAI")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                    uri
                } else null
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloads, "UntitledAI").apply { mkdirs() }
                val targetFile = File(targetDir, safeName)
                targetFile.writeText(content, Charsets.UTF_8)
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            null
        }
    }
}
