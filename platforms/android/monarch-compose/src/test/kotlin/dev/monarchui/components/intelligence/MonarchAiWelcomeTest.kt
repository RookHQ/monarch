package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import dev.monarchui.components.intelligence.aiwelcome.MonarchAiWelcome
import dev.monarchui.components.intelligence.aiwelcome.MonarchAiWelcomeTokens
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
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val Assistant = MonarchAiAgentRef(name = "Dossier assistant", role = "Writing assistant")
private val Starters = listOf(MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page."))

internal object MonarchAiWelcomeFixtures : ComponentFixtures {
    override val id = "ai-welcome"
    override val fixtures = listOf(
        fixture("title only, AI mark with an icon") {
            MonarchAiWelcome(title = "How can I help?", icon = { MonarchIcon(TestIcon, contentDescription = null) })
        },
        fixture("agent and description") {
            MonarchAiWelcome(
                title = "How can I help?",
                agent = Assistant,
                description = "I can read the page you are on.",
            )
        },
        fixture("capabilities") {
            MonarchAiWelcome(title = "Ask about projects", capabilities = listOf("Find a project", "Summarize a week"))
        },
        fixture("starters and footer") {
            MonarchAiWelcome(title = "Hi", footer = { MonarchAiDisclaimer() }) {
                MonarchAiStarterPrompts(prompts = Starters, onSelect = {})
            }
        },
        fixture("long title") {
            MonarchAiWelcome(title = "Welcome back to the quarterly planning assistant for the finance team")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiWelcomeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun agentTitleAndDescriptionAreReadInOrder() {
        rule.setMonarchContent {
            MonarchAiWelcome(title = "How can I help?", agent = Assistant, description = "I read the page.")
        }
        rule.onNodeWithContentDescription("AI assistant: Dossier assistant, Writing assistant").assertExists()
        rule.onNodeWithText("How can I help?").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("I read the page.").assertExists()
        rule.onNodeWithText("·").assertDoesNotExist()
    }

    @Test
    fun capabilitiesAreANamedGroupOfOneElementEach() {
        rule.setMonarchContent {
            MonarchAiWelcome(title = "Hi", capabilities = listOf("Find a project", "Summarize a week"))
        }
        rule.onNodeWithContentDescription("What I can help with").assertExists()
        rule.onNodeWithText("Find a project").assertExists()
        rule.onNodeWithText("Summarize a week").assertExists()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiWelcome(
                title = "Hi",
                agent = MonarchAiAgentRef(name = "Ada"),
                capabilities = listOf("Plan"),
                capabilitiesLabel = "Ce que je sais faire",
                agentPrefix = "Assistant IA :",
            )
        }
        rule.onNodeWithContentDescription("Ce que je sais faire").assertExists()
        rule.onNodeWithContentDescription("Assistant IA : Ada").assertExists()
    }

    @Test
    fun startersAreTappableAndFooterIsShown() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiWelcome(title = "Hi", footer = { MonarchAiDisclaimer(text = "Check important facts.") }) {
                MonarchAiStarterPrompts(prompts = Starters, onSelect = { chosen += it.prompt })
            }
        }
        rule.onNodeWithText("Summarize this page", substring = true)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("Summarize this page."), chosen)
        rule.onNodeWithText("Check important facts.", substring = true).assertExists()
    }

    @Test
    fun capabilityMarkSitsBeforeItsTextRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiWelcome(
                title = "Hi",
                capabilities = listOf("Plan"),
                capabilityIcon = { MonarchIcon(TestIcon, contentDescription = "mark") },
            )
        }
        rule.onNodeWithContentDescription("mark", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Plan", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun titleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiWelcome(title = "Welcome back to the planning assistant", capabilities = listOf("Plan a sprint"))
        }
        rule.onNodeWithText("Welcome back to the planning assistant").assertTextFullyVisible()
        rule.onNodeWithText("Plan a sprint", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun colorsComeFromTheAiRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val mark = MonarchAiWelcomeTokens.markColors(colors)
            assertEquals(colors.aiSubtle, mark.container)
            assertEquals(colors.aiStrong, mark.content)
            assertEquals(colors.aiBorder, mark.border)
            assertEquals(colors.aiStrong, MonarchAiWelcomeTokens.agentNameColor(colors))
            assertEquals(colors.textMuted, MonarchAiWelcomeTokens.mutedColor(colors))
        }
    }
}
