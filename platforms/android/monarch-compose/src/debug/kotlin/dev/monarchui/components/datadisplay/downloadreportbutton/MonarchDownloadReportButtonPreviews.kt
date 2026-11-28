package dev.monarchui.components.datadisplay.downloadreportbutton

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchDownloadReportButtonPreview() {
    PreviewFrame {
        MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Downloaded })
        MonarchDownloadReportButton(
            onDownload = { MonarchDownloadReportResult.Downloaded },
            formats = listOf(
                MonarchDownloadReportFormat.Csv,
                MonarchDownloadReportFormat("PDF", "pdf", "For printing", "application/pdf"),
            ),
            variant = MonarchButtonVariant.Primary,
        )
        MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Downloaded }, enabled = false)
    }
}
