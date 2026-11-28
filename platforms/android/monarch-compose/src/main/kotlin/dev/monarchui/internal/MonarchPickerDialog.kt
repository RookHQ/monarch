package dev.monarchui.internal

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.dialog.MonarchDialogScrim
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * The platform picker dialogs for components that own the picker state: a date range, a date with words above
 * the calendar, or a time. Each is the Material 3 dialog themed like [MonarchDatePickerDialog]: the raised
 * surface with 16dp corners, the overlay scrim, the Monarch picker colours, a ghost Cancel beside a primary
 * confirm button. The platform keeps the calendar or the clock, the input modes, the locale and the dialog
 * behaviour (decision D10.1).
 *
 * @param onDismissRequest called on back, a tap outside, or Cancel.
 * @param confirmLabel the confirm button.
 * @param onConfirm called by the confirm button. Close the dialog in it.
 * @param cancelLabel the cancel button.
 * @param confirmEnabled whether the confirm button can be pressed, for example once a first day is picked.
 * @param neutralLabel an optional third action before Cancel that sets the choice aside, such as "None".
 * @param onNeutral called by the neutral action. Close the dialog in it.
 * @param content the picker, for example [MonarchDateRangePicker], with any words above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchPickerDialog(
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String,
    confirmEnabled: Boolean = true,
    neutralLabel: String? = null,
    onNeutral: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = monarchDatePickerColors(MonarchTheme.colors)
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            MonarchButton(
                text = confirmLabel,
                onClick = onConfirm,
                // Material pads its text buttons less: line the button up with the 24dp content edge.
                modifier = Modifier.padding(
                    end = MonarchTokens.Space.space4.dp,
                    bottom = MonarchTokens.Space.space2.dp,
                ),
                enabled = confirmEnabled,
                variant = MonarchButtonVariant.Primary,
            )
        },
        dismissButton = {
            Row(Modifier.padding(bottom = MonarchTokens.Space.space2.dp)) {
                if (neutralLabel != null && onNeutral != null) {
                    MonarchButton(text = neutralLabel, onClick = onNeutral, variant = MonarchButtonVariant.Ghost)
                }
                MonarchButton(text = cancelLabel, onClick = onDismissRequest, variant = MonarchButtonVariant.Ghost)
            }
        },
        shape = MonarchTheme.shapes.xl,
        tonalElevation = MonarchTokens.Space.space0.dp,
        colors = colors,
    ) {
        MonarchDialogScrim()
        MonarchDatePickerTheme { content() }
    }
}

/** The Material 3 `DatePicker` in Monarch colours, fitted to the width of the dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchDatePicker(state: DatePickerState, modifier: Modifier = Modifier) {
    val colors = monarchDatePickerColors(MonarchTheme.colors)
    DatePicker(state = state, modifier = modifier.monarchFitWidth(), colors = colors, showModeToggle = false)
}

/**
 * The Material 3 `DateRangePicker` in Monarch colours. [title] replaces the platform's "Select dates" line when
 * the dialog is about one item, for example the name of a task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchDateRangePicker(
    state: DateRangePickerState,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
) {
    val colors = monarchDatePickerColors(MonarchTheme.colors)
    if (title == null) {
        DateRangePicker(state = state, modifier = modifier, colors = colors)
    } else {
        DateRangePicker(state = state, modifier = modifier, colors = colors, title = title)
    }
}

/**
 * The Material 3 `TimePickerDialog` themed like the Monarch dialog, with the clock or the keyboard entry in
 * [content] ([MonarchTimePicker] or [MonarchTimeInput]).
 *
 * @param onDismissRequest called on back, a tap outside, or Cancel.
 * @param title the line above the picker.
 * @param confirmLabel the confirm button.
 * @param onConfirm called by the confirm button. Close the dialog in it.
 * @param cancelLabel the cancel button.
 * @param modeToggle the button that switches between the clock and the keyboard entry, if any.
 * @param content the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchTimePickerDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String,
    modeToggle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            MonarchButton(text = confirmLabel, onClick = onConfirm, variant = MonarchButtonVariant.Primary)
        },
        title = title,
        modeToggleButton = modeToggle,
        dismissButton = {
            MonarchButton(text = cancelLabel, onClick = onDismissRequest, variant = MonarchButtonVariant.Ghost)
        },
        shape = MonarchTheme.shapes.xl,
        containerColor = MonarchTheme.colors.bgRaised,
    ) {
        MonarchDialogScrim()
        content()
    }
}

/** The Material 3 clock `TimePicker` in Monarch colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchTimePicker(state: TimePickerState, modifier: Modifier = Modifier) {
    TimePicker(state = state, modifier = modifier, colors = monarchTimePickerColors(MonarchTheme.colors))
}

/** The Material 3 keyboard `TimeInput` in Monarch colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchTimeInput(state: TimePickerState, modifier: Modifier = Modifier) {
    TimeInput(state = state, modifier = modifier, colors = monarchTimePickerColors(MonarchTheme.colors))
}

/** The Material time picker in Monarch colours: a muted dial, the accent selector, accent-tinted chosen fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun monarchTimePickerColors(colors: MonarchColors): TimePickerColors = TimePickerDefaults.colors(
    clockDialColor = colors.bgMuted,
    clockDialSelectedContentColor = colors.accentOnSolid,
    clockDialUnselectedContentColor = colors.textDefault,
    selectorColor = colors.accentSolid,
    containerColor = colors.bgRaised,
    periodSelectorBorderColor = colors.borderDefault,
    periodSelectorSelectedContainerColor = colors.accentSubtle,
    periodSelectorUnselectedContainerColor = colors.bgRaised,
    periodSelectorSelectedContentColor = colors.accentStrong,
    periodSelectorUnselectedContentColor = colors.textMuted,
    timeSelectorSelectedContainerColor = colors.accentSubtle,
    timeSelectorUnselectedContainerColor = colors.bgMuted,
    timeSelectorSelectedContentColor = colors.accentStrong,
    timeSelectorUnselectedContentColor = colors.textDefault,
)
