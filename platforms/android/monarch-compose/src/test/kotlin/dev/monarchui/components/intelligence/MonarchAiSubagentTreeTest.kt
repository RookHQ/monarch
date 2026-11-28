@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagent
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagentStatus
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagentTree
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagentTreeLabels
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagentTreeRules
import dev.monarchui.components.intelligence.aisubagenttree.MonarchAiSubagentTreeTokens
import dev.monarchui.foundation.MonarchTone
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
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val T0 = 1_700_000_000_000L
private const val MINUTE = 60_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val Researcher = MonarchAiSubagent(
    id = "researcher",
    name = "Researcher",
    status = MonarchAiSubagentStatus.Done,
    role = "Find the latest filings",
    toolCalls = 1,
    startedAt = T0,
    endedAt = T0 + 3 * MINUTE,
    summary = "Found 3 filings.",
    partId = "call_1",
)
private val Analyst = MonarchAiSubagent(
    id = "analyst",
    name = "Analyst",
    status = MonarchAiSubagentStatus.NeedsInput,
    stepsDone = 1,
)
private val Lead = MonarchAiSubagent(
    id = "lead",
    name = "Lead",
    status = MonarchAiSubagentStatus.Working,
    activity = "Reading the filings",
    stepsDone = 2,
    stepsTotal = 5,
    children = listOf(Researcher, Analyst),
)

private val Chevron: @androidx.compose.runtime.Composable () -> Unit = {
    MonarchIcon(TestIcon, contentDescription = null)
}

internal object MonarchAiSubagentTreeFixtures : ComponentFixtures {
    override val id = "ai-subagent-tree"
    override val fixtures = listOf(
        fixture("lead with a sub-agent, open") {
            MonarchAiSubagentTree(
                agents = listOf(Lead.copy(children = listOf(Analyst))),
                expandIcon = Chevron,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
        fixture("ended sub-agent with times") {
            MonarchAiSubagentTree(
                agents = listOf(Researcher),
                expandIcon = Chevron,
                locale = Locale.US,
                timeZone = Utc.id,
            )
        },
        fixture("selected agent with actions") {
            MonarchAiSubagentTree(
                agents = listOf(Analyst),
                expandIcon = Chevron,
                selection = "analyst",
                onRespond = {},
                onOpen = {},
                onStop = {},
            )
        },
        fixture("every status") {
            MonarchAiSubagentTree(
                agents = MonarchAiSubagentStatus.entries.take(3).map { MonarchAiSubagent(it.name, it.name, it) },
                expandIcon = Chevron,
            )
        },
        fixture("ended statuses") {
            MonarchAiSubagentTree(
                agents = MonarchAiSubagentStatus.entries.drop(3).map { MonarchAiSubagent(it.name, it.name, it) },
                expandIcon = Chevron,
            )
        },
        fixture("closed lead") {
            MonarchAiSubagentTree(agents = listOf(Lead), expandIcon = Chevron, expanded = emptySet())
        },
        fixture("empty") { MonarchAiSubagentTree(agents = emptyList(), expandIcon = Chevron) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSubagentTreeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun time(millis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).apply { timeZone = Utc }.format(millis)

    @Test
    fun summaryAndEachAgentAreReadInWords() {
        rule.setMonarchContent {
            MonarchAiSubagentTree(agents = listOf(Lead), expandIcon = Chevron, locale = Locale.US, timeZone = Utc.id)
        }
        rule.onNodeWithText("3 agents: 1 working, 1 needs you, 1 done").assertExists()
        rule.onNodeWithContentDescription("Agents").assertExists()
        rule.onNodeWithContentDescription("Lead, Working, Reading the filings, 2 of 5 steps").assertExists()
        rule.onNodeWithContentDescription(
            "Researcher, Done, Find the latest filings, 1 tool call, ${time(T0)} to ${time(T0 + 3 * MINUTE)}, " +
                "Found 3 filings.",
        ).assertExists()
        rule.onNodeWithContentDescription("Analyst, Needs input, 1 step").assertExists()
    }

    @Test
    fun selectingAnAgentShowsOnlyTheActionsThatApply() {
        val selected = mutableListOf<String>()
        val actions = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiSubagentTree(
                agents = listOf(Lead),
                expandIcon = Chevron,
                onSelect = { selected += it.id },
                onRespond = { actions += "respond ${it.id}" },
                onOpenPart = { actions += "part ${it.partId}" },
                onStop = { actions += "stop ${it.id}" },
            )
        }
        val analyst = rule.onNodeWithContentDescription("Analyst, Needs input, 1 step")
        analyst.assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        analyst.assertIsSelected()
        assertEquals(listOf("analyst"), selected)
        rule.onNodeWithContentDescription("Actions for Analyst").assertExists()
        rule.onNodeWithText("Stop").assertDoesNotExist()
        rule.onNodeWithText("Respond").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Lead, Working", substring = true).performClick()
        rule.onNodeWithText("Respond").assertDoesNotExist()
        rule.onNodeWithText("Show in conversation").assertDoesNotExist()
        rule.onNodeWithText("Stop").performClick()
        rule.onNodeWithContentDescription("Researcher", substring = true).performClick()
        rule.onNodeWithText("Show in conversation").performClick()
        assertEquals(listOf("respond analyst", "stop lead", "part call_1"), actions)
    }

    @Test
    fun theToggleClosesAndOpensABranchAndIsHoisted() {
        var open by mutableStateOf(setOf("lead"))
        rule.setMonarchContent {
            MonarchAiSubagentTree(
                agents = listOf(Lead),
                expandIcon = Chevron,
                expanded = open,
                onExpandedChange = { open = it },
            )
        }
        rule.onNodeWithContentDescription("Collapse Lead").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(emptySet(), open)
        rule.onNodeWithContentDescription("Analyst", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Expand Lead").performClick()
        rule.onNodeWithContentDescription("Analyst", substring = true).assertExists()
    }

    @Test
    fun statusChangesAreAnnouncedButNotTheFirstLook() {
        var agents by mutableStateOf(listOf(Lead))
        rule.setMonarchContent { MonarchAiSubagentTree(agents = agents, expandIcon = Chevron) }
        rule.onNode(hasContentDescription("Lead: Working")).assertDoesNotExist()
        agents = listOf(Lead.copy(status = MonarchAiSubagentStatus.Done))
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Lead: Done"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun emptyTreeAndLabelsReplaceStrings() {
        rule.setMonarchContent {
            MonarchAiSubagentTree(
                agents = listOf(Analyst),
                expandIcon = Chevron,
                selection = "analyst",
                onRespond = {},
                labels = MonarchAiSubagentTreeLabels(
                    respond = "Répondre",
                    status = { if (it == MonarchAiSubagentStatus.NeedsInput) "Attend" else it.name },
                    agentCount = { "$it agent(s)" },
                ),
            )
        }
        rule.onNodeWithText("Répondre").assertExists()
        rule.onNodeWithText("1 agent(s): 1 needs you").assertExists()
        rule.onNodeWithContentDescription("Analyst, Attend, 1 step").assertExists()
    }

    @Test
    fun emptyTreeSaysSo() {
        rule.setMonarchContent { MonarchAiSubagentTree(agents = emptyList(), expandIcon = Chevron) }
        rule.onNodeWithText("No agents yet").assertExists()
    }

    @Test
    fun theToggleSitsBeforeTheAgentRightToLeftAndNamesWrap() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchAiSubagentTree(agents = listOf(Lead), expandIcon = Chevron)
        }
        rule.onNodeWithContentDescription("Collapse Lead")
            .assertIsBefore(rule.onNodeWithText("Lead", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("Reading the filings", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun logicFlattensCountsOpensAndFindsNews() {
        val tokens = MonarchAiSubagentTreeTokens
        assertEquals(listOf("lead", "researcher", "analyst"), tokens.flatten(listOf(Lead)).map { it.id })
        assertEquals(setOf("lead"), tokens.defaultExpanded(listOf(Lead)))
        val idle = Lead.copy(status = MonarchAiSubagentStatus.Done, children = listOf(Researcher))
        assertEquals(emptySet(), tokens.defaultExpanded(listOf(idle)))
        assertEquals(1, tokens.lines(listOf(Lead), emptySet()).size)
        assertEquals(listOf(0, 1, 1), tokens.lines(listOf(Lead), setOf("lead")).map { it.depth })
        assertEquals(setOf("a"), tokens.toggled(emptySet(), "a"))
        assertTrue(tokens.news(null, listOf(Lead)).isEmpty())
        val before = mapOf("lead" to MonarchAiSubagentStatus.Working)
        val known = before + ("researcher" to MonarchAiSubagentStatus.Done)
        assertEquals(listOf("analyst"), tokens.news(known, listOf(Lead)).map { it.id })
        assertTrue(MonarchAiSubagentTreeRules.canStop(Lead))
        assertFalse(MonarchAiSubagentTreeRules.canStop(Researcher))
        assertTrue(MonarchAiSubagentTreeRules.canRespond(Analyst))
        assertTrue(MonarchAiSubagentTreeRules.canOpenPart(Researcher))
        assertFalse(MonarchAiSubagentTreeRules.canOpenPart(Analyst))
        assertEquals(MonarchTone.Warning, tokens.tone(MonarchAiSubagentStatus.NeedsInput))
        assertEquals(MonarchTone.Danger, tokens.tone(MonarchAiSubagentStatus.Failed))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentSubtle, tokens.rowColor(true, colors))
            assertEquals(colors.accentSolid, tokens.selectedMarkColor(colors))
        }
    }
}
