package com.airoleplay.app.utils

import java.io.File

object ImageUtils {
    fun resolveModel(path: String?): Any? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("/") || path.startsWith("file://")) {
            File(path.removePrefix("file://"))
        } else {
            path // Could be a URI string or asset path
        }
    }
}
