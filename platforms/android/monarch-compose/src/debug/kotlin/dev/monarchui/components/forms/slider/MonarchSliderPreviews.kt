package dev.monarchui.components.forms.slider

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSliderPreview() {
    PreviewFrame {
        MonarchSlider(
            value = 40f,
            onValueChange = {},
            valueRange = 0f..100f,
            label = "Volume",
            valueLabel = { "${it.toInt()}%" },
        )
        MonarchSlider(value = 0.6f, onValueChange = {}, enabled = false)
    }
}
