package com.ramesh.sms.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs [task] one at a time and coalesces requests that arrive while it is running.
 *
 * The inbox used to cancel the in-flight reload whenever another refresh was requested
 * (a new SMS, a rewritten filter verdict, a tab change). The cancelled reload never published
 * its result, so the list only caught up after the process was restarted. Here a request that
 * arrives during a run marks the run as stale and is served by exactly one follow-up run —
 * nothing is dropped and nothing is cancelled.
 */
internal class RefreshScheduler(
    private val scope: CoroutineScope,
    private val task: suspend () -> Unit
) {

    private var worker: Job? = null
    private var pending = false

    /** True while [task] is running; used by callers that only want to know about activity. */
    val isRunning: Boolean
        get() = worker?.isActive == true

    /**
     * Requests a run. Safe to call from the main thread only, which is where view models
     * receive their callbacks.
     */
    fun request() {
        if (isRunning) {
            pending = true
            return
        }
        worker = scope.launch {
            do {
                pending = false
                try {
                    task()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // A failed refresh must not kill the queue; the next request retries.
                }
            } while (pending)
        }
    }

    /** Cancels the queue, e.g. from `onCleared`. */
    fun cancel() {
        pending = false
        worker?.cancel()
        worker = null
    }
}
