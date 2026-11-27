package com.rookhq.monarch.components.workflow.runningtimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.MonarchTimeInput
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Date
import java.util.TimeZone

/** Asks before throwing the running time away. */
@Composable
internal fun RunningTimerDiscardDialog(
    text: RunningTimerText,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.discardTitle,
        description = text.discardMessage,
        dismissible = !busy,
        actions = {
            MonarchButton(
                text = text.cancel,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !busy,
            )
            MonarchButton(
                text = text.discardConfirm,
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
                loading = busy,
            )
        },
    )
}

/**
 * Asks when the person stopped: the start time, the themed `TimeInput`
 * that suggests an hour after the start (or now if that is sooner), and the
 * length of work that will be saved. The chosen time of day is the latest
 * such moment at or before now; Save is disabled while it is in the future
 * or less than a minute after the start. The dialog cannot be dismissed
 * while it saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
internal fun RunningTimerStopEarlierDialog(
    start: Long,
    now: Long,
    zone: TimeZone,
    text: RunningTimerText,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val tokens = MonarchRunningTimerTokens
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val (hour, minute) = remember(start) { tokens.hourMinute(tokens.suggestion(start, now), zone) }
    val picker = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    val chosen = tokens.resolve(picker.hour, picker.minute, now, zone)
    val problem = when (tokens.stopError(chosen, start, now)) {
        RunningTimerStopError.Future -> text.stoppedAtFuture
        RunningTimerStopError.TooEarly -> text.stoppedAtTooEarly
        null -> null
    }
    val startedText = remember(start, locale, zone) {
        android.icu.text.DateFormat.getInstanceForSkeleton(StartedSkeleton, locale).apply {
            timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
        }.format(Date(start))
    }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.stoppedEarlierTitle,
        description = text.stoppedEarlierMessage,
        dismissible = !busy,
        // A tap outside must not lose what was typed; back still closes it.
        dismissOnClickOutside = false,
        actions = {
            MonarchButton(
                text = text.cancel,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !busy,
            )
            MonarchButton(
                text = text.save,
                onClick = { onSave(chosen) },
                variant = MonarchButtonVariant.Primary,
                enabled = problem == null,
                loading = busy,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            Text(text.started(startedText), color = MonarchTheme.colors.textDefault)
            MonarchTimeInput(state = picker)
            Text(
                text = problem ?: text.workDuration(
                    tokens.hoursMinutes((chosen - start) / tokens.MINUTE_MILLIS),
                ),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = if (problem != null) MonarchTheme.colors.dangerStrong else MonarchTheme.colors.textMuted,
            )
            error?.let {
                Text(
                    text = it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MonarchTheme.colors.dangerStrong,
                )
            }
        }
    }
}

private const val StartedSkeleton = "yMMMdjmm"
