package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.currencyinput.MonarchCurrencyInput
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardGroup
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardOption
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString

/**
 * The form of a `MonarchAiSpendCap`. The draft lives here, so it starts from
 * the saved settings each time the form opens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchAiSpendCapEditor(
    spent: Long,
    limit: Long?,
    currencyCode: String,
    unit: MonarchAiSpendCapUnit,
    periodText: String,
    warnAt: Double,
    behavior: MonarchAiSpendCapBehavior,
    maxLimit: Long?,
    warnAtOptions: List<Double>,
    saving: Boolean,
    format: MonarchAiSpendCapFormat,
    labels: MonarchAiSpendCapLabels,
    onSave: (MonarchAiSpendCapSettings) -> Unit,
    onCancel: () -> Unit,
) {
    val math = MonarchAiSpendCapMath
    val shares = math.shares(warnAtOptions, warnAt)
    var draft by rememberSaveable { mutableStateOf(limit?.let { math.toMinor(it, currencyCode, unit) }) }
    var draftWarnAt by rememberSaveable { mutableStateOf(math.initialShare(shares, warnAt)) }
    var draftBehavior by rememberSaveable { mutableStateOf(behavior) }
    var problem by rememberSaveable { mutableStateOf<MonarchAiSpendCapCheck?>(null) }
    var warnedBelow by rememberSaveable { mutableStateOf<Long?>(null) }
    var removing by rememberSaveable { mutableStateOf(false) }
    val locked = saving || removing
    val spentText = format.money(spent)
    val focus = LocalFocusManager.current
    val submit = {
        // The amount field reads its text when it loses focus; a tap on Save does not move focus.
        focus.clearFocus()
        val next = draft?.let { math.fromMinor(it, currencyCode, unit) }
        when (val check = MonarchAiSpendCapTokens.check(next, spent, maxLimit, warnedBelow)) {
            MonarchAiSpendCapCheck.Ok ->
                if (next != null) onSave(MonarchAiSpendCapSettings(next, draftWarnAt, draftBehavior))
            MonarchAiSpendCapCheck.BelowSpent -> {
                warnedBelow = next
                problem = check
            }
            else -> {
                warnedBelow = null
                problem = check
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.Gap)) {
        MonarchCurrencyInput(
            minorUnits = draft,
            currencyCode = currencyCode,
            onMinorUnitsChange = {
                // The field also reports on every commit; only a new amount clears the error and the warning.
                if (it != draft) {
                    draft = it
                    problem = null
                    warnedBelow = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = monarchString(labels.amountLabel, R.string.monarch_aispendcap_amount_label),
            helperText = labels.amountHint?.invoke(spentText, periodText)
                ?: stringResource(R.string.monarch_aispendcap_amount_hint, spentText, periodText),
            errorText = errorText(problem, maxLimit, format, labels),
            allowNegative = true,
            enabled = !locked,
        )
        if (problem == MonarchAiSpendCapCheck.BelowSpent) {
            SpendCapBelowSpentNotice(spentText, periodText, draftBehavior, labels)
        }
        MonarchSelect(
            options = shares.map { share ->
                val percent = format.percent(share)
                MonarchSelectOption(
                    value = share,
                    label = labels.warnAtOption?.invoke(percent)
                        ?: stringResource(R.string.monarch_aispendcap_warn_at_option, percent),
                )
            },
            selected = draftWarnAt,
            onSelect = { draftWarnAt = it },
            modifier = Modifier.fillMaxWidth(),
            label = monarchString(labels.warnAtLabel, R.string.monarch_aispendcap_warn_at_label),
            enabled = !locked,
        )
        MonarchRadioCardGroup(
            options = MonarchAiSpendCapBehavior.entries.map {
                MonarchRadioCardOption(
                    value = it,
                    title = monarchAiSpendCapOption(it, labels),
                    description = monarchAiSpendCapDescription(it, labels),
                )
            },
            selected = draftBehavior,
            onSelect = { draftBehavior = it },
            label = monarchString(labels.behaviorLabel, R.string.monarch_aispendcap_behavior_label),
            enabled = !locked,
        )
        SpendCapFormActions(
            canRemove = limit != null,
            confirmBelow = warnedBelow != null,
            saving = saving,
            labels = labels,
            onRemove = { removing = true },
            onCancel = onCancel,
            onSave = { if (!locked) submit() },
        )
    }
    if (removing) {
        MonarchDialog(
            onDismissRequest = { removing = false },
            title = monarchString(labels.removeConfirmTitle, R.string.monarch_aispendcap_remove_confirm_title),
            description = monarchString(labels.removeConfirmBody, R.string.monarch_aispendcap_remove_confirm_body),
            actions = {
                MonarchButton(
                    text = monarchString(labels.removeCancel, R.string.monarch_aispendcap_remove_cancel),
                    onClick = { removing = false },
                    variant = MonarchButtonVariant.Ghost,
                )
                MonarchButton(
                    text = monarchString(labels.removeLimit, R.string.monarch_aispendcap_remove_limit),
                    onClick = {
                        removing = false
                        onSave(MonarchAiSpendCapSettings(null, warnAt, behavior))
                    },
                    variant = MonarchButtonVariant.Danger,
                    loading = saving,
                )
            },
        )
    }
}

/** The error under the amount field for a failed check, or `null`. The below-spent warning is a notice instead. */
@Composable
private fun errorText(
    problem: MonarchAiSpendCapCheck?,
    maxLimit: Long?,
    format: MonarchAiSpendCapFormat,
    labels: MonarchAiSpendCapLabels,
): String? = when (problem) {
    MonarchAiSpendCapCheck.Required -> monarchString(labels.errorRequired, R.string.monarch_aispendcap_error_required)
    MonarchAiSpendCapCheck.Positive -> monarchString(labels.errorPositive, R.string.monarch_aispendcap_error_positive)
    MonarchAiSpendCapCheck.Max -> {
        val max = format.money(maxLimit ?: 0)
        labels.errorMax?.invoke(max) ?: stringResource(R.string.monarch_aispendcap_error_max, max)
    }
    else -> null
}

/** Remove limit, Cancel and Save (or Save anyway), aligned to the end and wrapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendCapFormActions(
    canRemove: Boolean,
    confirmBelow: Boolean,
    saving: Boolean,
    labels: MonarchAiSpendCapLabels,
    onRemove: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(MonarchAiSpendCapTokens.ItemGap),
    ) {
        if (canRemove) {
            MonarchButton(
                text = monarchString(labels.removeLimit, R.string.monarch_aispendcap_remove_limit),
                onClick = onRemove,
                variant = MonarchButtonVariant.Danger,
                size = MonarchButtonSize.Sm,
                enabled = !saving,
            )
        }
        MonarchButton(
            text = monarchString(labels.cancel, R.string.monarch_aispendcap_cancel),
            onClick = onCancel,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = !saving,
        )
        MonarchButton(
            text = if (confirmBelow) {
                monarchString(labels.saveAnyway, R.string.monarch_aispendcap_save_anyway)
            } else {
                monarchString(labels.save, R.string.monarch_aispendcap_save)
            },
            onClick = onSave,
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
            loading = saving,
        )
    }
}

/** The warning that the new limit is below what is spent; Save becomes Save anyway. Announced politely. */
@Composable
private fun SpendCapBelowSpentNotice(
    spentText: String,
    periodText: String,
    behavior: MonarchAiSpendCapBehavior,
    labels: MonarchAiSpendCapLabels,
) {
    val resource = if (behavior == MonarchAiSpendCapBehavior.Pause) {
        R.string.monarch_aispendcap_below_spent_pause
    } else {
        R.string.monarch_aispendcap_below_spent_notify
    }
    MonarchInlineNotice(
        message = labels.belowSpent?.invoke(spentText, periodText, behavior)
            ?: stringResource(resource, spentText, periodText),
        tone = MonarchTone.Warning,
        announcement = MonarchBannerAnnouncement.Polite,
    )
}
