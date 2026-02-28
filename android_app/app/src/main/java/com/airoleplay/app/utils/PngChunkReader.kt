package com.airoleplay.app.utils

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream

object PngChunkReader {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun readTavernCardData(inputStream: InputStream): String? {
        // Read and verify signature
        val signature = ByteArray(8)
        if (inputStream.read(signature) != 8 || !signature.contentEquals(PNG_SIGNATURE)) {
            return null // Not a valid PNG
        }

        while (true) {
            // Read chunk length (4 bytes)
            val lengthBytes = ByteArray(4)
            if (inputStream.read(lengthBytes) != 4) break
            val length = ByteBuffer.wrap(lengthBytes).int
            if (length < 0) break // Invalid length

            // Read chunk type (4 bytes)
            val typeBytes = ByteArray(4)
            if (inputStream.read(typeBytes) != 4) break
            val type = String(typeBytes, StandardCharsets.US_ASCII)

            // Read chunk data
            val data = ByteArray(length)
            if (length > 0) {
                var read = 0
                while (read < length) {
                    val count = inputStream.read(data, read, length - read)
                    if (count == -1) break
                    read += count
                }
            }

            // Read CRC (4 bytes)
            val crcBytes = ByteArray(4)
            inputStream.read(crcBytes) // We skip verifying CRC for speed and simplicity

            // Process text chunks
            if (type == "tEXt") {
                val textData = String(data, StandardCharsets.ISO_8859_1)
                val nullIndex = textData.indexOf('\u0000')
                if (nullIndex != -1) {
                    val keyword = textData.substring(0, nullIndex)
                    val text = textData.substring(nullIndex + 1)
                    if (keyword == "chara") {
                        return text
                    }
                }
            } else if (type == "zTXt") {
                // zTXt format: Keyword + Null + CompressionMethod(1 byte) + CompressedText
                val nullIndex = data.indexOf(0.toByte())
                if (nullIndex != -1 && nullIndex + 1 < data.size) {
                    val keyword = String(data, 0, nullIndex, StandardCharsets.ISO_8859_1)
                    if (keyword == "chara") {
                        val compMethod = data[nullIndex + 1]
                        if (compMethod == 0.toByte()) { // 0 = zlib deflate
                            val compressedData = data.copyOfRange(nullIndex + 2, data.size)
                            try {
                                val inflater = InflaterInputStream(compressedData.inputStream())
                                val result = ByteArrayOutputStream()
                                inflater.copyTo(result)
                                return String(result.toByteArray(), StandardCharsets.UTF_8)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } else if (type == "iTXt") {
                 // iTXt format: Keyword + Null + CompressionFlag(1 byte) + CompressionMethod(1 byte) + LanguageTag + Null + TranslatedKeyword + Null + Text
                 val null1 = data.indexOf(0.toByte())
                 if (null1 != -1 && null1 + 2 < data.size) {
                     val keyword = String(data, 0, null1, StandardCharsets.UTF_8)
                     if (keyword == "chara") {
                         val isCompressed = data[null1 + 1] == 1.toByte()
                         var startIdx = null1 + 3
                         // skip LanguageTag
                         while(startIdx < data.size && data[startIdx] != 0.toByte()) startIdx++
                         startIdx++ // skip null
                         // skip TranslatedKeyword
                         while(startIdx < data.size && data[startIdx] != 0.toByte()) startIdx++
                         startIdx++ // skip null

                         if (startIdx < data.size) {
                             if (!isCompressed) {
                                 return String(data, startIdx, data.size - startIdx, StandardCharsets.UTF_8)
                             } else {
                                val compressedData = data.copyOfRange(startIdx, data.size)
                                try {
                                    val inflater = InflaterInputStream(compressedData.inputStream())
                                    val result = ByteArrayOutputStream()
                                    inflater.copyTo(result)
                                    return String(result.toByteArray(), StandardCharsets.UTF_8)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                             }
                         }
                     }
                 }
            }

            if (type == "IEND") break
        }

        return null
    }
}
