package com.rookhq.monarch.components.identity.dataexportrequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroup
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * A "Download your data" card for account or privacy settings: the person
 * picks what to include and the file format, requests an export, follows
 * its progress in words, and downloads the file until it expires. The
 * product builds the file and reports each status; the card shows it.
 *
 * Built by Monarch from `MonarchCard`, a `MonarchBadge` for the status,
 * `MonarchCheckboxGroup` for the scope, `MonarchSelect` for the format, an
 * indeterminate `MonarchProgress` while preparing, `MonarchInlineNotice` for
 * a failed request and primary `MonarchButton`s. The status line is a polite
 * live region, so each change the product reports is announced.
 *
 * @param status where the export is.
 * @param modifier the modifier of the card.
 * @param requestedAt when the export was requested, in epoch milliseconds.
 * @param readyAt when the file became ready, in epoch milliseconds.
 * @param expiresAt when the download stops working, in epoch milliseconds.
 * @param scope kinds of data the person can choose to include. All are ticked at first.
 * @param formats file formats to choose from. No menu without them.
 * @param defaultFormat the format chosen at first. `null` chooses the first format.
 * @param preparationNote how long preparing takes and how the person will hear about it. Shown before requesting
 * and while the export is requested or preparing.
 * @param failureReason more detail when the export failed, such as the reason from the server.
 * @param pending the busy state of the request button, for apps that track it themselves. `null` shows it busy
 * while [onRequest] runs.
 * @param onRequest requests an export with the chosen scope and format; the app then moves [status] on. Throw to
 * show the failure: the exception's message, or a built-in sentence. Without it no request form is shown.
 * @param onDownload downloads the file while the export is ready, for example with the system download manager.
 * Without it no download button is shown.
 * @param labels replacements for the built-in strings and the date format.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList")
public fun MonarchDataExportRequest(
    status: MonarchDataExportStatus,
    modifier: Modifier = Modifier,
    requestedAt: Long? = null,
    readyAt: Long? = null,
    expiresAt: Long? = null,
    scope: List<MonarchDataExportScopeOption> = emptyList(),
    formats: List<MonarchDataExportFormatOption> = emptyList(),
    defaultFormat: String? = null,
    preparationNote: String? = null,
    failureReason: String? = null,
    pending: Boolean? = null,
    onRequest: (suspend (MonarchDataExportRequestDetails) -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    labels: MonarchDataExportRequestLabels = MonarchDataExportRequestLabels(),
) {
    val rules = MonarchDataExportRequestRules
    val tokens = MonarchDataExportRequestTokens
    val texts = dataExportRequestTexts(labels)
    val locale = LocalConfiguration.current.locales[0]
    val sending = remember { DataExportRequestSending() }
    val coroutineScope = rememberCoroutineScope()
    val date: (Long?) -> String? = { millis ->
        millis?.let { labels.date?.invoke(it) ?: rules.formatDate(it, locale) }
    }
    MonarchCard(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            DataExportRequestHeading(status, texts)
            DataExportRequestStatus(
                line = dataExportRequestStatusLine(status, date(requestedAt), date(readyAt), date(expiresAt), texts),
                reason = failureReason.takeIf { status == MonarchDataExportStatus.Failed },
            )
            if (status == MonarchDataExportStatus.Preparing) {
                MonarchProgress(modifier = Modifier.fillMaxWidth(), label = texts.preparingProgress)
            }
            if (preparationNote != null && rules.showsNote(status)) {
                Text(
                    text = preparationNote,
                    style = MonarchTheme.typography.caption,
                    color = tokens.noteColor(MonarchTheme.colors),
                )
            }
            if (onRequest != null && rules.canRequest(status, hasOnRequest = true)) {
                DataExportRequestForm(
                    form = DataExportRequestFormInput(status, scope, formats, defaultFormat, pending),
                    sending = sending,
                    send = { details ->
                        // Launched in the card's scope: the app may move the status on (and remove the form)
                        // while the request runs.
                        coroutineScope.launch { sending.run(details, onRequest, texts.requestFailed) }
                    },
                    texts = texts,
                )
            }
            if (status == MonarchDataExportStatus.Ready && onDownload != null) {
                MonarchButton(text = texts.download, onClick = onDownload, variant = MonarchButtonVariant.Primary)
            }
        }
    }
}

/** Whether a request runs and why the last one failed; kept by the card, not the form. */
private class DataExportRequestSending {
    var running by mutableStateOf(false)
    var failure by mutableStateOf<String?>(null)

    suspend fun run(
        details: MonarchDataExportRequestDetails,
        onRequest: suspend (MonarchDataExportRequestDetails) -> Unit,
        requestFailed: String,
    ) {
        failure = null
        running = true
        val outcome = runCatching { onRequest(details) }
        running = false
        val error = outcome.exceptionOrNull()
        if (error is CancellationException) throw error
        if (error != null) failure = error.message?.takeIf { it.isNotBlank() } ?: requestFailed
    }
}

/** The inputs of the request form. */
private class DataExportRequestFormInput(
    val status: MonarchDataExportStatus,
    val scope: List<MonarchDataExportScopeOption>,
    val formats: List<MonarchDataExportFormatOption>,
    val defaultFormat: String?,
    val pending: Boolean?,
)

/** The title as a heading and the status badge, which says the status in words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DataExportRequestHeading(status: MonarchDataExportStatus, texts: DataExportRequestTexts) {
    val tokens = MonarchDataExportRequestTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.HeadingGap),
        verticalArrangement = Arrangement.spacedBy(tokens.HeadingGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = texts.title, style = MonarchTextStyle.Subheading, heading = true)
        MonarchBadge(text = texts.status(status), tone = MonarchDataExportRequestRules.tone(status))
    }
    Text(
        text = texts.description,
        style = MonarchTheme.typography.body,
        color = tokens.noteColor(MonarchTheme.colors),
    )
}

/** The status line and the failure reason, in an always-present polite live region. */
@Composable
private fun DataExportRequestStatus(line: String?, reason: String?) {
    val spoken = listOfNotNull(line, reason).joinToString(" ")
    Column(
        modifier = Modifier.clearAndSetSemantics {
            if (spoken.isNotEmpty()) contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(MonarchDataExportRequestTokens.HeadingGap),
    ) {
        val color = MonarchDataExportRequestTokens.statusColor(MonarchTheme.colors)
        line?.let { Text(text = it, style = MonarchTheme.typography.body, color = color) }
        reason?.let { Text(text = it, style = MonarchTheme.typography.body, color = color) }
    }
}

/** The scope checklist, the format menu, a failed request and the request button. */
@Composable
private fun DataExportRequestForm(
    form: DataExportRequestFormInput,
    sending: DataExportRequestSending,
    send: (MonarchDataExportRequestDetails) -> Unit,
    texts: DataExportRequestTexts,
) {
    val rules = MonarchDataExportRequestRules
    var chosen by rememberSaveable { mutableStateOf(form.scope.map { it.id }) }
    var format by rememberSaveable { mutableStateOf(rules.initialFormat(form.formats, form.defaultFormat)) }
    var scopeError by rememberSaveable { mutableStateOf(false) }
    val busy = form.pending ?: sending.running
    val submit = submit@{
        if (busy) return@submit
        if (rules.scopeMissing(form.scope, chosen)) {
            scopeError = true
            return@submit
        }
        scopeError = false
        send(MonarchDataExportRequestDetails(rules.chosenScope(form.scope, chosen), format))
    }
    if (form.scope.isNotEmpty()) {
        MonarchCheckboxGroup(
            options = form.scope.map { MonarchCheckboxGroupOption(it.id, it.label, it.description) },
            value = chosen,
            onValueChange = {
                chosen = it
                if (it.isNotEmpty()) scopeError = false
            },
            enabled = !busy,
            invalid = scopeError,
            label = texts.form.scopeLegend,
            errorText = texts.form.scopeRequired.takeIf { scopeError },
        )
    }
    if (form.formats.isNotEmpty()) {
        MonarchSelect(
            options = form.formats.map { MonarchSelectOption(it.value, it.label) },
            selected = format,
            onSelect = { format = it },
            modifier = Modifier.fillMaxWidth(),
            label = texts.form.formatLabel,
            enabled = !busy,
        )
    }
    sending.failure?.let {
        MonarchInlineNotice(
            message = it,
            tone = MonarchTone.Danger,
            announcement = MonarchBannerAnnouncement.Assertive,
        )
    }
    MonarchButton(
        text = if (form.status == MonarchDataExportStatus.Idle) texts.form.request else texts.form.requestAgain,
        onClick = submit,
        variant = MonarchButtonVariant.Primary,
        loading = busy,
    )
}

/** The built-in strings of the card, or their replacements. */
private class DataExportRequestTexts(
    val title: String,
    val description: String,
    val status: (MonarchDataExportStatus) -> String,
    val download: String,
    val form: DataExportRequestFormTexts,
    val preparingProgress: String,
    val requestFailed: String,
    val lines: DataExportRequestLines,
)

/** The texts of the request form. */
private class DataExportRequestFormTexts(
    val request: String,
    val requestAgain: String,
    val scopeLegend: String,
    val scopeRequired: String,
    val formatLabel: String,
)

/** The status lines, each given formatted dates. */
private class DataExportRequestLines(
    val requested: (String?) -> String,
    val preparing: (String?) -> String,
    val ready: (String?, String?) -> String,
    val expired: (String?) -> String,
    val failed: String,
)

@Composable
private fun dataExportRequestTexts(labels: MonarchDataExportRequestLabels): DataExportRequestTexts {
    val statuses = MonarchDataExportStatus.entries.associateWith {
        monarchString(null, dataExportRequestStatusText(it))
    }
    return DataExportRequestTexts(
        title = monarchString(labels.title, R.string.monarch_dataexportrequest_title),
        description = monarchString(labels.description, R.string.monarch_dataexportrequest_description),
        status = { labels.status?.invoke(it) ?: statuses.getValue(it) },
        download = monarchString(labels.download, R.string.monarch_dataexportrequest_download),
        form = DataExportRequestFormTexts(
            request = monarchString(labels.request, R.string.monarch_dataexportrequest_request),
            requestAgain = monarchString(labels.requestAgain, R.string.monarch_dataexportrequest_request_again),
            scopeLegend = monarchString(labels.scopeLegend, R.string.monarch_dataexportrequest_scope_legend),
            scopeRequired = monarchString(labels.scopeRequired, R.string.monarch_dataexportrequest_scope_required),
            formatLabel = monarchString(labels.formatLabel, R.string.monarch_dataexportrequest_format_label),
        ),
        preparingProgress = monarchString(
            labels.preparingProgress,
            R.string.monarch_dataexportrequest_preparing_progress,
        ),
        requestFailed = monarchString(labels.requestFailed, R.string.monarch_dataexportrequest_request_failed),
        lines = dataExportRequestLines(labels),
    )
}

@Composable
private fun dataExportRequestLines(labels: MonarchDataExportRequestLabels): DataExportRequestLines {
    val locale = LocalConfiguration.current.locales[0]
    val requestedOn = monarchString(null, R.string.monarch_dataexportrequest_requested_on)
    val requested = monarchString(null, R.string.monarch_dataexportrequest_requested)
    val preparingOn = monarchString(null, R.string.monarch_dataexportrequest_preparing_on)
    val preparing = monarchString(null, R.string.monarch_dataexportrequest_preparing)
    val ready = monarchString(null, R.string.monarch_dataexportrequest_ready)
    val readyAt = monarchString(null, R.string.monarch_dataexportrequest_ready_at)
    val readyUntil = monarchString(null, R.string.monarch_dataexportrequest_ready_until)
    val expiredOn = monarchString(null, R.string.monarch_dataexportrequest_expired_on)
    val expired = monarchString(null, R.string.monarch_dataexportrequest_expired)
    val withDate: (String, String, String?) -> String = { format, plain, date ->
        if (date == null) plain else String.format(locale, format, date)
    }
    return DataExportRequestLines(
        requested = labels.requested ?: { withDate(requestedOn, requested, it) },
        preparing = labels.preparing ?: { withDate(preparingOn, preparing, it) },
        ready = labels.ready ?: { at, until ->
            listOfNotNull(
                ready,
                at?.let { String.format(locale, readyAt, it) },
                until?.let { String.format(locale, readyUntil, it) },
            ).joinToString(" ")
        },
        expired = labels.expired ?: { withDate(expiredOn, expired, it) },
        failed = monarchString(labels.failed, R.string.monarch_dataexportrequest_failed),
    )
}

private fun dataExportRequestStatusText(status: MonarchDataExportStatus): Int = when (status) {
    MonarchDataExportStatus.Idle -> R.string.monarch_dataexportrequest_status_idle
    MonarchDataExportStatus.Requested -> R.string.monarch_dataexportrequest_status_requested
    MonarchDataExportStatus.Preparing -> R.string.monarch_dataexportrequest_status_preparing
    MonarchDataExportStatus.Ready -> R.string.monarch_dataexportrequest_status_ready
    MonarchDataExportStatus.Expired -> R.string.monarch_dataexportrequest_status_expired
    MonarchDataExportStatus.Failed -> R.string.monarch_dataexportrequest_status_failed
}

private fun dataExportRequestStatusLine(
    status: MonarchDataExportStatus,
    requestedAt: String?,
    readyAt: String?,
    expiresAt: String?,
    texts: DataExportRequestTexts,
): String? = when (status) {
    MonarchDataExportStatus.Idle -> null
    MonarchDataExportStatus.Requested -> texts.lines.requested(requestedAt)
    MonarchDataExportStatus.Preparing -> texts.lines.preparing(requestedAt)
    MonarchDataExportStatus.Ready -> texts.lines.ready(readyAt, expiresAt)
    MonarchDataExportStatus.Expired -> texts.lines.expired(expiresAt)
    MonarchDataExportStatus.Failed -> texts.lines.failed
}
