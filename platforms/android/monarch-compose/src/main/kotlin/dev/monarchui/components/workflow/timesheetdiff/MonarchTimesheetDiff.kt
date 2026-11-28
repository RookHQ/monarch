package dev.monarchui.components.workflow.timesheetdiff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Date
import java.util.Locale

/**
 * What changed between two versions of a timesheet: added, removed and
 * changed entries, each named in words with its effect on the time, and a
 * totals line. For resubmissions, corrections and approval review.
 *
 * Built by Monarch from a `Column` of tinted lines, a `MonarchBadge` for the
 * kind of each line and tabular figures for the times. It is not lazy, so it
 * can sit in a card or a scrolling review screen; a review has tens of
 * changes, not thousands. Every line is one TalkBack element read in words:
 * "Removed, minus 45 minutes, Mon, Jan 12, 45 minutes, Pulse, non-billable".
 *
 * @param diff the entries added, removed and changed, from [MonarchTimesheetDiffResult.between] or the product.
 * @param modifier the modifier of the diff.
 * @param title the heading. `null` shows the built-in "What changed".
 * @param subtitle quiet text after the heading, such as "since version 3".
 * @param showTotals shows the totals line.
 * @param showBillable adds the billable change to the totals line. Turn it off when billing does not apply.
 * @param labels replacements for the built-in strings and formats.
 */
@Composable
public fun MonarchTimesheetDiff(
    diff: MonarchTimesheetDiffResult,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    showTotals: Boolean = true,
    showBillable: Boolean = true,
    labels: MonarchTimesheetDiffLabels = MonarchTimesheetDiffLabels(),
) {
    val text = rememberTimesheetDiffText(labels)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchTimesheetDiffTokens.Gap),
    ) {
        Heading(title ?: monarchString(labels.title, R.string.monarch_timesheetdiff_title), subtitle)
        if (diff.isEmpty) {
            Text(
                text = monarchString(labels.noChanges, R.string.monarch_timesheetdiff_no_changes),
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchTimesheetDiffTokens.ItemGap)) {
                diff.added.forEach { DiffLine(TimesheetDiffKind.Added, text.added, it.minutes, null, it, text) }
                diff.removed.forEach { DiffLine(TimesheetDiffKind.Removed, text.removed, -it.minutes, it, null, text) }
                diff.changed.forEach { change ->
                    val delta = change.after.minutes - change.before.minutes
                    DiffLine(
                        kind = TimesheetDiffKind.Changed,
                        name = text.changed(change.fields),
                        delta = delta.takeIf { it != 0 },
                        old = change.before,
                        new = change.after,
                        text = text,
                    )
                }
            }
        }
        if (showTotals) Totals(diff.totals, showBillable, text)
    }
}

/**
 * Compares [before] and [after] by entry id with
 * [MonarchTimesheetDiffResult.between] and shows the difference.
 *
 * @param before the entries of the old version.
 * @param after the entries of the new version.
 * @param modifier the modifier of the diff.
 * @param title the heading. `null` shows the built-in "What changed".
 * @param subtitle quiet text after the heading, such as "since version 3".
 * @param showTotals shows the totals line.
 * @param showBillable adds the billable change to the totals line.
 * @param labels replacements for the built-in strings and formats.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchTimesheetDiff(
    before: List<MonarchTimesheetDiffEntry>,
    after: List<MonarchTimesheetDiffEntry>,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    showTotals: Boolean = true,
    showBillable: Boolean = true,
    labels: MonarchTimesheetDiffLabels = MonarchTimesheetDiffLabels(),
) {
    val diff = remember(before, after) { MonarchTimesheetDiffResult.between(before, after) }
    MonarchTimesheetDiff(diff, modifier, title, subtitle, showTotals, showBillable, labels)
}

/** The resolved strings and formats of a diff. */
private class TimesheetDiffText(
    val added: String,
    val removed: String,
    val changed: (List<String>) -> String,
    val before: String,
    val after: String,
    val delta: (Int) -> String,
    val spokenDelta: (Int) -> String,
    /** The line of an entry, for the eye or (spoken) for TalkBack. */
    val line: (MonarchTimesheetDiffEntry, Boolean) -> String,
    val totals: (String, String?) -> String,
)

@Composable
private fun rememberTimesheetDiffText(labels: MonarchTimesheetDiffLabels): TimesheetDiffText {
    val tokens = MonarchTimesheetDiffTokens
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { TimesheetDiffDates(locale) }
    val noProject = monarchString(labels.noProject, R.string.monarch_timesheetdiff_no_project)
    val nonBillable = monarchString(labels.nonBillable, R.string.monarch_timesheetdiff_non_billable)
    val changedFormat = stringResource(R.string.monarch_timesheetdiff_changed)
    val builtInFields = builtInFieldNames()
    val plus = stringResource(R.string.monarch_timesheetdiff_plus)
    val minus = stringResource(R.string.monarch_timesheetdiff_minus)
    val totalFormat = stringResource(R.string.monarch_timesheetdiff_totals)
    val billableFormat = stringResource(R.string.monarch_timesheetdiff_totals_billable)
    val custom = labels.entry
    val spokenDuration: (Int) -> String = { tokens.spoken(it, locale) }
    val spokenDelta: (Int) -> String = { minutes ->
        val words = tokens.spoken(minutes, locale)
        when {
            minutes > 0 -> plus.format(words)
            minutes < 0 -> minus.format(words)
            else -> words
        }
    }
    return TimesheetDiffText(
        added = monarchString(labels.added, R.string.monarch_timesheetdiff_added),
        removed = monarchString(labels.removed, R.string.monarch_timesheetdiff_removed),
        changed = { fields ->
            val names = tokens.fieldNames(fields, labels.fields, builtInFields)
            labels.changed?.invoke(names) ?: changedFormat.format(names.joinToString(", "))
        },
        before = monarchString(labels.before, R.string.monarch_timesheetdiff_before),
        after = monarchString(labels.after, R.string.monarch_timesheetdiff_after),
        delta = labels.delta ?: tokens::signed,
        spokenDelta = labels.delta ?: spokenDelta,
        line = { entry, spoken ->
            val duration: (Int) -> String = if (spoken) spokenDuration else tokens::compact
            custom?.invoke(entry)
                ?: tokens.line(entry, dates.day(entry.date), duration, noProject, nonBillable, spoken)
        },
        totals = labels.totals ?: { total, billable ->
            if (billable == null) totalFormat.format(total) else billableFormat.format(total, billable)
        },
    )
}

@Composable
private fun builtInFieldNames(): Map<String, String> {
    val fields = MonarchTimesheetDiffFields
    return mapOf(
        fields.DATE to stringResource(R.string.monarch_timesheetdiff_field_date),
        fields.MINUTES to stringResource(R.string.monarch_timesheetdiff_field_minutes),
        fields.PROJECT to stringResource(R.string.monarch_timesheetdiff_field_project),
        fields.ACTIVITY to stringResource(R.string.monarch_timesheetdiff_field_activity),
        fields.TASK to stringResource(R.string.monarch_timesheetdiff_field_task),
        fields.BILLABLE to stringResource(R.string.monarch_timesheetdiff_field_billable),
        fields.NOTE to stringResource(R.string.monarch_timesheetdiff_field_note),
        fields.START to stringResource(R.string.monarch_timesheetdiff_field_start),
        fields.END to stringResource(R.string.monarch_timesheetdiff_field_end),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Heading(title: String, subtitle: String?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchTimesheetDiffTokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = MonarchTheme.colors.textDefault,
        )
        subtitle?.let {
            Text(text = it, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

/**
 * One line: the kind and the change of time, then the entry. An added entry
 * has only [new], a removed one only [old] (struck through), a changed one
 * both, the old line struck through above the new one.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiffLine(
    kind: TimesheetDiffKind,
    name: String,
    delta: Int?,
    old: MonarchTimesheetDiffEntry?,
    new: MonarchTimesheetDiffEntry?,
    text: TimesheetDiffText,
) {
    val tokens = MonarchTimesheetDiffTokens
    val colors = tokens.colors(MonarchTheme.colors, kind)
    val both = old != null && new != null
    val spoken = listOfNotNull(
        name,
        delta?.let(text.spokenDelta),
        old?.let { if (both) "${text.before}: ${text.line(it, true)}" else text.line(it, true) },
        new?.let { if (both) "${text.after}: ${text.line(it, true)}" else text.line(it, true) },
    ).joinToString(", ")
    val shape = MonarchTheme.shapes.md
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.container, shape)
            .drawBehind {
                val width = tokens.BarWidth.toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(colors.bar, topLeft = Offset(x, 0f), size = Size(width, size.height))
            }
            .padding(start = tokens.ItemPadding + tokens.BarWidth, end = tokens.ItemPadding)
            .padding(vertical = tokens.ItemPadding)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchBadge(text = name, tone = tokens.tone(kind))
            delta?.let {
                Text(
                    text = text.delta(it),
                    style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.label),
                    color = colors.delta,
                )
            }
        }
        old?.let {
            Text(
                text = text.line(it, false),
                style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body),
                color = colors.struck,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        new?.let {
            Text(
                text = text.line(it, false),
                style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body),
                color = colors.text,
            )
        }
    }
}

@Composable
private fun Totals(totals: MonarchTimesheetDiffTotals, showBillable: Boolean, text: TimesheetDiffText) {
    val visible = text.totals(text.delta(totals.total), text.delta(totals.billable).takeIf { showBillable })
    val spoken = text.totals(
        text.spokenDelta(totals.total),
        text.spokenDelta(totals.billable).takeIf { showBillable },
    )
    Text(
        text = visible,
        modifier = Modifier.semantics { contentDescription = spoken },
        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.label),
        color = MonarchTheme.colors.textDefault,
    )
}

/** The date of an entry, "Mon, Jan 12", read in UTC as the Material 3 date picker writes it. */
private class TimesheetDiffDates(locale: Locale) {
    private val format = android.icu.text.DateFormat.getInstanceForSkeleton(SKELETON, locale).apply {
        timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
    }

    fun day(millis: Long): String = format.format(Date(millis))

    private companion object {
        const val SKELETON = "EEEMMMd"
    }
}
