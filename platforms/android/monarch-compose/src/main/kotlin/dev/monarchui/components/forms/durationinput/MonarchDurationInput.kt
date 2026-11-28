package dev.monarchui.components.forms.durationinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.R
import dev.monarchui.components.forms.textfield.MonarchFieldStepper
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A field for a length of time typed as text ("1h 30m", "90", "1.5h",
 * "1:30", "2 hours 15 minutes"), saved as whole minutes, with − and +
 * buttons that step on a grid.
 *
 * Built on `MonarchTextField` with autocorrection off, and the − and +
 * buttons at the end of the box, each a 48dp target. The text is read when the field loses focus, on the Done
 * key and before a step, never on every keystroke, and written back as
 * "1h 30m". Text that cannot be read, or a duration outside [minimum] and
 * [maximum], is kept on screen, flagged and explained, and never passed to
 * [onMinutesChange]. With a hardware keyboard, the up and down arrows step by
 * [step] and Page Up and Page Down by an hour. TalkBack reads the duration in
 * words ("1 hour, 30 minutes") as the field's state.
 *
 * @param minutes the duration in whole minutes, or `null` while the field is empty.
 * @param onMinutesChange called with the new duration, or `null` when the field is cleared, after the text is
 * read or a step is taken.
 * @param modifier the modifier of the field.
 * @param label the visible label.
 * @param placeholder replaces the built-in placeholder "e.g. 1h 30m" (`monarch_durationinput_placeholder`),
 * shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText an error from the app. It replaces the field's own messages while present.
 * @param minimum the shortest allowed duration in minutes. Shorter text is flagged; steps stop at it.
 * @param maximum the longest allowed duration in minutes. Longer text is flagged; steps stop at it.
 * @param step the minutes one press of − or + (or an arrow key) removes or adds, on a grid from zero.
 * @param largestUnit writes 26 hours as "26h" ([MonarchDurationInputUnit.Hours]) or "1d 2h"
 * ([MonarchDurationInputUnit.Days]).
 * @param minutesPerDay the minutes in a "d" when reading and writing; 480 for eight-hour work days.
 * @param enabled whether the user can edit the field and use the buttons.
 * @param readOnly whether the text can be selected and copied but not changed.
 * @param showSteppers shows the − and + buttons and the increase and decrease actions.
 * @param format writes minutes as text. `null` writes "0m", "45m", "1h 30m".
 * @param parse reads trimmed, non-empty text as minutes, or returns `null` when it cannot. `null` uses the
 * built-in reader. Empty text always clears the value.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchDurationInput(
    minutes: Int?,
    onMinutesChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    minimum: Int = 0,
    maximum: Int? = null,
    step: Int = MonarchDurationInputTokens.DEFAULT_STEP,
    largestUnit: MonarchDurationInputUnit = MonarchDurationInputUnit.Hours,
    minutesPerDay: Int = DurationInputMath.MINUTES_PER_DAY,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showSteppers: Boolean = true,
    format: ((Int) -> String)? = null,
    parse: ((String) -> Int?)? = null,
    labels: MonarchDurationInputLabels = MonarchDurationInputLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val write: (Int) -> String = format ?: { DurationInputMath.format(it, largestUnit, minutesPerDay) }
    val shown = minutes?.let(write).orEmpty()
    val draft = remember(shown) { mutableStateOf(shown) }
    val problem = remember(shown) { mutableStateOf<DurationInputProblem?>(null) }
    var focused by remember { mutableStateOf(false) }
    val editor = DurationEditor(
        minutes = minutes,
        bounds = DurationBounds(minimum, maximum, step),
        write = write,
        read = { text -> MonarchDurationInputTokens.read(text, parse, minutesPerDay) },
        draft = draft,
        problem = problem,
        onMinutesChange = onMinutesChange,
    )
    val locked = !enabled || readOnly
    val stepper = DurationStepper(
        shown = showSteppers,
        canDecrease = !locked && MonarchDurationInputTokens.canDecrease(minutes, minimum),
        canIncrease = !locked && MonarchDurationInputTokens.canIncrease(minutes, maximum),
        decreaseName = monarchString(labels.decrement, R.string.monarch_durationinput_decrement),
        increaseName = monarchString(labels.increment, R.string.monarch_durationinput_increment),
    )
    val message = problemMessage(problem.value, minimum, maximum, labels, write)
    val spoken = minutes?.let { labels.valueText?.invoke(it) ?: MonarchDurationInputTokens.valueText(it, locale) }

    MonarchTextField(
        value = draft.value,
        onValueChange = editor::edit,
        modifier = modifier
            .onFocusChanged { state ->
                val now = state.isFocused || state.hasFocus
                if (focused && !now) editor.commit()
                focused = now
            }
            .onPreviewKeyEvent { event ->
                val delta = MonarchDurationInputTokens.keyDelta(event.key, step)
                    ?.takeIf { !locked && event.type == KeyEventType.KeyDown }
                delta?.let { editor.stepBy(it.first, it.second) }
                delta != null
            }
            .semantics {
                if (spoken != null) stateDescription = spoken
                customActions = stepper.actions(editor)
            },
        label = label,
        placeholder = monarchString(placeholder, R.string.monarch_durationinput_placeholder),
        helperText = helperText,
        errorText = MonarchDurationInputTokens.shownError(errorText, message),
        enabled = enabled,
        readOnly = readOnly,
        trailingIcon = stepper.buttons(editor),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { editor.commit() }),
        interactionSource = interactionSource,
    )
}

/** The bounds and the grid of a duration field. */
private class DurationBounds(val minimum: Int, val maximum: Int?, val step: Int)

/** Reads, steps and saves the text of a duration field. */
private class DurationEditor(
    val minutes: Int?,
    val bounds: DurationBounds,
    val write: (Int) -> String,
    val read: (String) -> DurationInputParse,
    val draft: MutableState<String>,
    val problem: MutableState<DurationInputProblem?>,
    val onMinutesChange: (Int?) -> Unit,
) {
    fun edit(text: String) {
        draft.value = text
        problem.value = null
    }

    private fun set(next: Int?) {
        draft.value = next?.let(write).orEmpty()
        problem.value = null
        if (next != minutes) onMinutesChange(next)
    }

    fun commit() {
        when (val result = MonarchDurationInputTokens.commit(read(draft.value), bounds.minimum, bounds.maximum)) {
            is DurationInputCommit.Save -> set(result.minutes)
            is DurationInputCommit.Reject -> problem.value = result.problem
        }
    }

    fun stepBy(direction: Int, size: Int = bounds.step) {
        val base = MonarchDurationInputTokens.stepBase(read(draft.value), minutes, bounds.minimum)
        set(DurationInputMath.stepped(base, size, direction, bounds.minimum, bounds.maximum))
    }
}

/** The − and + buttons of a duration field and the matching TalkBack actions. */
private class DurationStepper(
    val shown: Boolean,
    val canDecrease: Boolean,
    val canIncrease: Boolean,
    val decreaseName: String,
    val increaseName: String,
) {
    fun actions(editor: DurationEditor): List<CustomAccessibilityAction> = if (!shown) {
        emptyList()
    } else {
        listOfNotNull(
            CustomAccessibilityAction(decreaseName) { editor.stepBy(-1); true }.takeIf { canDecrease },
            CustomAccessibilityAction(increaseName) { editor.stepBy(1); true }.takeIf { canIncrease },
        )
    }

    /** The − and + buttons at the end of the box. */
    fun buttons(editor: DurationEditor): (@Composable () -> Unit)? = if (!shown) {
        null
    } else {
        {
            MonarchFieldStepper(
                decreaseName = decreaseName,
                increaseName = increaseName,
                canDecrease = canDecrease,
                canIncrease = canIncrease,
                tint = { MonarchDurationInputTokens.stepper(it, MonarchTheme.colors) },
                onDecrease = { editor.stepBy(-1) },
                onIncrease = { editor.stepBy(1) },
            )
        }
    }
}

@Composable
private fun problemMessage(
    problem: DurationInputProblem?,
    minimum: Int,
    maximum: Int?,
    labels: MonarchDurationInputLabels,
    write: (Int) -> String,
): String? = when (problem) {
    DurationInputProblem.Invalid -> monarchString(labels.invalid, R.string.monarch_durationinput_invalid)
    DurationInputProblem.OutOfRange -> rangeMessage(minimum, maximum, labels, write)
    null -> null
}

@Composable
private fun rangeMessage(
    minimum: Int,
    maximum: Int?,
    labels: MonarchDurationInputLabels,
    write: (Int) -> String,
): String {
    labels.range?.let { return it(minimum, maximum) }
    return when {
        maximum != null && minimum > 0 ->
            stringResource(R.string.monarch_durationinput_between, write(minimum), write(maximum))
        maximum != null -> stringResource(R.string.monarch_durationinput_at_most, write(maximum))
        else -> stringResource(R.string.monarch_durationinput_at_least, write(minimum))
    }
}
