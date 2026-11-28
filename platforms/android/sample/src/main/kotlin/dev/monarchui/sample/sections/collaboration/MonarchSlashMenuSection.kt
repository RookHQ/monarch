package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenu
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuItem
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuQuery
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of SlashMenu: a notes field where a slash at the start of a line inserts Markdown. */
object MonarchSlashMenuSection : CataloguePage {
    override val title = "SlashMenu"

    private val markdown = mapOf("heading" to "## ", "bullets" to "- ", "checklist" to "- [ ] ", "quote" to "> ")

    private val commands = listOf(
        MonarchSlashMenuItem(id = "heading", label = "Heading", description = "Large section title", group = "Text"),
        MonarchSlashMenuItem(id = "quote", label = "Quote", description = "Text set apart", group = "Text"),
        MonarchSlashMenuItem(id = "bullets", label = "Bulleted list", description = "A simple list", group = "Lists"),
        MonarchSlashMenuItem(
            id = "checklist",
            label = "Checklist",
            description = "Items to tick off",
            group = "Lists",
            keywords = listOf("todo"),
        ),
        MonarchSlashMenuItem(
            id = "table",
            label = "Table",
            description = "Not available in notes",
            group = "Blocks",
            enabled = false,
        ),
    )

    @Composable
    override fun Content() {
        var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue("Agenda\n/", selection = TextRange(START_CARET)))
        }
        val query = MonarchSlashMenuQuery.find(value)
        CatalogueCaption("Type / at the start of a line")
        Column {
            MonarchSlashMenu(
                query = query,
                items = commands,
                onSelect = { item ->
                    if (query != null) value = query.replacing(value, markdown[item.id].orEmpty())
                },
                groupOrder = listOf("Text", "Lists"),
            )
            MonarchTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Notes",
                singleLine = false,
            )
        }
        CatalogueCaption("Nothing matches")
        MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/zzz"), items = commands, onSelect = {})
    }

    private const val START_CARET = 8
}
