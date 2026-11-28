package dev.monarchui.sample.sections.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.files.filelist.MonarchFileList
import dev.monarchui.components.files.filelist.MonarchFileListItem
import dev.monarchui.components.files.filelist.MonarchFileListLayout
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val ListHeightSteps = 4

/** The catalogue page of FileList: project files as rows or a grid, with selection, a menu and bulk delete. */
object MonarchFileListSection : CataloguePage {
    override val title = "FileList"

    private val all = listOf(
        MonarchFileListItem(id = "1", name = "Q3 report.pdf", sizeBytes = 2_400_000, meta = "Edited yesterday"),
        MonarchFileListItem(id = "2", name = "Budget 2027.xlsx", sizeBytes = 48_000, badges = listOf("Shared")),
        MonarchFileListItem(id = "3", name = "Launch plan.docx", sizeBytes = 310_000),
        MonarchFileListItem(id = "4", name = "Old logo.png", sizeBytes = 12_000, enabled = false),
    )

    @Composable
    override fun Content() {
        var grid by rememberSaveable { mutableStateOf(false) }
        var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
        var deleted by rememberSaveable { mutableStateOf(listOf<String>()) }
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Project files")
        MonarchSwitchRow(checked = grid, onCheckedChange = { grid = it }, label = "Show as a grid")
        MonarchFileList(
            items = all.filterNot { it.id in deleted },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * ListHeightSteps),
            layout = if (grid) MonarchFileListLayout.Grid else MonarchFileListLayout.List,
            selected = selected.toSet(),
            onSelectedChange = { selected = it.toList() },
            onOpen = { opened = it.name },
            label = "Project files",
            bulkActions = listOf(
                MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {
                    deleted = deleted + selected
                    selected = emptyList()
                },
            ),
        ) { file ->
            var open by remember { mutableStateOf(false) }
            Box {
                MonarchButton(
                    text = "More",
                    onClick = { open = true },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
                MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
                    MonarchMenuItem(
                        text = "Delete ${file.name}",
                        onClick = {
                            open = false
                            deleted = deleted + file.id
                        },
                        destructive = true,
                    )
                }
            }
        }
        opened?.let { MonarchText(text = "Opened $it", color = MonarchTheme.colors.textMuted) }
    }
}
