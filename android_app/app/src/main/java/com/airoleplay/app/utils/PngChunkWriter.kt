package com.airoleplay.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object PngChunkWriter {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun writeTavernCard(
        sourceAvatarPath: String?,
        destinationPath: String,
        charaDataBase64: String,
        characterName: String = "A"
    ): Result<Unit> {
        return try {
            val sourceFile = sourceAvatarPath?.let { File(it) }

            val inputStream = if (sourceFile?.exists() == true) {
                FileInputStream(sourceFile)
            } else {
                createPlaceholderPng(characterName)
            }

            val outputFile = File(destinationPath)
            val outputStream = FileOutputStream(outputFile)

            // Read signature
            val signature = ByteArray(8)
            inputStream.read(signature)
            outputStream.write(signature)

            var iendReached = false

            while (!iendReached) {
                val lengthBytes = ByteArray(4)
                if (inputStream.read(lengthBytes) != 4) break
                val length = ByteBuffer.wrap(lengthBytes).int

                val typeBytes = ByteArray(4)
                if (inputStream.read(typeBytes) != 4) break
                val type = String(typeBytes, StandardCharsets.US_ASCII)

                val data = ByteArray(length)
                if (length > 0) {
                    var read = 0
                    while (read < length) {
                        val count = inputStream.read(data, read, length - read)
                        if (count == -1) break
                        read += count
                    }
                }

                val crcBytes = ByteArray(4)
                inputStream.read(crcBytes)

                // Inject our text chunk before IEND
                if (type == "IEND") {
                    writeTextChunk(outputStream, "chara", charaDataBase64)

                    // Write the original IEND chunk
                    outputStream.write(lengthBytes)
                    outputStream.write(typeBytes)
                    outputStream.write(data)
                    outputStream.write(crcBytes)
                    iendReached = true
                } else if (type != "tEXt" || !String(data, StandardCharsets.ISO_8859_1).startsWith("chara\\u0000")) {
                    // Skip existing chara chunks, write everything else
                    outputStream.write(lengthBytes)
                    outputStream.write(typeBytes)
                    outputStream.write(data)
                    outputStream.write(crcBytes)
                }
            }

            inputStream.close()
            outputStream.close()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun writeTextChunk(outputStream: FileOutputStream, keyword: String, text: String) {
        val keywordBytes = keyword.toByteArray(StandardCharsets.ISO_8859_1)
        val nullByte = byteArrayOf(0)
        val textBytes = text.toByteArray(StandardCharsets.ISO_8859_1)

        val chunkData = ByteArray(keywordBytes.size + 1 + textBytes.size)
        System.arraycopy(keywordBytes, 0, chunkData, 0, keywordBytes.size)
        System.arraycopy(nullByte, 0, chunkData, keywordBytes.size, 1)
        System.arraycopy(textBytes, 0, chunkData, keywordBytes.size + 1, textBytes.size)

        val lengthBytes = ByteBuffer.allocate(4).putInt(chunkData.size).array()
        val typeBytes = "tEXt".toByteArray(StandardCharsets.US_ASCII)

        // CRC Calculation (Type + Data)
        val crc = java.util.zip.CRC32()
        crc.update(typeBytes)
        crc.update(chunkData)
        val crcValue = crc.value.toInt()
        val crcBytes = ByteBuffer.allocate(4).putInt(crcValue).array()

        outputStream.write(lengthBytes)
        outputStream.write(typeBytes)
        outputStream.write(chunkData)
        outputStream.write(crcBytes)
    }

    private fun createPlaceholderPng(characterName: String): java.io.InputStream {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background circle
        val paint = Paint().apply {
            color = Color.parseColor("#8B5CF6")
            isAntiAlias = true
        }
        canvas.drawCircle(200f, 200f, 200f, paint)

        // Get initial
        val initial = if (characterName.isNotBlank()) characterName.substring(0, 1).uppercase() else "?"

        // Draw initial text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 200f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Calculate vertical center offset
        val xPos = (canvas.width / 2).toFloat()
        val yPos = ((canvas.height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2))

        canvas.drawText(initial, xPos, yPos, textPaint)

        val tempFile = File.createTempFile("placeholder", ".png")
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()

        return FileInputStream(tempFile)
    }
}
