package dev.monarchui.components.forms.numberinput

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchNumberInputPreview() {
    PreviewFrame {
        MonarchNumberInput(value = 1.0, onValueChange = {}, label = "Quantity", minimum = 0.0, maximum = 99.0)
        MonarchNumberInput(value = 0.3, onValueChange = {}, label = "Price", step = 0.1, showSteppers = false)
        MonarchNumberInput(value = null, onValueChange = {}, label = "Seats", errorText = "Choose at least one seat.")
        MonarchNumberInput(value = 4.0, onValueChange = {}, label = "Guests", enabled = false)
    }
}
