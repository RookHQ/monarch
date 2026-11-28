package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionKind
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPicker
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of MentionPicker. */
object MonarchMentionPickerSection : CataloguePage {
    override val title = "MentionPicker"

    private val directory = listOf(
        MonarchMentionItem(id = "u1", label = "Ana Lima", detail = "Product designer"),
        MonarchMentionItem(id = "u2", label = "Anders Berg", detail = "No access", enabled = false),
        MonarchMentionItem(id = "u3", label = "Ben Okafor", detail = "Engineering"),
        MonarchMentionItem(id = "c1", label = "announcements", kind = MonarchMentionKind.Channel),
        MonarchMentionItem(id = "c2", label = "design", kind = MonarchMentionKind.Channel),
    )

    /** Stands in for the app's search, which would run in a LaunchedEffect keyed by the query. */
    private fun search(query: MonarchMentionQuery): List<MonarchMentionItem> {
        val kind = if (query.trigger == '#') MonarchMentionKind.Channel else MonarchMentionKind.Person
        return directory.filter { it.kind == kind && it.label.contains(query.text, ignoreCase = true) }
    }

    @Composable
    override fun Content() {
        var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue("Thanks @an"))
        }
        val query = MonarchMentionQuery.find(value, triggers = setOf('@', '#'))
        CatalogueCaption("Type @ for people or # for channels")
        Column {
            MonarchMentionPicker(
                query = query,
                items = query?.let(::search).orEmpty(),
                onSelect = { item ->
                    if (query != null) value = query.replacing(value, item.token(query.trigger) + " ")
                },
            )
            MonarchTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Message",
                singleLine = false,
            )
        }
        MonarchText(text = value.text)
        CatalogueCaption("Searching")
        MonarchMentionPicker(
            query = MonarchMentionQuery.find("@be"),
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Loading,
        )
        CatalogueCaption("Search failed")
        MonarchMentionPicker(
            query = MonarchMentionQuery.find("@be"),
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Failed,
            onRetry = {},
        )
    }
}
