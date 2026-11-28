@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity.dataexportrequest

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
