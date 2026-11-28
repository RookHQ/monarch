package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.workflow.runningtimer.MonarchRunningTimer
import dev.monarchui.components.workflow.runningtimer.MonarchRunningTimerLabels
import dev.monarchui.components.workflow.runningtimer.MonarchRunningTimerTokens
import dev.monarchui.components.workflow.runningtimer.MonarchRunningTimerVariant
import dev.monarchui.components.workflow.runningtimer.RunningTimerStopError
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Wednesday 4 November 2026, 09:30 UTC. */
private const val Start = 1_793_784_600_000L
private const val Now = Start + 249_000L
private const val Hour = 3_600_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

internal object MonarchRunningTimerFixtures : ComponentFixtures {
    override val id = "running-timer"
    override val fixtures = listOf(
        fixture("panel, running, every action") {
            MonarchRunningTimer(
                startedAt = Start,
                label = "ENG-142",
                now = Now,
                onStop = {},
                onStopAt = {},
                onDiscard = {},
            )
        },
        fixture("panel, stopped, start disabled") {
            MonarchRunningTimer(startedAt = null, now = Now, startEnabled = false, onStart = {})
        },
        fixture("panel, long running") {
            MonarchRunningTimer(startedAt = Start, now = Start + 13 * Hour, onStop = {})
        },
        fixture("indicator, running, opens, with icon") {
            Column {
                MonarchRunningTimer(
                    startedAt = Start,
                    variant = MonarchRunningTimerVariant.Indicator,
                    label = "ENG-142",
                    now = Now,
                    menuIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                    onStop = {},
                    onDiscard = {},
                    onOpen = {},
                )
                MonarchRunningTimer(
                    startedAt = Start,
                    variant = MonarchRunningTimerVariant.Indicator,
                    now = Start + 13 * Hour,
                    onStop = {},
                    onStopAt = {},
                )
            }
        },
        fixture("indicator, stopped and shown, pending") {
            Column {
                MonarchRunningTimer(
                    startedAt = null,
                    variant = MonarchRunningTimerVariant.Indicator,
                    showWhenStopped = true,
                    now = Now,
                )
                MonarchRunningTimer(startedAt = null, now = Now, pending = true, onStart = {})
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRunningTimerTest {
    @get:Rule
    val rule = createComposeRule()

    private fun menuItem(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(isPopup()))

    @Test
    fun theClockIsReadInWordsWithItsState() {
        rule.setMonarchContent { MonarchRunningTimer(startedAt = Start, label = "ENG-142", now = Now, onStop = {}) }
        rule.onNodeWithText("0:04:09", useUnmergedTree = true).assertExists()
        val clock = rule.onNodeWithContentDescription("ENG-142, 4 minutes, 9 seconds")
        assertEquals("Timer running", clock.stateDescription())
    }

    @Test
    fun aStoppedPanelOffersStartAndItCallsBack() {
        var started = 0
        rule.setMonarchContent { MonarchRunningTimer(startedAt = null, now = Now, onStart = { started++ }) }
        rule.onNodeWithText("0:00:00", useUnmergedTree = true).assertExists()
        assertEquals("Timer stopped", rule.onNodeWithContentDescription("0 seconds").stateDescription())
        rule.onNodeWithText("Start timer").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(1, started)
    }

    @Test
    fun startCanBeDisabledAndPendingDisablesTheOtherButtons() {
        rule.setMonarchContent {
            Column {
                MonarchRunningTimer(startedAt = null, now = Now, startEnabled = false, onStart = {})
                MonarchRunningTimer(startedAt = Start, now = Now, pending = true, onStop = {}, onStopAt = {})
            }
        }
        rule.onNodeWithText("Start timer").assertIsNotEnabled()
        rule.onNodeWithText("Stop").assertIsNotEnabled()
        rule.onNodeWithText("I stopped earlier").assertIsNotEnabled()
    }

    @Test
    fun aFailedStopShowsTheErrorAndAnUnnamedOneTheBuiltInMessage() {
        rule.setMonarchContent {
            Column {
                MonarchRunningTimer(startedAt = Start, now = Now, onStop = { error("Server down") })
                MonarchRunningTimer(
                    startedAt = Start,
                    now = Now,
                    labels = MonarchRunningTimerLabels(stop = "Halt"),
                    onStop = { throw IllegalStateException() },
                )
            }
        }
        rule.onNodeWithText("Stop").performClick()
        rule.onNodeWithText("Halt").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Server down")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("That did not work. Try again.").assertExists()
    }

    @Test
    fun aLongRunningTimerWarnsInWords() {
        rule.setMonarchContent {
            MonarchRunningTimer(startedAt = Start, now = Start + 2 * Hour, warnAfterMillis = Hour, onStop = {})
        }
        rule.onNodeWithText("over 1h", substring = true).assertExists()
    }

    @Test
    fun discardIsConfirmedFirst() {
        var discarded = 0
        rule.setMonarchContent { MonarchRunningTimer(startedAt = Start, now = Now, onDiscard = { discarded++ }) }
        rule.onNodeWithText("Discard without logging").performClick()
        rule.onNodeWithText("Discard the running timer?").assertExists()
        rule.onNodeWithText("Discard").performClick()
        rule.waitForIdle()
        assertEquals(1, discarded)
        rule.onNodeWithText("Discard the running timer?").assertDoesNotExist()
    }

    @Test
    fun stoppingEarlierSuggestsAnHourAfterTheStartAndSavesIt() {
        var stoppedAt: Long? = null
        rule.setMonarchContent {
            MonarchRunningTimer(
                startedAt = Start,
                now = Start + 3 * Hour,
                timeZone = Utc.id,
                onStopAt = { stoppedAt = it },
            )
        }
        rule.onNodeWithText("I stopped earlier").performClick()
        rule.onNodeWithText("When did you stop?").assertExists()
        rule.onNodeWithText("That is 1h 00m of work.").assertExists()
        rule.onNodeWithText("Save this time").assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals(Start + Hour, stoppedAt)
    }

    @Test
    fun aChosenTimeIsTheLatestSuchMomentAndIsChecked() {
        val tokens = MonarchRunningTimerTokens
        // 09:30 UTC start; at 01:00 the next day, 23:15 means the evening before.
        val now = Start + 15 * Hour + 30 * 60_000L
        assertEquals(Start + 13 * Hour + 45 * 60_000L, tokens.resolve(23, 15, now, Utc))
        assertEquals(Start - 24 * Hour + 30 * 60_000L, tokens.resolve(10, 0, Start + 15 * 60_000L, Utc))
        assertEquals(RunningTimerStopError.TooEarly, tokens.stopError(Start + 30_000L, Start, now))
        assertEquals(RunningTimerStopError.Future, tokens.stopError(now + 1, Start, now))
        assertNull(tokens.stopError(Start + Hour, Start, now))
        assertEquals(Start + Hour, tokens.suggestion(Start, Start + 3 * Hour))
        assertEquals(Start + 600_000L, tokens.suggestion(Start, Start + 600_000L))
    }

    @Test
    fun clockAndDurationFormats() {
        val tokens = MonarchRunningTimerTokens
        assertEquals("0:04:09", tokens.clock(249))
        assertEquals("100:00:00", tokens.clock(360_000))
        assertEquals(0, tokens.elapsedSeconds(Start + 5_000L, Start))
        assertEquals("2h 05m", tokens.hoursMinutes(125))
        assertEquals("12h", tokens.hoursMinutes(720, dropZeroMinutes = true))
        assertEquals("1 hour, 0 minutes, 5 seconds", tokens.spoken(3_605, Locale.US))
    }

    @Test
    fun theIndicatorOpensStopsAndHasAMenu() {
        var opened = 0
        var stopped = 0
        rule.setMonarchContent {
            MonarchRunningTimer(
                startedAt = Start,
                variant = MonarchRunningTimerVariant.Indicator,
                label = "ENG-142",
                now = Now,
                onStop = { stopped++ },
                onDiscard = {},
                onOpen = { opened++ },
            )
        }
        rule.onNodeWithContentDescription("ENG-142, 4 minutes, 9 seconds")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Stop timer and log time")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf(1, 1), listOf(opened, stopped))
        rule.onNodeWithContentDescription("More timer actions")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        menuItem("Discard without logging").assertExists()
    }

    @Test
    fun aStoppedIndicatorIsHiddenUnlessAskedButStartAndStopAreAnnounced() {
        var startedAt by mutableStateOf<Long?>(null)
        rule.setMonarchContent {
            MonarchRunningTimer(startedAt = startedAt, variant = MonarchRunningTimerVariant.Indicator, now = Now)
        }
        rule.onNodeWithText("0:00:00", useUnmergedTree = true).assertDoesNotExist()
        startedAt = Start
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Timer running"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        startedAt = null
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Timer stopped")).assertExists()
    }

    @Test
    fun theLabelComesBeforeTheClockRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchRunningTimer(
                startedAt = Start,
                variant = MonarchRunningTimerVariant.Indicator,
                label = "ENG-142",
                now = Now,
            )
        }
        rule.onNodeWithText("ENG-142", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("0:04:09", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theClockIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRunningTimer(startedAt = Start, now = Start + 100 * Hour, onStop = {})
        }
        rule.onNodeWithText("100:00:00", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRunningTimer(
                startedAt = null,
                now = Now,
                labels = MonarchRunningTimerLabels(start = "Empezar", stopped = "Parado"),
                onStart = {},
            )
        }
        rule.onNodeWithText("Empezar").assertExists()
        assertEquals("Parado", rule.onNodeWithContentDescription("0 seconds").stateDescription())
    }

    @Test
    fun coloursFollowTheState() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchRunningTimerTokens
            assertEquals(colors.accentStrong, tokens.colors(colors, true, false, false).clock)
            assertEquals(colors.accentSubtle, tokens.colors(colors, true, false, false).container)
            assertEquals(colors.warningSubtle, tokens.colors(colors, true, true, false).container)
            assertEquals(colors.dangerSubtle, tokens.colors(colors, true, true, true).container)
            assertEquals(colors.textMuted, tokens.colors(colors, false, false, false).clock)
            assertEquals(colors.warningStrong, tokens.colors(colors, true, false, false).warning)
            assertEquals(colors.dangerStrong, tokens.colors(colors, true, false, false).error)
        }
    }
}
