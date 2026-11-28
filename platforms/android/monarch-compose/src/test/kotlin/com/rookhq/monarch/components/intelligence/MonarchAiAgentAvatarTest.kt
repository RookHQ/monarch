package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatarLabels
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatarTokens
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research", codename = "RA")

internal object MonarchAiAgentAvatarFixtures : ComponentFixtures {
    override val id = "ai-agent-avatar"
    override val fixtures = listOf(
        fixture("every state with its text") {
            androidx.compose.foundation.layout.Column {
                MonarchAiAgentState.entries.forEach {
                    MonarchAiAgentAvatar(agent = Agent, state = it, showState = true)
                }
            }
        },
        fixture("every size") {
            androidx.compose.foundation.layout.Row {
                MonarchAvatarSize.entries.forEach { MonarchAiAgentAvatar(agent = Agent, size = it) }
            }
        },
        fixture("name of symbols only, decorative") {
            MonarchAiAgentAvatar(
                agent = MonarchAiAgentRef(name = "★"),
                decorative = true,
                state = MonarchAiAgentState.Error,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiAgentAvatarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun readsTheNameAndTheKindAsAnImage() {
        rule.setMonarchContent { MonarchAiAgentAvatar(agent = Agent) }
        rule.onNodeWithContentDescription("Research agent, AI agent")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    @Test
    fun aStateOtherThanIdleIsReadInWords() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiAgentAvatar(agent = Agent, state = MonarchAiAgentState.Working) }
        rule.mainClock.advanceTimeByFrame()
        val node = rule.onNodeWithContentDescription("Research agent, AI agent, Working")
        assertEquals("Working", node.stateDescription())
    }

    @Test
    fun decorativeHidesTheAvatarButKeepsABusyState() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiAgentAvatar(agent = Agent, decorative = true)
                MonarchAiAgentAvatar(agent = Agent, decorative = true, state = MonarchAiAgentState.Waiting)
            }
        }
        rule.onAllNodesWithContentDescription("Research agent", substring = true).assertCountEquals(0)
        rule.onNodeWithContentDescription("Waiting for you").assertExists()
    }

    @Test
    fun visibleStateIsShownAndNotReadTwice() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiAgentAvatar(agent = Agent, state = MonarchAiAgentState.Error, showState = true)
        }
        rule.onNodeWithText("Error", useUnmergedTree = true)
            .assertTextFullyVisible()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        rule.onNodeWithContentDescription("Research agent, AI agent, Error").assertExists()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiAgentAvatar(
                agent = Agent,
                state = MonarchAiAgentState.Error,
                labels = MonarchAiAgentAvatarLabels(kind = "Agent IA", state = { "Erreur" }),
            )
        }
        rule.onNodeWithContentDescription("Research agent, Agent IA, Erreur").assertExists()
    }

    @Test
    fun initialsComeFromTheCodenameAndTheColourFromTheId() {
        val tokens = MonarchAiAgentAvatarTokens
        assertEquals("RA", tokens.initialsSource(Agent))
        assertEquals("Research agent", tokens.initialsSource(Agent.copy(codename = " ")))
        assertEquals("research", tokens.seed(Agent))
        assertEquals("Research agent", tokens.seed(Agent.copy(id = null)))
        assertNull(tokens.spoken("A", "AI agent", MonarchAiAgentState.Idle, "Idle", decorative = true))
    }

    @Test
    fun ringsFollowTheStateAndReducedMotionInEveryTheme() {
        val tokens = MonarchAiAgentAvatarTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertNull(tokens.ring(MonarchAiAgentState.Idle, colors, reducedMotion = false))
            val working = tokens.ring(MonarchAiAgentState.Working, colors, reducedMotion = false)!!
            assertEquals(listOf(colors.aiStrong, colors.aiSubtle, colors.aiStrong), working.colors)
            assertEquals(listOf(colors.aiStrong), tokens.ring(MonarchAiAgentState.Working, colors, true)!!.colors)
            assertTrue(tokens.ring(MonarchAiAgentState.Waiting, colors, false)!!.dashed)
            assertEquals(listOf(colors.dangerStrong), tokens.ring(MonarchAiAgentState.Error, colors, false)!!.colors)
            assertEquals(colors.dangerSolid, tokens.badgeColor(MonarchAiAgentState.Error, colors))
            assertEquals(colors.aiSolid, tokens.badgeColor(MonarchAiAgentState.Working, colors))
            assertNotEquals(colors.aiSolid, tokens.badgeContentColor(MonarchAiAgentState.Idle, colors))
        }
        assertTrue(tokens.turns(MonarchAiAgentState.Working, reducedMotion = false))
        assertFalse(tokens.turns(MonarchAiAgentState.Working, reducedMotion = true))
    }
}
