@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRoster
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterLabels
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterMember
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterTokens
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterVariant
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Coordinator = MonarchAiAgentRef(name = "Coordinator", id = "coordinator", role = "Plans the work")
private val Research = MonarchAiAgentRef(name = "Research agent", id = "research")
private val Writer = MonarchAiAgentRef(name = "Writing agent", id = "writer")
private val Review = MonarchAiAgentRef(name = "Review agent", id = "review")

private fun team(research: MonarchAiAgentState = MonarchAiAgentState.Waiting) = listOf(
    MonarchAiMultiAgentRosterMember(Coordinator, MonarchAiAgentState.Working, "Splitting the request"),
    MonarchAiMultiAgentRosterMember(Research, research, "Needs access to the CRM"),
    MonarchAiMultiAgentRosterMember(Writer),
)

internal object MonarchAiMultiAgentRosterFixtures : ComponentFixtures {
    override val id = "ai-multi-agent-roster"
    override val fixtures = listOf(
        fixture("list in every state") {
            MonarchAiMultiAgentRoster(
                members = team() + MonarchAiMultiAgentRosterMember(Review, MonarchAiAgentState.Error),
                showSummary = false,
            )
        },
        fixture("list with summary") { MonarchAiMultiAgentRoster(members = team().take(2)) },
        fixture("stack with overflow") {
            MonarchAiMultiAgentRoster(
                members = team() + MonarchAiMultiAgentRosterMember(Review, MonarchAiAgentState.Error),
                variant = MonarchAiMultiAgentRosterVariant.Stack,
                maxVisible = 2,
            )
        },
        fixture("stack without summary") {
            MonarchAiMultiAgentRoster(
                members = team(),
                variant = MonarchAiMultiAgentRosterVariant.Stack,
                showSummary = false,
            )
        },
        fixture("long task") {
            MonarchAiMultiAgentRoster(
                members = listOf(
                    MonarchAiMultiAgentRosterMember(
                        MonarchAiAgentRef(name = "Quarterly revenue reconciliation agent", role = "Finance"),
                        MonarchAiAgentState.Working,
                        "Matching every invoice of the quarter against the bank statements",
                    ),
                ),
            )
        },
        fixture("empty") { MonarchAiMultiAgentRoster(members = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMultiAgentRosterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRowIsOneElementWithNameKindRoleTaskAndState() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiMultiAgentRoster(members = team()) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Coordinator (AI agent), Plans the work, Splitting the request, Working")
            .assertExists()
            .assertHasNoClickAction()
        rule.onNodeWithContentDescription("Research agent (AI agent), Needs access to the CRM, Waiting for you")
            .assertExists()
        rule.onNodeWithContentDescription("Writing agent (AI agent), Idle").assertExists()
        rule.onNodeWithContentDescription("Agents").assertExists()
        rule.onNodeWithText("3 agents · 1 working · 1 waiting for you").assertExists()
    }

    @Test
    fun theStackPutsAgentsThatNeedAttentionFirstAndReadsTheHiddenOnes() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiMultiAgentRoster(
                members = team() + MonarchAiMultiAgentRosterMember(Review, MonarchAiAgentState.Error),
                variant = MonarchAiMultiAgentRosterVariant.Stack,
                maxVisible = 2,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Review agent, AI agent", substring = true).assertExists()
        rule.onNodeWithContentDescription("Coordinator, Working; Writing agent, Idle").assertExists()
        rule.onNodeWithText("4 agents · 1 working · 1 waiting for you · 1 with errors").assertExists()
        rule.onNodeWithContentDescription("Review agent, AI agent", substring = true, useUnmergedTree = true)
            .assertIsBefore(
                rule.onNodeWithContentDescription("Research agent, AI agent", substring = true, useUnmergedTree = true),
                LayoutDirection.Ltr,
            )
    }

    @Test
    fun theStackRunsFromTheEndRightToLeft() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiMultiAgentRoster(members = team(), variant = MonarchAiMultiAgentRosterVariant.Stack)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Research agent, AI agent", substring = true, useUnmergedTree = true)
            .assertIsBefore(
                rule.onNodeWithContentDescription("Coordinator, AI agent", substring = true, useUnmergedTree = true),
                LayoutDirection.Rtl,
            )
    }

    @Test
    fun aStateChangeIsAnnouncedButNotTheFirstState() {
        var members by mutableStateOf(team())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiMultiAgentRoster(members = members) }
        rule.mainClock.advanceTimeBy(100)
        rule.onAllNodes(hasContentDescription("Research agent: Waiting for you")).assertCountEquals(0)
        members = team(research = MonarchAiAgentState.Error)
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Research agent: Error")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun announcementsCanBeTurnedOffAndTheLabelsReplaceTheStrings() {
        var members by mutableStateOf(team())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiMultiAgentRoster(
                members = members,
                announcesChanges = false,
                labels = MonarchAiMultiAgentRosterLabels(
                    label = "Agenten",
                    agentKind = "KI",
                    state = { it.name },
                    summary = { total, counts -> "$total / ${counts[MonarchAiAgentState.Working]}" },
                ),
            )
        }
        rule.mainClock.advanceTimeBy(100)
        members = team(research = MonarchAiAgentState.Error)
        rule.mainClock.advanceTimeBy(100)
        rule.onAllNodes(hasContentDescription("Research agent: Error")).assertCountEquals(0)
        rule.onNodeWithContentDescription("Research agent (KI), Needs access to the CRM, Error").assertExists()
        rule.onNodeWithContentDescription("Agenten").assertExists()
        rule.onNodeWithText("3 / 1").assertExists()
    }

    @Test
    fun anEmptyRosterSaysSoWithoutASummary() {
        rule.setMonarchContent { MonarchAiMultiAgentRoster(members = emptyList()) }
        rule.onNodeWithText("No agents are working on this.").assertExists()
        rule.onAllNodes(hasContentDescription("Agents")).assertCountEquals(0)
    }

    @Test
    fun longTasksWrapAtTheLargestFontScale() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMultiAgentRoster(
                members = listOf(
                    MonarchAiMultiAgentRosterMember(Research, MonarchAiAgentState.Idle, "Comparing three sources"),
                ),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Comparing three sources", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Idle", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStateTextIsAfterTheNameInBothDirections() {
        assertEquals(listOf("Idle"), MonarchAiMultiAgentRosterTokens.rowDetails(null, " ", "Idle"))
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiMultiAgentRoster(members = listOf(MonarchAiMultiAgentRosterMember(Writer)))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Writing agent", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Idle", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theLogicSortsCountsAndFindsChanges() {
        val tokens = MonarchAiMultiAgentRosterTokens
        val members = team() + MonarchAiMultiAgentRosterMember(Review, MonarchAiAgentState.Error)
        assertEquals(
            listOf("review", "research", "coordinator", "writer"),
            tokens.sortedForStack(members).map { it.id },
        )
        assertEquals(1, tokens.counts(members)[MonarchAiAgentState.Idle])
        assertTrue(tokens.changed(null, members).isEmpty())
        val before = tokens.snapshot(team())
        assertEquals(listOf("review"), tokens.changed(before, members).map { it.id })
        assertEquals("Agent", MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(name = "Agent")).id)
    }

    @Test
    fun coloursFollowTheStateInEveryTheme() {
        val tokens = MonarchAiMultiAgentRosterTokens
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            assertEquals(Color.Transparent, tokens.rowColor(MonarchAiAgentState.Working, colors))
            assertEquals(colors.warningSubtle, tokens.rowColor(MonarchAiAgentState.Waiting, colors))
            assertEquals(colors.dangerSubtle, tokens.rowColor(MonarchAiAgentState.Error, colors))
            assertEquals(colors.textMuted, tokens.stateColor(MonarchAiAgentState.Idle, colors))
            assertEquals(colors.aiStrong, tokens.stateColor(MonarchAiAgentState.Working, colors))
            assertEquals(colors.warningStrong, tokens.stateColor(MonarchAiAgentState.Waiting, colors))
            assertEquals(colors.dangerStrong, tokens.stateColor(MonarchAiAgentState.Error, colors))
        }
    }
}
