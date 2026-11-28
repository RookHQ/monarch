package dev.monarchui.components.files

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.calloutblock.MonarchCalloutBlock
import dev.monarchui.components.files.calloutblock.MonarchCalloutBlockTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal object MonarchCalloutBlockFixtures : ComponentFixtures {
    override val id = "callout-block"
    override val fixtures = MonarchTone.entries.map { tone ->
        fixture("${tone.name}, with an icon") {
            MonarchCalloutBlock(
                text = "Back up the workspace before you migrate.",
                tone = tone,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        }
    } + listOf(
        fixture("without an icon") { MonarchCalloutBlock(text = "A note without an icon.") },
        fixture("several paragraphs and a button") {
            MonarchCalloutBlock(tone = MonarchTone.Info) {
                Text("Tip", style = MonarchTheme.typography.bodyStrong)
                Text("Callouts can hold more than one paragraph.")
                TextButton(onClick = {}) { Text("Learn more") }
            }
        },
        fixture("long text") {
            MonarchCalloutBlock(
                text = "The migration copies every project, member and integration to the new region and " +
                    "cannot be undone once it has started.",
                tone = MonarchTone.Warning,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCalloutBlockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theToneIsSpokenOnTheBlockAndTheContentStaysSeparate() {
        rule.setMonarchContent {
            MonarchCalloutBlock(
                text = "Back up first.",
                tone = MonarchTone.Warning,
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = "ignored") },
            )
        }
        rule.onNodeWithContentDescription("Warning").assertExists()
        rule.onNodeWithText("Back up first.").assertExists()
        rule.onNodeWithContentDescription("ignored").assertDoesNotExist()
    }

    @Test
    fun theNeutralToneHasNoSpokenName() {
        rule.setMonarchContent { MonarchCalloutBlock(text = "A note.", modifier = Modifier.testTag("block")) }
        val config = rule.onNodeWithTag("block").fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsProperties.ContentDescription))
        assertEquals(true, config[SemanticsProperties.IsTraversalGroup])
    }

    @Test
    fun theToneLabelReplacesTheBuiltInName() {
        rule.setMonarchContent { MonarchCalloutBlock(text = "x", tone = MonarchTone.Danger, toneLabel = "Danger") }
        rule.onNodeWithContentDescription("Danger").assertExists()
        rule.onNodeWithContentDescription("Error").assertDoesNotExist()
    }

    @Test
    fun theDefaultNamesOfTheTones() {
        val expected = mapOf(
            MonarchTone.Info to "Information",
            MonarchTone.Success to "Success",
            MonarchTone.Warning to "Warning",
            MonarchTone.Danger to "Error",
        )
        val environment = MonarchTestEnvironment()
        var tone by mutableStateOf(MonarchTone.Info)
        rule.setMonarchContent(environment) { MonarchCalloutBlock(text = "x", tone = tone) }
        expected.forEach { (next, name) ->
            tone = next
            rule.onNodeWithContentDescription(name).assertExists()
        }
    }

    @Test
    fun anActionInsideTheContentStaysReachable() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchCalloutBlock(tone = MonarchTone.Info) {
                Text("Tip")
                TextButton(onClick = { clicks++ }) { Text("Learn more") }
            }
        }
        rule.onNodeWithText("Learn more").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun theIconIsAtTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCalloutBlock(
                text = "Text",
                icon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, modifier = Modifier.testTag("icon"))
                },
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.onNodeWithTag("icon", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Text"), direction)
        }
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCalloutBlock(text = "The migration cannot be undone once it has started.")
        }
        rule.onNodeWithText("The migration cannot be undone once it has started.").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val warning = MonarchCalloutBlockTokens.colors(MonarchTone.Warning, colors)
            assertEquals(colors.warningSubtle, warning.container, theme.id)
            assertEquals(colors.warningBorder, warning.border, theme.id)
            assertEquals(colors.warningStrong, warning.icon, theme.id)
            assertEquals(colors.textDefault, warning.content, theme.id)
            val neutral = MonarchCalloutBlockTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.bgSubtle, neutral.container, theme.id)
            assertEquals(colors.borderSubtle, neutral.border, theme.id)
            assertEquals(colors.textMuted, neutral.icon, theme.id)
        }
    }
}
