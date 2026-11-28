package dev.monarchui.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportButton
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportFormat
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportResult
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/** The catalogue page of DownloadReportButton. The files are made up on the spot and saved where you choose. */
object MonarchDownloadReportButtonSection : CataloguePage {
    override val title = "DownloadReportButton"

    private const val STEPS = 10
    private const val STEP_MILLIS = 150L
    private val formats = listOf(
        MonarchDownloadReportFormat("CSV", "csv", "Opens in any spreadsheet", "text/csv"),
        MonarchDownloadReportFormat("Text", "txt", "A plain text summary", "text/plain"),
        MonarchDownloadReportFormat("By email", "email", "Sent when it is ready"),
    )

    @Composable
    override fun Content() {
        var fail by rememberSaveable { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }
        val icon: @Composable () -> Unit = {
            MonarchIcon(SampleIcons.Download, null, size = MonarchIconSize.Sm)
        }
        MonarchSwitchRow(checked = fail, onCheckedChange = { fail = it }, label = "Make the download fail")
        CatalogueCaption("One format, saved with the system dialog")
        MonarchDownloadReportButton(
            onDownload = { format ->
                prepare { progress = it }
                check(!fail) { "offline" }
                MonarchDownloadReportResult.File("report.${format.id}") { it.write("id,total\n1,42\n".toByteArray()) }
            },
            progress = progress,
            icon = icon,
        )
        CatalogueCaption("Several formats, one prepared in the background")
        MonarchDownloadReportButton(
            onDownload = { format ->
                prepare { progress = it }
                check(!fail) { "offline" }
                when (format.id) {
                    "email" -> MonarchDownloadReportResult.Queued()
                    else -> MonarchDownloadReportResult.File("report.${format.id}") {
                        it.write("Total: 42\n".toByteArray())
                    }
                }
            },
            formats = formats,
            progress = progress,
            variant = MonarchButtonVariant.Primary,
            icon = icon,
        )
    }

    private suspend fun prepare(onProgress: (Float) -> Unit) {
        for (step in 0..STEPS) {
            onProgress(step / STEPS.toFloat())
            delay(STEP_MILLIS)
        }
    }
}
