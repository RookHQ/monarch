package com.rookhq.monarch.components.datagrid

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTable
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableColumn
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableLabels
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableLayout
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRules
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSort
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSortDirection
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableTexts
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class Ticket(val id: String, val status: String, val owner: String, val replies: Int)

private val Tickets = listOf(
    Ticket("Ticket 42", "Open", "Ana Lima", 3),
    Ticket("Ticket 7", "Closed", "", 12),
    Ticket("Ticket 13", "Open", "Ben Okafor", 1),
)

private val Columns = listOf<MonarchDataTableColumn<Ticket>>(
    MonarchDataTableColumn("Ticket", id = "id", hideable = false) { it.id },
    MonarchDataTableColumn("Status", id = "status") { it.status },
    MonarchDataTableColumn("Owner", id = "owner") { it.owner },
    MonarchDataTableColumn("Replies", id = "replies", numeric = true) { it.replies.toString() },
)

@Composable
private fun Table(
    modifier: Modifier = Modifier,
    rows: List<Ticket> = Tickets,
    layout: MonarchDataTableLayout = MonarchDataTableLayout.List,
    sort: MonarchDataTableSort? = null,
    onSortChange: ((MonarchDataTableSort) -> Unit)? = null,
    selection: Set<String>? = null,
    onSelectionChange: ((Set<String>) -> Unit)? = null,
    hidden: Set<String> = emptySet(),
    loading: Boolean = false,
    errorMessage: String? = null,
    filtered: Boolean = false,
    rowActions: ((Ticket) -> List<MonarchDataTableRowAction>)? = null,
    onOpen: ((Ticket) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onClearFilters: (() -> Unit)? = null,
    labels: MonarchDataTableLabels = MonarchDataTableLabels(),
) {
    MonarchDataTable(
        caption = "Open tickets",
        rows = rows,
        columns = Columns,
        rowKey = { it.id },
        modifier = modifier,
        sort = sort,
        onSortChange = onSortChange,
        selection = selection,
        onSelectionChange = onSelectionChange,
        hiddenColumns = hidden,
        layout = layout,
        loading = loading,
        errorMessage = errorMessage,
        filtered = filtered,
        rowSelectable = { it.status == "Open" },
        rowActions = rowActions,
        onRetry = onRetry,
        onClearFilters = onClearFilters,
        onOpen = onOpen,
        labels = labels,
    )
}

internal object MonarchDataTableFixtures : ComponentFixtures {
    override val id = "data-table"
    override val fixtures = listOf(
        fixture("list, sortable and selectable") {
            Table(
                rows = Tickets.take(2),
                sort = MonarchDataTableSort("replies"),
                onSortChange = {},
                selection = setOf("Ticket 42"),
                onSelectionChange = {},
            )
        },
        fixture("list, actions and open") {
            Table(rows = Tickets.take(1), rowActions = { listOf(MonarchDataTableRowAction("Archive") {}) }, onOpen = {})
        },
        fixture("table") {
            Table(
                rows = Tickets.take(2),
                layout = MonarchDataTableLayout.Table,
                sort = MonarchDataTableSort("status"),
                onSortChange = {},
            )
        },
        fixture("table, selection and actions") {
            Table(
                rows = Tickets.take(2),
                layout = MonarchDataTableLayout.Table,
                selection = emptySet(),
                onSelectionChange = {},
                hidden = setOf("owner", "status"),
                rowActions = { listOf(MonarchDataTableRowAction("Archive", danger = true) {}) },
            )
        },
        fixture("loading") { Table(rows = emptyList(), loading = true) },
        fixture("failed") { Table(rows = emptyList(), errorMessage = "The server did not answer.", onRetry = {}) },
        fixture("filtered") { Table(rows = emptyList(), filtered = true, onClearFilters = {}) },
        fixture("empty") { Table(rows = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDataTableTest {
    @get:Rule
    val rule = createComposeRule()

    private fun texts(node: androidx.compose.ui.test.SemanticsNodeInteraction): List<String> =
        node.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

    @Test
    fun aRecordIsOneElementReadAsItsNameAndFields() {
        rule.setMonarchContent { Table() }
        rule.onNodeWithContentDescription("Open tickets").assertExists()
        val record = rule.onNode(hasText("Ticket 7") and hasText("Status"))
        assertEquals(listOf("Ticket 7", "Status", "Closed", "Owner", "—", "Replies", "12"), texts(record))
        val description = record.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertEquals(listOf("No value"), description)
    }

    @Test
    fun rowsSortByTheChosenColumnWithEmptyCellsLast() {
        val tokens = MonarchDataTableRules
        val byReplies = tokens.sortedRows(Tickets, Columns, MonarchDataTableSort("replies"))
        assertEquals(listOf("Ticket 13", "Ticket 42", "Ticket 7"), byReplies.map { it.id })
        val descending = MonarchDataTableSort("replies", MonarchDataTableSortDirection.Descending)
        val down = tokens.sortedRows(Tickets, Columns, descending).map { it.id }
        assertEquals(listOf("Ticket 7", "Ticket 42", "Ticket 13"), down)
        val byOwner = MonarchDataTableSort("owner", MonarchDataTableSortDirection.Descending)
        assertEquals("Ticket 7", tokens.sortedRows(Tickets, Columns, byOwner).last().id)
        assertEquals(Tickets, tokens.sortedRows(Tickets, Columns, MonarchDataTableSort("gone")))
        assertEquals(1200.5, tokens.numberOf("€1,200.50"))
        val again = tokens.nextSort(MonarchDataTableSort("status"), "status")
        assertEquals(MonarchDataTableSort("status", MonarchDataTableSortDirection.Descending), again)
        assertEquals(MonarchDataTableSort("owner"), tokens.nextSort(MonarchDataTableSort("status"), "owner"))
    }

    @Test
    fun theLayoutFollowsTheTextSize() {
        val tokens = MonarchDataTableRules
        assertTrue(tokens.isTable(MonarchDataTableLayout.Automatic, 1f))
        assertEquals(false, tokens.isTable(MonarchDataTableLayout.Automatic, LargestFontScale))
        assertTrue(tokens.isTable(MonarchDataTableLayout.Table, LargestFontScale))
        assertEquals(false, tokens.isTable(MonarchDataTableLayout.List, 1f))
    }

    @Test
    fun sortableHeadersAreButtonsThatReportTheNextSort() {
        var sort by mutableStateOf<MonarchDataTableSort?>(MonarchDataTableSort("status"))
        rule.setMonarchContent {
            Table(layout = MonarchDataTableLayout.Table, sort = sort, onSortChange = { sort = it })
        }
        val status = rule.onNode(isHeading() and hasText("Status"))
        assertEquals("Sorted ascending", status.stateDescription())
        status.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchDataTableSort("status", MonarchDataTableSortDirection.Descending), sort)
        val owner = rule.onNode(isHeading() and hasText("Owner"))
        assertEquals("Not sorted", owner.stateDescription())
        owner.performClick()
        assertEquals(MonarchDataTableSort("owner"), sort)
    }

    @Test
    fun theSortMenuOfTheListLayoutSortsAndReadsTheSort() {
        var sort by mutableStateOf<MonarchDataTableSort?>(null)
        rule.setMonarchContent { Table(sort = sort, onSortChange = { sort = it }) }
        val button = rule.onNodeWithText("Sort")
        assertEquals("Not sorted", button.stateDescription())
        button.performClick()
        rule.onNode(hasText("Replies") and hasClickAction()).performClick()
        assertEquals(MonarchDataTableSort("replies"), sort)
        assertEquals("Replies, Sorted ascending", rule.onNodeWithText("Sort").stateDescription())
    }

    @Test
    fun withoutOnSortChangeTheTableKeepsItsOwnSortAsOnIos() {
        rule.setMonarchContent { Table(layout = MonarchDataTableLayout.Table, sort = MonarchDataTableSort("replies")) }
        val replies = rule.onNode(isHeading() and hasText("Replies", substring = true))
        assertEquals("Sorted ascending", replies.stateDescription())
        // The columns are at least 128dp wide, so the last ones sit past the edge of a phone until scrolled to.
        replies.performScrollTo().performClick()
        assertEquals("Sorted descending", replies.stateDescription())
        // Descending by replies: Ticket 7 (12) comes first.
        val seven = rule.onNodeWithText("Ticket 7", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        val fortyTwo = rule.onNodeWithText("Ticket 42", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        assertTrue(seven < fortyTwo)
        rule.onNode(isHeading() and hasText("Owner")).performScrollTo().performClick()
        assertEquals("Not sorted", replies.stateDescription())
    }

    @Test
    fun theSortMarksAndTheEmptyCellCanBeReplaced() {
        // The marks and the dash are hidden from TalkBack (it reads the sort state and "No value"), so they are
        // checked on the resolved texts.
        val built = MonarchDataTableTexts(MonarchDataTableLabels()) { id ->
            when (id) {
                R.string.monarch_datatable_mark_ascending -> "↑"
                R.string.monarch_datatable_mark_descending -> "↓"
                R.string.monarch_datatable_empty_cell -> "—"
                else -> ""
            }
        }
        assertEquals("↑", built.mark(MonarchDataTableSortDirection.Ascending))
        assertEquals("—", built.emptyCell)
        val replaced = MonarchDataTableTexts(
            MonarchDataTableLabels(ascendingMark = "▲", descendingMark = "▼", emptyCell = "n/a"),
        ) { "" }
        assertEquals("▲", replaced.mark(MonarchDataTableSortDirection.Ascending))
        assertEquals("▼", replaced.mark(MonarchDataTableSortDirection.Descending))
        assertEquals("", replaced.mark(null))
        assertEquals("n/a", replaced.emptyCell)
        rule.setMonarchContent {
            Table(layout = MonarchDataTableLayout.Table, labels = MonarchDataTableLabels(emptyCell = "n/a"))
        }
        // Ticket 7 has no owner: its cell shows the replacement, and TalkBack still reads "No value".
        rule.onNodeWithContentDescription("No value", useUnmergedTree = true).assertExists()
    }

    @Test
    fun checkboxesSelectRowsAndSelectAllSelectsTheSelectableOnes() {
        var selection by mutableStateOf(emptySet<String>())
        rule.setMonarchContent { Table(selection = selection, onSelectionChange = { selection = it }) }
        val box = rule.onNodeWithContentDescription("Select Ticket 42")
        box.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(setOf("Ticket 42"), selection)
        rule.onNodeWithContentDescription("Select Ticket 7").assertDoesNotExist()
        rule.onNodeWithText("1 selected", substring = true).assertExists()
        val all = rule.onNode(hasText("Select all rows"))
        assertEquals(
            ToggleableState.Indeterminate,
            all.fetchSemanticsNode().config[SemanticsProperties.ToggleableState],
        )
        all.performClick()
        assertEquals(setOf("Ticket 42", "Ticket 13"), selection)
        all.assertIsOn().performClick()
        assertEquals(emptySet(), selection)
    }

    @Test
    fun rowActionsAreAMenuAndTalkBackActions() {
        val done = mutableListOf<String>()
        rule.setMonarchContent {
            Table(
                rows = Tickets.take(1),
                rowActions = { listOf(MonarchDataTableRowAction("Archive") { done += it.id }) },
            )
        }
        val button = rule.onNodeWithContentDescription("Actions for Ticket 42")
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Archive").performClick()
        assertEquals(listOf("Ticket 42"), done)
        val record = rule.onNode(hasText("Ticket 42") and hasText("Status"))
        record.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Archive" }.action()
        assertEquals(listOf("Ticket 42", "Ticket 42"), done)
    }

    @Test
    fun aRowWithOnOpenIsAButton() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent { Table(rows = Tickets.take(1), onOpen = { opened += it.id }) }
        val record = rule.onNode(hasText("Ticket 42") and hasText("Status"))
        assertEquals("Open details", record.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        record.performClick()
        assertEquals(listOf("Ticket 42"), opened)
    }

    @Test
    fun hiddenColumnsAreLeftOut() {
        rule.setMonarchContent { Table(layout = MonarchDataTableLayout.Table, hidden = setOf("owner", "id")) }
        rule.onNode(isHeading() and hasText("Owner")).assertDoesNotExist()
        rule.onNode(isHeading() and hasText("Ticket")).assertExists()
    }

    @Test
    fun withoutRowsTheStatesExplainWhy() {
        var retries = 0
        var cleared = 0
        var rows by mutableStateOf(emptyList<Ticket>())
        var loading by mutableStateOf(true)
        var error by mutableStateOf<String?>(null)
        var filtered by mutableStateOf(false)
        rule.setMonarchContent {
            Table(
                rows = rows,
                loading = loading,
                errorMessage = error,
                filtered = filtered,
                onRetry = { retries++ },
                onClearFilters = { cleared++ },
            )
        }
        rule.onNode(hasContentDescription("Loading rows")).assertExists()
        loading = false
        error = "The server did not answer."
        rule.waitForIdle()
        rule.onNodeWithText("These rows could not be loaded", substring = true).assertExists()
        error = null
        filtered = true
        rule.waitForIdle()
        rule.onNodeWithText("No rows match these filters", substring = true).assertExists()
        filtered = false
        rule.waitForIdle()
        rule.onNodeWithText("Nothing here yet", substring = true).assertExists()
        rows = Tickets.take(1)
        loading = true
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Loading rows")).assertExists()
        rule.onNodeWithText("Ticket 42").assertExists()
        assertEquals(0, retries + cleared)
    }

    @Test
    fun theCheckboxComesBeforeTheRecordInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Table(selection = emptySet(), onSelectionChange = {}) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Select Ticket 42")
                .assertIsBefore(rule.onNodeWithText("Ticket 42"), direction)
        }
    }

    @Test
    fun aWideTablePlacesCellsInOneRow() {
        rule.setMonarchContent {
            Table(modifier = Modifier.requiredWidth(900.dp), layout = MonarchDataTableLayout.Automatic)
        }
        val id = rule.onNodeWithText("Ticket 42", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = rule.onNode(hasText("Ticket 42") and hasText("Open")).fetchSemanticsNode().boundsInRoot
        assertTrue(status.width > id.width)
        rule.onNode(isHeading() and hasText("Replies")).assertExists()
    }

    @Test
    fun recordsWrapWholeAtTheLargestFontScale() {
        val long = Ticket("Ticket 99 about the onboarding emails that never arrived", "Open", "Ana Lima", 2)
        rule.setMonarchContent(fontScale = LargestFontScale) { Table(rows = listOf(long)) }
        rule.onNodeWithText(long.id, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Table(
                selection = emptySet(),
                onSelectionChange = {},
                onSortChange = {},
                rowActions = { listOf(MonarchDataTableRowAction("Archiver") {}) },
                labels = MonarchDataTableLabels(
                    selectAll = "Tout sélectionner",
                    selectRow = { "Sélectionner $it" },
                    rowActions = { "Actions pour $it" },
                    sort = "Trier",
                    notSorted = "Non trié",
                    noValue = "Aucune valeur",
                ),
            )
        }
        rule.onNodeWithText("Tout sélectionner").assertExists()
        rule.onNodeWithContentDescription("Sélectionner Ticket 42").assertExists()
        rule.onNodeWithContentDescription("Actions pour Ticket 42").assertExists()
        assertEquals("Non trié", rule.onNodeWithText("Trier").stateDescription())
        rule.onNode(hasContentDescription("Aucune valeur")).assertExists()
    }

    @Test
    fun theRowColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchDataTableTokens
            assertEquals(colors.accentSubtle, tokens.rowColor(true, colors))
            assertEquals(colors.bgSurface, tokens.rowColor(false, colors))
            assertEquals(colors.textMuted, tokens.headerColor(colors))
            assertEquals(colors.textDefault, tokens.openableNameColor(colors))
        }
    }
}
