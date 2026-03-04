package com.airoleplay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import com.airoleplay.app.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId ORDER BY lastActiveAt DESC")
    fun getSessionsForCharacter(characterId: Long): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND isActive = 1 ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id >= :messageId")
    suspend fun deleteMessagesFromId(sessionId: Long, messageId: Long)

    @Query("UPDATE chat_messages SET isActive = 0 WHERE id = :messageId")
    suspend fun deactivateMessage(messageId: Long)

    @Query("SELECT * FROM chat_messages WHERE swipeGroupId = :groupId ORDER BY timestamp ASC")
    suspend fun getMessagesBySwipeGroupId(groupId: String): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET isActive = 0 WHERE swipeGroupId = :groupId")
    suspend fun deactivateAllInSwipeGroup(groupId: String)

    @Query("UPDATE chat_messages SET isActive = 1 WHERE id = :messageId")
    suspend fun activateMessage(messageId: Long)

    // Memory Bank queries
    @Query("SELECT * FROM memories WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getMemoriesForSession(sessionId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE characterId = :characterId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveMemoriesForCharacter(characterId: Long): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
}

