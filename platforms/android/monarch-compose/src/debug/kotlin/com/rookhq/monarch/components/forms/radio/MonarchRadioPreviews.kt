package com.rookhq.monarch.components.forms.radio

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRadioPreview() {
    PreviewFrame {
        MonarchRadio(selected = true, onClick = {}, label = "Daily")
        MonarchRadio(selected = false, onClick = {}, label = "Weekly", description = "On Mondays.")
        MonarchRadio(selected = false, onClick = {}, label = "Never", enabled = false)
    }
}
