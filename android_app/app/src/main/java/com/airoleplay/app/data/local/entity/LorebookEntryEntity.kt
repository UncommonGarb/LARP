package com.airoleplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "lorebook_entries",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["characterId"])]
)
data class LorebookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long? = null, // If null, it's a global lorebook entry
    val title: String,
    val keywords: String, // Comma separated
    val content: String,
    val insertionOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isConstant: Boolean = false // Always inject regardless of keywords
)
