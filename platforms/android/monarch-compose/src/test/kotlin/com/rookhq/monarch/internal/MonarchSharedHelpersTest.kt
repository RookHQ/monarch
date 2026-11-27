package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.monarchMinimumTouchTargetWithVisibleIntrinsics
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The shared helpers in internal/ that replace per-component copies. */
@RunWith(RobolectricTestRunner::class)
class MonarchSharedHelpersTest {
    @get:Rule
    val rule = createComposeRule()

    private val now = 1_800_000_000_000L
    private val minute = 60_000L
    private val day = 86_400_000L

    @Test
    fun theClockTicksOnWholeIntervalsAndAFixedTimeStopsIt() {
        assertEquals(minute, MonarchClock.untilNextTick(now, minute))
        assertEquals(minute - 15_000L, MonarchClock.untilNextTick(now + 15_000L, minute))
        var time = now
        var read = 0L
        var fixed by mutableStateOf<Long?>(null)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { read = rememberMonarchClock(fixed = fixed, currentTime = { time }) }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(now, read)
        time = now + minute
        rule.mainClock.advanceTimeBy(minute)
        rule.mainClock.advanceTimeByFrame()
        assertEquals(now + minute, read)
        rule.runOnUiThread { fixed = 42L }
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(42L, read)
    }

    @Test
    fun theRelativeClockTicksEveryFiveSecondsWhileATimeIsFresh() {
        // `now` is a whole minute, so whole 5-second ticks fall on now + 5 s, now + 10 s.
        val saved = now - 2_000L
        var time = now
        var read = 0L
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { read = rememberMonarchRelativeClock(listOf(saved), currentTime = { time }) }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(now, read)
        time = now + 5_000L
        rule.mainClock.advanceTimeBy(5_000L)
        rule.mainClock.advanceTimeByFrame()
        assertEquals(now + 5_000L, read, "A time in its first minute is redrawn every 5 seconds")
    }

    @Test
    fun theAnnouncerReadsTheSameTextAgainOnRepeat() {
        val announcer = MonarchAnnouncer()
        rule.setMonarchContent { MonarchAnnouncerNode(announcer, Modifier.testTag("live")) }
        val node = rule.onNodeWithTag("live")
        assertEquals(
            LiveRegionMode.Polite,
            node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion),
        )
        assertNull(node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
        announcer.announce("Copied")
        rule.waitForIdle()
        val first = node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertEquals("Copied", first)
        announcer.announce("Copied")
        rule.waitForIdle()
        val second = node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertNotEquals(first, second, "A repeat changes the node, so TalkBack reads it again")
        assertEquals("Copied", second.replace(MonarchAnnouncer.REPEAT_MARK, ""))
        announcer.clear()
        rule.waitForIdle()
        assertNull(node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun theLiveStatusLineIsAlwaysPresentAndPolite() {
        var text by mutableStateOf<String?>(null)
        rule.setMonarchContent { MonarchLiveStatus(text = text, modifier = Modifier.testTag("status")) }
        val node = rule.onNodeWithTag("status")
        assertEquals(LiveRegionMode.Polite, node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion))
        text = "3 results"
        rule.waitForIdle()
        assertEquals(listOf("3 results"), node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun strokeGlyphsAreOnTheSixteenUnitGrid() {
        val cross = MonarchStrokeGlyphGeometry.Cross.flatMap { listOf(it.first, it.second) }
        val points = MonarchStrokeGlyphGeometry.Check + cross
        points.forEach { point ->
            assertTrue(point.x in 0f..MonarchStrokeGlyphGeometry.GRID && point.y in 0f..MonarchStrokeGlyphGeometry.GRID)
        }
        rule.setMonarchContent {
            MonarchStrokeGlyph.entries.forEach { glyph ->
                MonarchStrokeGlyphMark(glyph, MonarchTheme.colors.textDefault, Modifier.size(16.dp).testTag(glyph.name))
            }
        }
        MonarchStrokeGlyph.entries.forEach { rule.onNodeWithTag(it.name).assertExists() }
    }

    @Test
    fun utcDatesAreWrittenInUtcWhateverTheDeviceZone() {
        val saved = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            val march4 = 1_772_582_400_000L // 2026-03-04T00:00:00Z
            assertEquals("Mar 4, 2026", MonarchUtcDates.dateText(march4, Locale.US))
            assertEquals("Mar 4", MonarchUtcDates.monthDay(Locale.US)(march4))
            assertEquals(march4, MonarchUtcDates.startOfDay(march4 + 13 * 3_600_000L))
            assertEquals(-1L, MonarchUtcDates.daysBetween(march4, march4 - 1))
            assertEquals(2L, MonarchUtcDates.daysBetween(march4, march4 + 2 * day))
        } finally {
            java.util.TimeZone.setDefault(saved)
        }
    }

    @Test
    fun everyPresenceHasAName() {
        val names = mutableMapOf<MonarchPresenceStatus, String>()
        rule.setMonarchContent { MonarchPresenceStatus.entries.forEach { names[it] = monarchPresenceName(it) } }
        rule.waitForIdle()
        assertEquals("Online", names[MonarchPresenceStatus.Online])
        assertEquals("Do not disturb", names[MonarchPresenceStatus.DoNotDisturb])
        assertEquals(MonarchPresenceStatus.entries.size, names.values.toSet().size)
    }

    @Test
    fun numbersFollowTheLocaleAndAMissingValueIsADash() {
        assertEquals("1,234.5", MonarchNumberText.number(Locale.US)(1234.5))
        assertEquals("1.234,5", MonarchNumberText.number(Locale.GERMANY)(1234.5))
        assertEquals("12.5%", MonarchNumberText.percent(Locale.US)(0.125))
        assertEquals("1.2K", MonarchNumberText.compact(Locale.US)(1200.0))
        assertEquals(MonarchNumberText.EMPTY_MARK, MonarchNumberText.orEmpty(null, MonarchNumberText.number(Locale.US)))
    }

    @Test
    fun theEmptyValueIsReadAsAWordAsTheNodeText() {
        rule.setMonarchContent {
            MonarchEmptyValueText(
                spokenLabel = "Not set",
                style = MonarchTheme.typography.body,
                color = Color.Unspecified,
                modifier = Modifier.testTag("empty"),
            )
        }
        val config = rule.onNodeWithTag("empty").fetchSemanticsNode().config
        assertEquals("Not set", config[SemanticsProperties.Text].single().text)
        assertNull(config.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun freshIdsSkipTheTakenOnes() {
        assertEquals("level-1", monarchFreshId("level", emptyList()))
        assertEquals("level-3", monarchFreshId("level", listOf("level-1", "level-2", "other")))
    }

    @Test
    fun focusAfterRemovalGoesToTheNextThenThePreviousItem() {
        val ids = listOf("a", "b", "c")
        assertEquals("c", MonarchFocusAfterRemoval.target(ids, "b"))
        assertEquals("b", MonarchFocusAfterRemoval.target(ids, "c"))
        assertNull(MonarchFocusAfterRemoval.target(listOf("a"), "a"))
        assertNull(MonarchFocusAfterRemoval.target(ids, "z"))
    }

    @Test
    fun theActionRunnerTracksBusyAndFailedAndRunsOnceAtATime() {
        lateinit var runner: MonarchActionRunner
        rule.setMonarchContent { runner = rememberMonarchActionRunner() }
        val gate = CompletableDeferred<Unit>()
        rule.runOnIdle { assertTrue(runner.run { gate.await() }) }
        rule.runOnIdle {
            assertTrue(runner.busy)
            assertFalse(runner.run { }, "A second run is refused while busy")
            gate.complete(Unit)
        }
        rule.waitForIdle()
        assertEquals(MonarchActionState.Idle, runner.state)
        rule.runOnIdle { runner.run { error("offline") } }
        rule.waitForIdle()
        assertEquals(MonarchActionState.Failed, runner.state)
        runner.reset()
        assertEquals(MonarchActionState.Idle, runner.state)
    }

    @Test
    fun theVisibleIntrinsicsTargetReportsTheVisibleHeight() {
        rule.setMonarchContent {
            Row(Modifier.height(IntrinsicSize.Min).testTag("row")) {
                Box(Modifier.monarchMinimumTouchTargetWithVisibleIntrinsics().size(24.dp))
            }
        }
        rule.onNodeWithTag("row").assertHeightIsEqualTo(24.dp)
    }
}
