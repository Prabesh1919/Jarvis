package com.jarvis.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity mapping user/assistant conversation history.
 */
@Entity(tableName = "conversation_messages")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val role: String, // "user", "assistant"
    val content: String,
    val contextSummary: String? = null // Associated device telemetry context
)

/**
 * Key-value entity mapping long-term preferences, nicknames, or semantic rules.
 */
@Entity(tableName = "semantic_preferences")
data class SemanticPreference(
    @PrimaryKey val key: String,
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entity tracking persistent workflow states for crash recovery and idempotency.
 */
@Entity(tableName = "workflow_states")
data class WorkflowState(
    @PrimaryKey val id: String,
    val name: String,
    val currentStepIndex: Int,
    val status: String, // "PENDING", "RUNNING", "COMPLETED", "FAILED", "PAUSED_WAITING_USER"
    val serializedSteps: String, // JSON representation of the list of ToolCall
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Entity tracking private user documents.
 */
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val content: String,
    val addedTime: Long = System.currentTimeMillis()
)

/**
 * Entity tracking fragments of parsed user documents for semantic search.
 */
@Entity(tableName = "document_chunks")
data class DocumentChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val chunkText: String,
    val tokenCount: Int,
    val chunkIndex: Int
)
