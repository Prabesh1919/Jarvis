package com.jarvis.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database configuration for the assistant memory system.
 * Idempotent, singleton-backed thread safety is enforced.
 */
@Database(
    entities = [
        ConversationMessage::class,
        SemanticPreference::class,
        WorkflowState::class,
        Document::class,
        DocumentChunk::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        /**
         * Resolves the singleton instance of MemoryDatabase.
         */
        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "jarvis_memory_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
