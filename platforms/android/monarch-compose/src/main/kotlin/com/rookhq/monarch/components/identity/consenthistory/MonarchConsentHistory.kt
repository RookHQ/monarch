package com.rookhq.monarch.components.identity.consenthistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A person's recorded consent decisions, newest first: when, how, which
 * policy version and rules applied, and whether each category was allowed.
 *
 * Built by Monarch from bordered records in a `Column`, with
 * `MonarchEmptyState` when there are none and a `MonarchButton` that runs
 * [onExport]. The title and each record's date are headings, so TalkBack's
 * heading navigation moves between decisions. Each line and each category is
 * one TalkBack element read as its name and value ("Analytics, Allowed"), so
 * nothing depends on colour. The records are not lazy: a consent history is
 * short and usually sits inside a scrolling screen.
 *
 * @param records the decisions, in any order; shown newest first.
 * @param modifier the modifier of the history.
 * @param categories the product's categories, for their names and order. Ids in a record that are not listed
 * are shown as the raw id, after the listed ones, sorted.
 * @param title the heading. `null` uses the built-in "Consent history" (`monarch_consenthistory_title`).
 * @param description a sentence under the heading.
 * @param showRelativeTime also shows how long ago each decision was ("3 days ago"), updated every minute.
 * @param formatDate formats each decision's date. `null` uses a medium date and short time in the
 * configuration's locale and the device's time zone.
 * @param exportTitle the title of the download button; name the format. `null` uses "Download history"
 * (`monarch_consenthistory_export`).
 * @param onExport builds and shares the file, for example with a `CreateDocument` launcher or a share sheet.
 * The button shows while there are records and is busy while this runs; throwing shows the export failure.
 * @param labels replacements for the other built-in strings.
 * @param allowedIcon an icon before the state of an allowed category, such as a check mark. Monarch bundles no icons.
 * @param deniedIcon an icon before the state of a category that is not allowed, such as a cross.
 */
@Composable
public fun MonarchConsentHistory(
    records: List<MonarchConsentRecord>,
    modifier: Modifier = Modifier,
    categories: List<MonarchConsentCategory> = emptyList(),
    title: String? = null,
    description: String? = null,
    showRelativeTime: Boolean = false,
    formatDate: ((Long) -> String)? = null,
    exportTitle: String? = null,
    onExport: (suspend () -> Unit)? = null,
    labels: MonarchConsentHistoryLabels = MonarchConsentHistoryLabels(),
    allowedIcon: (@Composable () -> Unit)? = null,
    deniedIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchConsentHistoryTokens
    val colors = MonarchTheme.colors
    val export = rememberExport(onExport)
    val failedText = monarchString(labels.exportFailed, R.string.monarch_consenthistory_export_failed)
    val now = minuteClock(showRelativeTime && records.isNotEmpty(), records.map { it.dateMillis })
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(
                text = monarchString(title, R.string.monarch_consenthistory_title),
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = colors.textDefault,
            )
            if (description != null) {
                Text(text = description, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
        if (export.failed) Text(text = failedText, style = MonarchTheme.typography.caption, color = colors.dangerStrong)
        if (records.isEmpty()) {
            MonarchEmptyState(
                title = monarchString(labels.emptyTitle, R.string.monarch_consenthistory_empty_title),
                size = MonarchEmptyStateSize.Sm,
                message = monarchString(labels.emptyMessage, R.string.monarch_consenthistory_empty_message),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.RecordGap)) {
                tokens.sorted(records).forEach { record ->
                    Record(
                        record,
                        categories,
                        if (showRelativeTime) now else null,
                        formatDate,
                        labels,
                        allowedIcon,
                        deniedIcon,
                    )
                }
            }
            if (onExport != null) {
                MonarchButton(
                    text = monarchString(exportTitle, R.string.monarch_consenthistory_export),
                    onClick = export.run,
                    size = MonarchButtonSize.Sm,
                    loading = export.running,
                )
            }
        }
        // Always present, so a failed download is announced once as a change of this live region.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (export.failed) contentDescription = failedText
            },
        )
    }
}

@Composable
private fun Record(
    record: MonarchConsentRecord,
    categories: List<MonarchConsentCategory>,
    now: Long?,
    formatDate: ((Long) -> String)?,
    labels: MonarchConsentHistoryLabels,
    allowedIcon: (@Composable () -> Unit)?,
    deniedIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchConsentHistoryTokens
    val colors = MonarchTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val date = formatDate?.invoke(record.dateMillis) ?: tokens.formatDate(record.dateMillis, locale)
    val method = labels.method?.invoke(record.method) ?: stringResource(tokens.methodName(record.method))
    val regime = labels.regime?.invoke(record.regime) ?: stringResource(tokens.regimeName(record.regime))
    val signal = if (record.globalPrivacyControl) {
        monarchString(labels.signalPresent, R.string.monarch_consenthistory_signal_present)
    } else {
        null
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = tokens.RecordPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
                Text(
                    text = stringResource(R.string.monarch_consenthistory_date_title),
                    modifier = Modifier.width(tokens.LabelWidth),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = date,
                        modifier = Modifier.semantics { heading() },
                        style = MonarchTheme.typography.body,
                        color = colors.textDefault,
                    )
                    if (now != null) {
                        Text(
                            text = tokens.relativeTime(record.dateMillis, now),
                            style = MonarchTheme.typography.caption,
                            color = colors.textMuted,
                        )
                    }
                }
            }
            Line(
                monarchString(labels.methodTitle, R.string.monarch_consenthistory_method_title),
                method,
                signal,
                strong = true,
            )
            Line(
                monarchString(labels.policyVersionTitle, R.string.monarch_consenthistory_policy_version_title),
                record.policyVersion,
                null,
            )
            Line(monarchString(labels.regimeTitle, R.string.monarch_consenthistory_regime_title), regime, null)
            val entries = tokens.entries(record, categories)
            if (entries.isNotEmpty()) Categories(entries, labels, allowedIcon, deniedIcon)
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

/** A line of a record: its name at the start and its value and any note beside it, read as one element. */
@Composable
private fun Line(name: String, value: String, note: String?, strong: Boolean = false) {
    val colors = MonarchTheme.colors
    val tokens = MonarchConsentHistoryTokens
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = tokens.spokenLine(name, value, note)
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        Text(
            text = name,
            modifier = Modifier.width(tokens.LabelWidth),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = if (strong) MonarchTheme.typography.bodyStrong else MonarchTheme.typography.body,
                color = colors.textDefault,
            )
            if (note != null) Text(text = note, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun Categories(
    entries: List<MonarchConsentHistoryEntry>,
    labels: MonarchConsentHistoryLabels,
    allowedIcon: (@Composable () -> Unit)?,
    deniedIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchConsentHistoryTokens
    val colors = MonarchTheme.colors
    val allowed = monarchString(labels.allowed, R.string.monarch_consenthistory_allowed)
    val denied = monarchString(labels.denied, R.string.monarch_consenthistory_denied)
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        Text(
            text = monarchString(labels.categoriesTitle, R.string.monarch_consenthistory_categories_title),
            modifier = Modifier.width(tokens.LabelWidth),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
        entries.forEach { entry ->
            val state = if (entry.allowed) allowed else denied
            Row(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = tokens.spokenLine(entry.label, state, null)
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalAlignment = Alignment.Top,
            ) {
                (if (entry.allowed) allowedIcon else deniedIcon)?.let { icon ->
                    CompositionLocalProvider(LocalContentColor provides tokens.categoryColor(entry.allowed, colors)) {
                        icon()
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        append(entry.label)
                        withStyle(SpanStyle(color = colors.textMuted)) { append(" · $state") }
                    },
                    style = MonarchTheme.typography.body,
                    color = colors.textDefault,
                )
            }
        }
        }
    }
}

/** The state of the download and a way to start it. */
private class ConsentExport(val running: Boolean, val failed: Boolean, val run: () -> Unit)

@Composable
private fun rememberExport(onExport: (suspend () -> Unit)?): ConsentExport {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onExport)
    var running by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    return ConsentExport(running, failed) {
        val export = current
        if (!running && export != null) {
            running = true
            failed = false
            scope.launch {
                val outcome = runCatching { export() }
                running = false
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                failed = outcome.isFailure
            }
        }
    }
}

/** The current time, redrawn as often as the freshest of [times] needs while [running], so relative times stay true. */
@Composable
private fun minuteClock(running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, running = running)
