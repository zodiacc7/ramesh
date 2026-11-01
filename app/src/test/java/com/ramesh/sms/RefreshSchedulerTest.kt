package com.ramesh.sms

import com.ramesh.sms.ui.viewmodel.RefreshScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The inbox refresh queue.
 *
 * Regression covered here: a refresh requested while another one was running used to cancel
 * the first run, so its result never reached the UI ("new SMS only shows up after a restart").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshSchedulerTest {

    @Test
    fun runsTheTaskImmediately() = runTest {
        var runs = 0
        val scheduler = RefreshScheduler(StandardTestDispatcher(testScheduler)) { runs++ }

        scheduler.request()
        advanceUntilIdle()

        assertEquals(1, runs)
        assertFalse(scheduler.isRunning)
    }

    @Test
    fun aRequestDuringARunIsServedByASecondRun() = runTest {
        var runs = 0
        val gate = CompletableDeferred<Unit>()
        val scheduler = RefreshScheduler(StandardTestDispatcher(testScheduler)) {
            runs++
            if (runs == 1) gate.await()
        }

        scheduler.request()
        advanceUntilIdle()
        assertTrue(scheduler.isRunning)

        // Two requests while the first run is blocked.
        scheduler.request()
        scheduler.request()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("coalesced into exactly one follow-up run", 2, runs)
    }

    @Test
    fun theRunningTaskIsNeverCancelled() = runTest {
        var completedRuns = 0
        val gate = CompletableDeferred<Unit>()
        val scheduler = RefreshScheduler(StandardTestDispatcher(testScheduler)) {
            if (completedRuns == 0) gate.await()
            completedRuns++
        }

        scheduler.request()
        advanceUntilIdle()
        scheduler.request()
        advanceUntilIdle()
        assertEquals("first run must still be alive", 0, completedRuns)
        assertTrue(scheduler.isRunning)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, completedRuns)
    }

    @Test
    fun aFailingTaskDoesNotStopLaterRuns() = runTest {
        var attempts = 0
        val scheduler = RefreshScheduler(StandardTestDispatcher(testScheduler)) {
            attempts++
            if (attempts == 1) error("boom")
        }

        scheduler.request()
        advanceUntilIdle()
        scheduler.request()
        advanceUntilIdle()

        assertEquals(2, attempts)
    }

    @Test
    fun multipleRequestsAfterIdleRunOnce() = runTest {
        var runs = 0
        val scheduler = RefreshScheduler(StandardTestDispatcher(testScheduler)) { runs++ }

        scheduler.request()
        scheduler.request()
        scheduler.request()
        advanceUntilIdle()

        assertEquals(1, runs)
    }
}
