package dev.monarchui.sample.sections.technical

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.technical.methodbadge.MonarchMethodBadge
import dev.monarchui.components.typography.codeblock.MonarchCodeBlock
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private class LogEntry(val id: String, val method: String, val path: String, val status: Int, val millis: Long)

private val Entries = listOf(
    LogEntry("req_8f2c41", "POST", "/v1/payments", 502, 1_204),
    LogEntry("req_77a0d9", "GET", "/v1/items?limit=20", 200, 38),
    LogEntry("req_1c9e03", "DELETE", "/v1/items/42", 404, 12),
)

private const val SuccessClass = 2
private const val RedirectClass = 3
private const val ClientErrorClass = 4
private const val StatusClassDivisor = 100

/** The tone of an HTTP status: success for 2xx, info for 3xx, warning for 4xx and danger for 5xx. */
private fun statusTone(status: Int): MonarchTone = when (status / StatusClassDivisor) {
    SuccessClass -> MonarchTone.Success
    RedirectClass -> MonarchTone.Info
    ClientErrorClass -> MonarchTone.Warning
    else -> MonarchTone.Danger
}

/** The catalogue page of LogDrawer: a recipe of a sheet, a description list with copy and a code block. */
object LogDrawerSection : CataloguePage {
    override val title = "LogDrawer"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val now = remember { System.currentTimeMillis() }
        var openId by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Requests")
        Entries.forEach { entry ->
            MonarchButton(
                text = "${entry.method} ${entry.path} · ${entry.status}",
                onClick = { openId = entry.id },
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Ghost,
            )
        }
        val entry = Entries.firstOrNull { it.id == openId } ?: return
        MonarchBottomSheet(onDismissRequest = { openId = null }, title = "Request details", fullyExpanded = false) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                    MonarchMethodBadge(method = entry.method)
                    MonarchText(text = entry.path, style = MonarchTextStyle.Code)
                }
                MonarchDescriptionList(
                    items = listOf(
                        MonarchDescriptionItem("Status", entry.status.toString()) {
                            MonarchBadge(text = entry.status.toString(), tone = statusTone(entry.status))
                        },
                        MonarchDescriptionItem("Request ID", entry.id, copyable = true),
                        MonarchDescriptionItem(
                            "Time",
                            DateUtils.formatDateTime(
                                context,
                                now,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
                            ),
                        ),
                        MonarchDescriptionItem("Duration", "${entry.millis} ms"),
                    ),
                )
                MonarchCodeBlock(
                    code = "{\n  \"error\": \"upstream_timeout\",\n  \"request_id\": \"${entry.id}\"\n}",
                    language = "json",
                    title = "Response",
                )
                MonarchButton(
                    text = "Close",
                    onClick = { this@MonarchBottomSheet.dismiss() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
