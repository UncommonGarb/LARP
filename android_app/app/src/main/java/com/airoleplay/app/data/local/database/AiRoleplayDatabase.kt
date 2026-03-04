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
import com.airoleplay.app.data.local.entity.GlobalSettingsEntity
import com.airoleplay.app.data.local.entity.MemoryEntity
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
        BackendConnectionEntity::class,
        GlobalSettingsEntity::class,
        MemoryEntity::class
    ],
    version = 10,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `global_settings` (`id` INTEGER NOT NULL, `globalSystemPrompt` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isError INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add generation params to global_settings
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultTemperature REAL NOT NULL DEFAULT 0.8")
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultTopP REAL NOT NULL DEFAULT 0.9")
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultTopK INTEGER NOT NULL DEFAULT 40")
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultRepetitionPenalty REAL NOT NULL DEFAULT 1.1")
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultMaxNewTokens INTEGER NOT NULL DEFAULT 400")
                db.execSQL("ALTER TABLE global_settings ADD COLUMN defaultContextSizeLimit INTEGER NOT NULL DEFAULT 4096")

                // 2. Add useCustomSettings to backend_connections
                db.execSQL("ALTER TABLE backend_connections ADD COLUMN useCustomSettings INTEGER NOT NULL DEFAULT 0")

                // 3. Create memories table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `memories` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `characterId` INTEGER NOT NULL,
                    `content` TEXT NOT NULL,
                    `isPinned` INTEGER NOT NULL DEFAULT 0,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON DELETE CASCADE
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_sessionId` ON `memories` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_characterId` ON `memories` (`characterId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE global_settings ADD COLUMN trimIncompleteSentences INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lorebook_entries ADD COLUMN category TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE lorebook_entries ADD COLUMN metadataJson TEXT")
                db.execSQL("ALTER TABLE lorebook_entries ADD COLUMN lastTriggeredAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN burnPacing TEXT NOT NULL DEFAULT 'REALISTIC'")
                db.execSQL("ALTER TABLE characters ADD COLUMN autonomyLevel INTEGER NOT NULL DEFAULT 50")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN speechPatterns TEXT")
                db.execSQL("ALTER TABLE characters ADD COLUMN fearsFlaws TEXT")
                db.execSQL("ALTER TABLE characters ADD COLUMN likesDislikes TEXT")
            }
        }

        fun getDatabase(context: Context): AiRoleplayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiRoleplayDatabase::class.java,
                    "ai_roleplay_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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

            // Default global settings
            settingsDao.insertGlobalSettings(com.airoleplay.app.data.local.entity.GlobalSettingsEntity())

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
