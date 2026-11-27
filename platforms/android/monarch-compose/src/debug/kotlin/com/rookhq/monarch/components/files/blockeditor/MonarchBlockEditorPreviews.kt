package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextMark
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextSpan
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
