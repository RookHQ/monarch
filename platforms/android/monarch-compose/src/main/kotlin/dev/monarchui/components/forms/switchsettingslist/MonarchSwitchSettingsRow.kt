package dev.monarchui.components.forms.switchsettingslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.components.forms.switch.MonarchSwitch
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One setting of a `MonarchSwitchSettingsList`: a switch row (`MonarchSwitch`)
 * that saves the new value on its own.
 *
 * When the user toggles the row, the switch shows the new value at once and
 * [onSave] runs. When it returns, [onCheckedChange] receives the new value
 * and "Saved" shows for a short time. When it throws, the switch returns to
 * [checked] and the row shows "Couldn't save." with the reason and a retry
 * button. Toggles while a save runs are ignored. The row is one toggleable
 * TalkBack element with the switch role, labelled by [label] and
 * [description]; "Saved" and the error are announced.
 *
 * @param checked the saved value.
 * @param onCheckedChange called with the new value after [onSave] returned.
 * @param onSave saves the new value, for example on a server. Throw to report a failure. It runs in the
 * composition's coroutine scope and is cancelled when the row leaves the composition.
 * @param label the name of the setting.
 * @param modifier the modifier of the row.
 * @param enabled whether the user can toggle the row.
 * @param description what the setting does, under the label.
 * @param labels replacements for the built-in strings of this row; `null` properties keep the list's.
 * @param onStatusChange called when the save status changes.
 */
@Composable
public fun MonarchSwitchSettingsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSave: suspend (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    labels: MonarchSwitchSettingsListLabels? = null,
    onStatusChange: ((MonarchSwitchSettingsStatus) -> Unit)? = null,
) {
    val tokens = MonarchSwitchSettingsListTokens
    val config = LocalMonarchSwitchSettingsList.current
    val merged = labels?.or(config.labels) ?: config.labels
    val currentSave by rememberUpdatedState(onSave)
    val currentChange by rememberUpdatedState(onCheckedChange)
    val currentStatusChange by rememberUpdatedState(onStatusChange)
    val currentErrorMessage by rememberUpdatedState(merged.errorMessage)
    var status by remember { mutableStateOf(MonarchSwitchSettingsStatus.Idle) }
    var pending by remember { mutableStateOf<Boolean?>(null) }
    var failedValue by remember { mutableStateOf<Boolean?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun update(next: MonarchSwitchSettingsStatus) {
        if (status != next) {
            status = next
            currentStatusChange?.invoke(next)
        }
    }

    fun save(value: Boolean) {
        if (status == MonarchSwitchSettingsStatus.Saving) return
        pending = value
        update(MonarchSwitchSettingsStatus.Saving)
        scope.launch {
            try {
                currentSave(value)
                pending = null
                failedValue = null
                currentChange(value)
                update(MonarchSwitchSettingsStatus.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
                pending = null
                failedValue = value
                errorDetail = tokens.errorDetail(failure, currentErrorMessage)
                update(MonarchSwitchSettingsStatus.Error)
            }
        }
    }

    LaunchedEffect(status) {
        if (status == MonarchSwitchSettingsStatus.Saved) {
            delay(config.savedDurationMillis)
            update(MonarchSwitchSettingsStatus.Idle)
        }
    }

    val texts = rowTexts(merged, label, errorDetail)
    val shown = tokens.shownValue(checked, pending)
    val announcement = texts.announcement(status)

    val card = config.card
    val key = remember { Any() }
    if (card != null) {
        DisposableEffect(card, key) { onDispose { card.rowTops.remove(key) } }
    }
    val inCard = if (card == null) {
        Modifier
    } else {
        Modifier
            .onPlaced { card.rowTops[key] = it.positionInRoot().y }
            .padding(horizontal = tokens.RowHorizontalPadding, vertical = tokens.RowVerticalPadding)
    }
    Column(modifier = modifier.then(inCard)) {
        MonarchSwitch(
            checked = shown,
            onCheckedChange = { save(it) },
            modifier = Modifier.semantics { if (status == MonarchSwitchSettingsStatus.Error) error(texts.error) },
            enabled = enabled,
            label = label,
            description = description,
        )
        SaveStatusLine(status, texts, enabled) { failedValue?.let { save(it) } }
        // Always present, so "Saved" and the error are announced once as a change of this live region.
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement != null) contentDescription = announcement
            },
        )
    }
}

/** The built-in or replaced strings of one row. */
private class RowTexts(
    val saving: String,
    val saved: String,
    val error: String,
    val retry: String,
    val retryName: String,
) {
    /** What the live region says for a status: "Saved", the error, or nothing. */
    fun announcement(status: MonarchSwitchSettingsStatus): String? = when (status) {
        MonarchSwitchSettingsStatus.Saved -> saved
        MonarchSwitchSettingsStatus.Error -> error
        MonarchSwitchSettingsStatus.Idle, MonarchSwitchSettingsStatus.Saving -> null
    }
}

@Composable
private fun rowTexts(labels: MonarchSwitchSettingsListLabels, label: String, errorDetail: String?): RowTexts {
    val sentence = monarchString(labels.error, R.string.monarch_switchsettingslist_error)
    val detailed = stringResource(R.string.monarch_switchsettingslist_error_detail, sentence, errorDetail.orEmpty())
    val retry = monarchString(labels.retry, R.string.monarch_switchsettingslist_retry)
    val retryName = stringResource(R.string.monarch_switchsettingslist_retry_named, retry, label)
    return RowTexts(
        saving = monarchString(labels.saving, R.string.monarch_switchsettingslist_saving),
        saved = monarchString(labels.saved, R.string.monarch_switchsettingslist_saved),
        error = if (errorDetail == null) sentence else detailed,
        retry = retry,
        retryName = labels.retryLabel?.invoke(label) ?: retryName,
    )
}

/** The line under the row: the spinner and "Saving", "Saved", or the error with Retry. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveStatusLine(
    status: MonarchSwitchSettingsStatus,
    texts: RowTexts,
    enabled: Boolean,
    onRetry: () -> Unit,
) {
    val tokens = MonarchSwitchSettingsListTokens
    val color = tokens.statusColor(status, MonarchTheme.colors)
    when (status) {
        MonarchSwitchSettingsStatus.Saving -> Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.StatusGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchProgress(
                modifier = Modifier.size(tokens.SpinnerSize),
                variant = MonarchProgressVariant.Circular,
                label = texts.saving,
            )
            StatusText(texts.saving, color)
        }
        MonarchSwitchSettingsStatus.Saved -> StatusText(texts.saved, color)
        MonarchSwitchSettingsStatus.Error -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.StatusGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            StatusText(texts.error, color)
            MonarchButton(
                text = texts.retry,
                onClick = onRetry,
                modifier = Modifier.semantics { contentDescription = texts.retryName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            )
        }
        MonarchSwitchSettingsStatus.Idle -> Unit
    }
}

/** Status text, hidden from TalkBack: the live region and the row's error say it. */
@Composable
private fun StatusText(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.clearAndSetSemantics { },
        style = MonarchTheme.typography.caption,
        color = color,
    )
}
