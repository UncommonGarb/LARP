package com.airoleplay.app.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun resolveModel(path: String?): Any? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("/") || path.startsWith("file://")) {
            File(path.removePrefix("file://"))
        } else {
            path // Could be a URI string or asset path
        }
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
