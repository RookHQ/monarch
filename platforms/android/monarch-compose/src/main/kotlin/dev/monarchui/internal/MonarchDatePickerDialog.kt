package dev.monarchui.internal

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
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
 * The Material 3 `DatePickerDialog` with Monarch buttons, opened by a date
 * field button (business hours exceptions, time off, schedules). Dates are
 * epoch milliseconds at midnight UTC, as `DatePickerState` gives them.
 *
 * The platform keeps the calendar, the input mode, the year list, the
 * locale and the dialog behaviour; Monarch themes it like the Monarch dialog:
 * the raised surface with 16dp corners, the overlay
 * scrim, a 20/600 headline, the muted title and weekdays, the chosen day on
 * the accent fill, today ringed in the accent, and a ghost Cancel beside a
 * primary confirm button.
 *
 * @param initial the date shown first, or `null` for today.
 * @param earliest the first date that can be chosen, or `null` for no limit.
 * @param confirmLabel the confirm button.
 * @param cancelLabel the cancel button.
 * @param onPick called with the chosen date when the person confirms.
 * @param onClose called when the dialog closes, after [onPick] or without a choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchDatePickerDialog(
    initial: Long?,
    earliest: Long?,
    confirmLabel: String,
    cancelLabel: String,
    onPick: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = earliest == null || utcTimeMillis >= earliest
        },
    )
    MonarchDatePickerDialog(state, confirmLabel, cancelLabel, onPick, onClose)
}

/** [MonarchDatePickerDialog] with a given state, for a caller that owns it and for previews. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchDatePickerDialog(
    state: DatePickerState,
    confirmLabel: String,
    cancelLabel: String,
    onPick: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val colors = monarchDatePickerColors(MonarchTheme.colors)
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            MonarchButton(
                text = confirmLabel,
                onClick = {
                    state.selectedDateMillis?.let(onPick)
                    onClose()
                },
                // Material pads its text buttons less: line the button up with the 24dp content edge.
                modifier = Modifier.padding(
                    end = MonarchTokens.Space.space4.dp,
                    bottom = MonarchTokens.Space.space2.dp,
                ),
                variant = MonarchButtonVariant.Primary,
            )
        },
        dismissButton = {
            MonarchButton(
                text = cancelLabel,
                onClick = onClose,
                modifier = Modifier.padding(bottom = MonarchTokens.Space.space2.dp),
                variant = MonarchButtonVariant.Ghost,
            )
        },
        shape = MonarchTheme.shapes.xl,
        tonalElevation = MonarchTokens.Space.space0.dp,
        colors = colors,
    ) {
        MonarchDialogScrim()
        MonarchDatePickerTheme {
            DatePicker(state = state, modifier = Modifier.monarchFitWidth(), colors = colors, showModeToggle = false)
        }
    }
}

/**
 * Material's type inside the picker, with the Monarch headline: the chosen
 * date at 20/600 instead of the 24 title role.
 */
@Composable
internal fun MonarchDatePickerTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography.copy(headlineLarge = MonarchTheme.typography.heading)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = typography, shapes = MaterialTheme.shapes) {
        content()
    }
}

/** The Material date picker in Monarch colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun monarchDatePickerColors(colors: MonarchColors): DatePickerColors = DatePickerDefaults.colors(
    containerColor = colors.bgRaised,
    titleContentColor = colors.textMuted,
    headlineContentColor = colors.textDefault,
    weekdayContentColor = colors.textMuted,
    subheadContentColor = colors.textMuted,
    navigationContentColor = colors.textMuted,
    yearContentColor = colors.textDefault,
    disabledYearContentColor = colors.textDisabled,
    currentYearContentColor = colors.accentStrong,
    selectedYearContentColor = colors.accentOnSolid,
    selectedYearContainerColor = colors.accentSolid,
    dayContentColor = colors.textDefault,
    disabledDayContentColor = colors.textDisabled,
    selectedDayContentColor = colors.accentOnSolid,
    selectedDayContainerColor = colors.accentSolid,
    todayContentColor = colors.accentStrong,
    todayDateBorderColor = colors.accentStrong,
    dayInSelectionRangeContentColor = colors.accentStrong,
    dayInSelectionRangeContainerColor = colors.accentSubtle,
    dividerColor = colors.borderSubtle,
)
