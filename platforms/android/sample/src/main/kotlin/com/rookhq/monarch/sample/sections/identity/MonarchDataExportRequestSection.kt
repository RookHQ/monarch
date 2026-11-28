@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportFormatOption
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequest
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportScopeOption
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedStepMillis = 1_200L
private const val ExportLifetimeMillis = 7 * 24 * 3_600_000L

/** The catalogue page of DataExportRequest. */
object MonarchDataExportRequestSection : CataloguePage {
    override val title = "DataExportRequest"

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableStateOf(MonarchDataExportStatus.Idle) }
        var requestedAt by rememberSaveable { mutableStateOf<Long?>(null) }
        val now = remember { System.currentTimeMillis() }
        CatalogueCaption("Request, follow and download (simulated)")
        MonarchDataExportRequest(
            status = status,
            requestedAt = requestedAt,
            readyAt = requestedAt?.let { it + SimulatedStepMillis },
            expiresAt = requestedAt?.let { it + ExportLifetimeMillis },
            scope = listOf(
                MonarchDataExportScopeOption("profile", "Profile and settings"),
                MonarchDataExportScopeOption("messages", "Messages and comments", "Every conversation you joined"),
                MonarchDataExportScopeOption("files", "Files you uploaded"),
            ),
            formats = listOf(
                MonarchDataExportFormatOption("json", "JSON (machine-readable)"),
                MonarchDataExportFormatOption("csv", "CSV (spreadsheets)"),
            ),
            preparationNote = "It can take up to 48 hours. We will email you when it is ready.",
            onRequest = {
                delay(SimulatedStepMillis)
                requestedAt = System.currentTimeMillis()
                status = MonarchDataExportStatus.Preparing
                delay(SimulatedStepMillis)
                status = MonarchDataExportStatus.Ready
            },
            onDownload = { status = MonarchDataExportStatus.Expired },
        )
        CatalogueCaption("Failed")
        MonarchDataExportRequest(
            status = MonarchDataExportStatus.Failed,
            requestedAt = now,
            failureReason = "The archive was larger than 10 GB.",
            onRequest = { delay(SimulatedStepMillis) },
        )
    }
}
