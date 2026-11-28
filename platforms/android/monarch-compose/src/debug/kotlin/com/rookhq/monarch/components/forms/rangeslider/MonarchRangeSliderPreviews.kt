package com.rookhq.monarch.components.forms.rangeslider

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
