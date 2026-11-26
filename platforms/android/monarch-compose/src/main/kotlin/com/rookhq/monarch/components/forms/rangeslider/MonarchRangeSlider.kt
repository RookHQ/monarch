package com.rookhq.monarch.components.forms.rangeslider

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.components.forms.slider.MonarchSliderTokens
import com.rookhq.monarch.components.forms.slider.SliderGroup
import com.rookhq.monarch.components.forms.slider.SliderThumb
import com.rookhq.monarch.components.forms.slider.SliderTrack
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A slider for a range of two numbers (a price from 20 to 150), in the Monarch look of [MonarchSlider]. The
 * thumbs do not cross; TalkBack names them "Range start" and "Range end".
 *
 * @param value the current range.
 * @param onValueChange called with the new range while the user drags.
 * @param modifier the modifier of the slider (or of the whole group when [label] or [valueLabel] is given).
 * @param enabled whether the user can change the range.
 * @param valueRange the range of values.
 * @param steps the number of steps between the ends; 0 is continuous.
 * @param onValueChangeFinished called when the user lifts a thumb.
 * @param invalid draws the range and thumbs in the danger colour.
 * @param label the visible label above the slider.
 * @param valueLabel formats one end of the range; the label row shows "start – end" and TalkBack reads it as the
 * state. `null` shows no value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    invalid: Boolean = false,
    label: String? = null,
    valueLabel: ((Float) -> String)? = null,
) {
    val startSource = remember { MutableInteractionSource() }
    val endSource = remember { MutableInteractionSource() }
    val shown = valueLabel?.let { "${it(value.start)} – ${it(value.endInclusive)}" }
    val colors = MonarchSliderTokens.colors(enabled, invalid, MonarchTheme.colors)
    SliderGroup(label, shown, enabled, modifier) { sliderModifier ->
        RangeSlider(
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
            startInteractionSource = startSource,
            endInteractionSource = endSource,
            startThumb = { SliderThumb(colors, startSource) },
            endThumb = { SliderThumb(colors, endSource) },
            track = { state ->
                SliderTrack(
                    colors,
                    MonarchSliderTokens.fraction(state.activeRangeStart, state.valueRange),
                    MonarchSliderTokens.fraction(state.activeRangeEnd, state.valueRange),
                )
            },
        )
    }
}
