package dev.monarchui.components.technical.logviewer

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewStart = 1_763_100_000_000L

private val PreviewLines = listOf(
    MonarchLogLine("1", "Server listening on :8080", time = PreviewStart, source = "api"),
    MonarchLogLine("2", "Cache warmed in 120 ms", MonarchLogLevel.Debug, PreviewStart + 40, "cache"),
    MonarchLogLine("3", "Slow query on invoices (900 ms)", MonarchLogLevel.Warn, PreviewStart + 900, "db"),
    MonarchLogLine("4", "Payment provider timed out", MonarchLogLevel.Error, PreviewStart + 1_200, "billing"),
)

@MonarchPreviews
@Composable
internal fun MonarchLogViewerPreview() {
    PreviewFrame {
        MonarchLogViewer(
            lines = PreviewLines,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 5),
            query = "query",
            searchMode = MonarchLogViewerSearchMode.Highlight,
            showLineNumbers = true,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchLogViewerStatesPreview() {
    PreviewFrame {
        val height = Modifier.height(MonarchTheme.spacing.space24 * 4)
        MonarchLogViewer(lines = emptyList(), modifier = height, loading = true)
        MonarchLogViewer(lines = PreviewLines, modifier = height, query = "zzz")
    }
}
