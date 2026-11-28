package dev.monarchui.components.intelligence

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPromptsLayout
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPromptsTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.MonarchControlState
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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

private val starters = listOf(
    MonarchAiStarterPrompt(
        id = "summary",
        title = "Summarise a document",
        prompt = "Summarise the attached document in five points.",
        icon = { MonarchIcon(TestIcon, contentDescription = null) },
    ),
    MonarchAiStarterPrompt("reply", "Draft a reply", "Draft a short, polite reply to this email."),
)

internal object MonarchAiStarterPromptsFixtures : ComponentFixtures {
    override val id = "ai-starter-prompts"
    override val fixtures = listOf(
        fixture("grid") { MonarchAiStarterPrompts(prompts = starters, onSelect = {}) },
        fixture("list") {
            MonarchAiStarterPrompts(prompts = starters, onSelect = {}, layout = MonarchAiStarterPromptsLayout.List)
        },
        fixture("prompt hidden") { MonarchAiStarterPrompts(prompts = starters, onSelect = {}, hidePrompt = true) },
        fixture("disabled") { MonarchAiStarterPrompts(prompts = starters.take(1), onSelect = {}, enabled = false) },
        fixture("long text") {
            MonarchAiStarterPrompts(
                prompts = listOf(
                    MonarchAiStarterPrompt(
                        "long",
                        "Prepare the agenda for the quarterly planning meeting",
                        "List the decisions to take, the people who must attend and the documents to read first.",
                    ),
                ),
                onSelect = {},
                hidePrompt = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiStarterPromptsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun choosingAStarterHandsItToTheApp() {
        var chosen: MonarchAiStarterPrompt? = null
        rule.setMonarchContent { MonarchAiStarterPrompts(prompts = starters, onSelect = { chosen = it }) }
        rule.onNodeWithText("Draft a reply").performClick()
        assertEquals("reply", chosen?.id)
        assertEquals("Draft a short, polite reply to this email.", chosen?.prompt)
    }

    @Test
    fun eachStarterIsOneButtonReadAsItsTitleAndPrompt() {
        rule.setMonarchContent { MonarchAiStarterPrompts(prompts = starters, onSelect = {}) }
        rule.onNode(hasText("Summarise a document") and hasText("Summarise the attached document in five points."))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun withHidePromptOnlyTheTitleIsShownAndRead() {
        rule.setMonarchContent { MonarchAiStarterPrompts(prompts = starters, onSelect = {}, hidePrompt = true) }
        rule.onNodeWithText("Draft a short, polite reply to this email.").assertDoesNotExist()
        rule.onNodeWithText("Draft a reply").assertExists()
    }

    @Test
    fun theGroupIsNamedAndTheNameCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiStarterPrompts(prompts = starters, onSelect = {}, modifier = Modifier.testTag("group"))
        }
        rule.onNodeWithTag("group").assertContentDescriptionEquals("Suggested prompts")
    }

    @Test
    fun theReplacedGroupName() {
        rule.setMonarchContent {
            MonarchAiStarterPrompts(starters, {}, Modifier.testTag("group"), label = "Pour commencer")
        }
        rule.onNodeWithTag("group").assertContentDescriptionEquals("Pour commencer")
    }

    @Test
    fun disabledStartersCannotBeChosen() {
        var chosen = 0
        rule.setMonarchContent { MonarchAiStarterPrompts(prompts = starters, onSelect = { chosen++ }, enabled = false) }
        rule.onNodeWithText("Draft a reply").assertIsNotEnabled().performClick()
        assertEquals(0, chosen)
    }

    @Test
    fun withNoStartersNothingIsShown() {
        rule.setMonarchContent {
            MonarchAiStarterPrompts(prompts = emptyList(), onSelect = {}, modifier = Modifier.testTag("group"))
        }
        rule.onNodeWithTag("group").assertDoesNotExist()
    }

    @Test
    fun everyStarterHasA48dpTarget() {
        rule.setMonarchContent { MonarchAiStarterPrompts(prompts = starters, onSelect = {}, hidePrompt = true) }
        rule.onNodeWithText("Draft a reply").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontScale() {
        val title = "Prepare the agenda for the quarterly planning meeting"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiStarterPrompts(listOf(MonarchAiStarterPrompt("a", title, "Agenda")), {}, hidePrompt = true)
        }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theGridFitsAsManyColumnsAsTheWidthAllowsAndOneAtLargeText() {
        val grid = MonarchAiStarterPromptsLayout.Grid
        assertEquals(1, MonarchAiStarterPromptsTokens.columns(grid, 320.dp, 1f))
        assertEquals(2, MonarchAiStarterPromptsTokens.columns(grid, 600.dp, 1f))
        assertEquals(3, MonarchAiStarterPromptsTokens.columns(grid, 700.dp, 1f))
        assertEquals(1, MonarchAiStarterPromptsTokens.columns(grid, 700.dp, LargestFontScale))
        assertEquals(1, MonarchAiStarterPromptsTokens.columns(MonarchAiStarterPromptsLayout.List, 700.dp, 1f))
    }

    @Test
    fun aPressedStarterTakesTheAssistantFillAndBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val resting = MonarchAiStarterPromptsTokens.colors(MonarchControlState(), colors)
            val pressed = MonarchAiStarterPromptsTokens.colors(MonarchControlState(pressed = true), colors)
            val disabled = MonarchAiStarterPromptsTokens.colors(MonarchControlState(enabled = false), colors)
            assertEquals(colors.bgSurface, resting.container, theme.id)
            assertEquals(colors.borderDefault, resting.border, theme.id)
            assertEquals(colors.aiSubtleHover, pressed.container, theme.id)
            assertEquals(colors.aiBorder, pressed.border, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertEquals(colors.aiStrong, MonarchAiStarterPromptsTokens.iconColor(true, colors), theme.id)
            assertEquals(colors.textMuted, MonarchAiStarterPromptsTokens.promptColor(true, colors), theme.id)
        }
    }
}
