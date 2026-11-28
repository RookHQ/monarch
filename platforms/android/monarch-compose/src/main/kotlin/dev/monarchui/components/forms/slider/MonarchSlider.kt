package dev.monarchui.components.forms.slider

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.theme.MonarchTheme

/**
 * A slider for one number in a range: a 6dp track, the accent range, an 18dp thumb with a 2dp accent border, a
 * 48dp touch target, and an optional label with the value above it.
 *
 * It keeps what makes the Material 3 `Slider` right on a phone (the drag and fling physics, the keyboard arrows,
 * the TalkBack "adjustable" actions with `setProgress`, the 48dp height) and replaces its look: Monarch colours,
 * Monarch track and thumb sizes, the Monarch focus ring on the thumb for keyboard and D-pad focus.
 *
 * Migrating from Material: `Slider(value, onValueChange, modifier, enabled, valueRange, steps,
 * onValueChangeFinished, colors, interactionSource)` becomes `MonarchSlider(value, onValueChange, modifier,
 * enabled, valueRange, steps, onValueChangeFinished, interactionSource = s)`. Remove `colors`, `thumb` and `track`.
 *
 * @param value the current value.
 * @param onValueChange called with the new value while the user drags or presses an arrow key.
 * @param modifier the modifier of the slider (or of the whole group when [label] or [valueLabel] is given).
 * @param enabled whether the user can change the value.
 * @param valueRange the range of values.
 * @param steps the number of steps between the ends; 0 is continuous.
 * @param onValueChangeFinished called when the user lifts the thumb.
 * @param invalid draws the range and thumb in the danger colour.
 * @param label the visible label above the slider, and its TalkBack name.
 * @param valueLabel turns the value into the text shown at the end of the label row and read by TalkBack as the
 * state, for example `{ "${it.toInt()} %" }`. `null` shows no value.
 * @param interactionSource the source of the press, drag and focus interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    invalid: Boolean = false,
    label: String? = null,
    valueLabel: ((Float) -> String)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shown = valueLabel?.invoke(value)
    val colors = MonarchSliderTokens.colors(enabled, invalid, MonarchTheme.colors)
    SliderGroup(label, shown, enabled, modifier) { sliderModifier ->
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = sliderModifier.semantics {
                if (label != null) contentDescription = label
                if (shown != null) stateDescription = shown
            },
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = source,
            thumb = { SliderThumb(colors, source) },
            track = { state ->
                SliderTrack(colors, 0f, MonarchSliderTokens.fraction(state.value, state.valueRange))
            },
        )
    }
}
