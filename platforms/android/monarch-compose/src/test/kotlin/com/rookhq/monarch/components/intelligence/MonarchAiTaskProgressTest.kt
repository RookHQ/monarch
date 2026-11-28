package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.components.intelligence.aitaskprogress.AiTaskEta
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskProgress
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskProgressLabels
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskProgressTokens
import com.rookhq.monarch.components.intelligence.aitaskprogress.MonarchAiTaskStatus
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TaskNow = 1_763_100_000_000L
private const val FiveMinutes = 300_000L
private const val ThreeMinutes = 180_000L

private val taskSteps = listOf(
    MonarchAiPlanStep("read", "Read the documents", MonarchAiPlanStepState.Done),
    MonarchAiPlanStep("draft", "Draft the summary", MonarchAiPlanStepState.Todo),
)

internal object MonarchAiTaskProgressFixtures : ComponentFixtures {
    override val id = "ai-task-progress"
    override val fixtures = listOf(
        fixture("running with controls") {
            MonarchAiTaskProgress(
                title = "Summarising 40 documents",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow - FiveMinutes,
                progress = 0.4f,
                etaMillis = ThreeMinutes,
                now = TaskNow,
                onNotifyingChange = {},
                onMinimize = {},
                onCancel = {},
            )
        },
        fixture("running, indeterminate") {
            MonarchAiTaskProgress(
                title = "Indexing",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow,
                now = TaskNow,
            )
        },
        fixture("paused with steps") {
            MonarchAiTaskProgress(
                title = "Drafting",
                status = MonarchAiTaskStatus.Paused,
                startedAt = TaskNow - FiveMinutes,
                steps = taskSteps,
                now = TaskNow,
                notifying = true,
                onNotifyingChange = {},
            )
        },
        fixture("done, minimize icon") {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Done,
                startedAt = TaskNow - FiveMinutes,
                now = TaskNow,
                onMinimize = {},
                minimizeIcon = { MonarchIcon(TestIcon, null) },
            )
        },
        fixture("failed") {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Failed,
                startedAt = TaskNow - FiveMinutes,
                progress = 0.7f,
                now = TaskNow,
                onCancel = {},
            )
        },
        fixture("cancelled, long title") {
            MonarchAiTaskProgress(
                title = "Translating every help center article into German, French and Japanese",
                status = MonarchAiTaskStatus.Cancelled,
                startedAt = TaskNow - FiveMinutes,
                now = TaskNow,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiTaskProgressTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBarIsNamedByTheTitleAndReadsThePercentage() {
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Summarising",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow - FiveMinutes,
                steps = taskSteps,
                now = TaskNow,
            )
        }
        val bar = rule.onNodeWithContentDescription("Summarising progress")
        assertEquals("50% complete", bar.stateDescription())
        bar.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.5f, 0f..1f)),
        )
        rule.onNodeWithText("Summarising").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Running", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Started 5 minutes ago").assertExists()
    }

    @Test
    fun withoutProgressARunningTaskIsIndeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Indexing",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow,
                now = TaskNow,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val bar = rule.onNodeWithContentDescription("Indexing progress")
        assertEquals("Running", bar.stateDescription())
        bar.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
    }

    @Test
    fun theTimeLeftIsShownWhileRunning() {
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow,
                progress = 0.25f,
                etaMillis = ThreeMinutes,
                now = TaskNow,
            )
        }
        rule.onNodeWithText("About 3 minutes left").assertExists()
        rule.onNodeWithText("25% complete").assertExists()
    }

    @Test
    fun cancelAndNotifyAreOfferedOnlyWhileActive() {
        var status by mutableStateOf(MonarchAiTaskStatus.Running)
        var notifying by mutableStateOf(false)
        var cancelled = 0
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = status,
                startedAt = TaskNow,
                progress = 0.5f,
                notifying = notifying,
                now = TaskNow,
                onNotifyingChange = { notifying = it },
                onCancel = { cancelled++ },
            )
        }
        val cancel = rule.onNodeWithText("Cancel")
        cancel.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, cancelled)
        val notify = rule.onNodeWithText("Notify me when done")
        notify.assertTouchTargetAtLeast48dp(rule.density)
        notify.performClick()
        assertTrue(notifying)
        status = MonarchAiTaskStatus.Done
        rule.waitForIdle()
        rule.onNodeWithText("Cancel").assertDoesNotExist()
        rule.onNodeWithText("Notify me when done").assertDoesNotExist()
    }

    @Test
    fun theNotifyToggleReadsItsState() {
        var notifying by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Paused,
                startedAt = TaskNow,
                notifying = notifying,
                now = TaskNow,
                onNotifyingChange = { notifying = it },
            )
        }
        val notify = rule.onNodeWithText("Notify me when done")
        notify.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        notify.performClick()
        notify.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun minimizeIsAWrittenButtonOrANamedIconButton() {
        var minimized = 0
        var icon by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Done,
                startedAt = TaskNow,
                now = TaskNow,
                onMinimize = { minimized++ },
                minimizeIcon = if (icon) ({ MonarchIcon(TestIcon, null) }) else null,
            )
        }
        rule.onNodeWithText("Minimize").assertTouchTargetAtLeast48dp(rule.density).performClick()
        icon = true
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Minimize").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(2, minimized)
    }

    @Test
    fun aStatusChangeIsAnnouncedAndAFailureAssertively() {
        var status by mutableStateOf(MonarchAiTaskStatus.Running)
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = status,
                startedAt = TaskNow,
                progress = 0.5f,
                now = TaskNow,
            )
        }
        rule.onNodeWithContentDescription("Export: Running").assertDoesNotExist()
        status = MonarchAiTaskStatus.Paused
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Export: Paused")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        status = MonarchAiTaskStatus.Failed
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Export: Failed")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiTaskProgress(
                title = "Translating every help center article",
                status = MonarchAiTaskStatus.Cancelled,
                startedAt = TaskNow,
                now = TaskNow,
            )
        }
        rule.onNodeWithText("Translating every help center article").assertTextFullyVisible()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiTaskProgress(
                title = "Export",
                status = MonarchAiTaskStatus.Running,
                startedAt = TaskNow - FiveMinutes,
                progress = 0.5f,
                etaMillis = ThreeMinutes,
                now = TaskNow,
                onCancel = {},
                labels = MonarchAiTaskProgressLabels(
                    status = { "State ${it.name}" },
                    progressLabel = { "$it bar" },
                    progressValue = { "$it of 100" },
                    started = { "Began $it" },
                    eta = { "${it / 1000} s" },
                    cancel = "Stop",
                ),
            )
        }
        assertEquals("50 of 100", rule.onNodeWithContentDescription("Export bar").stateDescription())
        rule.onNodeWithText("State Running", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Began 5 minutes ago").assertExists()
        rule.onNodeWithText("180 s").assertExists()
        rule.onNodeWithText("Stop").assertExists()
    }

    @Test
    fun theRulesMatchTheWeb() {
        val tokens = MonarchAiTaskProgressTokens
        assertEquals(0.5f, tokens.fraction(null, taskSteps, MonarchAiTaskStatus.Running))
        assertEquals(1f, tokens.fraction(2f, emptyList(), MonarchAiTaskStatus.Running))
        assertEquals(1f, tokens.fraction(null, emptyList(), MonarchAiTaskStatus.Done))
        assertNull(tokens.fraction(null, emptyList(), MonarchAiTaskStatus.Paused))
        assertNull(tokens.barValue(null, MonarchAiTaskStatus.Running))
        assertEquals(0f, tokens.barValue(null, MonarchAiTaskStatus.Cancelled))
        assertEquals(AiTaskEta.UnderAMinute, tokens.eta(59_000L))
        assertEquals(AiTaskEta.Minutes(1), tokens.eta(60_000L))
        assertEquals(AiTaskEta.Minutes(59), tokens.eta(59L * 60_000L))
        assertEquals(AiTaskEta.Hours(2), tokens.eta(100L * 60_000L))
        assertFalse(tokens.showsEta(MonarchAiTaskStatus.Paused, ThreeMinutes))
        assertFalse(tokens.showsEta(MonarchAiTaskStatus.Running, -1L))
        assertNull(tokens.announcement(null, MonarchAiTaskStatus.Running))
        assertNull(tokens.announcement(MonarchAiTaskStatus.Running, MonarchAiTaskStatus.Running))
        assertEquals(false, tokens.announcement(MonarchAiTaskStatus.Running, MonarchAiTaskStatus.Done))
        assertEquals(true, tokens.announcement(MonarchAiTaskStatus.Running, MonarchAiTaskStatus.Failed))
    }

    @Test
    fun eachStatusHasItsTone() {
        val tokens = MonarchAiTaskProgressTokens
        // The running badge uses the AI tone, as the web's status pill does.
        assertEquals(MonarchTone.Ai, tokens.tone(MonarchAiTaskStatus.Running))
        assertEquals(MonarchTone.Warning, tokens.tone(MonarchAiTaskStatus.Paused))
        assertEquals(MonarchTone.Success, tokens.tone(MonarchAiTaskStatus.Done))
        assertEquals(MonarchTone.Danger, tokens.tone(MonarchAiTaskStatus.Failed))
        assertEquals(MonarchTone.Neutral, tokens.tone(MonarchAiTaskStatus.Cancelled))
    }

    @Test
    fun theBarTakesTheToneOfTheStatusAsOnTheWeb() {
        val tokens = MonarchAiTaskProgressTokens
        assertEquals(MonarchTone.Accent, tokens.barTone(MonarchAiTaskStatus.Running))
        assertEquals(MonarchTone.Warning, tokens.barTone(MonarchAiTaskStatus.Paused))
        assertEquals(MonarchTone.Success, tokens.barTone(MonarchAiTaskStatus.Done))
        assertEquals(MonarchTone.Danger, tokens.barTone(MonarchAiTaskStatus.Failed))
        assertEquals(MonarchTone.Neutral, tokens.barTone(MonarchAiTaskStatus.Cancelled))
    }
}
