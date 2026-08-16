package com.sumit.pocketgpt.di

import android.content.Context
import androidx.room.Room
import com.sumit.pocketgpt.data.local.ChatMessageDao
import com.sumit.pocketgpt.data.local.ConversationDao
import com.sumit.pocketgpt.data.local.PocketGptDatabase
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
    fun providePocketGptDatabase(@ApplicationContext context: Context): PocketGptDatabase {
        return Room.databaseBuilder(
            context,
            PocketGptDatabase::class.java,
            "pocketgpt.db",
        )
            // No migration path exists yet for the v1 -> v2 schema (conversations table +
            // chat_messages FK). Fine pre-release with no user data to preserve; revisit
            // with a real Migration once this ships.
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideChatMessageDao(database: PocketGptDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideConversationDao(database: PocketGptDatabase): ConversationDao {
        return database.conversationDao()
    }
}
