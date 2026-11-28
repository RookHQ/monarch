package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.components.files.editortoolbar.MonarchEditorToolbar
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of EditorToolbar: the default toolbar over a note, and the inline tools only. */
object MonarchEditorToolbarSection : CataloguePage {
    override val title = "EditorToolbar"

    private val styles = listOf(MonarchEditorTool.Paragraph) + (1..3).map { MonarchEditorTool.heading(it) }
    private val allTools = MonarchEditorTool.DefaultGroups.flatten()

    @Composable
    override fun Content() {
        var activeIds by rememberSaveable { mutableStateOf(listOf(MonarchEditorTool.Paragraph.id)) }
        var link by rememberSaveable { mutableStateOf("") }
        var note by rememberSaveable { mutableStateOf("Quarterly plan") }
        val active = allTools.filter { it.id in activeIds }.toSet()
        CatalogueCaption("Default tools")
        MonarchEditorToolbar(
            onAction = { tool ->
                activeIds = when {
                    tool in styles -> activeIds.filterNot { id -> styles.any { it.id == id } } + tool.id
                    tool.id in activeIds -> activeIds - tool.id
                    tool == MonarchEditorTool.Undo || tool == MonarchEditorTool.Redo -> activeIds
                    else -> activeIds + tool.id
                }
            },
            active = active,
            disabled = setOf(MonarchEditorTool.Redo),
            onLink = { address ->
                link = address.orEmpty()
                val linkId = MonarchEditorTool.Link.id
                activeIds = if (address == null) activeIds - linkId else activeIds + linkId
            },
        )
        MonarchTextField(value = note, onValueChange = { note = it }, label = "Note", singleLine = false)
        MonarchText(
            text = "On: ${activeIds.joinToString()}" + if (link.isEmpty()) "" else "; link $link",
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("Inline tools only")
        MonarchEditorToolbar(onAction = {}, groups = MonarchEditorTool.InlineGroups)
    }
}
