package com.rookhq.monarch.components.intelligence

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTour
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTourLabels
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTourStep
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTourTokens
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
import kotlin.test.assertTrue

private val Steps = listOf(
    MonarchAiOnboardingTourStep("Sees", "Open page."),
    MonarchAiOnboardingTourStep("Does", "Drafts."),
    MonarchAiOnboardingTourStep("Off", "In Settings."),
)

internal object MonarchAiOnboardingTourFixtures : ComponentFixtures {
    override val id = "ai-onboarding-tour"
    override val fixtures = listOf(
        fixture("one step") { MonarchAiOnboardingTour(steps = Steps.take(1), onDisable = {}, onFinish = {}) },
        fixture("first of two, skip text") {
            MonarchAiOnboardingTour(steps = Steps.take(2), onDisable = {}, onFinish = {}, onSkip = {})
        },
        fixture("last of two, skip icon") {
            MonarchAiOnboardingTour(
                steps = Steps.take(2),
                step = 1,
                onStepChange = {},
                onDisable = {},
                onFinish = {},
                skipIcon = { Icon(TestIcon, contentDescription = null) },
                onSkip = {},
            )
        },
        fixture("no steps") { MonarchAiOnboardingTour(steps = emptyList(), onDisable = {}, onFinish = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiOnboardingTourTest {
    @get:Rule
    val rule = createComposeRule()

    private fun title(text: String) = rule.onNode(hasText(text) and isHeading())

    @Test
    fun theFirstStepHasNextAndTurnOffButNoBack() {
        rule.setMonarchContent { MonarchAiOnboardingTour(steps = Steps, onDisable = {}, onFinish = {}) }
        title("Sees").assertIsDisplayed()
        rule.onNodeWithText("Back").assertDoesNotExist()
        rule.onNodeWithText("Next").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Turn off AI").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun nextAndBackMoveThroughTheStepsAndTheLastFinishes() {
        var finished = 0
        rule.setMonarchContent { MonarchAiOnboardingTour(steps = Steps, onDisable = {}, onFinish = { finished++ }) }
        rule.onNodeWithText("Next").performClick()
        title("Does").assertIsDisplayed()
        rule.onNodeWithText("Back").assertExists()
        rule.onNodeWithText("Next").performClick()
        title("Off").assertIsDisplayed()
        rule.onNodeWithText("Next").assertDoesNotExist()
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, finished)
        rule.onNodeWithText("Back").performClick()
        title("Does").assertIsDisplayed()
    }

    @Test
    fun turnOffAiIsOnEveryStep() {
        var disabled = 0
        var step by mutableIntStateOf(0)
        rule.setMonarchContent {
            MonarchAiOnboardingTour(
                steps = Steps,
                step = step,
                onStepChange = { step = it },
                onDisable = { disabled++ },
                onFinish = {},
            )
        }
        Steps.indices.forEach {
            step = it
            rule.onNodeWithText("Turn off AI").performClick()
        }
        assertEquals(Steps.size, disabled)
    }

    @Test
    fun aStepChangeIsAnnouncedPolitely() {
        rule.setMonarchContent { MonarchAiOnboardingTour(steps = Steps, onDisable = {}, onFinish = {}) }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(polite).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithContentDescription("Step 2 of 3: Does").assert(polite)
    }

    @Test
    fun aSwipeMovesToTheNextStep() {
        var reported = -1
        rule.setMonarchContent {
            var step by remember { mutableIntStateOf(0) }
            MonarchAiOnboardingTour(
                steps = Steps,
                step = step,
                onStepChange = {
                    step = it
                    reported = it
                },
                onDisable = {},
                onFinish = {},
            )
        }
        // The title is narrow, so drag from it across most of the 320dp window.
        title("Sees").performTouchInput { swipeLeft(startX = centerX, endX = centerX - 280.dp.toPx()) }
        rule.waitForIdle()
        title("Does").assertIsDisplayed()
        assertEquals(1, reported)
        rule.onNodeWithText("Back").assertExists()
    }

    @Test
    fun aStepOutsideTheRangeIsClamped() {
        rule.setMonarchContent {
            MonarchAiOnboardingTour(steps = Steps, step = 9, onStepChange = {}, onDisable = {}, onFinish = {})
        }
        rule.onNodeWithText("Got it").assertExists()
        title("Off").assertIsDisplayed()
    }

    @Test
    fun skipIsATextButtonOrANamedIconButton() {
        var skipped = 0
        rule.setMonarchContent {
            MonarchAiOnboardingTour(
                steps = Steps,
                onDisable = {},
                onFinish = {},
                skipIcon = { Icon(TestIcon, contentDescription = null) },
                onSkip = { skipped++ },
            )
        }
        rule.onNodeWithContentDescription("Skip introduction").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, skipped)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiOnboardingTour(
                steps = Steps.take(1),
                onDisable = {},
                onFinish = {},
                labels = MonarchAiOnboardingTourLabels(
                    finish = "Compris",
                    disable = "Désactiver l’IA",
                    skip = "Passer",
                ),
                onSkip = {},
            )
        }
        rule.onNodeWithText("Compris").assertExists()
        rule.onNodeWithText("Désactiver l’IA").assertExists()
        rule.onNodeWithContentDescription("Passer").assertExists()
    }

    @Test
    fun backIsBeforeNextInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiOnboardingTour(steps = Steps, step = 1, onStepChange = {}, onDisable = {}, onFinish = {})
        }
        rule.onNodeWithText("Back").assertIsBefore(rule.onNodeWithText("Next"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiOnboardingTour(steps = Steps.take(1), onDisable = {}, onFinish = {})
        }
        title("Sees").assertTextFullyVisible()
    }

    @Test
    fun theLogicDecidesTheButtonsAndColours() {
        assertEquals(2, MonarchAiOnboardingTourTokens.clamp(9, 3))
        assertEquals(0, MonarchAiOnboardingTourTokens.clamp(-1, 3))
        assertEquals(0, MonarchAiOnboardingTourTokens.clamp(4, 0))
        assertFalse(MonarchAiOnboardingTourTokens.hasBack(0))
        assertTrue(MonarchAiOnboardingTourTokens.isLast(0, 0))
        assertTrue(MonarchAiOnboardingTourTokens.isLast(2, 3))
        assertFalse(MonarchAiOnboardingTourTokens.showsStepper(1))
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, MonarchAiOnboardingTourTokens.stepColor(c), theme.id)
            assertEquals(c.aiSolid, MonarchAiOnboardingTourTokens.pipColor(true, c), theme.id)
            assertEquals(c.bgEmphasis, MonarchAiOnboardingTourTokens.pipColor(false, c), theme.id)
        }
        assertTrue(MonarchAiOnboardingTourTokens.pipWidth(true) > MonarchAiOnboardingTourTokens.pipWidth(false))
    }
}
