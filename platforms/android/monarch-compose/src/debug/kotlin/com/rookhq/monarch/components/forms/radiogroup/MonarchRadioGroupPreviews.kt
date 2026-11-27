package com.rookhq.monarch.components.forms.radiogroup

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.radio.MonarchRadio
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
