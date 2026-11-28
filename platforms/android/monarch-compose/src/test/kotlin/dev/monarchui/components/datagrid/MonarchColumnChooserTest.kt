package dev.monarchui.components.datagrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooser
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooserColumn
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooserLabels
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooserTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertTrue

private val Columns = listOf(
    MonarchColumnChooserColumn("Name", id = "name", hideable = false),
    MonarchColumnChooserColumn("Status", id = "status"),
    MonarchColumnChooserColumn("Owner", id = "owner"),
)

private val ManyColumns = Columns + listOf("Created", "Updated", "Priority", "Region", "Plan")
    .map { MonarchColumnChooserColumn(it) }

@Composable
private fun Chooser(
    columns: List<MonarchColumnChooserColumn> = Columns,
    hidden: Set<String> = emptySet(),
    onHidden: (Set<String>) -> Unit = {},
    order: List<String> = emptyList(),
    onOrderChange: ((List<String>) -> Unit)? = null,
    minimumVisible: Int = 1,
    onReset: (() -> Unit)? = null,
    labels: MonarchColumnChooserLabels = MonarchColumnChooserLabels(),
) {
    MonarchColumnChooser(
        columns = columns,
        hiddenColumns = hidden,
        onHiddenColumnsChange = onHidden,
        modifier = Modifier.heightIn(max = 460.dp),
        order = order,
        onOrderChange = onOrderChange,
        minimumVisible = minimumVisible,
        onReset = onReset,
        labels = labels,
    )
}

internal object MonarchColumnChooserFixtures : ComponentFixtures {
    override val id = "column-chooser"
    override val fixtures = listOf(
        fixture("all shown") { Chooser() },
        fixture("some hidden, with reset") { Chooser(hidden = setOf("owner"), onReset = {}) },
        fixture("minimum reached") { Chooser(hidden = setOf("status"), minimumVisible = 2) },
        fixture("reorderable") { Chooser(onOrderChange = {}, order = listOf("owner", "name", "status")) },
        fixture("searchable") { Chooser(columns = ManyColumns.take(8)) },
        fixture("long labels") {
            Chooser(columns = listOf(MonarchColumnChooserColumn("Expected close date of the opportunity in the CRM")))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchColumnChooserTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announcement(): String? = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

    @Test
    fun theTitleAndCountAreOneHeading() {
        rule.setMonarchContent { Chooser(hidden = setOf("owner")) }
        rule.onNode(isHeading()).assertTextEqualsAll("Columns", "2 of 3 shown")
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTextEqualsAll(vararg texts: String) {
        val node = fetchSemanticsNode()
        assertEquals(texts.toList(), node.config[SemanticsProperties.Text].map { it.text })
    }

    @Test
    fun eachColumnIsASwitchAndTogglingReportsTheHiddenSet() {
        var hidden by mutableStateOf(emptySet<String>())
        rule.setMonarchContent { Chooser(hidden = hidden, onHidden = { hidden = it }) }
        val status = rule.onNodeWithText("Status")
        status.assertIsOn().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(setOf("status"), hidden)
        rule.onNodeWithText("Status").assertIsOff()
        rule.onNodeWithText("Show all").performClick()
        assertEquals(emptySet(), hidden)
    }

    @Test
    fun requiredColumnsAndTheLastShownColumnsCannotBeSwitchedOff() {
        rule.setMonarchContent { Chooser(hidden = setOf("status"), minimumVisible = 2) }
        rule.onNodeWithText("Name", substring = true).assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("Always shown", substring = true).assertExists()
        rule.onNodeWithText("Owner").assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("Status").assertIsOff().assertIsEnabled()
    }

    @Test
    fun theRulesOfOrderVisibilityAndSearch() {
        val tokens = MonarchColumnChooserTokens
        assertEquals(listOf("owner", "name", "status"), tokens.ordered(Columns, listOf("owner")).map { it.id })
        assertEquals(listOf("status", "name", "owner"), tokens.ordered(Columns, listOf("status", "gone")).map { it.id })
        assertTrue(tokens.isShown(Columns[0], setOf("name")))
        assertFalse(tokens.canToggle(Columns[1], emptySet(), shownCount = 1, minimumVisible = 1))
        assertTrue(tokens.canToggle(Columns[1], setOf("status"), shownCount = 1, minimumVisible = 1))
        assertEquals(listOf("Owner"), tokens.matching(Columns, " own ").map { it.label })
        assertEquals(listOf("b", "c", "a"), tokens.moved(listOf("a", "b", "c"), 0, 9))
    }

    @Test
    fun aMoveActionReportsTheNewOrderAndIsAnnounced() {
        var order by mutableStateOf(listOf("name", "status", "owner"))
        rule.setMonarchContent { Chooser(order = order, onOrderChange = { order = it }) }
        rule.onNodeWithContentDescription("Reorder Status").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Move up" }.action()
        rule.waitForIdle()
        assertEquals(listOf("status", "name", "owner"), order)
        assertEquals("Status moved to position 1 of 3.", announcement())
    }

    @Test
    fun theSearchFiltersRowsAndTurnsReorderingOff() {
        rule.setMonarchContent { Chooser(columns = ManyColumns, onOrderChange = {}) }
        rule.onNodeWithContentDescription("Reorder Status").assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("reg")
        rule.waitForIdle()
        rule.onNodeWithText("Region").assertExists()
        rule.onNodeWithText("Status").assertDoesNotExist()
        rule.onNodeWithContentDescription("Reorder Region").assertDoesNotExist()
    }

    @Test
    fun aSearchWithoutMatchesSaysSo() {
        rule.setMonarchContent { Chooser(columns = ManyColumns) }
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No columns match.").assertExists()
    }

    @Test
    fun resetRunsTheAppsCallback() {
        var resets = 0
        rule.setMonarchContent { Chooser(onReset = { resets++ }) }
        rule.onNodeWithText("Show all").assertDoesNotExist()
        rule.onNodeWithText("Reset").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, resets)
    }

    @Test
    fun theHandleComesBeforeTheColumnNameInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Chooser(onOrderChange = {}) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Reorder Owner").assertIsBefore(rule.onNodeWithText("Owner"), direction)
        }
    }

    @Test
    fun aLongLabelWrapsWholeAtTheLargestFontScale() {
        val long = "Expected close date of the opportunity in the CRM"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Chooser(columns = listOf(MonarchColumnChooserColumn(long), MonarchColumnChooserColumn("Owner")))
        }
        rule.onNodeWithText(long, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Chooser(
                hidden = setOf("owner"),
                onOrderChange = {},
                onReset = {},
                labels = MonarchColumnChooserLabels(
                    title = "Colonnes",
                    shownCount = { shown, total -> "$shown sur $total" },
                    required = "Toujours visible",
                    showAll = "Tout afficher",
                    reset = "Rétablir",
                    handle = { "Déplacer $it" },
                ),
            )
        }
        rule.onNode(isHeading()).assertTextEqualsAll("Colonnes", "2 sur 3")
        rule.onNodeWithText("Toujours visible", substring = true).assertExists()
        rule.onNodeWithText("Tout afficher").assertExists()
        rule.onNodeWithText("Rétablir").assertExists()
        rule.onNodeWithContentDescription("Déplacer Owner").assertExists()
    }

    @Test
    fun theCountUsesTheMutedTextInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchColumnChooserTokens.countColor(colors))
        }
    }
}
