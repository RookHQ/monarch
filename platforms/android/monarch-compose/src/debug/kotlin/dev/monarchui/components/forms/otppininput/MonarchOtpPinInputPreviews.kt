package dev.monarchui.components.forms.otppininput

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchOtpPinInputPreview() {
    PreviewFrame {
        MonarchOtpPinInput(value = "123", onValueChange = {}, label = "Verification code", groupSize = 3)
        MonarchOtpPinInput(value = "12", onValueChange = {}, label = "PIN", length = 4, masked = true)
        MonarchOtpPinInput(
            value = "AB12",
            onValueChange = {},
            label = "Room code",
            length = 4,
            mode = MonarchOtpPinInputMode.Alphanumeric,
            size = MonarchOtpPinInputSize.Lg,
            errorText = "That code has expired.",
        )
        MonarchOtpPinInput(value = "", onValueChange = {}, label = "Verification code", enabled = false)
    }
}
