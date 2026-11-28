package dev.monarchui.components.collaboration.richtexteditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewText = "Release notes\nThe export now keeps formatting.\n• Faster sync"

private val PreviewSpans = listOf(
    MonarchRichTextSpan(MonarchRichTextMark.Heading2, 0, 13),
    MonarchRichTextSpan(MonarchRichTextMark.Bold, 18, 24),
    MonarchRichTextSpan(MonarchRichTextMark.Link, 35, 45, url = "https://example.com"),
)

private const val PreviewLimit = 60

@OptIn(ExperimentalMonarchApi::class)
@MonarchPreviews
@Composable
internal fun MonarchRichTextEditorPreview() {
    PreviewFrame {
        MonarchRichTextEditor(
            label = "Description",
            state = remember { MonarchRichTextEditorState(PreviewText, PreviewSpans) },
            prompt = "What changed?",
            maxLength = PreviewLimit,
        )
        MonarchRichTextEditor(
            label = "Reply",
            state = remember { MonarchRichTextEditorState() },
            prompt = "Reply to the thread",
            tools = emptyList(),
            invalid = true,
        )
    }
}

@OptIn(ExperimentalMonarchApi::class)
@MonarchPreviews
@Composable
internal fun MonarchRichTextEditorReadOnlyPreview() {
    PreviewFrame {
        MonarchRichTextEditor(
            label = "Description",
            state = remember { MonarchRichTextEditorState(PreviewText, PreviewSpans) },
            readOnly = true,
        )
    }
}
