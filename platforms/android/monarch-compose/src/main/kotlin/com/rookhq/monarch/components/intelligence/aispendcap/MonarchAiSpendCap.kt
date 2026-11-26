package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A spending limit for AI usage: what has been spent against the limit this
 * period, whether that is fine, close or reached, and a form to change the
 * limit, when to warn and what happens at the limit. It reports decisions
 * through [onSave]; it never bills, blocks or fetches anything itself.
 *
 * Built by Monarch from a card with a heading, a `MonarchStatusBadge` for the
 * state, a Material 3 `LinearProgressIndicator` in the tone of the state,
 * `MonarchInlineNotice` when AI is paused, and an inline form of
 * `MonarchCurrencyInput`, `MonarchSelect`, `MonarchRadioCardGroup` and
 * `MonarchButton`s; removing the limit is confirmed in a
 * `MonarchDialog`. Amounts are integers in [unit], never floats. A change of
 * state after the card first appears is announced once through a polite
 * live region.
 *
 * @param spent spent so far this period, as an integer in [unit]. Negative values count as zero.
 * @param limit the limit as an integer in [unit], or `null` when none is set.
 * @param currencyCode the ISO 4217 code of every amount.
 * @param period the window the limit applies to.
 * @param modifier the modifier of the card.
 * @param unit what the integers count: the currency's minor unit, or micros.
 * @param warnAt the share of the limit, from 0 to 1, at which the state becomes warning.
 * @param behavior what happens at the limit.
 * @param resetsAtMillis when the period starts again, in epoch milliseconds (UTC); shown with the day and month,
 * and the time for a daily limit.
 * @param breakdown spending per agent or feature, listed under the bar.
 * @param maxLimit the highest limit this person may set, as an integer in [unit].
 * @param warnAtOptions the warn-at choices; the current value is always offered, values outside 0 to 1 are left out.
 * @param editing whether the form is open, when the app controls it. `null` keeps the state in the card, closed
 * at first.
 * @param onEditingChange called when the form opens or closes.
 * @param canEdit whether this person may change the limit. `false` shows a read-only note and no form.
 * @param saving shows Save as busy and disables the form while the app saves.
 * @param timeZone the time zone of the reset date. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device
 * does not know, is the device's zone.
 * @param labels replacements for every built-in string.
 * @param onSave called with the new settings on Save, or with a `null` limit once removal is confirmed.
 * @param onRequestIncrease for people who cannot edit: adds "Request a higher limit".
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiSpendCap(
    spent: Long,
    limit: Long?,
    currencyCode: String,
    period: MonarchAiSpendCapPeriod,
    modifier: Modifier = Modifier,
    unit: MonarchAiSpendCapUnit = MonarchAiSpendCapUnit.Minor,
    warnAt: Double = MonarchAiSpendCapTokens.DEFAULT_WARN_AT,
    behavior: MonarchAiSpendCapBehavior = MonarchAiSpendCapBehavior.Pause,
    resetsAtMillis: Long? = null,
    breakdown: List<MonarchAiSpendCapBreakdownItem> = emptyList(),
    maxLimit: Long? = null,
    warnAtOptions: List<Double> = MonarchAiSpendCapTokens.DefaultWarnAtOptions,
    editing: Boolean? = null,
    onEditingChange: ((Boolean) -> Unit)? = null,
    canEdit: Boolean = true,
    saving: Boolean = false,
    timeZone: String? = null,
    labels: MonarchAiSpendCapLabels = MonarchAiSpendCapLabels(),
    onSave: ((MonarchAiSpendCapSettings) -> Unit)? = null,
    onRequestIncrease: (() -> Unit)? = null,
) {
    val tokens = MonarchAiSpendCapTokens
    val colors = MonarchTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val format = rememberSpendCapFormat(locale, currencyCode, unit, timeZone)
    val (open, setOpen) = rememberSpendCapEditing(editing, canEdit, onEditingChange)
    val state = tokens.state(spent, limit, warnAt)
    val used = spent.coerceAtLeast(0)
    val periodText = monarchAiSpendCapPeriod(period, labels)
    val amountText = monarchAiSpendCapAmount(format.money(used), limit?.let(format::money), periodText, labels)
    val title = monarchString(labels.title, R.string.monarch_aispendcap_title)
    val stateText = monarchAiSpendCapState(state, labels)
    val paused = state == MonarchAiSpendCapState.Reached && behavior == MonarchAiSpendCapBehavior.Pause
    val announcement = spendCapAnnouncement(
        state,
        labels.announce?.invoke(title, stateText, amountText)
            ?: stringResource(R.string.monarch_aispendcap_announce, title, stateText, amountText),
    )
    val request = onRequestIncrease.takeIf { !canEdit }
    Column(
        modifier = modifier
            .monarchSurface(tokens.cardColors(colors), MonarchTheme.shapes.lg)
            .padding(tokens.Padding)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        SpendCapHeader(title, stateText, state, onEdit = { setOpen(true) }.takeIf { canEdit && !open }, limit, labels)
        Text(text = amountText, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
        tokens.fraction(used, limit)?.let { SpendCapBar(it, state, amountText, labels) }
        SpendCapFacts(used, limit, warnAt, behavior, resetsAtMillis, period, format, labels)
        if (paused) {
            SpendCapPausedNotice(
                limitText = format.money(limit ?: 0),
                periodText = periodText,
                onRaise = { setOpen(true) }.takeIf { canEdit && !open },
                onRequestIncrease = request,
                labels = labels,
            )
        }
        if (breakdown.isNotEmpty()) SpendCapBreakdown(breakdown, format, labels)
        if (open) {
            MonarchAiSpendCapEditor(
                spent = used,
                limit = limit,
                currencyCode = currencyCode,
                unit = unit,
                periodText = periodText,
                warnAt = warnAt,
                behavior = behavior,
                maxLimit = maxLimit,
                warnAtOptions = warnAtOptions,
                saving = saving,
                format = format,
                labels = labels,
                onSave = { settings ->
                    onSave?.invoke(settings)
                    setOpen(false)
                },
                onCancel = { setOpen(false) },
            )
        }
        if (!canEdit) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchIcon(
                    MonarchGlyphs.Lock,
                    contentDescription = null,
                    size = MonarchIconSize.Sm,
                    tint = colors.textMuted,
                )
                Text(
                    text = monarchString(labels.readOnly, R.string.monarch_aispendcap_read_only),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            // While paused, the request is the notice's action instead.
            if (!paused) request?.let { SpendCapRequestButton(it, labels) }
        }
        // Always present, so a new state is announced as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The formatter of the card, rebuilt when the locale, the currency, the unit or the time zone changes. */
@Composable
private fun rememberSpendCapFormat(
    locale: java.util.Locale,
    currencyCode: String,
    unit: MonarchAiSpendCapUnit,
    timeZone: String?,
): MonarchAiSpendCapFormat {
    val zone = monarchTimeZone(timeZone)
    return remember(locale, currencyCode, unit, zone) { MonarchAiSpendCapFormat(locale, currencyCode, unit, zone) }
}

/** Whether the form is open, kept inside when [editing] is `null`, and the function that opens and closes it. */
@Composable
private fun rememberSpendCapEditing(
    editing: Boolean?,
    canEdit: Boolean,
    onEditingChange: ((Boolean) -> Unit)?,
): Pair<Boolean, (Boolean) -> Unit> {
    var editingInside by rememberSaveable { mutableStateOf(false) }
    val open = canEdit && (editing ?: editingInside)
    return open to { next: Boolean ->
        if (editing == null) editingInside = next
        onEditingChange?.invoke(next)
    }
}

/**
 * The title and the state badge on the start, and, when the person may edit and the form is closed, Edit limit
 * or Set a limit on the end, as the web's header.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendCapHeader(
    title: String,
    stateText: String,
    state: MonarchAiSpendCapState,
    onEdit: (() -> Unit)?,
    limit: Long?,
    labels: MonarchAiSpendCapLabels,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap),
        verticalAlignment = Alignment.Top,
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
            MonarchStatusBadge(
                definition = MonarchStatusDefinition(
                    label = stateText,
                    tone = MonarchAiSpendCapTokens.tone(state),
                    // The web's state glyph, so the state is never told by colour alone.
                    icon = {
                        MonarchIcon(
                            MonarchAiSpendCapTokens.stateGlyph(state),
                            contentDescription = null,
                            size = MonarchIconSize.Sm,
                        )
                    },
                ),
                size = MonarchStatusBadgeSize.Sm,
            )
        }
        if (onEdit != null) {
            MonarchButton(
                text = if (limit == null) {
                    monarchString(labels.setLimit, R.string.monarch_aispendcap_set_limit)
                } else {
                    monarchString(labels.edit, R.string.monarch_aispendcap_edit)
                },
                onClick = onEdit,
                size = MonarchButtonSize.Sm,
                leadingIcon = {
                    MonarchIcon(MonarchGlyphs.Edit, contentDescription = null, size = MonarchIconSize.Sm)
                },
            )
        }
    }
}

/** The bar in the tone of the state, named for TalkBack with the amount line as its state. */
@Composable
private fun SpendCapBar(
    fraction: Float,
    state: MonarchAiSpendCapState,
    amountText: String,
    labels: MonarchAiSpendCapLabels,
) {
    val meterLabel = monarchString(labels.meterLabel, R.string.monarch_aispendcap_meter_label)
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = meterLabel
                stateDescription = amountText
            },
        color = MonarchAiSpendCapTokens.barColor(state, MonarchTheme.colors),
        trackColor = MonarchAiSpendCapTokens.trackColor(MonarchTheme.colors),
        // One continuous bar as on the web: no Material 3 track gap and no stop dot.
        gapSize = MonarchTheme.spacing.space0,
        drawStopIndicator = {},
    )
}

/** The danger notice shown while AI is paused at the limit, with Raise limit or Request a higher limit. */
@Composable
private fun SpendCapPausedNotice(
    limitText: String,
    periodText: String,
    onRaise: (() -> Unit)?,
    onRequestIncrease: (() -> Unit)?,
    labels: MonarchAiSpendCapLabels,
) {
    MonarchInlineNotice(
        message = labels.pausedBody?.invoke(limitText, periodText)
            ?: stringResource(R.string.monarch_aispendcap_paused_body, limitText, periodText),
        tone = MonarchTone.Danger,
        title = monarchString(labels.pausedTitle, R.string.monarch_aispendcap_paused_title),
        action = when {
            onRaise != null -> {
                {
                    MonarchButton(
                        text = monarchString(labels.raiseLimit, R.string.monarch_aispendcap_raise_limit),
                        onClick = onRaise,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
            onRequestIncrease != null -> {
                { SpendCapRequestButton(onRequestIncrease, labels) }
            }
            else -> null
        },
    )
}

/** Request a higher limit, for people who cannot edit. */
@Composable
private fun SpendCapRequestButton(onClick: () -> Unit, labels: MonarchAiSpendCapLabels) {
    MonarchButton(
        text = monarchString(labels.requestIncrease, R.string.monarch_aispendcap_request_increase),
        onClick = onClick,
        size = MonarchButtonSize.Sm,
    )
}

/** Remaining or over, when it warns, what happens at the limit and when the period resets, as muted captions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendCapFacts(
    used: Long,
    limit: Long?,
    warnAt: Double,
    behavior: MonarchAiSpendCapBehavior,
    resetsAtMillis: Long?,
    period: MonarchAiSpendCapPeriod,
    format: MonarchAiSpendCapFormat,
    labels: MonarchAiSpendCapLabels,
) {
    val facts = buildList {
        if (limit != null) {
            add(
                if (used > limit) {
                    val amount = format.money(used - limit)
                    labels.over?.invoke(amount) ?: stringResource(R.string.monarch_aispendcap_over, amount)
                } else {
                    val amount = format.money(limit - used)
                    labels.remaining?.invoke(amount) ?: stringResource(R.string.monarch_aispendcap_remaining, amount)
                },
            )
            val percent = format.percent(warnAt)
            add(labels.warnsAt?.invoke(percent) ?: stringResource(R.string.monarch_aispendcap_warns_at, percent))
            add(monarchAiSpendCapSummary(behavior, labels))
        }
        if (resetsAtMillis != null) {
            val date = format.date(resetsAtMillis, period)
            add(labels.resets?.invoke(date) ?: stringResource(R.string.monarch_aispendcap_resets, date))
        }
    }
    if (facts.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.LineGap),
    ) {
        facts.forEach {
            Text(text = it, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

/** The breakdown: a caption, then one row per agent or feature read as its label and amount. */
@Composable
private fun SpendCapBreakdown(
    breakdown: List<MonarchAiSpendCapBreakdownItem>,
    format: MonarchAiSpendCapFormat,
    labels: MonarchAiSpendCapLabels,
) {
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.LineGap)) {
        MonarchText(
            text = monarchString(labels.breakdownLabel, R.string.monarch_aispendcap_breakdown_label),
            style = MonarchTextStyle.Caption,
            color = colors.textMuted,
            heading = true,
        )
        breakdown.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap),
            ) {
                Text(text = item.label, modifier = Modifier.weight(1f), style = MonarchTheme.typography.body)
                Text(text = format.money(item.spent.coerceAtLeast(0)), style = MonarchTheme.typography.body)
            }
        }
    }
}

/** The announcement: empty until the state first changes after the card appeared, then [text] as it was then. */
@Composable
private fun spendCapAnnouncement(state: MonarchAiSpendCapState, text: String): String {
    val first = remember { state }
    val current by rememberUpdatedState(text)
    var announced by remember { mutableStateOf("") }
    // Only a change of state is news; the amount moves on every request.
    LaunchedEffect(state) { if (state != first) announced = current }
    return announced
}
