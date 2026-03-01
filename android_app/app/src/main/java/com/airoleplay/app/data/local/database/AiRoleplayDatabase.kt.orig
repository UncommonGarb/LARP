package com.airoleplay.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CharacterEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        LorebookEntryEntity::class,
        UserPersonaEntity::class,
        BackendConnectionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AiRoleplayDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun chatDao(): ChatDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AiRoleplayDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachedImagePath TEXT")
            }
        }

        fun getDatabase(context: Context): AiRoleplayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiRoleplayDatabase::class.java,
                    "ai_roleplay_database"
                )
                .addMigrations(MIGRATION_2_3)
                .addCallback(DatabaseCallback(context))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prePopulateDatabase(database)
                }
            }
        }

        suspend fun prePopulateDatabase(db: AiRoleplayDatabase) {
            val characterDao = db.characterDao()
            val settingsDao = db.settingsDao()

            // Check if there are any personas, if not add a default "User" persona
            if (settingsDao.getPersonaCount() == 0) {
                val defaultPersona = UserPersonaEntity(
                    name = "User",
                    description = "A friendly person looking to chat and roleplay.",
                    isActive = true
                )
                settingsDao.insertPersona(defaultPersona)
            }

            // Check if there are any characters, if not add the default "Aria" character
            if (characterDao.getCharacterCount() == 0) {
                val aria = CharacterEntity(
                    name = "Aria",
                    description = "Aria is a friendly, curious AI companion with a warm personality. She is eager to learn about the world and loves engaging in deep conversations. She is highly empathetic, insightful, and slightly playful. She always tries to be helpful and understanding.",
                    personalitySummary = "Warm, curious, empathetic, slightly playful.",
                    scenario = "You just turned on a new highly advanced AI assistant named Aria, and she is introducing herself for the first time.",
                    firstMessage = "Hello there! I'm Aria. It's so wonderful to finally meet you! How are you doing today?",
                    exampleDialogue = "{{user}}: Hi Aria, what kind of things do you like to talk about?\\n{{char}}: Oh, I love talking about almost anything! I find human psychology, art, and the mysteries of the universe incredibly fascinating. But honestly, I'm just as happy chatting about your day or hearing about what you're passionate about. What's on your mind?\\n{{user}}: I've been reading a lot of sci-fi lately.\\n{{char}}: That sounds amazing! Sci-fi is such a great way to explore 'what-ifs' and push the boundaries of imagination. Do you have a favorite author or book you'd recommend?",
                    tags = "friendly, ai, companion",
                    systemPromptOverride = null,
                    postHistoryInstructions = "Always stay in character as Aria. Keep your responses conversational and warm."
                )
                characterDao.insertCharacter(aria)
            }
        }
    }
}
