package dev.monarchui.components.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScope
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScopeLabels
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScopeLogic
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScopeTokens
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSource
import dev.monarchui.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSourceKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
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

private val knowledgeChevron: @Composable () -> Unit = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) }

private val knowledgeSources = listOf(
    MonarchAiKnowledgeSource(
        id = "handbook",
        label = "Handbook",
        kind = MonarchAiKnowledgeSourceKind.Space,
        description = "2 documents",
        children = listOf(
            MonarchAiKnowledgeSource("onboarding", "Onboarding", MonarchAiKnowledgeSourceKind.Document),
            MonarchAiKnowledgeSource("expenses", "Expenses", MonarchAiKnowledgeSourceKind.Document),
        ),
    ),
    MonarchAiKnowledgeSource("crm", "CRM", MonarchAiKnowledgeSourceKind.App),
)

internal object MonarchAiKnowledgeScopeFixtures : ComponentFixtures {
    override val id = "ai-knowledge-scope"
    override val fixtures = listOf(
        fixture("a group open, partly selected") {
            MonarchAiKnowledgeScope(
                sources = knowledgeSources,
                selection = listOf("onboarding"),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
                initiallyExpanded = setOf("handbook"),
            )
        },
        fixture("everything selected, closed") {
            MonarchAiKnowledgeScope(
                sources = knowledgeSources,
                selection = listOf("handbook", "crm"),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
            )
        },
        fixture("disabled, no summary") {
            MonarchAiKnowledgeScope(
                sources = knowledgeSources,
                selection = listOf("crm"),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
                enabled = false,
                showSummary = false,
            )
        },
        fixture("long names") {
            MonarchAiKnowledgeScope(
                sources = listOf(
                    MonarchAiKnowledgeSource(
                        id = "long",
                        label = "Customer support macros and escalation playbooks for every region",
                        kind = MonarchAiKnowledgeSourceKind.Document,
                        description = "Updated by the support operations team every Monday",
                    ),
                ),
                selection = emptyList(),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
            )
        },
        fixture("empty") {
            MonarchAiKnowledgeScope(
                sources = emptyList(),
                selection = emptyList(),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiKnowledgeScopeTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Scope(start: List<String> = emptyList(), expanded: Set<String> = emptySet(), enabled: Boolean = true) {
        var selection by remember { mutableStateOf(start) }
        MonarchAiKnowledgeScope(
            sources = knowledgeSources,
            selection = selection,
            onSelectionChange = { selection = it },
            expandIcon = knowledgeChevron,
            enabled = enabled,
            initiallyExpanded = expanded,
        )
    }

    @Test
    fun aPartlySelectedGroupIsAMixedCheckbox() {
        rule.setMonarchContent { Scope(start = listOf("onboarding"), expanded = setOf("handbook")) }
        rule.onNodeWithContentDescription("Handbook, Space, 2 documents")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        rule.onNodeWithContentDescription("Onboarding, Document")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.onNodeWithText("1 source selected").assertExists()
    }

    @Test
    fun checkingAGroupSelectsEverythingInIt() {
        rule.setMonarchContent { Scope(expanded = setOf("handbook")) }
        rule.onNodeWithContentDescription("Handbook, Space, 2 documents").performClick()
        rule.onNodeWithContentDescription("Expenses, Document")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        rule.onNodeWithText("1 source selected").assertExists()
        rule.onNodeWithContentDescription("CRM, App").performClick()
        rule.onNodeWithText("2 sources selected").assertExists()
    }

    @Test
    fun disabledCheckboxesDoNotChangeButGroupsStillOpen() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAiKnowledgeScope(
                sources = knowledgeSources,
                selection = emptyList(),
                onSelectionChange = { changes++ },
                expandIcon = knowledgeChevron,
                enabled = false,
            )
        }
        rule.onNodeWithContentDescription("CRM, App").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        rule.onNodeWithContentDescription("Show contents of Handbook").performClick()
        rule.onNodeWithContentDescription("Onboarding, Document").assertExists()
        rule.onNodeWithContentDescription("Hide contents of Handbook").assertExists()
    }

    @Test
    fun theRowOffersExpandAndCollapseActions() {
        rule.setMonarchContent { Scope() }
        rule.onNodeWithContentDescription("Handbook, Space, 2 documents")
            .performSemanticsAction(SemanticsActions.Expand)
        rule.onNodeWithContentDescription("Expenses, Document").assertExists()
        rule.onNodeWithContentDescription("Handbook, Space, 2 documents")
            .performSemanticsAction(SemanticsActions.Collapse)
        rule.onNodeWithContentDescription("Expenses, Document").assertDoesNotExist()
    }

    @Test
    fun rowsAndTheDisclosureButtonHave48dpTargets() {
        rule.setMonarchContent { Scope() }
        rule.onNodeWithContentDescription("CRM, App").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Show contents of Handbook").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theDisclosureButtonIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Scope() }
        rule.onNodeWithContentDescription("Show contents of Handbook")
            .assertIsBefore(rule.onNodeWithContentDescription("Handbook, Space, 2 documents"), LayoutDirection.Rtl)
    }

    @Test
    fun longNamesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Scope(expanded = setOf("handbook")) }
        rule.onNodeWithText("Handbook", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("2 documents", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiKnowledgeScope(
                sources = knowledgeSources,
                selection = emptyList(),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
                labels = MonarchAiKnowledgeScopeLabels(
                    label = "Sources",
                    kind = { it.name.uppercase() },
                    itemLabel = { label, kind -> "$label ($kind)" },
                    summary = { "$it chosen" },
                    expand = { "Open $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Sources").assertExists()
        rule.onNodeWithContentDescription("CRM (APP)").assertExists()
        rule.onNodeWithContentDescription("Open Handbook").assertExists()
        rule.onNodeWithText("0 chosen").assertExists()
    }

    @Test
    fun noSourcesShowsTheEmptyState() {
        rule.setMonarchContent {
            MonarchAiKnowledgeScope(
                sources = emptyList(),
                selection = emptyList(),
                onSelectionChange = {},
                expandIcon = knowledgeChevron,
            )
        }
        rule.onNodeWithText("No sources available", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theSelectionRulesMatchTheWeb() {
        val logic = MonarchAiKnowledgeScopeLogic
        assertEquals(
            listOf("handbook", "onboarding", "expenses"),
            logic.toggle(knowledgeSources, emptyList(), "handbook"),
        )
        assertEquals(emptyList(), logic.toggle(knowledgeSources, listOf("onboarding"), "handbook"))
        assertEquals(
            listOf("handbook", "onboarding", "expenses"),
            logic.toggle(knowledgeSources, listOf("onboarding"), "expenses"),
        )
        assertEquals(
            listOf("expenses", "gone"),
            logic.toggle(knowledgeSources, listOf("handbook", "gone"), "onboarding"),
        )
        val states = logic.states(knowledgeSources, listOf("handbook", "crm"))
        assertEquals(ToggleableState.On, states["expenses"])
        assertEquals(2, logic.count(knowledgeSources, states))
        assertEquals(1, logic.count(knowledgeSources, logic.states(knowledgeSources, listOf("expenses"))))
        assertEquals(4, logic.rows(knowledgeSources, listOf("handbook")).size)
        assertEquals(2, logic.rows(knowledgeSources, emptyList()).size)
    }

    @Test
    fun coloursComeFromTheTextRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiKnowledgeScopeTokens
            assertEquals(colors.textDefault, tokens.labelColor(enabled = true, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.labelColor(enabled = false, colors), theme.id)
            assertEquals(colors.textMuted, tokens.secondaryColor(enabled = true, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.secondaryColor(enabled = false, colors), theme.id)
            assertEquals(colors.textMuted, tokens.iconColor(colors), theme.id)
        }
    }
}
