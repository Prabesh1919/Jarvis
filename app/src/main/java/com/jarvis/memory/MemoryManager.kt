package com.jarvis.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centered Memory Manager interface. Coordinates read and write operations
 * strictly on background coroutine pools (Dispatchers.IO).
 */
object MemoryManager {

    private var database: MemoryDatabase? = null

    /**
     * Initializes the memory database instance. Call this inside Application or launch MainActivity onCreate().
     */
    fun initialize(context: Context) {
        if (database == null) {
            database = MemoryDatabase.getDatabase(context)
        }
    }

    private fun getDao(): MemoryDao {
        return database?.memoryDao() ?: throw IllegalStateException("MemoryManager must be initialized before accessing storage.")
    }

    /**
     * Appends a message to the conversation history. Offloaded to Dispatchers.IO.
     */
    suspend fun saveMessage(role: String, content: String, contextSummary: String? = null) = withContext(Dispatchers.IO) {
        val message = ConversationMessage(role = role, content = content, contextSummary = contextSummary)
        getDao().insertMessage(message)
    }

    /**
     * Retrieves the latest N messages from conversation history. Offloaded to Dispatchers.IO.
     */
    suspend fun getRecentMessages(limit: Int): List<ConversationMessage> = withContext(Dispatchers.IO) {
        getDao().getRecentMessages(limit)
    }

    /**
     * Retrieves all conversation messages. Offloaded to Dispatchers.IO.
     */
    suspend fun getFullHistory(): List<ConversationMessage> = withContext(Dispatchers.IO) {
        getDao().getFullHistory()
    }

    /**
     * Erases all logs in the chat history. Offloaded to Dispatchers.IO.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        getDao().clearHistory()
    }

    /**
     * Stores or replaces a key-value setting/relationship parameter. Offloaded to Dispatchers.IO.
     */
    suspend fun savePreference(key: String, value: String) = withContext(Dispatchers.IO) {
        val preference = SemanticPreference(key = key, value = value)
        getDao().savePreference(preference)
    }

    /**
     * Retrieves a saved preference/parameter value by key. Offloaded to Dispatchers.IO.
     */
    suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        getDao().getPreference(key)?.value
    }

    /**
     * Removes a preference/parameter value by key. Offloaded to Dispatchers.IO.
     */
    suspend fun deletePreference(key: String) = withContext(Dispatchers.IO) {
        getDao().deletePreference(key)
    }

    /**
     * Erases both preferences and conversation tables safely. Offloaded to Dispatchers.IO.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        getDao().clearHistory()
        getDao().clearPreferences()
    }

    // --- WORKFLOW STATES ---

    /**
     * Saves or replaces a workflow state in the database. Offloaded to Dispatchers.IO.
     */
    suspend fun saveWorkflowState(state: WorkflowState) = withContext(Dispatchers.IO) {
        getDao().saveWorkflowState(state)
    }

    /**
     * Retrieves a workflow state by ID. Offloaded to Dispatchers.IO.
     */
    suspend fun getWorkflowState(id: String): WorkflowState? = withContext(Dispatchers.IO) {
        getDao().getWorkflowState(id)
    }

    /**
     * Retrieves all saved workflow states. Offloaded to Dispatchers.IO.
     */
    suspend fun getAllWorkflowStates(): List<WorkflowState> = withContext(Dispatchers.IO) {
        getDao().getAllWorkflowStates()
    }

    /**
     * Deletes a workflow state by ID. Offloaded to Dispatchers.IO.
     */
    suspend fun deleteWorkflowState(id: String) = withContext(Dispatchers.IO) {
        getDao().deleteWorkflowState(id)
    }
}
