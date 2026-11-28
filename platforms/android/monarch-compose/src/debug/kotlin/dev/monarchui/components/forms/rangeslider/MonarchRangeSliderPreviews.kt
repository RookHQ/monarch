package dev.monarchui.components.forms.rangeslider

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRangeSliderPreview() {
    PreviewFrame {
        MonarchRangeSlider(
            value = 20f..150f,
            onValueChange = {},
            valueRange = 0f..200f,
            label = "Price",
            valueLabel = { "${it.toInt()} EUR" },
        )
    }
}
