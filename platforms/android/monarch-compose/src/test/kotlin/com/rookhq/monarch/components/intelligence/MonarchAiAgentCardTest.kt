@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCard
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCardLabels
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCardTokens
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCardVariant
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentTool
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Analyst")
private val Search = MonarchAiAgentTool("Search documents", "Finds files in the shared drive")

internal object MonarchAiAgentCardFixtures : ComponentFixtures {
    override val id = "ai-agent-card"
    override val fixtures = listOf(
        fixture("full, description and capabilities") {
            MonarchAiAgentCard(
                agent = Agent,
                description = "Reads the shared drive and writes briefs.",
                capabilities = listOf("Summarises documents", "Drafts briefs"),
            )
        },
        fixture("full, tools") {
            MonarchAiAgentCard(agent = Agent, tools = listOf(Search, MonarchAiAgentTool("Write")))
        },
        fixture("full, data, owner and an action") {
            MonarchAiAgentCard(agent = Agent, dataAccess = listOf("Documents in this space"), owner = "Sam Rivera") {
                MonarchButton(text = "Start a chat", onClick = {})
            }
        },
        fixture("compact") {
            MonarchAiAgentCard(
                agent = Agent,
                description = "Writes briefs.",
                capabilities = listOf("Drafts briefs"),
                tools = listOf(Search),
                variant = MonarchAiAgentCardVariant.Compact,
            )
        },
        fixture("working, no role") {
            MonarchAiAgentCard(agent = MonarchAiAgentRef(name = "Triage"), state = MonarchAiAgentState.Working)
        },
        fixture("error state") { MonarchAiAgentCard(agent = Agent, state = MonarchAiAgentState.Error) },
        fixture("long name") {
            MonarchAiAgentCard(agent = MonarchAiAgentRef(name = "Quarterly revenue reconciliation agent for finance"))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiAgentCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theNameAndRoleLineAreOneHeadingThatSaysAiAgent() {
        rule.setMonarchContent { MonarchAiAgentCard(agent = Agent) }
        rule.onNodeWithContentDescription("Research agent, Analyst · AI agent").assert(isHeading())
    }

    @Test
    fun theFullCardShowsEverySectionWithHeadings() {
        rule.setMonarchContent {
            MonarchAiAgentCard(
                agent = Agent,
                description = "Writes briefs.",
                capabilities = listOf("Drafts briefs"),
                tools = listOf(Search),
                dataAccess = listOf("Documents in this space"),
                owner = "Sam Rivera",
            )
        }
        rule.onNodeWithText("Writes briefs.").assertExists()
        listOf("What it can do", "Tools", "Data it can see").forEach { rule.onNodeWithText(it).assert(isHeading()) }
        rule.onNodeWithText("Drafts briefs").assertExists()
        rule.onNodeWithText("Documents in this space").assertExists()
        rule.onNodeWithContentDescription("Search documents, Finds files in the shared drive").assertExists()
        rule.onNodeWithContentDescription("Owner, Sam Rivera").assertExists()
    }

    @Test
    fun theCompactCardCountsToolsAndHidesTheOtherSections() {
        rule.setMonarchContent {
            MonarchAiAgentCard(
                agent = Agent,
                capabilities = listOf("Drafts briefs"),
                tools = listOf(Search, MonarchAiAgentTool("Write")),
                dataAccess = listOf("Documents"),
                owner = "Sam Rivera",
                variant = MonarchAiAgentCardVariant.Compact,
            ) { MonarchButton(text = "Start a chat", onClick = {}) }
        }
        rule.onNodeWithText("2 tools").assertExists()
        rule.onNodeWithText("Drafts briefs").assertExists()
        rule.onNode(hasContentDescription("What it can do")).assertExists()
        rule.onNodeWithText("Documents").assertDoesNotExist()
        rule.onNodeWithContentDescription("Owner, Sam Rivera").assertDoesNotExist()
        rule.onNodeWithText("Start a chat").assertDoesNotExist()
    }

    @Test
    fun oneToolIsSingular() {
        rule.setMonarchContent {
            MonarchAiAgentCard(agent = Agent, tools = listOf(Search), variant = MonarchAiAgentCardVariant.Compact)
        }
        rule.onNodeWithText("1 tool").assertExists()
    }

    @Test
    fun actionsAreSeparateButtons() {
        var started = 0
        rule.setMonarchContent {
            MonarchAiAgentCard(agent = Agent) { MonarchButton(text = "Start a chat", onClick = { started++ }) }
        }
        rule.onNode(hasText("Start a chat") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, started)
    }

    @Test
    fun aStateOtherThanIdleIsReadFromTheAvatar() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiAgentCard(agent = Agent, state = MonarchAiAgentState.Error) }
        rule.mainClock.advanceTimeByFrame()
        val described = rule.onAllNodes(hasContentDescription("Error", substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
        assertTrue(described.isNotEmpty())
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiAgentCard(
                agent = Agent,
                capabilities = listOf("Résume"),
                tools = listOf(Search),
                owner = "Sam",
                labels = MonarchAiAgentCardLabels(
                    kind = "Agent IA",
                    capabilities = "Ce qu’il sait faire",
                    tools = "Outils",
                    owner = "Responsable",
                ),
            )
        }
        rule.onNodeWithContentDescription("Research agent, Analyst · Agent IA").assertExists()
        rule.onNodeWithText("Ce qu’il sait faire").assertExists()
        rule.onNodeWithText("Outils").assertExists()
        rule.onNodeWithContentDescription("Responsable, Sam").assertExists()
    }

    @Test
    fun theToolCountCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiAgentCard(
                agent = Agent,
                tools = listOf(Search),
                variant = MonarchAiAgentCardVariant.Compact,
                labels = MonarchAiAgentCardLabels(toolCount = { "$it outil" }),
            )
        }
        rule.onNodeWithText("1 outil").assertExists()
    }

    @Test
    fun theToolCountIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAgentCard(agent = Agent, tools = listOf(Search), variant = MonarchAiAgentCardVariant.Compact)
        }
        rule.onNodeWithContentDescription("Research agent, Analyst · AI agent")
            .assertIsBefore(rule.onNodeWithText("1 tool"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongDescriptionWrapsAtTheLargestFontSize() {
        val text = "Reads the shared drive and writes short briefs for the team"
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiAgentCard(agent = Agent, description = text) }
        rule.onNodeWithText(text).assertTextFullyVisible()
    }

    @Test
    fun theLogicBuildsTheSpokenTexts() {
        val t = MonarchAiAgentCardTokens
        assertEquals("Analyst · AI agent", t.subtitle("Analyst", "AI agent"))
        assertEquals("AI agent", t.subtitle(null, "AI agent"))
        assertEquals("AI agent", t.subtitle(" ", "AI agent"))
        assertEquals("Search documents, Finds files in the shared drive", t.toolText(Search))
        assertEquals("Write", t.toolText(MonarchAiAgentTool("Write", "")))
        assertEquals("Owner, Sam", t.ownerText("Owner", "Sam"))
        assertEquals(MonarchAvatarSize.Lg, t.avatarSize(MonarchAiAgentCardVariant.Full))
        assertEquals(MonarchAvatarSize.Md, t.avatarSize(MonarchAiAgentCardVariant.Compact))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, MonarchAiAgentCardTokens.nameColor(c), theme.id)
            assertEquals(c.textMuted, MonarchAiAgentCardTokens.mutedColor(c), theme.id)
            assertEquals(c.textSubtle, MonarchAiAgentCardTokens.headingColor(c), theme.id)
        }
    }

    @Test
    fun nameIsReadOnceNotTwice() {
        rule.setMonarchContent { MonarchAiAgentCard(agent = Agent) }
        val named = rule.onAllNodes(hasText("Research agent")).fetchSemanticsNodes()
        assertTrue(named.isEmpty(), "the visible name is cleared under the merged heading")
        val heading = rule.onNodeWithContentDescription("Research agent", substring = true).fetchSemanticsNode()
        assertTrue(heading.config.contains(SemanticsProperties.Heading))
    }
}
