package com.airoleplay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    // Backend Connections
    @Query("SELECT * FROM backend_connections")
    fun getAllConnections(): Flow<List<BackendConnectionEntity>>

    @Query("SELECT * FROM backend_connections WHERE isActive = 1 LIMIT 1")
    fun getActiveConnection(): Flow<BackendConnectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: BackendConnectionEntity): Long

    @Update
    suspend fun updateConnection(connection: BackendConnectionEntity)

    @Delete
    suspend fun deleteConnection(connection: BackendConnectionEntity)

    @Query("UPDATE backend_connections SET isActive = 0")
    suspend fun deactivateAllConnections()

    // User Personas
    @Query("SELECT * FROM user_personas")
    fun getAllPersonas(): Flow<List<UserPersonaEntity>>

    @Query("SELECT * FROM user_personas WHERE isActive = 1 LIMIT 1")
    fun getActivePersona(): Flow<UserPersonaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: UserPersonaEntity): Long

    @Update
    suspend fun updatePersona(persona: UserPersonaEntity)

    @Delete
    suspend fun deletePersona(persona: UserPersonaEntity)

    @Query("UPDATE user_personas SET isActive = 0")
    suspend fun deactivateAllPersonas()

    @Query("SELECT count(id) FROM user_personas")
    suspend fun getPersonaCount(): Int

    // Lorebook
    @Query("SELECT * FROM lorebook_entries WHERE characterId = :characterId OR characterId IS NULL ORDER BY insertionOrder ASC")
    fun getLorebookEntriesForCharacter(characterId: Long): Flow<List<LorebookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLorebookEntry(entry: LorebookEntryEntity): Long

    @Update
    suspend fun updateLorebookEntry(entry: LorebookEntryEntity)

    @Delete
    suspend fun deleteLorebookEntry(entry: LorebookEntryEntity)
}
