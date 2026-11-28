package com.rookhq.monarch.components.feedback

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatus
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicator
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicatorLabels
import com.rookhq.monarch.components.feedback.savestatusindicator.MonarchSaveStatusIndicatorTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
private fun minutesAgo(minutes: Int): Long = System.currentTimeMillis() - minutes * DateUtils.MINUTE_IN_MILLIS

internal object MonarchSaveStatusIndicatorFixtures : ComponentFixtures {
    override val id = "save-status-indicator"
    override val fixtures = listOf(
        fixture("idle, unsaved and saving") {
            Column {
                MonarchSaveStatusIndicator(status = MonarchSaveStatus.Idle)
                MonarchSaveStatusIndicator(status = MonarchSaveStatus.Unsaved)
                MonarchSaveStatusIndicator(status = MonarchSaveStatus.Saving)
            }
        },
        fixture("saved with time and icon") {
            MonarchSaveStatusIndicator(
                status = MonarchSaveStatus.Saved,
                savedAt = minutesAgo(2),
                icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("error with retry") {
            MonarchSaveStatusIndicator(status = MonarchSaveStatus.Error, onRetry = {})
        },
        fixture("long text") {
            MonarchSaveStatusIndicator(
                status = MonarchSaveStatus.Error,
                errorMessage = "You are offline. Your changes are kept on this device until you reconnect.",
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSaveStatusIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    /** Applies a state change and runs a few frames, without waiting for the endless progress ring. */
    private fun settle() {
        rule.waitForIdle()
        repeat(4) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()
    }

    @Test
    fun theStatusAndTimeAreOneElement() {
        rule.setMonarchContent { MonarchSaveStatusIndicator(status = MonarchSaveStatus.Saved, savedAt = minutesAgo(2)) }
        rule.onNodeWithContentDescription("Saved, 2 minutes ago").assertExists()
    }

    @Test
    fun idleDrawsNoTextButKeepsTheLiveRegion() {
        rule.setMonarchContent { MonarchSaveStatusIndicator(status = MonarchSaveStatus.Idle) }
        rule.onNode(polite).assertExists()
        rule.onNodeWithContentDescription("Saved").assertDoesNotExist()
    }

    @Test
    fun theOutcomeIsAnnouncedButNotTheFirstStatusNorSaving() {
        var status by mutableStateOf(MonarchSaveStatus.Saved)
        rule.setMonarchContent { MonarchSaveStatusIndicator(status = status) }
        rule.onNode(polite and hasContentDescription("Saved")).assertDoesNotExist()
        rule.mainClock.autoAdvance = false
        status = MonarchSaveStatus.Saving
        settle()
        rule.onNode(polite and hasContentDescription("Saving…")).assertDoesNotExist()
        status = MonarchSaveStatus.Saved
        settle()
        rule.onNode(polite and hasContentDescription("Saved")).assertExists()
        status = MonarchSaveStatus.Error
        settle()
        rule.onNode(polite and hasContentDescription("Couldn't save")).assertExists()
    }

    @Test
    fun savingIsAnnouncedWhenAsked() {
        var status by mutableStateOf(MonarchSaveStatus.Unsaved)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchSaveStatusIndicator(status = status, announceSaving = true) }
        rule.mainClock.advanceTimeByFrame()
        status = MonarchSaveStatus.Saving
        settle()
        rule.onNode(polite and hasContentDescription("Saving…")).assertExists()
    }

    @Test
    fun retryAppearsOnlyOnErrorAndCallsBack() {
        var retries = 0
        var status by mutableStateOf(MonarchSaveStatus.Saved)
        rule.setMonarchContent { MonarchSaveStatusIndicator(status = status, onRetry = { retries++ }) }
        rule.onNodeWithText("Retry").assertDoesNotExist()
        status = MonarchSaveStatus.Error
        rule.onNodeWithText("Retry").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theErrorMessageAndLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            Column {
                MonarchSaveStatusIndicator(status = MonarchSaveStatus.Error, errorMessage = "You are offline")
                MonarchSaveStatusIndicator(
                    status = MonarchSaveStatus.Unsaved,
                    labels = MonarchSaveStatusIndicatorLabels(unsaved = "Draft"),
                )
                MonarchSaveStatusIndicator(
                    status = MonarchSaveStatus.Error,
                    onRetry = {},
                    labels = MonarchSaveStatusIndicatorLabels(error = "Not saved", retry = "Try again"),
                )
            }
        }
        rule.onNodeWithContentDescription("You are offline").assertExists()
        rule.onNodeWithContentDescription("Draft").assertExists()
        rule.onNodeWithContentDescription("Not saved").assertExists()
        rule.onNodeWithText("Try again").assertExists()
    }

    @Test
    fun retryFollowsTheTextInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSaveStatusIndicator(status = MonarchSaveStatus.Error, onRetry = {})
        }
        rule.onNodeWithContentDescription("Couldn't save")
            .assertIsBefore(rule.onNodeWithText("Retry"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongMessageWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSaveStatusIndicator(
                status = MonarchSaveStatus.Error,
                modifier = Modifier.width(300.dp),
                errorMessage = "You are offline. Changes stay on this device.",
                onRetry = {},
            )
        }
        rule.onNodeWithText("You are offline", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Retry").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theSpinnerIsStillWhenAnimationsAreOff() {
        assertTrue(MonarchSaveStatusIndicatorTokens.showsSpinner(MonarchSaveStatus.Saving, reducedMotion = false))
        assertFalse(MonarchSaveStatusIndicatorTokens.showsSpinner(MonarchSaveStatus.Saving, reducedMotion = true))
        assertFalse(MonarchSaveStatusIndicatorTokens.showsSpinner(MonarchSaveStatus.Saved, reducedMotion = false))
        assertTrue(MonarchSaveStatusIndicatorTokens.showsTime(MonarchSaveStatus.Saved, 1L))
        assertFalse(MonarchSaveStatusIndicatorTokens.showsTime(MonarchSaveStatus.Error, 1L))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        val t = MonarchSaveStatusIndicatorTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertNull(t.markColor(MonarchSaveStatus.Idle, colors), theme.id)
            assertEquals(colors.textSubtle, t.markColor(MonarchSaveStatus.Saving, colors), theme.id)
            assertEquals(colors.warningStrong, t.markColor(MonarchSaveStatus.Unsaved, colors), theme.id)
            assertEquals(colors.successStrong, t.markColor(MonarchSaveStatus.Saved, colors), theme.id)
            assertEquals(colors.dangerStrong, t.markColor(MonarchSaveStatus.Error, colors), theme.id)
            assertEquals(colors.dangerStrong, t.textColor(MonarchSaveStatus.Error, colors), theme.id)
            assertEquals(colors.textMuted, t.textColor(MonarchSaveStatus.Saved, colors), theme.id)
        }
    }
}
