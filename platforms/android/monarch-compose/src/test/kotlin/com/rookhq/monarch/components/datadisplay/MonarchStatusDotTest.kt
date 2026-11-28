package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.statusdot.MonarchStatusDot
import com.rookhq.monarch.components.datadisplay.statusdot.MonarchStatusDotSize
import com.rookhq.monarch.components.datadisplay.statusdot.MonarchStatusDotTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertTextFullyVisible
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

internal object MonarchStatusDotFixtures : ComponentFixtures {
    override val id = "status-dot"
    override val fixtures = MonarchStatusDotSize.entries.map { size ->
        fixture("every tone, ${size.name}") {
            Column {
                MonarchTone.entries.forEach { tone -> MonarchStatusDot(label = tone.name, tone = tone, size = size) }
            }
        }
    } + listOf(
        fixture("pulsing") { MonarchStatusDot(label = "Recording", tone = MonarchTone.Danger, pulsing = true) },
        fixture("label hidden") { MonarchStatusDot(label = "Online", tone = MonarchTone.Success, labelHidden = true) },
        fixture("long text") {
            MonarchStatusDot(
                label = "Waiting for the deployment of the previous release to finish",
                tone = MonarchTone.Info,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStatusDotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theDotAndTheLabelAreOneElementReadAsTheLabel() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchStatusDot(label = "Running", tone = MonarchTone.Success, pulsing = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Running").assertHasNoClickAction()
        rule.onAllNodesWithText("Running").assertCountEquals(1)
    }

    @Test
    fun aHiddenLabelIsNotDrawnButIsStillRead() {
        rule.setMonarchContent { MonarchStatusDot(label = "Online", labelHidden = true) }
        rule.onNodeWithContentDescription("Online").assertExists()
        rule.onAllNodesWithText("Online", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theLabelGrowsWithTheFontSizeSettingWithoutBeingCut() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStatusDot(label = "Deploying to production", size = MonarchStatusDotSize.Md)
        }
        rule.onNodeWithText("Deploying to production", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theDotComesBeforeTheLabelInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchStatusDot(label = "Paused", modifier = Modifier.testTag("dot")) }
        val dot = MonarchStatusDotTokens.dotSize(MonarchStatusDotSize.Sm)
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val whole = rule.onNodeWithTag("dot").getUnclippedBoundsInRoot()
            val text = rule.onNodeWithText("Paused", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val follows = if (direction == LayoutDirection.Ltr) {
                text.left >= whole.left + dot
            } else {
                text.right <= whole.right - dot
            }
            assertTrue(follows, "$direction: the label must follow the dot")
        }
    }

    @Test
    fun theDotUsesTheSolidColourOfTheToneInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textSubtle, MonarchStatusDotTokens.dotColor(MonarchTone.Neutral, colors), theme.id)
            assertEquals(colors.infoSolid, MonarchStatusDotTokens.dotColor(MonarchTone.Info, colors), theme.id)
            assertEquals(colors.successSolid, MonarchStatusDotTokens.dotColor(MonarchTone.Success, colors), theme.id)
            assertEquals(colors.warningSolid, MonarchStatusDotTokens.dotColor(MonarchTone.Warning, colors), theme.id)
            assertEquals(colors.dangerSolid, MonarchStatusDotTokens.dotColor(MonarchTone.Danger, colors), theme.id)
        }
    }

    @Test
    fun thePulseIsNotDrawnWhenAnimationsAreOff() {
        assertTrue(MonarchStatusDotTokens.showsPulse(pulsing = true, reducedMotion = false))
        assertFalse(MonarchStatusDotTokens.showsPulse(pulsing = true, reducedMotion = true))
        assertFalse(MonarchStatusDotTokens.showsPulse(pulsing = false, reducedMotion = false))
    }
}
