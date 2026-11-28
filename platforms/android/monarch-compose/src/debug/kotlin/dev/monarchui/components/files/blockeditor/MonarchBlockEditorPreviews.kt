package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextMark
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextSpan
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewDocument = listOf(
    MonarchBlockEditorBlock.Text("title", MonarchBlockEditorTextStyle.Heading1, "Launch plan"),
    MonarchBlockEditorBlock.Text(
        "intro",
        text = "Ship the export on Friday.",
        spans = listOf(MonarchRichTextSpan(MonarchRichTextMark.Bold, 0, 4)),
    ),
    MonarchBlockEditorBlock.Text("todo", MonarchBlockEditorTextStyle.Checklist, "QA sign-off", checked = true),
    MonarchBlockEditorBlock.Callout("note", "Support is told on Thursday.", tone = MonarchTone.Warning),
)

@OptIn(ExperimentalMonarchApi::class)
@MonarchPreviews
@Composable
internal fun MonarchBlockEditorPreview() {
    PreviewFrame {
        MonarchBlockEditor(
            label = "Document",
            state = rememberMonarchBlockEditorState(PreviewDocument),
            modifier = Modifier.height(420.dp),
        )
    }
}

@OptIn(ExperimentalMonarchApi::class)
@MonarchPreviews
@Composable
internal fun MonarchBlockEditorReadOnlyPreview() {
    PreviewFrame {
        MonarchBlockEditor(
            label = "Document",
            state = rememberMonarchBlockEditorState(PreviewDocument),
            modifier = Modifier.height(320.dp),
            readOnly = true,
        )
    }
}
