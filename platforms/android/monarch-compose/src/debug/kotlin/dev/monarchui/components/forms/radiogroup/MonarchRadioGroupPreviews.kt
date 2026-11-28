package dev.monarchui.components.forms.radiogroup

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.radio.MonarchRadio
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRadioGroupPreview() {
    PreviewFrame {
        MonarchRadioGroup(label = "Delivery") {
            MonarchRadio(selected = true, onClick = {}, label = "Standard")
            MonarchRadio(selected = false, onClick = {}, label = "Express")
        }
    }
}
