package dev.monarchui.components.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datagrid.datagrid.MonarchDataGrid
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridColumn
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridDensity
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridEdit
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridLabels
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridMenuAbilities
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridMenuItem
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridPin
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridRules
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridTokens
import dev.monarchui.components.datagrid.datagrid.monarchDataGridMenuItems
import dev.monarchui.components.datagrid.datatable.MonarchDataTableLayout
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSort
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSortDirection
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellEditor
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class Invoice(
    val id: String,
    val number: String,
    val customer: String,
    val status: String,
    val amount: Double,
    val due: String,
)

private val Invoices = listOf(
    Invoice("i1", "INV-1", "Example Ltd", "Sent", 120.0, "2026-03-01"),
    Invoice("i2", "INV-2", "Sample Co", "Paid", 80.5, ""),
    Invoice("i3", "INV-3", "Demo GmbH", "Draft", 400.0, "2026-01-15"),
)

private val Narrow: Dp = 96.dp

private const val LoadingDelayMillis = 2_000L

private val Statuses = MonarchEditableCellEditor.Select(
    listOf("Draft", "Sent", "Paid").map { MonarchSelectOption(it, it) },
)

/** The columns; [onEdit] receives every saved value as (row number, column id, value). */
private fun columns(onEdit: (String, String, Any?) -> Unit = { _, _, _ -> }) = listOf<MonarchDataGridColumn<Invoice>>(
    MonarchDataGridColumn(
        "Invoice",
        id = "number",
        width = Narrow,
        hideable = false,
        pinned = MonarchDataGridPin.Start,
    ) { it.number },
    MonarchDataGridColumn(
        "Customer",
        id = "customer",
        width = Narrow,
        edit = MonarchDataGridEdit(
            editor = MonarchEditableCellEditor.Text,
            value = { it.customer },
            onCommit = { row, value -> onEdit(row.number, "customer", value) },
            required = true,
        ),
    ) { it.customer },
    MonarchDataGridColumn(
        "Status",
        id = "status",
        width = Narrow,
        edit = MonarchDataGridEdit(
            editor = Statuses,
            value = { it.status },
            onCommit = { row, value -> onEdit(row.number, "status", value) },
            editable = { it.status != "Paid" },
        ),
    ) { it.status },
    MonarchDataGridColumn("Amount", id = "amount", width = Narrow, numeric = true) {
        String.format(Locale.US, "%.2f", it.amount)
    },
    MonarchDataGridColumn("Due", id = "due", width = Narrow) { it.due },
)

@Composable
@Suppress("LongParameterList")
private fun Grid(
    modifier: Modifier = Modifier,
    rows: List<Invoice> = Invoices,
    layout: MonarchDataTableLayout = MonarchDataTableLayout.Table,
    density: MonarchDataGridDensity = MonarchDataGridDensity.Comfortable,
    sort: MonarchDataTableSort? = null,
    onSortChange: ((MonarchDataTableSort?) -> Unit)? = null,
    selection: Set<String>? = null,
    onSelectionChange: ((Set<String>) -> Unit)? = null,
    hidden: Set<String> = setOf("amount", "due"),
    onHiddenChange: ((Set<String>) -> Unit)? = null,
    pinning: Map<String, MonarchDataGridPin>? = null,
    onPinningChange: ((Map<String, MonarchDataGridPin>) -> Unit)? = null,
    widths: Map<String, Dp> = emptyMap(),
    onWidthsChange: ((Map<String, Dp>) -> Unit)? = null,
    loading: Boolean = false,
    rowActions: ((Invoice) -> List<MonarchDataTableRowAction>)? = null,
    onRowOpen: ((Invoice) -> Unit)? = null,
    onEndReached: (() -> Unit)? = null,
    onEdit: (String, String, Any?) -> Unit = { _, _, _ -> },
    readOnly: Boolean = false,
    emptyState: (@Composable () -> Unit)? = null,
    labels: MonarchDataGridLabels = MonarchDataGridLabels(),
) {
    val all = columns(onEdit)
    MonarchDataGrid(
        label = "Invoices",
        rows = rows,
        columns = if (readOnly) all.map { MonarchDataGridColumn(it.title, it.id, it.width, value = it.value) } else all,
        rowKey = { it.id },
        modifier = modifier,
        layout = layout,
        density = density,
        sort = sort,
        onSortChange = onSortChange,
        selection = selection,
        onSelectionChange = onSelectionChange,
        hiddenColumns = hidden,
        onHiddenColumnsChange = onHiddenChange,
        pinning = pinning,
        onPinningChange = onPinningChange,
        columnWidths = widths,
        onColumnWidthsChange = onWidthsChange,
        loading = loading,
        rowLabel = { it.number },
        rowSelectable = { it.status != "Draft" },
        rowActions = rowActions,
        onRowOpen = onRowOpen,
        onEndReached = onEndReached,
        emptyState = emptyState,
        labels = labels,
    )
}

internal object MonarchDataGridFixtures : ComponentFixtures {
    override val id = "data-grid"
    override val fixtures = listOf(
        fixture("grid, sortable, pinned and editable") {
            Grid(rows = Invoices.take(2), sort = MonarchDataTableSort("customer"), onSortChange = {})
        },
        fixture("grid, selection and actions") {
            Grid(
                rows = Invoices.take(2),
                selection = setOf("i1"),
                onSelectionChange = {},
                hidden = setOf("customer", "status", "due"),
                widths = mapOf("amount" to MonarchDataGridTokens.MinColumnWidth - 32.dp),
                rowActions = { listOf(MonarchDataTableRowAction("Void", danger = true) {}) },
            )
        },
        fixture("grid, compact and read-only") {
            Grid(rows = Invoices.take(2), density = MonarchDataGridDensity.Compact, readOnly = true, onRowOpen = {})
        },
        fixture("list, selection and sort") {
            Grid(
                rows = Invoices.take(2),
                layout = MonarchDataTableLayout.List,
                selection = setOf("i2"),
                onSelectionChange = {},
                onSortChange = {},
            )
        },
        fixture("list, actions and open") {
            Grid(
                rows = Invoices.take(1),
                layout = MonarchDataTableLayout.List,
                readOnly = true,
                onRowOpen = {},
                rowActions = { listOf(MonarchDataTableRowAction("Void") {}) },
            )
        },
        fixture("loading") { Grid(rows = emptyList(), loading = true) },
        fixture("empty") { Grid(rows = emptyList()) },
        fixture("empty, custom") { Grid(rows = emptyList(), emptyState = { MonarchText("No invoices this month") }) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDataGridTest {
    @get:Rule
    val rule = createComposeRule()

    private fun header(title: String) = rule.onNode(isHeading() and hasText(title))

    @Test
    fun headersAreHeadingsThatOpenTheColumnMenuAndSort() {
        var sort by mutableStateOf<MonarchDataTableSort?>(null)
        rule.setMonarchContent { Grid(sort = sort, onSortChange = { sort = it }) }
        rule.onNodeWithContentDescription("Invoices").assertExists()
        val status = header("Status")
        assertEquals("Not sorted", status.stateDescription())
        assertEquals(
            "Column options",
            status.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        status.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Sort descending").performClick()
        assertEquals(MonarchDataTableSort("status", MonarchDataTableSortDirection.Descending), sort)
        assertEquals("Sorted descending", header("Status").stateDescription())
        header("Status").performClick()
        rule.onNodeWithText("Sort descending").assertDoesNotExist()
        rule.onNodeWithText("Clear sort").performClick()
        assertEquals(null, sort)
    }

    @Test
    fun theGridSortsItsRowsUnlessTheServerDoes() {
        val down = MonarchDataTableSort("customer", MonarchDataTableSortDirection.Descending)
        assertEquals(
            listOf("INV-2", "INV-1", "INV-3"),
            MonarchDataGridRules.sortedRows(Invoices, columns(), down).map { it.number },
        )
        val byAmount = MonarchDataTableSort("amount")
        assertEquals(
            listOf("INV-2", "INV-1", "INV-3"),
            MonarchDataGridRules.sortedRows(Invoices, columns(), byAmount).map { it.number },
        )
        assertEquals(Invoices, MonarchDataGridRules.sortedRows(Invoices, columns(), MonarchDataTableSort("gone")))
    }

    @Test
    fun theColumnMenuPinsHidesAndResizesThroughTheCallbacks() {
        var pinning by mutableStateOf<Map<String, MonarchDataGridPin>?>(null)
        var hidden by mutableStateOf(setOf("amount", "due"))
        var widths by mutableStateOf(emptyMap<String, Dp>())
        rule.setMonarchContent {
            Grid(
                hidden = hidden,
                onHiddenChange = { hidden = it },
                pinning = pinning,
                onPinningChange = { pinning = it },
                widths = widths,
                onWidthsChange = { widths = it },
            )
        }
        header("Customer").performClick()
        rule.onNodeWithText("Pin to end").performClick()
        assertEquals(mapOf("number" to MonarchDataGridPin.Start, "customer" to MonarchDataGridPin.End), pinning)
        assertEquals("Pinned to end", header("Customer").stateDescription())
        header("Customer").performClick()
        rule.onNodeWithText("Widen column").performClick()
        assertEquals(mapOf("customer" to Narrow + MonarchDataGridTokens.ResizeStep), widths)
        header("Customer").performClick()
        rule.onNodeWithText("Hide column").performClick()
        assertEquals(setOf("amount", "due", "customer"), hidden)
        header("Customer").assertDoesNotExist()
        header("Invoice").performClick()
        rule.onNodeWithText("Hide column").assertDoesNotExist()
        rule.onNodeWithText("Unpin").performClick()
        assertEquals(mapOf("customer" to MonarchDataGridPin.End), pinning)
    }

    @Test
    fun theMenuOffersOnlyWhatTheAppAllows() {
        val column = columns()[1]
        val none = MonarchDataGridMenuAbilities(sort = false, pin = false, hide = false, resize = false)
        assertEquals(emptyList(), monarchDataGridMenuItems(column, null, null, Narrow, none))
        val all = MonarchDataGridMenuAbilities(sort = true, pin = true, hide = true, resize = true)
        assertEquals(
            listOf(
                MonarchDataGridMenuItem.SortDescending,
                MonarchDataGridMenuItem.ClearSort,
                MonarchDataGridMenuItem.PinStart,
                MonarchDataGridMenuItem.PinEnd,
                MonarchDataGridMenuItem.Hide,
                MonarchDataGridMenuItem.Widen,
            ),
            monarchDataGridMenuItems(
                column = column,
                sorted = MonarchDataTableSortDirection.Ascending,
                pin = null,
                width = MonarchDataGridTokens.MinColumnWidth,
                abilities = all,
            ),
        )
        val widest = mapOf("customer" to MonarchDataGridTokens.MaxColumnWidth)
        val widened = MonarchDataGridRules.resized(column, widest, 16.dp)
        assertEquals(MonarchDataGridTokens.MaxColumnWidth, widened.getValue("customer"))
        val plain = MonarchDataGridColumn<Invoice>("X") { "" }
        assertEquals(MonarchDataGridTokens.ColumnWidth, MonarchDataGridRules.widthOf(plain, emptyMap()))
    }

    @Test
    fun pinsThatWouldFillANarrowGridScrollWithTheRest() {
        val wide = mapOf("number" to 200.dp)
        val narrow = MonarchDataGridRules.arranged(columns(), emptySet(), emptyList(), null, wide, 320.dp)
        assertEquals(emptyList(), narrow.start)
        assertEquals(5, narrow.middle.size)
        val roomy = MonarchDataGridRules.arranged(columns(), setOf("due"), listOf("amount"), null, wide, 900.dp)
        assertEquals(listOf("number"), roomy.start.map { it.id })
        assertEquals(listOf("amount", "customer", "status"), roomy.middle.map { it.id })
    }

    @Test
    fun pinnedColumnsStayWhileTheOthersScrollTogether() {
        rule.setMonarchContent { Grid(hidden = emptySet()) }
        val pinned = rule.onNodeWithContentDescription("Invoice: INV-1")
        val before = pinned.fetchSemanticsNode().boundsInRoot.left
        val headerBefore = header("Status").fetchSemanticsNode().boundsInRoot.left
        rule.onNode(hasContentDescription("Customer for INV-1")).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(before, pinned.fetchSemanticsNode().boundsInRoot.left)
        val headerAfter = header("Status").fetchSemanticsNode().boundsInRoot.left
        assertTrue(headerAfter < headerBefore, "the header band scrolls with the rows")
    }

    @Test
    fun readOnlyCellsAreReadAsColumnAndValueAndOpenTheRow() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            Grid(hidden = setOf("customer", "status", "amount"), onRowOpen = { opened += it.number })
        }
        val due = rule.onNodeWithContentDescription("Due: 2026-03-01")
        assertEquals("Open details", due.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        due.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("INV-1"), opened)
        rule.onNodeWithContentDescription("Due: No value").assertExists()
    }

    @Test
    fun editableCellsCommitThroughTheColumnEdit() {
        val edits = mutableListOf<Triple<String, String, Any?>>()
        rule.setMonarchContent { Grid(onEdit = { row, column, value -> edits += Triple(row, column, value) }) }
        rule.onNodeWithContentDescription("Customer for INV-1")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Acme")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(listOf(Triple<String, String, Any?>("INV-1", "customer", "Acme")), edits)
        // A row the column does not let edit is read-only text.
        rule.onNodeWithContentDescription("Status: Paid").assertExists()
        rule.onNodeWithContentDescription("Status for INV-2").assertDoesNotExist()
        rule.onNodeWithContentDescription("Status for INV-1").performClick()
        rule.onNode(hasText("Draft") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(Triple<String, String, Any?>("INV-1", "status", "Draft"), edits.last())
    }

    @Test
    fun checkboxesSelectRowsAndTheHeaderSelectsAll() {
        var selection by mutableStateOf(emptySet<String>())
        rule.setMonarchContent { Grid(selection = selection, onSelectionChange = { selection = it }) }
        rule.onNodeWithContentDescription("Select INV-1").assertIsOff().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(setOf("i1"), selection)
        rule.onNodeWithContentDescription("Select INV-3").assertDoesNotExist()
        val all = rule.onNodeWithContentDescription("Select all rows")
        val state = all.fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
        assertEquals(ToggleableState.Indeterminate, state)
        all.performClick()
        assertEquals(setOf("i1", "i2"), selection)
        all.assertIsOn().performClick()
        assertEquals(emptySet(), selection)
    }

    @Test
    fun spaceTogglesTheFocusedRowAndCtrlASelectsAll() {
        var selection by mutableStateOf(emptySet<String>())
        rule.setMonarchContent {
            Grid(selection = selection, onSelectionChange = { selection = it }, readOnly = true)
        }
        val cell = rule.onNodeWithContentDescription("Customer: Sample Co")
        cell.performSemanticsAction(SemanticsActions.RequestFocus)
        cell.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(setOf("i2"), selection)
        cell.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.A) } }
        assertEquals(setOf("i1", "i2"), selection)
    }

    @Test
    fun rowActionsAreAMenuAndTalkBackActions() {
        val done = mutableListOf<String>()
        rule.setMonarchContent {
            Grid(
                rows = Invoices.take(1),
                hidden = setOf("customer", "status", "amount"),
                rowActions = { listOf(MonarchDataTableRowAction("Void") { done += it.number }) },
            )
        }
        val button = rule.onNodeWithContentDescription("Actions for INV-1")
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Void").performClick()
        assertEquals(listOf("INV-1"), done)
        val cell = rule.onNodeWithContentDescription("Due: 2026-03-01")
        cell.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Void" }.action()
        assertEquals(listOf("INV-1", "INV-1"), done)
    }

    @Test
    fun onPhonesARecordOpensItsEditorInASheet() {
        val edits = mutableListOf<Any?>()
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            Grid(
                rows = Invoices.take(1),
                layout = MonarchDataTableLayout.List,
                onRowOpen = { opened += it.number },
                onEdit = { _, _, value -> edits += value },
            )
        }
        val record = rule.onNode(hasText("INV-1") and hasText("Customer"))
        assertEquals("Edit", record.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        record.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Open details" }
        record.performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Customer for INV-1").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Acme")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals(listOf<Any?>("Acme"), edits)
        rule.onNodeWithText("Done").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Done").assertDoesNotExist()
        assertEquals(emptyList(), opened)
    }

    @Test
    fun aReadOnlyRecordOpensTheRowAndIsReadAsOneElement() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            Grid(
                rows = Invoices.drop(1).take(1),
                layout = MonarchDataTableLayout.List,
                readOnly = true,
                onRowOpen = { opened += it.number },
            )
        }
        val record = rule.onNode(hasText("INV-2") and hasText("Customer"))
        assertEquals("Open details", record.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        record.performClick()
        assertEquals(listOf("INV-2"), opened)
    }

    @Test
    fun theListLayoutSortsFromItsSortMenu() {
        var sort by mutableStateOf<MonarchDataTableSort?>(null)
        rule.setMonarchContent { Grid(layout = MonarchDataTableLayout.List, sort = sort, onSortChange = { sort = it }) }
        val button = rule.onNodeWithText("Sort")
        assertEquals("Not sorted", button.stateDescription())
        button.performClick()
        // The records also read "Customer"; the menu item is the one without the other fields.
        rule.onNode(hasText("Customer") and hasClickAction() and !hasText("Status")).performClick()
        assertEquals(MonarchDataTableSort("customer"), sort)
        assertEquals("Customer, Sorted ascending", rule.onNodeWithText("Sort").stateDescription())
    }

    @Test
    fun withoutRowsTheStatesExplainWhy() {
        var loading by mutableStateOf(true)
        var rows by mutableStateOf(emptyList<Invoice>())
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { Grid(rows = rows, loading = loading) }
        // The loading state names itself after a short delay, so a quick load is not announced.
        rule.mainClock.advanceTimeBy(LoadingDelayMillis)
        rule.onNode(hasContentDescription("Loading rows")).assertExists()
        loading = false
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Nothing here yet").assertExists()
        rows = Invoices.take(1)
        loading = true
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Invoice: INV-1").assertExists()
        rule.onNode(hasContentDescription("Loading rows")).assertExists()
    }

    @Test
    fun onEndReachedFiresWhenTheLastRowIsShownAndAgainAfterMoreRows() {
        var calls = 0
        var rows by mutableStateOf(Invoices.take(1))
        rule.setMonarchContent { Grid(rows = rows, onEndReached = { calls++ }) }
        rule.waitForIdle()
        assertEquals(1, calls)
        rows = Invoices
        rule.waitForIdle()
        assertEquals(2, calls)
    }

    @Test
    fun thePinnedColumnComesFirstInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Grid() }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            header("Invoice").assertIsBefore(header("Customer"), direction)
        }
    }

    @Test
    fun recordsWrapWholeAtTheLargestFontScale() {
        val long = Invoices.first().copy(customer = "A customer with a name far longer than the screen is wide")
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Grid(rows = listOf(long), layout = MonarchDataTableLayout.Automatic)
        }
        rule.onNodeWithText(long.customer, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Grid(
                selection = emptySet(),
                onSelectionChange = {},
                onSortChange = {},
                hidden = setOf("customer", "status", "amount"),
                rowActions = { listOf(MonarchDataTableRowAction("Annuler") {}) },
                labels = MonarchDataGridLabels(
                    selectAll = "Tout sélectionner",
                    selectRow = { "Sélectionner $it" },
                    rowActions = { "Actions pour $it" },
                    columnOptions = "Options de colonne",
                    sortAscending = "Tri croissant",
                    notSorted = "Non trié",
                    cell = { column, value -> "$column — $value" },
                    noValue = "Aucune valeur",
                ),
            )
        }
        rule.onNodeWithContentDescription("Tout sélectionner").assertExists()
        rule.onNodeWithContentDescription("Sélectionner INV-1").assertExists()
        rule.onNodeWithContentDescription("Actions pour INV-1").assertExists()
        rule.onNodeWithContentDescription("Due — Aucune valeur").assertExists()
        val due = header("Due")
        assertEquals("Non trié", due.stateDescription())
        assertEquals("Options de colonne", due.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        due.performClick()
        rule.onNodeWithText("Tri croissant").assertExists()
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchDataGridTokens
            assertEquals(colors.accentSubtle, tokens.rowColor(true, colors))
            assertEquals(colors.bgSurface, tokens.rowColor(false, colors))
            assertEquals(colors.bgSubtle, tokens.headerColor(colors))
            assertEquals(colors.textMuted, tokens.headerText(colors))
            assertEquals(colors.borderDefault, tokens.pinnedEdgeColor(colors))
            assertEquals(colors.textDefault, tokens.openableText(colors))
        }
        val tokens = MonarchDataGridTokens
        val compact = tokens.cellPadding(MonarchDataGridDensity.Compact)
        assertTrue(compact < tokens.cellPadding(MonarchDataGridDensity.Comfortable))
    }
}
