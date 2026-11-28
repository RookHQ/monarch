@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.identity.dataexportrequest

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewWeek = 604_800_000L

@MonarchPreviews
@Composable
internal fun MonarchDataExportRequestPreview() {
    PreviewFrame {
        MonarchDataExportRequest(
            status = MonarchDataExportStatus.Idle,
            scope = listOf(
                MonarchDataExportScopeOption("profile", "Profile and settings"),
                MonarchDataExportScopeOption("messages", "Messages and comments", "Every conversation you joined"),
            ),
            formats = listOf(
                MonarchDataExportFormatOption("json", "JSON (machine-readable)"),
                MonarchDataExportFormatOption("csv", "CSV (spreadsheets)"),
            ),
            preparationNote = "It can take up to 48 hours. We will email you when it is ready.",
            onRequest = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchDataExportRequestReadyPreview() {
    PreviewFrame {
        MonarchDataExportRequest(
            status = MonarchDataExportStatus.Ready,
            readyAt = PreviewNow,
            expiresAt = PreviewNow + PreviewWeek,
            onDownload = {},
        )
        MonarchDataExportRequest(
            status = MonarchDataExportStatus.Failed,
            failureReason = "The archive was too large.",
            onRequest = {},
        )
    }
}
