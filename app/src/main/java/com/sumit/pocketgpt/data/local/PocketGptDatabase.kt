package com.sumit.pocketgpt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, ConversationEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PocketGptDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
}
