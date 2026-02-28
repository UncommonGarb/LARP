package com.airoleplay.app.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.database.AiRoleplayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAiRoleplayDatabase(
        @ApplicationContext app: Context
    ): AiRoleplayDatabase {
        return AiRoleplayDatabase.getDatabase(app)
    }

    @Provides
    @Singleton
    fun provideCharacterDao(db: AiRoleplayDatabase): CharacterDao {
        return db.characterDao()
    }

    @Provides
    @Singleton
    fun provideChatDao(db: AiRoleplayDatabase): ChatDao {
        return db.chatDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(db: AiRoleplayDatabase): SettingsDao {
        return db.settingsDao()
    }
}
