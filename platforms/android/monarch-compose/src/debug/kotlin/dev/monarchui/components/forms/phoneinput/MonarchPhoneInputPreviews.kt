package dev.monarchui.components.forms.phoneinput

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchPhoneInputPreview() {
    PreviewFrame {
        MonarchPhoneInput(
            value = "+442079460958",
            onValueChange = {},
            label = "Phone",
            helperText = "We call only about your order.",
            locale = Locale.US,
        )
        MonarchPhoneInput(
            value = null,
            onValueChange = {},
            label = "Mobile",
            errorText = "Enter a mobile number.",
            locale = Locale.US,
        )
        MonarchPhoneInput(
            value = "+15550100123",
            onValueChange = {},
            label = "Office phone",
            enabled = false,
            locale = Locale.US,
        )
    }
}
