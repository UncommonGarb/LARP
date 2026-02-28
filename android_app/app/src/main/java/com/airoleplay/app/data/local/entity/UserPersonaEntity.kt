package com.airoleplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_personas")
data class UserPersonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val avatarImagePath: String? = null,
    val isActive: Boolean = false
)
