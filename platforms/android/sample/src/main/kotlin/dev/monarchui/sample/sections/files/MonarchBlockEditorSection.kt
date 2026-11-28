package dev.monarchui.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextMark
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextSpan
import dev.monarchui.components.files.blockeditor.MonarchBlockEditor
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorTextStyle
import dev.monarchui.components.files.blockeditor.rememberMonarchBlockEditorState
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
