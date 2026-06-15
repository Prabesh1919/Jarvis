package com.jarvis.personalization

import com.jarvis.memory.MemoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Learns user behavioral patterns by tracking consecutive tool execution sequences.
 * Suggests saved routines (macros) when recurring patterns are detected.
 */
object RoutineLearner {

    private const val PATTERN_THRESHOLD = 3 // Number of times a sequence must repeat to be suggested
    private const val MAX_SEQUENCE_LENGTH = 5

    // Tracks the rolling history of recently executed tool IDs
    private val recentExecutions = mutableListOf<String>()

    // Detected recurring patterns mapped to their occurrence count
    private val patternCounts = mutableMapOf<String, Int>()

    private val _suggestedRoutines = MutableStateFlow<List<RoutineSuggestion>>(emptyList())
    val suggestedRoutines: StateFlow<List<RoutineSuggestion>> = _suggestedRoutines.asStateFlow()

    /**
     * Records a tool execution. Should be called after each successful tool execution.
     * Analyzes trailing sequences for repetition patterns.
     */
    fun recordExecution(toolId: String) {
        recentExecutions.add(toolId)

        // Limit rolling history to prevent unbounded memory growth
        if (recentExecutions.size > 100) {
            recentExecutions.removeAt(0)
        }

        // Analyze trailing subsequences for repeating patterns
        analyzePatterns()
    }

    /**
     * Scans the recent execution history for repeating subsequences.
     * When a subsequence appears >= PATTERN_THRESHOLD times, it becomes a suggestion.
     */
    private fun analyzePatterns() {
        if (recentExecutions.size < 2) return

        for (len in 2..minOf(MAX_SEQUENCE_LENGTH, recentExecutions.size)) {
            val lastSequence = recentExecutions.takeLast(len)
            val key = lastSequence.joinToString("->")

            val count = (patternCounts[key] ?: 0) + 1
            patternCounts[key] = count

            if (count >= PATTERN_THRESHOLD) {
                val existing = _suggestedRoutines.value.toMutableList()
                if (existing.none { it.sequenceKey == key }) {
                    existing.add(
                        RoutineSuggestion(
                            sequenceKey = key,
                            toolSequence = lastSequence,
                            occurrences = count,
                            label = "Auto-detected routine: ${lastSequence.joinToString(" → ")}"
                        )
                    )
                    _suggestedRoutines.value = existing
                }
            }
        }
    }

    /**
     * Persists a suggested routine as a user preference for future quick-access.
     */
    suspend fun saveRoutine(suggestion: RoutineSuggestion) = withContext(Dispatchers.IO) {
        MemoryManager.savePreference(
            key = "routine:${suggestion.sequenceKey}",
            value = suggestion.toolSequence.joinToString(",")
        )
    }

    /**
     * Loads all previously saved routines from persistent storage.
     */
    suspend fun loadSavedRoutines(): List<String> = withContext(Dispatchers.IO) {
        // Return all preference keys starting with "routine:"
        // This is a simplified approach; production would use a dedicated query
        val routineKey = MemoryManager.getPreference("routine:saved_keys")
        routineKey?.split(",") ?: emptyList()
    }

    /**
     * Clears all learned patterns and suggestions. Useful for privacy resets.
     */
    fun reset() {
        recentExecutions.clear()
        patternCounts.clear()
        _suggestedRoutines.value = emptyList()
    }
}

/**
 * Data class representing a detected recurring tool sequence.
 */
data class RoutineSuggestion(
    val sequenceKey: String,
    val toolSequence: List<String>,
    val occurrences: Int,
    val label: String
)
