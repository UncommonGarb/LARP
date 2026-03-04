package com.airoleplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_settings")
data class GlobalSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val globalSystemPrompt: String = "",
    val defaultTemperature: Float = 0.8f,
    val defaultTopP: Float = 0.9f,
    val defaultTopK: Int = 40,
    val defaultRepetitionPenalty: Float = 1.1f,
    val defaultMaxNewTokens: Int = 400,
    val defaultContextSizeLimit: Int = 4096,
    val trimIncompleteSentences: Boolean = true
)
