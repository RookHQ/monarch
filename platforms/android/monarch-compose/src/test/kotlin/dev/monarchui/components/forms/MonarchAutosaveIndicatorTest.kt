@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.monarchui.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatusIndicatorLabels
import dev.monarchui.components.forms.autosaveindicator.MonarchAutosaveIndicator
import dev.monarchui.components.forms.autosaveindicator.MonarchAutosaveIndicatorTokens
import dev.monarchui.components.forms.autosaveindicator.MonarchAutosaveState
import dev.monarchui.components.forms.autosaveindicator.rememberMonarchAutosaveState
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A state that has reached [status] at once, on an unconfined test dispatcher. */
@Composable
private fun autosaveIn(status: MonarchSaveStatus): MonarchAutosaveState<String> = remember {
    val scope = TestScope(UnconfinedTestDispatcher())
    val state = MonarchAutosaveState(
        initialValue = "Draft",
        scope = scope,
        onSave = {
            when (status) {
                MonarchSaveStatus.Saving -> awaitCancellation()
                MonarchSaveStatus.Error -> throw IOException("Offline")
                else -> Unit
            }
        },
        retries = 0,
        initialSavedAt = 0L.takeIf { status == MonarchSaveStatus.Saved },
        clock = { 0L },
    )
    state.enabled = false
    if (status != MonarchSaveStatus.Idle && status != MonarchSaveStatus.Saved) state.update("Draft, edited")
    if (status == MonarchSaveStatus.Saving || status == MonarchSaveStatus.Error) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { state.flush() }
    }
    state
}

internal object MonarchAutosaveIndicatorFixtures : ComponentFixtures {
    override val id = "autosave-indicator"
    override val fixtures = listOf(
        fixture("unsaved and saved") {
            Column {
                MonarchAutosaveIndicator(state = autosaveIn(MonarchSaveStatus.Unsaved))
                MonarchAutosaveIndicator(state = autosaveIn(MonarchSaveStatus.Saved))
            }
        },
        fixture("saving") { MonarchAutosaveIndicator(state = autosaveIn(MonarchSaveStatus.Saving)) },
        fixture("error with retry") { MonarchAutosaveIndicator(state = autosaveIn(MonarchSaveStatus.Error)) },
        fixture("long error message") {
            MonarchAutosaveIndicator(
                state = autosaveIn(MonarchSaveStatus.Error),
                errorMessage = { "Your changes could not be saved because the connection to the server was lost" },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAutosaveIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private class Recorder {
        val saved = mutableListOf<String>()
        var running = 0
        var most = 0
    }

    private fun TestScope.autosave(
        recorder: Recorder,
        saveMillis: Long = 0L,
        fail: () -> Boolean = { false },
    ) = MonarchAutosaveState(
        initialValue = "a",
        scope = backgroundScope,
        onSave = { value ->
            recorder.running++
            recorder.most = maxOf(recorder.most, recorder.running)
            try {
                delay(saveMillis)
                if (fail()) throw IOException("Offline")
                recorder.saved += value
            } finally {
                recorder.running--
            }
        },
        clock = { testScheduler.currentTime },
    )

    @Test
    fun aChangeIsSavedOnceAfterTheDelay() = runTest {
        val recorder = Recorder()
        val state = autosave(recorder)
        assertEquals(MonarchSaveStatus.Idle, state.status)
        state.update("b")
        assertEquals(MonarchSaveStatus.Unsaved, state.status)
        assertTrue(state.dirty)
        advanceTimeBy(999)
        runCurrent()
        assertEquals(emptyList(), recorder.saved)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("b"), recorder.saved)
        assertEquals(MonarchSaveStatus.Saved, state.status)
        assertEquals(1_000L, state.savedAt)
        assertFalse(state.dirty)
    }

    @Test
    fun eachChangeRestartsTheDelayAndOnlyTheLastValueIsSaved() = runTest {
        val recorder = Recorder()
        val state = autosave(recorder)
        state.update("b")
        advanceTimeBy(500)
        state.update("c")
        state.update("c")
        advanceTimeBy(999)
        runCurrent()
        assertEquals(emptyList(), recorder.saved)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("c"), recorder.saved)
    }

    @Test
    fun aChangeBackToTheSavedValueCancelsTheSave() = runTest {
        val recorder = Recorder()
        val state = autosave(recorder)
        state.update("b")
        state.update("a")
        assertEquals(MonarchSaveStatus.Idle, state.status)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(emptyList(), recorder.saved)
    }

    @Test
    fun onlyOneSaveRunsAndAnEditDuringItIsSavedAfterIt() = runTest {
        val recorder = Recorder()
        val state = autosave(recorder, saveMillis = 300)
        state.update("b")
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(MonarchSaveStatus.Saving, state.status)
        state.update("c")
        assertEquals(MonarchSaveStatus.Saving, state.status)
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("b"), recorder.saved)
        assertEquals(MonarchSaveStatus.Unsaved, state.status)
        advanceTimeBy(1_400)
        runCurrent()
        assertEquals(listOf("b", "c"), recorder.saved)
        assertEquals(MonarchSaveStatus.Saved, state.status)
        assertEquals(1, recorder.most)
    }

    @Test
    fun aFailedSaveIsRetriedWithADoublingWaitThenFails() = runTest {
        val recorder = Recorder()
        var attempts = 0
        val state = autosave(recorder, fail = { attempts++ < 10 })
        state.update("b")
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(1, attempts)
        assertEquals(MonarchSaveStatus.Saving, state.status)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, attempts)
        advanceTimeBy(3_998)
        runCurrent()
        assertEquals(2, attempts)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, attempts)
        assertEquals(MonarchSaveStatus.Error, state.status)
        assertIs<IOException>(state.error)
        assertTrue(state.dirty)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(3, attempts)
    }

    @Test
    fun retryAndFlushSaveNow() = runTest {
        val recorder = Recorder()
        var failing = true
        val state = autosave(recorder, fail = { failing })
        state.update("b")
        assertFalse(state.flush())
        failing = false
        // The automatic retry is still waiting; flush saves now and resets it.
        assertTrue(state.flush())
        assertEquals(listOf("b"), recorder.saved)
        state.update("c")
        failing = true
        assertFalse(state.flush())
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(MonarchSaveStatus.Error, state.status)
        failing = false
        state.retry()
        runCurrent()
        assertEquals(listOf("b", "c"), recorder.saved)
        assertNull(state.error)
        assertEquals(MonarchSaveStatus.Saved, state.status)
    }

    @Test
    fun whileDisabledChangesAreOnlyMarkedUnsaved() = runTest {
        val recorder = Recorder()
        val state = autosave(recorder)
        state.enabled = false
        state.update("b")
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(MonarchSaveStatus.Unsaved, state.status)
        assertEquals(emptyList(), recorder.saved)
        state.enabled = true
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(listOf("b"), recorder.saved)
    }

    @Test
    fun theIndicatorShowsTheErrorAndRetrySavesAgain() {
        val scope = TestScope(UnconfinedTestDispatcher())
        var failing = true
        val saved = mutableListOf<String>()
        val state = MonarchAutosaveState(
            initialValue = "a",
            scope = scope,
            onSave = {
                if (failing) throw IOException("Offline")
                saved += it
            },
            retries = 0,
            clock = { 0L },
        )
        state.update("b")
        scope.testScheduler.advanceTimeBy(1_001)
        scope.testScheduler.runCurrent()
        rule.setMonarchContent {
            MonarchAutosaveIndicator(state = state, errorMessage = { "Couldn't save: ${it.message}" })
        }
        rule.onNode(hasContentDescription("Couldn't save: Offline")).assertExists()
        failing = false
        rule.onNodeWithText("Retry").assertTouchTargetAtLeast48dp(rule.density).performClick()
        scope.testScheduler.runCurrent()
        rule.waitForIdle()
        assertEquals(listOf("b"), saved)
        // The polite live region announces the outcome.
        rule.onNode(hasContentDescription("Saved")).assertExists()
    }

    @Test
    fun rememberSavesTheValueOfTheScreenAndTheLabelsApply() {
        var text by mutableStateOf("Draft")
        val saved = mutableListOf<String>()
        lateinit var state: MonarchAutosaveState<String>
        rule.setMonarchContent(fontScale = LargestFontScale) {
            state = rememberMonarchAutosaveState(value = text, onSave = { saved += it }, delayMillis = 60_000)
            MonarchAutosaveIndicator(
                state = state,
                labels = MonarchSaveStatusIndicatorLabels(unsaved = "Not saved yet"),
            )
        }
        assertEquals(MonarchSaveStatus.Idle, state.status)
        text = "Draft, edited"
        rule.waitForIdle()
        assertEquals(MonarchSaveStatus.Unsaved, state.status)
        rule.onNode(hasContentDescription("Not saved yet")).assertExists()
        rule.mainClock.advanceTimeBy(60_000)
        rule.waitForIdle()
        assertEquals(listOf("Draft, edited"), saved)
    }

    @Test
    fun theRules() {
        val t = MonarchAutosaveIndicatorTokens
        assertEquals(2_000L, t.retryWait(2_000L, 1))
        assertEquals(8_000L, t.retryWait(2_000L, 3))
        assertTrue(t.dirty(MonarchSaveStatus.Error))
        assertFalse(t.dirty(MonarchSaveStatus.Saved))
        assertEquals(MonarchSaveStatus.Idle, t.settled(null))
        assertEquals(MonarchSaveStatus.Saved, t.settled(5L))
    }
}
