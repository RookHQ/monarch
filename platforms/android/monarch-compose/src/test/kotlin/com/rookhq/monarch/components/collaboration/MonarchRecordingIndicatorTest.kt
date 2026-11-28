package com.rookhq.monarch.components.collaboration

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.recordingindicator.MonarchRecordingIndicator
import com.rookhq.monarch.components.collaboration.recordingindicator.MonarchRecordingIndicatorLabels
import com.rookhq.monarch.components.collaboration.recordingindicator.MonarchRecordingIndicatorTokens
import com.rookhq.monarch.components.collaboration.recordingindicator.MonarchRecordingIndicatorVariant
import com.rookhq.monarch.components.collaboration.recordingindicator.RecordingChange
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchRecordingIndicatorFixtures : ComponentFixtures {
    override val id = "recording-indicator"
    override val fixtures = MonarchRecordingIndicatorVariant.entries.flatMap { variant ->
        listOf(
            fixture("recording with time and stop, ${variant.name}") {
                MonarchRecordingIndicator(recording = true, variant = variant, elapsedMillis = 83_000L, onStop = {})
            },
            fixture("paused, ${variant.name}") {
                MonarchRecordingIndicator(recording = true, variant = variant, paused = true, elapsedMillis = 83_000L)
            },
        )
    } + listOf(
        fixture("without time") { MonarchRecordingIndicator(recording = true) },
        fixture("more than an hour") { MonarchRecordingIndicator(recording = true, elapsedMillis = 3_723_000L) },
        fixture("long labels") {
            MonarchRecordingIndicator(
                recording = true,
                paused = true,
                elapsedMillis = 5_000L,
                onStop = {},
                labels = MonarchRecordingIndicatorLabels(paused = "Aufzeichnung angehalten"),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRecordingIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theBadgeIsOneElementReadAsRecordingWithTheTimeInWords() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchRecordingIndicator(recording = true, elapsedMillis = 65_000L) }
        rule.mainClock.advanceTimeByFrame()
        val badge = rule.onNodeWithContentDescription("Recording")
        assertEquals(MonarchRecordingIndicatorTokens.spokenTime(65_000L), badge.stateDescription())
    }

    @Test
    fun aPausedBadgeSaysSo() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchRecordingIndicator(recording = true, paused = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Recording, Paused").assertExists()
    }

    @Test
    fun nothingIsDrawnWhileNotRecording() {
        rule.setMonarchContent { MonarchRecordingIndicator(recording = false, onStop = {}) }
        rule.onAllNodesWithContentDescription("Recording").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Stop recording").assertCountEquals(0)
    }

    @Test
    fun startPauseResumeAndStopAreAnnouncedOnceButNotTheFirstState() {
        var recording by mutableStateOf(true)
        var paused by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchRecordingIndicator(recording = recording, paused = paused) }
        rule.mainClock.advanceTimeByFrame()
        val announcer = rule.onNode(liveRegion)
        announcer.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))

        fun expect(text: String) {
            Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeBy(FRAMES)
            val said = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text))
            rule.onNode(liveRegion).assert(said)
        }
        paused = true
        expect("Recording paused")
        paused = false
        expect("Recording resumed")
        recording = false
        expect("Recording stopped")
        recording = true
        expect("Recording started")
    }

    @Test
    fun theStopButtonIsNamedAndStopsRecording() {
        var stops = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchRecordingIndicator(recording = true, onStop = { stops++ }) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Stop recording")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, stops)
    }

    @Test
    fun everyStringCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                MonarchRecordingIndicator(
                    recording = true,
                    onStop = {},
                    labels = MonarchRecordingIndicatorLabels(recording = "Aufnahme", stop = "Aufnahme beenden"),
                )
                MonarchRecordingIndicator(
                    recording = true,
                    paused = true,
                    labels = MonarchRecordingIndicatorLabels(recording = "Aufnahme", paused = "Pausiert"),
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Aufnahme").assertExists()
        rule.onNodeWithContentDescription("Aufnahme beenden").assertExists()
        rule.onNodeWithContentDescription("Aufnahme, Pausiert").assertExists()
    }

    @Test
    fun theBadgeComesBeforeTheStopButtonInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) { MonarchRecordingIndicator(recording = true, onStop = {}) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithContentDescription("Recording")
                .assertIsBefore(rule.onNodeWithContentDescription("Stop recording"), direction)
        }
    }

    @Test
    fun theBadgeGrowsWithTheFontSizeSetting() {
        val environment = MonarchTestEnvironment()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(environment) {
            MonarchRecordingIndicator(recording = true, modifier = Modifier.testTag("rec"), elapsedMillis = 5_000L)
        }
        rule.mainClock.advanceTimeByFrame()
        val normal = rule.onNodeWithTag("rec").fetchSemanticsNode().size
        environment.fontScale = LargestFontScale
        rule.mainClock.advanceTimeByFrame()
        val large = rule.onNodeWithTag("rec").fetchSemanticsNode().size
        assertTrue(large.width > normal.width && large.height > normal.height, "$normal grows to $large")
    }

    @Test
    fun onlyRealChangesAreAnnounced() {
        val t = MonarchRecordingIndicatorTokens
        assertEquals(RecordingChange.Started, t.change(false, false, recording = true, paused = false))
        assertEquals(RecordingChange.Stopped, t.change(true, true, recording = false, paused = true))
        assertEquals(RecordingChange.Paused, t.change(true, false, recording = true, paused = true))
        assertEquals(RecordingChange.Resumed, t.change(true, true, recording = true, paused = false))
        assertNull(t.change(false, false, recording = false, paused = true))
        assertNull(t.change(true, false, recording = true, paused = false))
    }

    @Test
    fun theTimeComesFromElapsedFirstThenSince() {
        val t = MonarchRecordingIndicatorTokens
        assertEquals(42_000L, t.recordedMillis(since = 1_000L, elapsedMillis = 42_000L, now = 99_000L))
        assertEquals(98_000L, t.recordedMillis(since = 1_000L, elapsedMillis = null, now = 99_000L))
        assertEquals(0L, t.recordedMillis(since = 5_000L, elapsedMillis = null, now = 1_000L))
        assertEquals(0L, t.recordedMillis(since = null, elapsedMillis = null, now = 1_000L))
        assertTrue(t.showsTime(showElapsed = null, since = 1L, elapsedMillis = null))
        assertFalse(t.showsTime(showElapsed = null, since = null, elapsedMillis = null))
        assertFalse(t.showsTime(showElapsed = false, since = 1L, elapsedMillis = null))
        assertTrue(t.ticks(recording = true, paused = false, since = 1L, elapsedMillis = null))
        assertFalse(t.ticks(recording = true, paused = true, since = 1L, elapsedMillis = null))
        assertFalse(t.ticks(recording = true, paused = false, since = 1L, elapsedMillis = 5L))
    }

    @Test
    fun theTimeIsWrittenAsAClockAndSpokenInWords() {
        val t = MonarchRecordingIndicatorTokens
        assertEquals(DateUtils.formatElapsedTime(65L), t.clock(65_400L))
        assertEquals("1 minute, 5 seconds", t.spokenTime(65_000L, Locale.US))
        assertEquals("1 hour, 2 minutes, 3 seconds", t.spokenTime(3_723_000L, Locale.US))
        assertEquals("1 hour", t.spokenTime(3_600_000L, Locale.US))
        assertEquals("0 seconds", t.spokenTime(400L, Locale.US))
    }

    @Test
    fun coloursComeFromTheDangerAndInverseRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val subtle = MonarchRecordingIndicatorTokens.colors(MonarchRecordingIndicatorVariant.Subtle, colors)
            assertEquals(colors.dangerSubtle, subtle.container, theme.id)
            assertEquals(colors.dangerStrong, subtle.content, theme.id)
            assertEquals(colors.dangerBorder, subtle.border, theme.id)
            val solid = MonarchRecordingIndicatorTokens.colors(MonarchRecordingIndicatorVariant.Solid, colors)
            assertEquals(colors.bgInverse, solid.container, theme.id)
            assertEquals(colors.textInverse, solid.content, theme.id)
            assertNull(solid.border, theme.id)
            assertEquals(
                colors.dangerSolid,
                MonarchRecordingIndicatorTokens.dotColor(MonarchRecordingIndicatorVariant.Subtle, colors),
            )
            assertEquals(
                colors.dangerStrongInverse,
                MonarchRecordingIndicatorTokens.dotColor(MonarchRecordingIndicatorVariant.Solid, colors),
            )
            val stop = MonarchRecordingIndicatorTokens.stopColors(colors)
            assertEquals(colors.bgSurface, stop.container, theme.id)
            assertEquals(colors.dangerStrong, stop.content, theme.id)
            assertEquals(colors.borderDefault, stop.border, theme.id)
        }
    }

    private companion object {
        const val FRAMES = 64L
    }
}
