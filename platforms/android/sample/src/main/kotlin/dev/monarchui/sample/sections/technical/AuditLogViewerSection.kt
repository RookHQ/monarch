package dev.monarchui.sample.sections.technical

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportButton
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportButtonLabels
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportResult
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datagrid.cursorlist.MonarchCursorList
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private class FieldChange(val field: String, val before: String?, val after: String?)

private class AuditEvent(
    val id: String,
    val minutesAgo: Long,
    val actor: String,
    val kind: String?,
    val action: String,
    val target: String,
    val ip: String,
    val device: String,
    val changes: List<FieldChange> = emptyList(),
)

private val Events = listOf(
    AuditEvent(
        "ev_1", 4, "Dana Fox", null, "Changed a role", "Sam Lee", "203.0.113.7", "Firefox on macOS",
        listOf(FieldChange("Role", "Member", "Admin")),
    ),
    AuditEvent(
        "ev_2", 38, "Nightly export", "Automation", "Exported invoices", "October invoices", "10.0.0.4", "Server",
    ),
    AuditEvent("ev_3", 95, "Billing key", "API", "Created a refund", "Invoice 42", "198.51.100.2", "API client"),
    AuditEvent("ev_4", 180, "Sam Lee", null, "Signed in", "Workspace", "203.0.113.9", "Chrome on Android"),
)

private const val MinuteMillis = 60_000L
private const val ListHeightSteps = 4

/** The catalogue page of AuditLogViewer: search, a filter, the events as rows and one event's details in a sheet. */
object AuditLogViewerSection : CataloguePage {
    override val title = "AuditLogViewer"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var query by rememberSaveable { mutableStateOf("") }
        var machinesOnly by rememberSaveable { mutableStateOf(false) }
        var openId by rememberSaveable { mutableStateOf<String?>(null) }
        val shown = Events.filter { event ->
            val text = "${event.actor} ${event.action} ${event.target}"
            (query.isBlank() || text.contains(query, ignoreCase = true)) && (!machinesOnly || event.kind != null)
        }
        CatalogueCaption("Search and filters")
        MonarchSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search the audit log",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchTag(text = "Automations and API", selected = machinesOnly, onSelectedChange = { machinesOnly = it })
            MonarchDownloadReportButton(
                onDownload = { MonarchDownloadReportResult.Queued("We will email you the CSV.") },
                labels = MonarchDownloadReportButtonLabels(label = "Export CSV"),
            )
        }
        CatalogueCaption("Events")
        MonarchCursorList(
            items = shown,
            label = "Audit log",
            hasMore = false,
            onLoadMore = {},
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * ListHeightSteps),
            key = { it.id },
            totalCount = shown.size,
        ) { event ->
            ListItem(
                headlineContent = { Text("${event.actor} · ${event.action}") },
                modifier = Modifier.clickable(onClickLabel = "Details") { openId = event.id },
                overlineContent = event.kind?.let { kind -> { MonarchBadge(text = kind) } },
                supportingContent = { Text(event.target) },
                leadingContent = {
                    MonarchAvatar(image = null, name = event.actor, size = MonarchAvatarSize.Sm, decorative = true)
                },
                trailingContent = {
                    Text(
                        DateUtils.getRelativeTimeSpanString(
                            now - event.minutesAgo * MinuteMillis,
                            now,
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                    )
                },
            )
        }
        Events.firstOrNull { it.id == openId }?.let { event ->
            AuditEventSheet(event, now - event.minutesAgo * MinuteMillis) { openId = null }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditEventSheet(event: AuditEvent, time: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    MonarchBottomSheet(onDismissRequest = onClose, title = "Event details", fullyExpanded = false) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
        ) {
            MonarchDescriptionList(
                items = listOf(
                    MonarchDescriptionItem(
                        "Time",
                        DateUtils.formatDateTime(
                            context,
                            time,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
                        ),
                    ),
                    MonarchDescriptionItem("Actor", listOfNotNull(event.actor, event.kind).joinToString(" · ")),
                    MonarchDescriptionItem("Action", event.action),
                    MonarchDescriptionItem("Target", event.target),
                    MonarchDescriptionItem("IP address", event.ip, copyable = true),
                    MonarchDescriptionItem("Device", event.device),
                ),
            )
            if (event.changes.isNotEmpty()) {
                MonarchText(text = "Changes", style = MonarchTextStyle.Subheading, heading = true)
                MonarchDescriptionList(
                    items = event.changes.map { change ->
                        MonarchDescriptionItem(change.field, "${change.before ?: "None"} → ${change.after ?: "None"}")
                    },
                )
            }
            MonarchButton(
                text = "Close",
                onClick = { this@MonarchBottomSheet.dismiss() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
