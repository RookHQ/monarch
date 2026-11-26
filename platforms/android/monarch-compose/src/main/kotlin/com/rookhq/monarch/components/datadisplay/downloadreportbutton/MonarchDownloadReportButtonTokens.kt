package com.rookhq.monarch.components.datadisplay.downloadreportbutton

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Where a download stands. */
internal sealed class MonarchDownloadReportPhase {
    data object Idle : MonarchDownloadReportPhase()

    data class Preparing(val format: MonarchDownloadReportFormat) : MonarchDownloadReportPhase()

    class Saving(val format: MonarchDownloadReportFormat, val file: MonarchDownloadReportResult.File) :
        MonarchDownloadReportPhase()

    data class Queued(val format: MonarchDownloadReportFormat, val message: String?) : MonarchDownloadReportPhase()

    data class Failed(val format: MonarchDownloadReportFormat, val error: Throwable) : MonarchDownloadReportPhase()
}

/**
 * The system save dialog (`ACTION_CREATE_DOCUMENT`) with the MIME type of the
 * format being saved, which `CreateDocument` otherwise fixes when it is made.
 */
internal class MonarchDownloadReportCreateDocument : ActivityResultContracts.CreateDocument(ANY_TYPE) {
    var nextMimeType: String = ANY_TYPE

    override fun createIntent(context: Context, input: String): Intent =
        super.createIntent(context, input).setType(nextMimeType)

    private companion object {
        const val ANY_TYPE = "application/octet-stream"
    }
}

/** Component tokens and logic of MonarchDownloadReportButton, built from primitives and roles only. */
internal object MonarchDownloadReportButtonTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** Whether a download runs, so a second one does not start. */
    fun busy(phase: MonarchDownloadReportPhase): Boolean =
        phase is MonarchDownloadReportPhase.Preparing || phase is MonarchDownloadReportPhase.Saving

    /** The format of the running download, if any. */
    fun runningFormat(phase: MonarchDownloadReportPhase): MonarchDownloadReportFormat? = when (phase) {
        is MonarchDownloadReportPhase.Preparing -> phase.format
        is MonarchDownloadReportPhase.Saving -> phase.format
        else -> null
    }

    /** The phase after `onDownload` returned [result]. */
    fun after(format: MonarchDownloadReportFormat, result: MonarchDownloadReportResult): MonarchDownloadReportPhase =
        when (result) {
            MonarchDownloadReportResult.Downloaded -> MonarchDownloadReportPhase.Idle
            is MonarchDownloadReportResult.Queued -> MonarchDownloadReportPhase.Queued(format, result.message)
            is MonarchDownloadReportResult.File -> MonarchDownloadReportPhase.Saving(format, result)
        }

    /** The progress drawn under the button: clamped to 0 to 1, or `null` when there is none or it is not finite. */
    fun shownProgress(phase: MonarchDownloadReportPhase, progress: Float?): Float? =
        progress?.takeIf { phase is MonarchDownloadReportPhase.Preparing && it.isFinite() }?.coerceIn(0f, 1f)

    fun noteColor(colors: MonarchColors): Color = colors.textMuted
}
