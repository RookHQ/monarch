package dev.monarchui.components.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.files.filecard.MonarchFileCardLabels
import dev.monarchui.components.files.filelist.MonarchFileList
import dev.monarchui.components.files.filelist.MonarchFileListItem
import dev.monarchui.components.files.filelist.MonarchFileListLabels
import dev.monarchui.components.files.filelist.MonarchFileListLayout
import dev.monarchui.components.files.filelist.MonarchFileListTokens
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.textLayout
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val files = listOf(
    MonarchFileListItem(id = "1", name = "Q3 report.pdf", sizeBytes = 2_400_000, meta = "Edited yesterday"),
    MonarchFileListItem(id = "2", name = "Budget 2027.xlsx", sizeBytes = 48_000, badges = listOf("Shared")),
    MonarchFileListItem(id = "3", name = "Old logo.png", sizeBytes = 12_000, enabled = false),
)

internal object MonarchFileListFixtures : ComponentFixtures {
    override val id = "file-list"
    override val fixtures = listOf(
        fixture("rows, read only") { MonarchFileList(items = files, onOpen = {}) },
        fixture("rows, some selected with actions") {
            MonarchFileList(
                items = files.take(2),
                selected = setOf("1"),
                onSelectedChange = {},
                bulkActions = listOf(MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {}),
            )
        },
        fixture("grid of cards") {
            MonarchFileList(items = files.take(2), layout = MonarchFileListLayout.Grid, onSelectedChange = {})
        },
        fixture("empty") { MonarchFileList(items = emptyList()) },
        fixture("long file name") {
            MonarchFileList(
                items = listOf(
                    MonarchFileListItem(id = "l", name = "Minutes of the quarterly planning meeting, final.docx"),
                ),
                actions = { MonarchButton(text = "Share", onClick = {}, size = MonarchButtonSize.Sm) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFileListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachFileOpensWhenEnabled() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent { MonarchFileList(items = files, onOpen = { opened += it.id }) }
        rule.onNodeWithText("Q3 report.pdf", substring = true).performClick()
        rule.onNodeWithText("Old logo.png", substring = true).performClick()
        assertEquals(listOf("1"), opened)
    }

    @Test
    fun selectAllSelectsTheEnabledFilesAndShowsTheBar() {
        var selected by mutableStateOf(emptySet<String>())
        rule.setMonarchContent {
            MonarchFileList(items = files, selected = selected, onSelectedChange = { selected = it })
        }
        rule.onNodeWithText("Select all files").assertIsOff().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(setOf("1", "2"), selected)
        rule.onNodeWithText("Select all files").assertIsOn()
        rule.onNodeWithText("2 selected").assertExists()
        rule.onNodeWithText("Select all files").performClick()
        assertEquals(emptySet(), selected)
    }

    @Test
    fun selectAllIsMixedWhenSomeFilesAreSelected() {
        rule.setMonarchContent {
            MonarchFileList(items = files, selected = setOf("2"), onSelectedChange = {})
        }
        val mixed = rule.onNodeWithText("Select all files").fetchSemanticsNode()
            .config[SemanticsProperties.ToggleableState]
        assertEquals(ToggleableState.Indeterminate, mixed)
    }

    @Test
    fun aFileCheckboxAddsAndRemovesItsId() {
        var selected by mutableStateOf(setOf("2"))
        rule.setMonarchContent {
            MonarchFileList(items = files, selected = selected, onSelectedChange = { selected = it })
        }
        rule.onNodeWithContentDescription("Select Q3 report.pdf").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(setOf("1", "2"), selected)
        rule.onNodeWithContentDescription("Select Budget 2027.xlsx").performClick()
        assertEquals(setOf("1"), selected)
        rule.onNodeWithContentDescription("Select Old logo.png").assertIsNotEnabled()
    }

    @Test
    fun theBarRunsActionsAndClearsTheSelection() {
        var selected by mutableStateOf(setOf("1", "2"))
        var deleted = 0
        rule.setMonarchContent {
            MonarchFileList(
                items = files,
                selected = selected,
                onSelectedChange = { selected = it },
                bulkActions = listOf(
                    MonarchBulkAction(id = "delete", title = "Delete", destructive = true) { deleted++ },
                ),
            )
        }
        rule.onNodeWithText("Delete").performClick()
        assertEquals(1, deleted)
        rule.onNodeWithText("Clear selection").performClick()
        assertEquals(emptySet(), selected)
        rule.onNodeWithText("Clear selection").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun escapeClearsTheSelection() {
        var selected by mutableStateOf(setOf("1"))
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            MonarchFileList(items = files, selected = selected, onSelectedChange = { selected = it })
        }
        // Toggleable rows take focus only in keyboard mode, as on a device with a hardware keyboard.
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val selectAll = rule.onNodeWithText("Select all files")
        selectAll.requestFocus()
        selectAll.performKeyInput { pressKey(Key.Escape) }
        assertEquals(emptySet(), selected)
    }

    @Test
    fun withoutSelectionThereIsNoCheckbox() {
        rule.setMonarchContent { MonarchFileList(items = files) }
        rule.onNodeWithText("Select all files").assertDoesNotExist()
        rule.onNodeWithContentDescription("Select Q3 report.pdf").assertDoesNotExist()
    }

    @Test
    fun anEmptyListShowsTheEmptyState() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchFileList(items = emptyList()) }
        rule.onNode(isHeading() and hasText("No files yet")).assertExists()
        assertFalse(rule.onNodeWithText("No files yet", useUnmergedTree = true).textLayout().didOverflowHeight)
    }

    @Test
    fun theAppCanReplaceTheEmptyState() {
        rule.setMonarchContent {
            MonarchFileList(items = emptyList(), empty = { MonarchEmptyState(title = "Drop files here") })
        }
        rule.onNodeWithText("Drop files here").assertExists()
        rule.onNodeWithText("No files yet").assertDoesNotExist()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchFileList(
                items = files,
                selected = setOf("1"),
                onSelectedChange = {},
                label = "Project files",
                labels = MonarchFileListLabels(
                    selectAll = "Choose every file",
                    card = MonarchFileCardLabels(select = { "Pick $it" }),
                    bulkActionBar = MonarchBulkActionBarLabels(clear = "Deselect"),
                ),
            )
        }
        rule.onNodeWithText("Choose every file").assertExists()
        rule.onNodeWithContentDescription("Pick Q3 report.pdf").assertExists()
        rule.onNodeWithText("Deselect").assertExists()
        rule.onNodeWithContentDescription("Project files").assertExists()
    }

    @Test
    fun eachFileHasItsActions() {
        val shared = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchFileList(
                items = files.take(1),
                actions = { file ->
                    MonarchButton(text = "Share", onClick = { shared += file.id }, size = MonarchButtonSize.Sm)
                },
            )
        }
        rule.onNodeWithText("Share").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("1"), shared)
    }

    @Test
    fun theCheckboxComesFirstRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFileList(items = files.take(1), onSelectedChange = {})
        }
        val name = rule.onNodeWithText("Q3 report.pdf", substring = true, useUnmergedTree = true)
        rule.onNodeWithContentDescription("Select Q3 report.pdf").assertIsBefore(name, LayoutDirection.Rtl)
    }

    @Test
    fun theSelectionRules() {
        val t = MonarchFileListTokens
        assertEquals(listOf("1", "2"), t.selectable(files))
        assertEquals(ToggleableState.Off, t.selectAllState(files, emptySet()))
        assertEquals(ToggleableState.Off, t.selectAllState(files, setOf("gone")))
        assertEquals(ToggleableState.Indeterminate, t.selectAllState(files, setOf("1")))
        assertEquals(ToggleableState.On, t.selectAllState(files, setOf("1", "2")))
        assertEquals(ToggleableState.Off, t.selectAllState(files.drop(2), emptySet()))
        assertEquals(setOf("1", "2"), t.toggleAll(files, setOf("1")))
        assertEquals(emptySet(), t.toggleAll(files, setOf("1", "2")))
        assertEquals(1, t.count(files, setOf("1", "gone")))
        assertEquals(setOf("a", "b"), t.toggle(setOf("a"), "b", on = true))
        assertEquals(t.GridMinCell * 2, t.gridMinCell(2f))
        assertEquals(t.GridMinCell, t.gridMinCell(0.85f))
        assertEquals(160.dp, t.GridMinCell)
    }

    @Test
    fun aSelectedFileIsAnnouncedAsChecked() {
        rule.setMonarchContent { MonarchFileList(items = files, selected = setOf("1"), onSelectedChange = {}) }
        rule.onNodeWithContentDescription("Select Q3 report.pdf").assertIsOn()
        rule.onNode(hasContentDescription("Select Budget 2027.xlsx")).assertIsOff()
    }
}
