package com.jarvis.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object exposing CRUD APIs for the memory database.
 */
@Dao
interface MemoryDao {

    // --- CONVERSATION LOGS ---

    @Query("SELECT * FROM conversation_messages ORDER BY timestamp ASC")
    suspend fun getFullHistory(): List<ConversationMessage>

    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ConversationMessage>

    @Insert
    suspend fun insertMessage(message: ConversationMessage)

    @Query("DELETE FROM conversation_messages")
    suspend fun clearHistory()

    // --- SEMANTIC PREFERENCES ---

    @Query("SELECT * FROM semantic_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): SemanticPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: SemanticPreference)

    @Query("DELETE FROM semantic_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)

    @Query("DELETE FROM semantic_preferences")
    suspend fun clearPreferences()

    // --- WORKFLOW STATES ---

    @Query("SELECT * FROM workflow_states WHERE id = :id")
    suspend fun getWorkflowState(id: String): WorkflowState?

    @Query("SELECT * FROM workflow_states ORDER BY lastUpdated DESC")
    suspend fun getAllWorkflowStates(): List<WorkflowState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorkflowState(state: WorkflowState)

    @Query("DELETE FROM workflow_states WHERE id = :id")
    suspend fun deleteWorkflowState(id: String)

    // --- KNOWLEDGE BASE ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDocument(doc: Document): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDocumentChunks(chunks: List<DocumentChunk>)

    @Query("SELECT * FROM documents ORDER BY addedTime DESC")
    suspend fun getAllDocuments(): List<Document>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocument(id: Long): Document?

    @Query("SELECT * FROM document_chunks")
    suspend fun getAllDocumentChunks(): List<DocumentChunk>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("DELETE FROM document_chunks WHERE documentId = :docId")
    suspend fun deleteDocumentChunks(docId: Long)

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Query("DELETE FROM document_chunks")
    suspend fun clearDocumentChunks()
}
