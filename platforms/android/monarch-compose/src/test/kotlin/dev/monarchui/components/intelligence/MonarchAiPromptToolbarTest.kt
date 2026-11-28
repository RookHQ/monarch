package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbar
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarButton
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarToggle
import dev.monarchui.components.intelligence.aiprompttoolbar.MonarchAiPromptToolbarTokens
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
import kotlin.test.assertNotEquals

internal object MonarchAiPromptToolbarFixtures : ComponentFixtures {
    override val id = "ai-prompt-toolbar"
    override val fixtures = listOf(
        fixture("buttons and toggles") {
            MonarchAiPromptToolbar {
                MonarchAiPromptToolbarButton(text = "Attach", onClick = {})
                MonarchAiPromptToolbarToggle(text = "Search the web", checked = true, onCheckedChange = {})
                MonarchAiPromptToolbarToggle(text = "Deep research", checked = false, onCheckedChange = {})
            }
        },
        fixture("icons and icon only") {
            MonarchAiPromptToolbar {
                MonarchAiPromptToolbarButton(
                    text = "Attach",
                    onClick = {},
                    hideLabel = true,
                    icon = { MonarchIcon(TestIcon, contentDescription = null) },
                )
                MonarchAiPromptToolbarToggle(
                    text = "Search the web",
                    checked = true,
                    onCheckedChange = {},
                    icon = { MonarchIcon(TestIcon, contentDescription = null) },
                )
                MonarchAiPromptToolbarToggle(
                    text = "Voice",
                    checked = false,
                    onCheckedChange = {},
                    hideLabel = true,
                    icon = { MonarchIcon(TestIcon, contentDescription = null) },
                )
            }
        },
        fixture("disabled") {
            MonarchAiPromptToolbar {
                MonarchAiPromptToolbarButton(text = "Attach", onClick = {}, enabled = false)
                MonarchAiPromptToolbarToggle(text = "Think", checked = true, onCheckedChange = {}, enabled = false)
                MonarchAiPromptToolbarToggle(text = "Draft", checked = false, onCheckedChange = {}, enabled = false)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiPromptToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theToolbarIsAGroupNamedPromptOptions() {
        rule.setMonarchContent {
            MonarchAiPromptToolbar(modifier = Modifier.testTag("toolbar")) {
                MonarchAiPromptToolbarButton(text = "Attach", onClick = {})
            }
        }
        rule.onNodeWithTag("toolbar")
            .assertContentDescriptionEquals("Prompt options")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun theGroupNameCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiPromptToolbar(modifier = Modifier.testTag("toolbar"), label = "Options for the reply") {
                MonarchAiPromptToolbarButton(text = "Attach", onClick = {})
            }
        }
        rule.onNodeWithTag("toolbar").assertContentDescriptionEquals("Options for the reply")
    }

    @Test
    fun aButtonCallsBackOnlyWhenEnabled() {
        var clicks = 0
        var enabled by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiPromptToolbarButton(text = "Attach", onClick = { clicks++ }, enabled = enabled)
        }
        rule.onNodeWithText("Attach").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, clicks)
        enabled = false
        rule.onNodeWithText("Attach").assertIsNotEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun anIconOnlyButtonKeepsItsLabelAsItsName() {
        rule.setMonarchContent {
            MonarchAiPromptToolbarButton(
                text = "Attach",
                onClick = {},
                hideLabel = true,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Attach").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Attach").assertDoesNotExist()
    }

    @Test
    fun hideLabelWithoutAnIconStillShowsTheLabel() {
        rule.setMonarchContent {
            MonarchAiPromptToolbarToggle(
                text = "Search the web",
                checked = false,
                onCheckedChange = {},
                hideLabel = true,
            )
        }
        rule.onNodeWithText("Search the web").assertExists()
    }

    @Test
    fun aToggleIsSelectedWhenOnAndFlipsWithTheSameLabel() {
        rule.setMonarchContent {
            var on by remember { mutableStateOf(false) }
            MonarchAiPromptToolbarToggle(text = "Search the web", checked = on, onCheckedChange = { on = it })
        }
        val toggle = rule.onNodeWithText("Search the web")
        toggle.assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density)
        toggle.performClick()
        rule.onNodeWithText("Search the web").assertIsSelected()
        rule.onNodeWithText("Search the web").performClick()
        rule.onNodeWithText("Search the web").assertIsNotSelected()
    }

    @Test
    fun aDisabledToggleDoesNotCallBack() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAiPromptToolbarToggle(
                text = "Think",
                checked = true,
                onCheckedChange = { changes++ },
                enabled = false,
            )
        }
        rule.onNodeWithText("Think").assertIsNotEnabled().assertIsSelected().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun anIconOnlyToggleIsNamedByItsLabel() {
        rule.setMonarchContent {
            MonarchAiPromptToolbarToggle(
                text = "Voice",
                checked = true,
                onCheckedChange = {},
                hideLabel = true,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Voice").assertIsSelected().assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun controlsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiPromptToolbar {
                MonarchAiPromptToolbarButton(text = "Attach", onClick = {})
                MonarchAiPromptToolbarToggle(text = "Web", checked = false, onCheckedChange = {})
            }
        }
        rule.onNodeWithText("Attach", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Web", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longLabelsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiPromptToolbar {
                MonarchAiPromptToolbarToggle(
                    text = "Search the web and the company wiki",
                    checked = true,
                    onCheckedChange = {},
                )
            }
        }
        rule.onNodeWithText("Search the web and the company wiki", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aToggleThatIsOnUsesTheAssistantRolesAndABorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val toggle = MonarchAiPromptToolbarTokens.toggleColors(colors)
            assertEquals(colors.aiSubtle, toggle.on.container, theme.id)
            assertEquals(colors.aiStrong, toggle.on.content, theme.id)
            assertEquals(colors.aiBorder, toggle.on.border, theme.id)
            assertEquals(colors.textMuted, toggle.off.content, theme.id)
            assertEquals(Color.Transparent, toggle.off.border, theme.id)
            assertEquals(colors.textDisabled, toggle.disabledOn.content, theme.id)
            assertNotEquals(Color.Transparent, toggle.disabledOn.border, theme.id)
        }
    }
}
