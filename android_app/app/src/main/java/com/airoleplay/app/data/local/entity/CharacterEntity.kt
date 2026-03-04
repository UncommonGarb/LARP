package com.airoleplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarImagePath: String? = null,
    val description: String,
    val personalitySummary: String,
    val scenario: String,
    val firstMessage: String,
    val exampleDialogue: String,
    val systemPromptOverride: String? = null,
    val postHistoryInstructions: String? = null,
    val tags: String, // Comma separated tags
    val authorsNote: String? = null,
    val authorsNoteDepth: Int = 2,
    val burnPacing: String = "REALISTIC", // SLOW, REALISTIC, FAST, INSTANT
    val autonomyLevel: Int = 50, // 0 to 100
    val createdAt: Long = System.currentTimeMillis()
)
