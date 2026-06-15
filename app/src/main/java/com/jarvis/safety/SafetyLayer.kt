package com.jarvis.safety

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton safety manager that controls automation permissions and coordinates emergency halts.
 * Fully thread-safe, non-blocking, and idempotent.
 */
object SafetyLayer {
    private val _isAutomationAllowed = MutableStateFlow(true)
    val isAutomationAllowed: StateFlow<Boolean> = _isAutomationAllowed.asStateFlow()

    // Thread-safe map to track running automation jobs
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * Registers an automation job to track and manage its lifecycle.
     * If automation is currently disabled, the job is immediately cancelled.
     */
    fun registerJob(taskId: String, job: Job) {
        if (!_isAutomationAllowed.value) {
            job.cancel()
            return
        }
        activeJobs[taskId] = job
        job.invokeOnCompletion {
            activeJobs.remove(taskId)
        }
    }

    /**
     * Unregisters an automation job when it finishes naturally.
     */
    fun unregisterJob(taskId: String) {
        activeJobs.remove(taskId)
    }

    /**
     * Sets the state of automation allowance. If set to false, it triggers an emergency stop.
     */
    fun setAutomationAllowed(allowed: Boolean) {
        _isAutomationAllowed.value = allowed
        if (!allowed) {
            emergencyStop()
        }
    }

    /**
     * Globally accessible kill switch. Instantly cancels all active coroutines/jobs
     * and sets isAutomationAllowed to false. Idempotent and thread-safe.
     */
    @Synchronized
    fun emergencyStop() {
        _isAutomationAllowed.value = false
        val iterator = activeJobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.cancel() // Cancel the coroutine job
            iterator.remove()
        }
    }
}
