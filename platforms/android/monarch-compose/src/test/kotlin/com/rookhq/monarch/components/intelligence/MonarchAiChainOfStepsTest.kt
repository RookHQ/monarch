package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aichainofsteps.AiChainRunningMark
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfSteps
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfStepsAnnouncementTexts
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfStepsLabels
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfStepsLogic
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfStepsTokens
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.foundation.MonarchMotion
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
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.LocalMonarchMotion
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun steps(vararg states: MonarchAiPlanStepState): List<MonarchAiPlanStep> {
    val names = listOf("Read the contract", "Find the renewal date", "Draft the reply", "Check the tone")
    return states.mapIndexed { index, state -> MonarchAiPlanStep("s$index", names[index], state) }
}

internal object MonarchAiChainOfStepsFixtures : ComponentFixtures {
    override val id = "ai-chain-of-steps"
    override val fixtures = listOf(
        fixture("running") {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Active, MonarchAiPlanStepState.Todo),
            )
        },
        fixture("failed and skipped with details") {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Failed, MonarchAiPlanStepState.Skipped),
                details = mapOf("s0" to "The file could not be opened."),
                collapsible = false,
            )
        },
        fixture("with state icons") {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Todo),
                stateIcon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("folded") {
            MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Failed))
        },
        fixture("folded and open") {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Done),
                initiallyExpanded = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiChainOfStepsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachStepIsReadAsItsLabelThenItsStateAndDetail() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Failed, MonarchAiPlanStepState.Active),
                details = mapOf("s0" to "Not found"),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Failed, Not found", rule.onNodeWithContentDescription("Read the contract").stateDescription())
        assertEquals("In progress", rule.onNodeWithContentDescription("Find the renewal date").stateDescription())
        rule.onNodeWithText("Failed", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Steps").assertExists()
    }

    @Test
    fun aFinishedListFoldsUnderASummaryThatOpens() {
        rule.setMonarchContent {
            MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Failed))
        }
        rule.onAllNodesWithContentDescription("Read the contract").assertCountEquals(0)
        rule.onNodeWithText("2 steps, 1 failed").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Read the contract").assertExists()
    }

    @Test
    fun aListThatMayNotFoldStaysOpen() {
        rule.setMonarchContent {
            MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Done), collapsible = false)
        }
        rule.onNodeWithContentDescription("Read the contract").assertExists()
        rule.onAllNodesWithContentDescription("1 step").assertCountEquals(0)
    }

    @Test
    fun startsFailuresAndTheEndAreAnnouncedButNotTheFirstState() {
        var current by mutableStateOf(steps(MonarchAiPlanStepState.Active, MonarchAiPlanStepState.Todo))
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiChainOfSteps(steps = current) }
        rule.mainClock.advanceTimeByFrame()
        rule.onAllNodesWithContentDescription("Step 1 of 2: Read the contract").assertCountEquals(0)
        current = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Active)
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Step 2 of 2: Find the renewal date")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        current = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Failed)
        rule.mainClock.advanceTimeBy(100)
        val both = "Step failed: Find the renewal date. Finished 2 steps, 1 failed"
        rule.onNodeWithContentDescription(both).assertExists()
    }

    @Test
    fun announcementsCanBeTurnedOff() {
        var current by mutableStateOf(steps(MonarchAiPlanStepState.Todo))
        rule.setMonarchContent { MonarchAiChainOfSteps(steps = current, announces = false) }
        current = steps(MonarchAiPlanStepState.Done)
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Finished 1 step").assertCountEquals(0)
    }

    @Test
    fun anEmptyListShowsNothing() {
        rule.setMonarchContent { MonarchAiChainOfSteps(steps = emptyList()) }
        rule.onAllNodesWithContentDescription("Steps").assertCountEquals(0)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Done),
                labels = MonarchAiChainOfStepsLabels(summary = { count, _ -> "$count pasos" }),
            )
        }
        rule.onNodeWithText("2 pasos").assertExists()
    }

    @Test
    fun theMarkIsAtTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiChainOfSteps(
                steps = steps(MonarchAiPlanStepState.Todo),
                stateIcon = { MonarchIcon(TestIcon, contentDescription = "mark") },
            )
        }
        rule.onNodeWithContentDescription("mark", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Read the contract", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun labelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Skipped), collapsible = false)
        }
        rule.onNodeWithText("Read the contract", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Skipped", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theAnnouncementLogicReportsOnlyNews() {
        val texts = MonarchAiChainOfStepsAnnouncementTexts(
            started = { label, position, total -> "start $position/$total $label" },
            failedStep = { "fail $it" },
            finished = { count, failed -> "end $count $failed" },
        )
        val first = steps(MonarchAiPlanStepState.Active, MonarchAiPlanStepState.Todo)
        assertEquals(emptyList(), MonarchAiChainOfStepsLogic.announcements(null, first, texts))
        val before = MonarchAiChainOfStepsLogic.snapshot(first)
        val done = steps(MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Done)
        assertEquals(listOf("end 2 0"), MonarchAiChainOfStepsLogic.announcements(before, done, texts))
        val finished = MonarchAiChainOfStepsLogic.snapshot(done)
        assertEquals(emptyList(), MonarchAiChainOfStepsLogic.announcements(finished, done, texts))
        assertFalse(MonarchAiChainOfStepsLogic.allFinished(emptyList()))
        assertTrue(MonarchAiChainOfStepsLogic.folded(done, collapsible = true))
        assertFalse(MonarchAiChainOfStepsLogic.folded(done, collapsible = false))
        assertEquals("Done", MonarchAiChainOfStepsLogic.spokenState("Done", " "))
    }

    @Test
    fun theColoursComeFromTheStateRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchAiChainOfStepsTokens
            assertEquals(c.aiStrong, t.markColor(MonarchAiPlanStepState.Active, c), theme.id)
            assertEquals(c.successStrong, t.markColor(MonarchAiPlanStepState.Done, c), theme.id)
            assertEquals(c.dangerStrong, t.markColor(MonarchAiPlanStepState.Failed, c), theme.id)
            assertEquals(c.textSubtle, t.markColor(MonarchAiPlanStepState.Todo, c), theme.id)
            assertEquals(c.textMuted, t.labelColor(MonarchAiPlanStepState.Skipped, c), theme.id)
            assertEquals(c.textDefault, t.labelColor(MonarchAiPlanStepState.Done, c), theme.id)
            assertEquals(c.dangerStrong, t.stateColor(MonarchAiPlanStepState.Failed, c), theme.id)
        }
        assertTrue(MonarchAiChainOfStepsTokens.filledMark(MonarchAiPlanStepState.Done))
        assertFalse(MonarchAiChainOfStepsTokens.filledMark(MonarchAiPlanStepState.Todo))
    }

    @Test
    fun underReducedMotionTheRunningStepShowsTheWebRingWithACentreDot() {
        assertEquals(AiChainRunningMark.RingWithDot, MonarchAiChainOfStepsTokens.runningMark(reducedMotion = true))
        assertEquals(AiChainRunningMark.Spinner, MonarchAiChainOfStepsTokens.runningMark(reducedMotion = false))
        assertTrue(MonarchAiChainOfStepsTokens.RunningDotSize < MonarchAiChainOfStepsTokens.DotSize)
        val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        rule.setMonarchContent {
            CompositionLocalProvider(LocalMonarchMotion provides MonarchMotion(reducedMotion = true)) {
                MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Active), collapsible = false)
            }
        }
        // No spinning indicator: the still mark is drawn instead.
        rule.onAllNodes(progress, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun withMotionTheRunningStepSpins() {
        rule.setMonarchContent {
            MonarchAiChainOfSteps(steps = steps(MonarchAiPlanStepState.Active), collapsible = false)
        }
        val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        rule.onAllNodes(progress, useUnmergedTree = true).assertCountEquals(1)
    }
}
