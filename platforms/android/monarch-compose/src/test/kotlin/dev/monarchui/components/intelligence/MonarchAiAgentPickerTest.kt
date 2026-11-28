@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiagentcard.MonarchAiAgentTool
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPicker
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerLabels
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerLayout
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerOption
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import kotlin.test.assertTrue

private val Research = MonarchAiAgentPickerOption(
    agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Analyst"),
    description = "Answers with citations.",
    capabilities = listOf("Cites sources"),
    tools = listOf(MonarchAiAgentTool("Search documents")),
)
private val Writer = MonarchAiAgentPickerOption(
    agent = MonarchAiAgentRef(name = "Writing agent", role = "Writer"),
    state = MonarchAiAgentState.Working,
)
private val Scheduler = MonarchAiAgentPickerOption(
    agent = MonarchAiAgentRef(name = "Scheduling agent", id = "scheduler"),
    enabled = false,
)

internal object MonarchAiAgentPickerFixtures : ComponentFixtures {
    override val id = "ai-agent-picker"
    override val fixtures = listOf(
        fixture("one chosen") {
            MonarchAiAgentPicker(agents = listOf(Research, Writer), selected = "research", onSelect = {})
        },
        fixture("none chosen, one unavailable") {
            MonarchAiAgentPicker(agents = listOf(Writer, Scheduler), selected = null, onSelect = {})
        },
        fixture("disabled") {
            MonarchAiAgentPicker(agents = listOf(Research), selected = "research", onSelect = {}, enabled = false)
        },
        fixture("grid") {
            MonarchAiAgentPicker(
                agents = listOf(Writer, Scheduler),
                selected = "Writing agent",
                onSelect = {},
                layout = MonarchAiAgentPickerLayout.Grid,
            )
        },
        fixture("empty") { MonarchAiAgentPicker(agents = emptyList(), selected = null, onSelect = {}) },
        fixture("long name") {
            MonarchAiAgentPicker(
                agents = listOf(
                    MonarchAiAgentPickerOption(
                        MonarchAiAgentRef(name = "Quarterly revenue reconciliation agent for the finance team"),
                    ),
                ),
                selected = null,
                onSelect = {},
            )
        },
    )
}

private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiAgentPickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachAgentIsOneRadioButtonWithItsState() {
        rule.setMonarchContent {
            MonarchAiAgentPicker(agents = listOf(Research, Writer, Scheduler), selected = "research", onSelect = {})
        }
        val radios = rule.onAllNodes(isRadio).fetchSemanticsNodes()
        assertEquals(3, radios.size)
        rule.onNode(isRadio and hasContentDescription("Research agent", substring = true)).assertIsSelected()
        rule.onNode(isRadio and hasContentDescription("Writing agent", substring = true)).assertIsNotSelected()
        rule.onNode(isRadio and hasContentDescription("Scheduling agent", substring = true)).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Choose an agent").assertExists()
    }

    @Test
    fun choosingAnAgentReportsItsKeyOnce() {
        var selected by mutableStateOf<String?>("research")
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiAgentPicker(
                agents = listOf(Research, Writer, Scheduler),
                selected = selected,
                onSelect = {
                    chosen += it
                    selected = it
                },
            )
        }
        rule.onNode(isRadio and hasContentDescription("Writing agent", substring = true))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(isRadio and hasContentDescription("Writing agent", substring = true)).assertIsSelected()
        rule.onNode(isRadio and hasContentDescription("Writing agent", substring = true)).performClick()
        rule.onNode(isRadio and hasContentDescription("Scheduling agent", substring = true)).performClick()
        assertEquals(listOf("Writing agent"), chosen, "the name is the key without an id; no repeat; disabled ignored")
    }

    @Test
    fun aDisabledPickerChoosesNothing() {
        rule.setMonarchContent {
            MonarchAiAgentPicker(
                agents = listOf(Research, Writer),
                selected = null,
                onSelect = { error("disabled") },
                enabled = false,
            )
        }
        rule.onNode(isRadio and hasContentDescription("Writing agent", substring = true))
            .assertIsNotEnabled()
            .performClick()
    }

    @Test
    fun anEmptyPickerSaysSo() {
        rule.setMonarchContent { MonarchAiAgentPicker(agents = emptyList(), selected = null, onSelect = {}) }
        rule.onNodeWithText("No agents are available.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchAiAgentPicker(
                    agents = listOf(Research),
                    selected = null,
                    onSelect = {},
                    labels = MonarchAiAgentPickerLabels(label = "Choisir un agent"),
                )
                MonarchAiAgentPicker(
                    agents = emptyList(),
                    selected = null,
                    onSelect = {},
                    labels = MonarchAiAgentPickerLabels(empty = "Aucun agent."),
                )
            }
        }
        rule.onNodeWithContentDescription("Choisir un agent").assertExists()
        rule.onNodeWithText("Aucun agent.").assertExists()
    }

    @Test
    fun theCardIsAtTheStartAndTheMarkAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAgentPicker(
                agents = listOf(MonarchAiAgentPickerOption(Writer.agent, description = "Drafts")),
                selected = "Writing agent",
                onSelect = {},
            )
        }
        val row = rule.onNode(isRadio).fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithText("Drafts", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(row.right - text.right < text.left - row.left, "the card sits at the right, the mark left")
    }

    @Test
    fun aLongDescriptionWrapsAtTheLargestFontSize() {
        val text = "Reads the documents in this space and answers with citations"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiAgentPicker(
                agents = listOf(MonarchAiAgentPickerOption(Research.agent, description = text)),
                selected = null,
                onSelect = {},
            )
        }
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLayoutRules() {
        val t = MonarchAiAgentPickerTokens
        assertEquals(1, t.columns(MonarchAiAgentPickerLayout.List, 900.dp, 1f))
        assertEquals(2, t.columns(MonarchAiAgentPickerLayout.Grid, 900.dp, 1f))
        assertEquals(1, t.columns(MonarchAiAgentPickerLayout.Grid, 400.dp, 1f))
        assertEquals(1, t.columns(MonarchAiAgentPickerLayout.Grid, 900.dp, 2f))
        assertTrue(t.choosable(Research, true))
        assertTrue(!t.choosable(Scheduler, true) && !t.choosable(Research, false))
        assertEquals("research", Research.key)
        assertEquals("Writing agent", Writer.key)
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.aiSolid, MonarchAiAgentPickerTokens.selectedBorder(c), theme.id)
            assertEquals(c.textMuted, MonarchAiAgentPickerTokens.emptyColor(c), theme.id)
        }
    }
}
