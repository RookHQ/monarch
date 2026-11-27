package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionItem
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionQuery
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextEditor
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextMark
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextSpan
import com.rookhq.monarch.components.collaboration.richtexteditor.rememberMonarchRichTextEditorState
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of RichTextEditor: release notes with a limit, and a reply with mentions. */
object MonarchRichTextEditorSection : CataloguePage {
    override val title = "RichTextEditor"

    private const val LIMIT = 500
    private const val NOTES = "Release notes\nThe export now keeps formatting.\n• Faster sync"

    private val notesSpans = listOf(
        MonarchRichTextSpan(MonarchRichTextMark.Heading2, 0, 13),
        MonarchRichTextSpan(MonarchRichTextMark.Bold, 18, 24),
    )

    private val people = listOf(
        MonarchMentionItem(id = "u1", label = "Ana Lima", detail = "Design"),
        MonarchMentionItem(id = "u2", label = "Ben Okafor", detail = "Engineering"),
    )

    private fun search(query: MonarchMentionQuery?): List<MonarchMentionItem> =
        query?.let { q -> people.filter { it.label.contains(q.text, ignoreCase = true) } }.orEmpty()

    @OptIn(ExperimentalMonarchApi::class)
    @Composable
    override fun Content() {
        val notes = rememberMonarchRichTextEditorState(NOTES, notesSpans)
        var readOnly by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Release notes, with a limit")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = readOnly, onCheckedChange = { readOnly = it })
            Text("Read only")
        }
        MonarchRichTextEditor(
            label = "Release notes",
            state = notes,
            prompt = "What changed?",
            maxLength = LIMIT,
            readOnly = readOnly,
        )
        MonarchText(text = "${notes.spans.size} formatted ranges", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("A reply with inline tools and mentions")
        val reply = rememberMonarchRichTextEditorState()
        var suggestions by rememberSaveable { mutableStateOf(listOf<String>()) }
        var mentioned by rememberSaveable { mutableStateOf("Nobody yet") }
        MonarchRichTextEditor(
            label = "Reply",
            state = reply,
            prompt = "Type @ to mention someone",
            tools = MonarchEditorTool.InlineGroups,
            mentionItems = people.filter { it.id in suggestions },
            onMentionSearch = { query -> suggestions = search(query).map { it.id } },
            onMention = { mentioned = it.label },
        )
        MonarchText(text = "Mentioned: $mentioned", color = MonarchTheme.colors.textMuted)
    }
}
