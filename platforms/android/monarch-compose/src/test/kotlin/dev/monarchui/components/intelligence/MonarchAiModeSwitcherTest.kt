package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiMode
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiModeSwitcher
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiModeSwitcherSize
import dev.monarchui.components.intelligence.aimodeswitcher.MonarchAiModeSwitcherTokens
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
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ownModes = listOf(
    MonarchAiMode("research", "Research", "Reads sources and cites them.", icon = { MonarchIcon(TestIcon, null) }),
    MonarchAiMode("write", "Write", "Drafts text for you to edit."),
    MonarchAiMode("automate", "Automate", "Not set up in this workspace.", enabled = false),
)

internal object MonarchAiModeSwitcherFixtures : ComponentFixtures {
    override val id = "ai-mode-switcher"
    override val fixtures = listOf(
        fixture("defaults, small") { MonarchAiModeSwitcher(selection = "ask", onSelectionChange = {}) },
        fixture("own modes, medium, one disabled") {
            MonarchAiModeSwitcher(
                selection = "write",
                onSelectionChange = {},
                modes = ownModes,
                size = MonarchAiModeSwitcherSize.Md,
            )
        },
        fixture("disabled") { MonarchAiModeSwitcher(selection = "agent", onSelectionChange = {}, enabled = false) },
        fixture("without description") {
            MonarchAiModeSwitcher(selection = "edit", onSelectionChange = {}, showDescription = false)
        },
        fixture("icon only") {
            MonarchAiModeSwitcher(
                selection = "a",
                onSelectionChange = {},
                modes = listOf(
                    MonarchAiMode("a", "Chat", icon = { MonarchIcon(TestIcon, null) }, iconOnly = true),
                    MonarchAiMode("b", "Code", icon = { MonarchIcon(TestIcon, null) }, iconOnly = true),
                ),
            )
        },
        fixture("unknown selection") { MonarchAiModeSwitcher(selection = "none", onSelectionChange = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiModeSwitcherTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun choosingAModeHandsItsIdToTheAppAndShowsItsDescription() {
        rule.setMonarchContent {
            var mode by remember { mutableStateOf("ask") }
            MonarchAiModeSwitcher(selection = mode, onSelectionChange = { mode = it })
        }
        rule.onNodeWithText("Answers questions. Changes nothing.").assertExists()
        rule.onNodeWithText("Agent").performClick()
        rule.onNodeWithText("Agent").assertIsSelected()
        rule.onNodeWithText("Ask").assertIsNotSelected()
        rule.onNodeWithText("Works through steps and uses tools. Asks before anything risky.").assertExists()
    }

    @Test
    fun segmentsAreRadioButtonsInANamedGroupWith48dpTargets() {
        rule.setMonarchContent { MonarchAiModeSwitcher(selection = "ask", onSelectionChange = {}) }
        rule.onNodeWithText("Edit")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasContentDescription("Mode")).assertExists()
    }

    @Test
    fun theGroupNameCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiModeSwitcher(selection = "ask", onSelectionChange = {}, label = "Mode de travail")
        }
        rule.onNode(hasContentDescription("Mode de travail")).assertExists()
    }

    @Test
    fun theDescriptionIsAPoliteLiveRegion() {
        rule.setMonarchContent { MonarchAiModeSwitcher(selection = "edit", onSelectionChange = {}) }
        rule.onNodeWithText("Proposes edits you review before they apply.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aDisabledModeAndADisabledSwitcherCannotBeChosen() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            Column {
                MonarchAiModeSwitcher("write", { chosen += it }, modes = ownModes, modifier = Modifier.testTag("own"))
                MonarchAiModeSwitcher("ask", { chosen += it }, enabled = false)
            }
        }
        rule.onNodeWithText("Automate").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Agent").assertIsNotEnabled().performClick()
        assertTrue(chosen.isEmpty())
        assertFalse(MonarchAiModeSwitcherTokens.accepts("automate", ownModes))
        assertFalse(MonarchAiModeSwitcherTokens.accepts("unknown", ownModes))
        assertTrue(MonarchAiModeSwitcherTokens.accepts("write", ownModes))
    }

    @Test
    fun withoutDescriptionsThereIsNoDescriptionLine() {
        val plain = listOf(MonarchAiMode("a", "One"), MonarchAiMode("b", "Two"))
        assertFalse(MonarchAiModeSwitcherTokens.describes(plain, showDescription = true))
        assertFalse(MonarchAiModeSwitcherTokens.describes(ownModes, showDescription = false))
        assertTrue(MonarchAiModeSwitcherTokens.describes(ownModes, showDescription = true))
        assertNull(MonarchAiModeSwitcherTokens.description("none", ownModes))
        rule.setMonarchContent { MonarchAiModeSwitcher("ask", {}, showDescription = false) }
        rule.onNodeWithText("Answers questions. Changes nothing.").assertDoesNotExist()
    }

    @Test
    fun anIconOnlySegmentIsReadAsTheModeLabel() {
        rule.setMonarchContent {
            MonarchAiModeSwitcher(
                selection = "a",
                onSelectionChange = {},
                modes = listOf(
                    MonarchAiMode("a", "Chat", icon = { MonarchIcon(TestIcon, null) }, iconOnly = true),
                    MonarchAiMode("b", "Code", icon = { MonarchIcon(TestIcon, null) }, iconOnly = true),
                ),
            )
        }
        rule.onNodeWithContentDescription("Code", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Code").assertDoesNotExist()
    }

    @Test
    fun segmentsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiModeSwitcher(selection = "ask", onSelectionChange = {})
        }
        rule.onNodeWithText("Ask").assertIsBefore(rule.onNodeWithText("Edit"), LayoutDirection.Rtl)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontScale() {
        val text = "Works through steps and uses tools. Asks before anything risky."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiModeSwitcher(selection = "agent", onSelectionChange = {})
        }
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Agent", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theDescriptionIsMutedAndDisabledWhenTheSwitcherIsInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAiModeSwitcherTokens.descriptionColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAiModeSwitcherTokens.descriptionColor(false, colors), theme.id)
        }
    }
}
