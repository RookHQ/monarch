package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/** What one row needs from the panel. */
internal class RenewalsPanelRowContext(
    val now: Long,
    val urgentWithinDays: Int,
    val ownerShown: Boolean,
    val locale: Locale,
    val labels: MonarchRenewalsPanelLabels,
)

/** The day count under a date: "in 12 days", "today", "3 days ago". */
@Composable
internal fun renewalsPanelDays(days: Int, labels: MonarchRenewalsPanelLabels): String =
    labels.daysUntil?.invoke(days) ?: when {
        days == 0 -> stringResource(R.string.monarch_renewalspanel_today)
        days > 0 -> pluralStringResource(R.plurals.monarch_renewalspanel_in_days, days, days)
        else -> pluralStringResource(R.plurals.monarch_renewalspanel_days_ago, -days, -days)
    }

/** The words of one row, written and spoken. */
internal class RenewalsPanelRowParts(
    val urgent: Boolean,
    val dueSoon: String,
    val dateLine: String,
    val amountNote: String?,
    val unpriced: String?,
    val owner: String?,
    val next: String?,
    val extra: String?,
    val spoken: String,
)

/** The date with the notice deadline or the day count: "Jul 13, 2026 · in 12 days". */
@Composable
private fun renewalsPanelDateLine(item: MonarchRenewalItem, days: Int?, context: RenewalsPanelRowContext): String {
    val (date, under) = renewalsPanelDateParts(item, days, context)
    return MonarchRenewalsPanelTokens.line(listOf(date, under)).orEmpty()
}

/** The date and, under it, the notice deadline or the day count. */
@Composable
internal fun renewalsPanelDateParts(
    item: MonarchRenewalItem,
    days: Int?,
    context: RenewalsPanelRowContext,
): Pair<String, String?> {
    val tokens = MonarchRenewalsPanelTokens
    val labels = context.labels
    val date = item.renewalDateMillis?.let { tokens.dateText(it, context.locale) }
        ?: monarchString(labels.unknownDate, R.string.monarch_renewalspanel_unknown_date)
    val under = item.noticeDeadlineMillis?.let { tokens.dateText(it, context.locale) }?.let {
        labels.noticeBy?.invoke(it) ?: stringResource(R.string.monarch_renewalspanel_notice_by, it)
    } ?: days?.let { renewalsPanelDays(it, labels) }
    return date to under
}

/** "Auto-renews", "Manual renewal" or `null`. */
@Composable
internal fun renewalsPanelRenewMode(item: MonarchRenewalItem, labels: MonarchRenewalsPanelLabels): String? =
    when (item.autoRenew) {
        true -> monarchString(labels.autoRenews, R.string.monarch_renewalspanel_auto_renews)
        false -> monarchString(labels.manualRenewal, R.string.monarch_renewalspanel_manual_renewal)
        null -> null
    }

/** The next step with its date, prefixed "Overdue:" when overdue. */
@Composable
private fun renewalsPanelNext(item: MonarchRenewalItem, context: RenewalsPanelRowContext): String {
    val tokens = MonarchRenewalsPanelTokens
    val labels = context.labels
    val step = item.nextAction?.let { action ->
        tokens.line(listOf(action, item.nextActionDateMillis?.let { tokens.dateText(it, context.locale) }))
    } ?: monarchString(labels.noNextAction, R.string.monarch_renewalspanel_no_next_action)
    val overdue = monarchString(labels.overdue, R.string.monarch_renewalspanel_overdue)
    return if (item.nextActionOverdue) "$overdue: $step" else step
}

/** Every string of [item]'s row, from the labels or the built-in strings. */
@Composable
internal fun renewalsPanelRowParts(
    item: MonarchRenewalItem,
    status: MonarchStatusDefinition,
    context: RenewalsPanelRowContext,
): RenewalsPanelRowParts {
    val tokens = MonarchRenewalsPanelTokens
    val labels = context.labels
    val days = tokens.daysUntil(item, context.now)
    val urgent = tokens.urgent(item, days, context.urgentWithinDays)
    val dueSoon = monarchString(labels.dueSoon, R.string.monarch_renewalspanel_due_soon)
    val amount = item.amount
    val currency = item.currencyCode
    val price = if (amount != null && currency != null) {
        MonarchMoneyFormat.format(amount, currency, context.locale)
    } else {
        null
    }
    val unpriced = monarchString(labels.unpriced, R.string.monarch_renewalspanel_unpriced).takeIf { price == null }
    val noOwner = monarchString(labels.noOwner, R.string.monarch_renewalspanel_no_owner)
    val owner = if (context.ownerShown) item.owner ?: noOwner else null
    val next = if (context.ownerShown) renewalsPanelNext(item, context) else null
    val probability = item.probability?.let {
        labels.probability?.invoke(it) ?: stringResource(R.string.monarch_renewalspanel_probability, it)
    }
    val dateLine = renewalsPanelDateLine(item, days, context)
    val amountNote = tokens.line(listOf(item.amountNote, renewalsPanelRenewMode(item, labels)))
    val extra = tokens.line(listOf(probability, item.outcome))
    val spoken = listOfNotNull(
        item.name,
        status.label,
        dueSoon.takeIf { urgent },
        item.description?.takeIf { it.isNotBlank() },
        dateLine,
        price ?: unpriced,
        amountNote,
        owner,
        next,
        extra,
    ).joinToString(", ")
    return RenewalsPanelRowParts(urgent, dueSoon, dateLine, amountNote, unpriced, owner, next, extra, spoken)
}

/**
 * One renewal: its text is one TalkBack element read in words (a button when
 * [onClick] is given), and the row action, when present, is a separate button
 * under it.
 */
@Composable
internal fun RenewalsPanelRow(
    item: MonarchRenewalItem,
    status: MonarchStatusDefinition,
    context: RenewalsPanelRowContext,
    onClick: (() -> Unit)?,
    onAction: (() -> Unit)?,
) {
    val tokens = MonarchRenewalsPanelTokens
    val labels = context.labels
    val parts = renewalsPanelRowParts(item, status, context)
    val pressable = if (onClick != null) {
        Modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(tokens.TileShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(pressable)
                .semantics(mergeDescendants = true) { contentDescription = parts.spoken },
        ) {
            // The texts stay in the tree; TalkBack reads the content description of the row.
            Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
                RenewalsPanelRowText(item, status, parts, context)
            }
        }
        if (onAction != null) {
            val action = monarchString(labels.action, R.string.monarch_renewalspanel_action)
            val spoken = labels.actionFor?.invoke(action, item.name)
                ?: stringResource(R.string.monarch_renewalspanel_action_for, action, item.name)
            MonarchButton(
                text = action,
                onClick = onAction,
                modifier = Modifier.semantics { contentDescription = spoken },
                variant = MonarchButtonVariant.Secondary,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun RenewalsPanelRowText(
    item: MonarchRenewalItem,
    status: MonarchStatusDefinition,
    parts: RenewalsPanelRowParts,
    context: RenewalsPanelRowContext,
) {
    val tokens = MonarchRenewalsPanelTokens
    val colors = MonarchTheme.colors
    val muted = tokens.mutedColor(colors)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = item.name, style = MonarchTextStyle.BodyStrong)
        // The badges are part of the row's words, not separate TalkBack stops.
        Box(Modifier.clearAndSetSemantics { }) {
            MonarchStatusBadge(definition = status, size = MonarchStatusBadgeSize.Sm)
        }
        if (parts.urgent) {
            Box(Modifier.clearAndSetSemantics { }) { MonarchBadge(text = parts.dueSoon, tone = MonarchTone.Warning) }
        }
    }
    item.description?.takeIf { it.isNotBlank() }?.let {
        MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
    }
    MonarchText(text = parts.dateLine)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val amount = item.amount
        val currency = item.currencyCode
        if (amount != null && currency != null) {
            MonarchMoney(amount, currency, locale = context.locale, style = MonarchTextStyle.BodyStrong)
        }
        parts.unpriced?.let { MonarchText(text = it, color = muted) }
        parts.amountNote?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
    }
    parts.owner?.let { MonarchText(text = it, style = MonarchTextStyle.Caption) }
    parts.next?.let {
        val color = if (item.nextActionOverdue) tokens.overdueColor(colors) else muted
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.nextActionOverdue) {
                CompositionLocalProvider(LocalContentColor provides color) {
                    Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Warning() }
                }
            }
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = color)
        }
    }
    parts.extra?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
}
