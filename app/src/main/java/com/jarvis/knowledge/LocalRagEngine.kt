package com.jarvis.knowledge

import android.content.Context
import com.jarvis.memory.Document
import com.jarvis.memory.DocumentChunk
import com.jarvis.memory.MemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln

/**
 * On-device local RAG engine. Uses highly optimized TF-IDF ranking algorithms
 * to retrieve relevant document passages without external server dependencies.
 */
object LocalRagEngine {

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then", "else", "when",
        "at", "by", "for", "with", "about", "against", "between", "into",
        "through", "during", "before", "after", "above", "below", "to",
        "from", "up", "down", "in", "out", "on", "off", "over", "under",
        "again", "further", "then", "once", "here", "there", "when",
        "where", "why", "how", "all", "any", "both", "each", "few",
        "more", "most", "other", "some", "such", "no", "nor", "not",
        "only", "own", "same", "so", "than", "too", "very", "s", "t",
        "can", "will", "just", "don", "should", "now", "is", "am", "are",
        "was", "were", "be", "been", "being", "have", "has", "had",
        "having", "do", "does", "did", "doing", "i", "me", "my", "myself",
        "we", "our", "ours", "ourselves", "you", "your", "yours",
        "yourself", "yourselves", "he", "him", "his", "himself", "she",
        "her", "hers", "herself", "it", "its", "itself", "they", "them",
        "their", "theirs", "themselves"
    )

    /**
     * Splits a document into overlapping chunks and stores both document metadata
     * and text chunks inside Room. Offloaded to Dispatchers.IO.
     */
    suspend fun ingestDocument(context: Context, title: String, uri: String, content: String): Long = withContext(Dispatchers.IO) {
        val db = MemoryDatabase.getDatabase(context)
        val dao = db.memoryDao()

        // 1. Save document metadata
        val docId = dao.saveDocument(Document(title = title, uri = uri, content = content))

        // 2. Perform chunking (chunk size = 500 characters, overlap = 100 characters)
        val chunks = mutableListOf<DocumentChunk>()
        val chunkSize = 500
        val overlap = 100
        var start = 0
        var index = 0

        while (start < content.length) {
            val end = minOf(start + chunkSize, content.length)
            val chunkText = content.substring(start, end)
            val tokens = chunkText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

            chunks.add(
                DocumentChunk(
                    documentId = docId,
                    chunkText = chunkText,
                    tokenCount = tokens,
                    chunkIndex = index++
                )
            )

            if (end == content.length) break
            start += (chunkSize - overlap)
        }

        if (chunks.isNotEmpty()) {
            dao.saveDocumentChunks(chunks)
        }

        docId
    }

    /**
     * Deletes a document and all related text chunks. Offloaded to Dispatchers.IO.
     */
    suspend fun deleteDocument(context: Context, docId: Long) = withContext(Dispatchers.IO) {
        val db = MemoryDatabase.getDatabase(context)
        val dao = db.memoryDao()
        dao.deleteDocument(docId)
        dao.deleteDocumentChunks(docId)
    }

    /**
     * Performs a TF-IDF semantic relevance query over all stored document chunks.
     * Returns the matching text passages sorted in descending order of relevance.
     */
    suspend fun queryRag(context: Context, query: String, topN: Int = 3): List<String> = withContext(Dispatchers.IO) {
        val db = MemoryDatabase.getDatabase(context)
        val dao = db.memoryDao()

        val allChunks = dao.getAllDocumentChunks()
        if (allChunks.isEmpty()) return@withContext emptyList()

        // 1. Tokenize query
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return@withContext emptyList()

        // 2. Precompute statistics for TF-IDF calculations
        val totalChunks = allChunks.size.toDouble()
        val documentFrequency = mutableMapOf<String, Int>()

        for (term in queryTerms) {
            var count = 0
            for (chunk in allChunks) {
                if (tokenize(chunk.chunkText).contains(term)) {
                    count++
                }
            }
            documentFrequency[term] = count
        }

        // 3. Compute TF-IDF score for each chunk
        val chunkScores = mutableListOf<Pair<DocumentChunk, Double>>()
        for (chunk in allChunks) {
            val chunkTerms = tokenize(chunk.chunkText)
            if (chunkTerms.isEmpty()) continue

            var score = 0.0
            for (term in queryTerms) {
                val termFrequencyInChunk = chunkTerms.count { it == term }.toDouble() / chunkTerms.size
                val chunksWithTerm = documentFrequency[term] ?: 0
                val idf = ln(1.0 + (totalChunks / (1.0 + chunksWithTerm)))
                score += termFrequencyInChunk * idf
            }

            if (score > 0.0) {
                chunkScores.add(Pair(chunk, score))
            }
        }

        // 4. Sort and return top relevance passages
        chunkScores.sortByDescending { it.second }
        chunkScores.take(topN).map { it.first.chunkText }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split("[^a-zA-Z0-9']".toRegex())
            .filter { it.isNotBlank() && !stopWords.contains(it) }
    }
}
