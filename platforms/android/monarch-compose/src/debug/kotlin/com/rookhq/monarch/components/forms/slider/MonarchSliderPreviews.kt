package com.rookhq.monarch.components.forms.slider

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
