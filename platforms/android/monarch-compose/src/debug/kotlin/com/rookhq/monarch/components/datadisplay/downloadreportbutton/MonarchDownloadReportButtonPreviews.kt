package com.rookhq.monarch.components.datadisplay.downloadreportbutton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
