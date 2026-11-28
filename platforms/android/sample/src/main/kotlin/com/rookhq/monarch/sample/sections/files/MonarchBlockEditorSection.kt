package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextMark
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextSpan
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditor
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlock
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorTextStyle
import com.rookhq.monarch.components.files.blockeditor.rememberMonarchBlockEditorState
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of BlockEditor. */
object MonarchBlockEditorSection : CataloguePage {
    override val title = "BlockEditor"

    private const val EDITOR_HEIGHT = 560

    @OptIn(ExperimentalMonarchApi::class)
    @Composable
    override fun Content() {
        val state = rememberMonarchBlockEditorState(
            listOf(
                MonarchBlockEditorBlock.Text("title", MonarchBlockEditorTextStyle.Heading1, "Launch plan"),
                MonarchBlockEditorBlock.Text(
                    "intro",
                    text = "Ship the export on Friday. Type / for blocks.",
                    spans = listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 0, "Ship".length)),
                ),
                MonarchBlockEditorBlock.Text(
                    "todo1",
                    MonarchBlockEditorTextStyle.Checklist,
                    "QA sign-off",
                    checked = true,
                ),
                MonarchBlockEditorBlock.Text("todo2", MonarchBlockEditorTextStyle.Checklist, "Release notes"),
                MonarchBlockEditorBlock.Callout("note", "Support is told on Thursday.", tone = MonarchTone.Warning),
                MonarchBlockEditorBlock.Toggle("faq", "Why Friday?", body = "Support is staffed on the weekend."),
            ),
        )
        var readOnly by rememberSaveable { mutableStateOf(false) }
        MonarchSwitchRow(checked = readOnly, onCheckedChange = { readOnly = it }, label = "Read only")
        CatalogueCaption("A document")
        MonarchBlockEditor(
            label = "Launch plan",
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(EDITOR_HEIGHT.dp),
            readOnly = readOnly,
        )
        MonarchText(text = "${state.blocks.size} blocks")
    }
}
