package com.rookhq.monarch.components.forms.numberinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.textfield.MonarchFieldStepper
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A number field that reads and writes numbers in the user's locale
 * ("1.234,5" in German), with − and + buttons that step without drift
 * (0.1 + 0.2 is 0.3).
 *
 * Built on `MonarchTextField` with the decimal keyboard, and the − and + buttons at the end of the
 * box, each a 48dp target. Every edit that reads as a number inside [minimum] and [maximum] is passed
 * to [onValueChange] at once, so a button tapped right after typing reads the typed number. The text
 * the person types stays as typed until the field loses focus or the Done key is pressed; then it is
 * shown in the locale format. Every edit that reads as a number is passed to [onValueChange] at once,
 * also a number outside the bounds, so a button tapped right after typing reads what is on screen;
 * validate the bounds before saving. Text that is not a number is kept on screen and not passed on.
 * Both are flagged and explained when the field loses focus or on Done, not while the person types.
 *
 * @param value the number, or `null` while the field is empty.
 * @param onValueChange called with the new number, or `null` when the field is cleared, on every edit that
 * reads as a number (in range or not) and after a step. The number is rounded to the digits shown, so the value is
 * always what is shown once the field is left.
 * @param modifier the modifier of the field.
 * @param label the visible label.
 * @param placeholder shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText an error from the app. It replaces the field's own messages while present.
 * @param minimum the smallest allowed value. Typed values below it are flagged; steps stop at it.
 * @param maximum the largest allowed value. Typed values above it are flagged; steps stop at it.
 * @param step the amount one press of − or + removes or adds. A value between two steps moves to the next
 * step first. Steps are counted from [minimum], or from 0.
 * @param enabled whether the user can edit the field and use the buttons.
 * @param readOnly whether the number can be selected and copied but not changed.
 * @param showSteppers shows the − and + buttons.
 * @param format replaces the locale's number format for display, for example to add a unit.
 * @param parse replaces the locale-aware reading of the text. It receives non-blank text and returns `null` when
 * the text is not a number.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
public fun MonarchNumberInput(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    minimum: Double? = null,
    maximum: Double? = null,
    step: Double = 1.0,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showSteppers: Boolean = true,
    format: ((Double) -> String)? = null,
    parse: ((String) -> Double?)? = null,
    labels: MonarchNumberInputLabels = MonarchNumberInputLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    NumberInputField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        bounds = NumberInputBounds(minimum, maximum, step),
        options = NumberInputOptions(enabled, readOnly, showSteppers),
        format = format,
        parse = parse,
        labels = labels,
        interactionSource = interactionSource,
    )
}

/** The range and the grid of a number field, as the caller gave them. */
internal class NumberInputBounds(
    val minimum: Double?,
    val maximum: Double?,
    val step: Double,
    /** The decimals the value is rounded to. `null` takes them from [step], with at least three. */
    val fractionDigits: Int? = null,
)

/** The state flags of a number field. */
internal class NumberInputOptions(val enabled: Boolean, val readOnly: Boolean, val showSteppers: Boolean)

/**
 * The number field behind `MonarchNumberInput` and `MonarchCurrencyInput`.
 * The currency field passes the currency's decimals in [bounds], so the value
 * sent while typing is already the stored amount.
 */
@Composable
@Suppress("LongParameterList")
internal fun NumberInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier,
    label: String?,
    placeholder: String?,
    helperText: String?,
    errorText: String?,
    bounds: NumberInputBounds,
    options: NumberInputOptions,
    format: ((Double) -> String)?,
    parse: ((String) -> Double?)?,
    labels: MonarchNumberInputLabels,
    interactionSource: MutableInteractionSource?,
) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    val places = bounds.fractionDigits
        ?: maxOf(MonarchNumberInputTokens.MIN_FRACTION_DIGITS, NumberInputMath.decimalPlaces(bounds.step))
    val formatter = remember(locale, places) { NumberInputMath.formatter(locale, places) }
    val show: (Double) -> String = format ?: { formatter.format(it) }
    val shown = value?.let(show).orEmpty()
    val stored = remember { mutableStateOf(NumberDraft.shown(shown, value)) }
    // The draft stays while it still stands for the value: typed text as long
    // as the app keeps the number it was sent (or keeps its value unchanged),
    // shown text as long as the formatted value is the same. Any other change
    // of the value by the app replaces it.
    val current = { stored.value.takeIf { it.standsFor(value, shown) } ?: NumberDraft.shown(shown, value) }
    val draft = current()
    SideEffect { if (stored.value != draft) stored.value = draft }
    var focused by remember { mutableStateOf(false) }
    val editor = NumberEditor(
        value = value,
        bounds = NumberBounds(bounds.minimum, bounds.maximum, bounds.step, places),
        show = show,
        read = parse ?: { NumberInputMath.parse(it, locale) },
        messages = NumberMessages(
            invalid = monarchString(labels.invalid, R.string.monarch_numberinput_invalid),
            range = rangeMessage(bounds.minimum?.let(show), bounds.maximum?.let(show), labels),
        ),
        draft = current,
        store = { stored.value = it },
        onValueChange = onValueChange,
    )
    val locked = !options.enabled || options.readOnly
    val stepper = NumberStepper(
        shown = options.showSteppers,
        canDecrease = !locked && editor.canDecrease,
        canIncrease = !locked && editor.canIncrease,
        decreaseName = monarchString(labels.decrement, R.string.monarch_numberinput_decrement),
        increaseName = monarchString(labels.increment, R.string.monarch_numberinput_increment),
    )

    MonarchTextField(
        value = draft.text,
        onValueChange = editor::edit,
        modifier = modifier
            .onFocusChanged { state ->
                val now = state.isFocused || state.hasFocus
                if (focused && !now) editor.commit()
                focused = now
            }
            .semantics { customActions = stepper.actions(editor) },
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText ?: draft.problem,
        enabled = options.enabled,
        readOnly = options.readOnly,
        trailingIcon = stepper.buttons(editor),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { editor.commit() }),
        interactionSource = interactionSource,
    )
}

/**
 * The text of a number field and what it stands for.
 *
 * @property text the text on screen.
 * @property value the number the text stands for: the number last sent for typed text, the value for shown text.
 * @property typed whether the person typed [text]. Typed text is kept as typed until the field is left.
 * @property seen for typed text, the value the app had when the person typed it.
 * @property shown the formatted value [text] was made from, for shown text.
 * @property problem the message shown under the field, set when the field is left.
 */
@Immutable
internal data class NumberDraft(
    val text: String,
    val value: Double?,
    val typed: Boolean,
    val shown: String,
    val seen: Double? = null,
    val problem: String? = null,
) {
    /** Whether this draft still stands for [current], whose formatted text is [currentShown]. */
    fun standsFor(current: Double?, currentShown: String): Boolean =
        if (typed) current == value || current == seen else shown == currentShown

    companion object {
        /** The formatted [value], as [shown]. */
        fun shown(shown: String, value: Double?): NumberDraft = NumberDraft(shown, value, typed = false, shown = shown)
    }
}

/** The bounds and the grid of a number field. */
private class NumberBounds(val minimum: Double?, val maximum: Double?, val step: Double, val places: Int) {
    /** Whether [value], rounded to the digits shown, is outside the bounds. */
    fun excludes(value: Double): Boolean =
        NumberInputMath.outOfRange(NumberInputMath.roundTo(value, places), minimum, maximum)
}

/** The messages of a number field, already resolved. */
private class NumberMessages(val invalid: String, val range: String?)

/** Reads, steps and saves the text of a number field. */
private class NumberEditor(
    val value: Double?,
    val bounds: NumberBounds,
    val show: (Double) -> String,
    val read: (String) -> Double?,
    val messages: NumberMessages,
    /** The draft now, read at the time of each event, so two events between frames see each other. */
    val draft: () -> NumberDraft,
    val store: (NumberDraft) -> Unit,
    val onValueChange: (Double?) -> Unit,
) {
    val canDecrease: Boolean get() = value == null || bounds.minimum == null || value > bounds.minimum
    val canIncrease: Boolean get() = value == null || bounds.maximum == null || value < bounds.maximum

    /**
     * Keeps [text] as typed and sends the number at once whenever it reads as
     * one, in range or not, so a Save tapped right after typing reads what is
     * on screen (C-021). Text that is not a number, and a number out of range,
     * is only explained when the field is left.
     */
    fun edit(text: String) {
        val next: Double? = when (val reading = if (text.isBlank()) Reading.Number(null) else readText(text)) {
            is Reading.Number -> reading.value
            is Reading.OutOfRange -> reading.value
            Reading.Invalid -> {
                store(NumberDraft(text, value, typed = true, shown = draft().shown, seen = value))
                return
            }
        }
        store(NumberDraft(text, next, typed = true, shown = draft().shown, seen = value))
        if (next != value) onValueChange(next)
    }

    private fun readText(text: String): Reading {
        val parsed = text.takeIf { it.isNotBlank() }?.let(read)?.takeIf { it.isFinite() }
        return when {
            parsed == null -> Reading.Invalid
            bounds.excludes(parsed) -> Reading.OutOfRange(NumberInputMath.roundTo(parsed, bounds.places))
            else -> Reading.Number(NumberInputMath.roundTo(parsed, bounds.places))
        }
    }

    private fun set(next: Double?) {
        val rounded = next?.let { NumberInputMath.roundTo(it, bounds.places) }
        val text = rounded?.let(show).orEmpty()
        store(NumberDraft.shown(text, rounded))
        if (rounded != value) onValueChange(rounded)
    }

    fun commit() {
        val now = draft()
        if (now.text.isBlank()) {
            set(null)
            return
        }
        when (val reading = readText(now.text)) {
            Reading.Invalid -> store(now.copy(problem = messages.invalid))
            // The number was already sent while typing; leaving the field explains the range.
            is Reading.OutOfRange -> store(now.copy(problem = messages.range))
            is Reading.Number -> set(reading.value)
        }
    }

    fun stepBy(direction: Int) {
        val current = (readText(draft().text) as? Reading.Number)?.value ?: value
        val base = bounds.minimum ?: 0.0
        val next = current?.let { NumberInputMath.stepValue(it, bounds.step, direction, base) } ?: base
        set(NumberInputMath.clamp(next, bounds.minimum, bounds.maximum))
    }

    /** What the text of the field reads as. */
    private sealed interface Reading {
        data class Number(val value: Double?) : Reading
        data object Invalid : Reading
        data class OutOfRange(val value: Double) : Reading
    }
}

/** The − and + buttons of a number field and the matching TalkBack actions. */
private class NumberStepper(
    val shown: Boolean,
    val canDecrease: Boolean,
    val canIncrease: Boolean,
    val decreaseName: String,
    val increaseName: String,
) {
    fun actions(editor: NumberEditor): List<CustomAccessibilityAction> = if (!shown) {
        emptyList()
    } else {
        listOfNotNull(
            CustomAccessibilityAction(decreaseName) { editor.stepBy(-1); true }.takeIf { canDecrease },
            CustomAccessibilityAction(increaseName) { editor.stepBy(1); true }.takeIf { canIncrease },
        )
    }

    /** The − and + buttons at the end of the box, as the web's number input has them. */
    fun buttons(editor: NumberEditor): (@Composable () -> Unit)? = if (!shown) {
        null
    } else {
        {
            MonarchFieldStepper(
                decreaseName = decreaseName,
                increaseName = increaseName,
                canDecrease = canDecrease,
                canIncrease = canIncrease,
                tint = { MonarchNumberInputTokens.stepper(it, MonarchTheme.colors) },
                onDecrease = { editor.stepBy(-1) },
                onIncrease = { editor.stepBy(1) },
            )
        }
    }
}

@Composable
private fun rangeMessage(minimum: String?, maximum: String?, labels: MonarchNumberInputLabels): String? {
    labels.range?.let { return it(minimum, maximum) }
    return when {
        minimum != null && maximum != null -> stringResource(R.string.monarch_numberinput_range, minimum, maximum)
        minimum != null -> stringResource(R.string.monarch_numberinput_range_minimum, minimum)
        maximum != null -> stringResource(R.string.monarch_numberinput_range_maximum, maximum)
        else -> null
    }
}
