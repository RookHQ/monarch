package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aistopbutton.MonarchAiStopButton
import com.rookhq.monarch.components.intelligence.aistopbutton.MonarchAiStopButtonTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertTrue

internal object MonarchAiStopButtonFixtures : ComponentFixtures {
    override val id = "ai-stop-button"
    override val fixtures = listOf(
        fixture("running, every size") {
            Column { MonarchButtonSize.entries.forEach { MonarchAiStopButton(onStop = {}, size = it) } }
        },
        fixture("symbol only") { MonarchAiStopButton(onStop = {}, showLabel = false) },
        fixture("disabled") { MonarchAiStopButton(onStop = {}, enabled = false) },
        fixture("stopped") { MonarchAiStopButton(onStop = {}, stopped = true) },
        fixture("stopped with Continue") { MonarchAiStopButton(onStop = {}, stopped = true, onContinue = {}) },
        fixture("long stopped text") {
            MonarchAiStopButton(
                onStop = {},
                stopped = true,
                onContinue = {},
                stoppedLabel = "The response was stopped before it finished.",
                continueLabel = "Continue writing",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiStopButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun stopIsAButtonThatCallsOnStop() {
        var stops = 0
        rule.setMonarchContent { MonarchAiStopButton(onStop = { stops++ }) }
        rule.onNodeWithText("Stop").assertHasClickAction().performClick()
        assertEquals(1, stops)
    }

    @Test
    fun aDisabledStopDoesNotCallOnStop() {
        var stops = 0
        rule.setMonarchContent { MonarchAiStopButton(onStop = { stops++ }, enabled = false) }
        rule.onNodeWithText("Stop").assertIsNotEnabled().performClick()
        assertEquals(0, stops)
    }

    @Test
    fun withoutTheVisibleTitleTheButtonIsStillNamedStop() {
        rule.setMonarchContent { MonarchAiStopButton(onStop = {}, showLabel = false) }
        rule.onNodeWithContentDescription("Stop").assertHasClickAction()
        rule.onNodeWithText("Stop").assertDoesNotExist()
    }

    @Test
    fun stoppingReplacesTheButtonWithTheStoppedTextInALiveRegionThatWasAlreadyThere() {
        var stopped by mutableStateOf(false)
        rule.setMonarchContent { MonarchAiStopButton(onStop = { stopped = true }, stopped = stopped) }
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(live).assertExists()
        rule.onNodeWithText("Stop").performClick()
        rule.onNodeWithText("Stop").assertDoesNotExist()
        rule.onNodeWithText("Stopped.").assert(live)
    }

    @Test
    fun continueAppearsOnlyWithOnContinueAndCallsIt() {
        var continued = 0
        rule.setMonarchContent { MonarchAiStopButton(onStop = {}, stopped = true, onContinue = { continued++ }) }
        rule.onNodeWithText("Continue").assertHasClickAction().performClick()
        assertEquals(1, continued)
    }

    @Test
    fun withoutOnContinueTheStoppedStateHasNothingToActivate() {
        rule.setMonarchContent { MonarchAiStopButton(onStop = {}, stopped = true) }
        rule.onNodeWithText("Continue").assertDoesNotExist()
        rule.onNodeWithText("Stopped.").assertExists()
    }

    @Test
    fun everyBuiltInStringCanBeReplaced() {
        var stopped by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiStopButton(
                onStop = {},
                stopped = stopped,
                onContinue = {},
                stopLabel = "Arrêter",
                stoppedLabel = "Arrêté.",
                continueLabel = "Continuer",
            )
        }
        rule.onNodeWithText("Arrêter").assertExists()
        stopped = true
        rule.onNodeWithText("Arrêté.").assertExists()
        rule.onNodeWithText("Continuer").assertExists()
    }

    @Test
    fun stopAndContinueKeepA48dpTargetAtTheSmallestSize() {
        rule.setMonarchContent { MonarchAiStopButton(onStop = {}, size = MonarchButtonSize.Sm, showLabel = false) }
        rule.onNodeWithContentDescription("Stop").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun continueKeepsA48dpTarget() {
        rule.setMonarchContent { MonarchAiStopButton(onStop = {}, stopped = true, onContinue = {}) }
        rule.onNodeWithText("Continue").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aLongStoppedTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiStopButton(
                onStop = {},
                stopped = true,
                onContinue = {},
                stoppedLabel = "The response was stopped before it finished.",
            )
        }
        rule.onNodeWithText("The response was stopped before it finished.").assertTextFullyVisible()
        rule.onNodeWithText("Continue").assertExists()
    }

    @Test
    fun theStoppedTextIsMutedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAiStopButtonTokens.stoppedTextColor(colors), theme.id)
        }
    }

    @Test
    fun theSquareGrowsWithTheButtonSize() {
        val sm = MonarchAiStopButtonTokens.glyphSize(MonarchButtonSize.Sm)
        val md = MonarchAiStopButtonTokens.glyphSize(MonarchButtonSize.Md)
        assertTrue(sm < md)
    }

}
