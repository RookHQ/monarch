package dev.monarchui.components.identity.consentpreferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.components.identity.consenthistory.MonarchConsentMethod
import dev.monarchui.components.identity.consenthistory.MonarchConsentRecord
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme

/**
 * Privacy choices for a sheet: one switch per category of optional storage
 * or processing, with Reject all, Accept all and Save choices, the saved
 * policy version and time, and Withdraw consent.
 *
 * Built by Monarch from a `MonarchSwitchRow` per category (the Material 3
 * `Switch` as one row), `MonarchButton`s and a `MonarchLink` to the policy.
 * It is the content of a Material 3 `ModalBottomSheet` that the app shows and
 * closes, because consent storage is the app's: the app passes the saved
 * choices and record and stores the decision from [onDecide]. Strictly
 * necessary categories are on, disabled and say "Always on"; with a Global
 * Privacy Control signal, sale-or-sharing categories are off, disabled and
 * say why, so TalkBack reads why a switch cannot change. Reject all and
 * Accept all have the same weight; they wrap onto separate lines when they
 * do not fit side by side.
 *
 * @param categories the categories in order.
 * @param onDecide called when a button is pressed, with the method (`AcceptAll`, `RejectAll`, `Custom` or
 * `Withdrawn`) and every category's state with the locks applied. Store the record and close the sheet here.
 * @param modifier the modifier of the content.
 * @param choices the saved choice by category id. Leave it empty until the person has chosen, so nothing optional
 * is on. The switches start from it again whenever it changes.
 * @param mode a switch per category, or only the status of each and the two buttons.
 * @param globalPrivacyControl whether a Global Privacy Control signal is on. Sale-or-sharing categories stay off.
 * @param record the saved decision: shows "Policy version 3 · Saved 2 days ago" and allows Withdraw consent.
 * @param allowWithdraw whether Withdraw consent is offered in categories mode when a decision is saved and not
 * already withdrawn.
 * @param now the reference time for the saved time, in epoch milliseconds. Without it the clock is used and
 * refreshed every minute.
 * @param onPolicyClick opens the privacy policy and shows the policy link. Monarch does not navigate.
 * @param labels replacements for every built-in string.
 */
@Composable
public fun MonarchConsentPreferences(
    categories: List<MonarchConsentPreferencesCategory>,
    onDecide: (MonarchConsentMethod, Map<String, Boolean>) -> Unit,
    modifier: Modifier = Modifier,
    choices: Map<String, Boolean> = emptyMap(),
    mode: MonarchConsentPreferencesMode = MonarchConsentPreferencesMode.Categories,
    globalPrivacyControl: Boolean = false,
    record: MonarchConsentRecord? = null,
    allowWithdraw: Boolean = true,
    now: Long? = null,
    onPolicyClick: (() -> Unit)? = null,
    labels: MonarchConsentPreferencesLabels = MonarchConsentPreferencesLabels(),
) {
    val tokens = MonarchConsentPreferencesTokens
    val colors = MonarchTheme.colors
    val draft = remember(choices) { mutableStateMapOf<String, Boolean>().apply { putAll(choices) } }
    val decide = { method: MonarchConsentMethod ->
        onDecide(method, tokens.decision(method, categories, draft, globalPrivacyControl))
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(
                text = monarchString(labels.title, R.string.monarch_consentpreferences_title),
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.heading,
                color = colors.textDefault,
            )
            Text(
                text = monarchString(labels.description, R.string.monarch_consentpreferences_description),
                style = MonarchTheme.typography.body,
                color = colors.textMuted,
            )
            if (onPolicyClick != null) {
                MonarchLink(
                    text = monarchString(labels.policy, R.string.monarch_consentpreferences_policy),
                    onClick = onPolicyClick,
                    variant = MonarchLinkVariant.Standalone,
                )
            }
        }
        Column {
            categories.forEachIndexed { index, category ->
                if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                if (mode == MonarchConsentPreferencesMode.Categories) {
                    CategorySwitch(category, draft, globalPrivacyControl, labels)
                } else {
                    CategoryStatus(category, choices[category.id] ?: false, globalPrivacyControl, labels)
                }
            }
        }
        if (record != null) {
            val clock = consentPreferencesClock(now, record.dateMillis)
            val locale = LocalConfiguration.current.locales[0]
            Text(
                text = tokens.recordLine(
                    monarchString(labels.policyVersion, R.string.monarch_consentpreferences_policy_version),
                    record.policyVersion,
                    monarchString(labels.saved, R.string.monarch_consentpreferences_saved),
                    tokens.savedTime(record.dateMillis, clock, locale),
                ),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
        }
        Buttons(
            mode = mode,
            withdraw = tokens.offersWithdraw(mode, allowWithdraw, record),
            decide = decide,
            labels = labels,
        )
    }
}

@Composable
private fun CategorySwitch(
    category: MonarchConsentPreferencesCategory,
    draft: MutableMap<String, Boolean>,
    globalPrivacyControl: Boolean,
    labels: MonarchConsentPreferencesLabels,
) {
    val tokens = MonarchConsentPreferencesTokens
    val colors = MonarchTheme.colors
    val locked = tokens.locked(category, globalPrivacyControl)
    val checked = tokens.effective(category, draft[category.id] ?: false, globalPrivacyControl)
    val signalNote = if (tokens.keptOffBySignal(category, globalPrivacyControl)) {
        categoryNote(category, globalPrivacyControl, labels)
    } else {
        null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = !locked,
                role = Role.Switch,
                onValueChange = { draft[category.id] = it },
            )
            .padding(vertical = tokens.TextGap * 3),
        horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(text = category.label, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            Text(
                text = listOfNotNull(category.description, signalNote).joinToString("\n"),
                style = MonarchTheme.typography.body,
                color = colors.textMuted,
            )
        }
        if (category.required) {
            Text(
                text = monarchString(labels.alwaysOn, R.string.monarch_consentpreferences_always_on),
                modifier = Modifier.padding(top = tokens.TextGap),
                style = MonarchTheme.typography.label,
                color = colors.textMuted,
            )
        }
        MonarchSwitch(checked = checked, onCheckedChange = null, enabled = !locked)
    }
}

/** A category in choice mode: its name, description and status word, read as one element. */
@Composable
private fun CategoryStatus(
    category: MonarchConsentPreferencesCategory,
    saved: Boolean,
    globalPrivacyControl: Boolean,
    labels: MonarchConsentPreferencesLabels,
) {
    val tokens = MonarchConsentPreferencesTokens
    val colors = MonarchTheme.colors
    val on = tokens.effective(category, saved, globalPrivacyControl)
    val status = when {
        category.required -> monarchString(labels.alwaysOn, R.string.monarch_consentpreferences_always_on)
        on -> monarchString(labels.on, R.string.monarch_consentpreferences_on)
        else -> monarchString(labels.off, R.string.monarch_consentpreferences_off)
    }
    val note = if (tokens.keptOffBySignal(category, globalPrivacyControl)) {
        categoryNote(category, globalPrivacyControl, labels)
    } else {
        null
    }
    val details = listOfNotNull(category.description, note)
    Column(
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = (listOf(category.label) + details).joinToString(", ")
                stateDescription = status
            },
        verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap), verticalAlignment = Alignment.Top) {
            Text(
                text = category.label,
                modifier = Modifier.weight(1f),
                style = MonarchTheme.typography.bodyStrong,
                color = colors.textDefault,
            )
            Text(text = status, style = MonarchTheme.typography.label, color = tokens.statusColor(on, colors))
        }
        details.forEach { Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted) }
    }
}

@Composable
private fun categoryNote(
    category: MonarchConsentPreferencesCategory,
    globalPrivacyControl: Boolean,
    labels: MonarchConsentPreferencesLabels,
): String? = when {
    category.required -> monarchString(labels.alwaysOn, R.string.monarch_consentpreferences_always_on)
    MonarchConsentPreferencesTokens.keptOffBySignal(category, globalPrivacyControl) ->
        monarchString(labels.globalPrivacyControl, R.string.monarch_consentpreferences_global_privacy_control)
    else -> null
}

@Composable
private fun Buttons(
    mode: MonarchConsentPreferencesMode,
    withdraw: Boolean,
    decide: (MonarchConsentMethod) -> Unit,
    labels: MonarchConsentPreferencesLabels,
) {
    val gap = MonarchConsentPreferencesTokens.ButtonGap
    FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        MonarchButton(
            text = monarchString(labels.rejectAll, R.string.monarch_consentpreferences_reject_all),
            onClick = { decide(MonarchConsentMethod.RejectAll) },
        )
        MonarchButton(
            text = monarchString(labels.acceptAll, R.string.monarch_consentpreferences_accept_all),
            onClick = { decide(MonarchConsentMethod.AcceptAll) },
        )
        if (mode == MonarchConsentPreferencesMode.Categories) {
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_consentpreferences_save),
                onClick = { decide(MonarchConsentMethod.Custom) },
                variant = MonarchButtonVariant.Primary,
            )
        }
    }
    if (withdraw) {
        MonarchButton(
            text = monarchString(labels.withdraw, R.string.monarch_consentpreferences_withdraw),
            onClick = { decide(MonarchConsentMethod.Withdrawn) },
            variant = MonarchButtonVariant.Ghost,
        )
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the age of [time] needs. */
@Composable
private fun consentPreferencesClock(fixed: Long?, time: Long): Long =
    rememberMonarchRelativeClock(listOf(time), fixed = fixed)
