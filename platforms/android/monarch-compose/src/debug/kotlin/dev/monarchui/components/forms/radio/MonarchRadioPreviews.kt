package dev.monarchui.components.forms.radio

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRadioPreview() {
    PreviewFrame {
        MonarchRadio(selected = true, onClick = {}, label = "Daily")
        MonarchRadio(selected = false, onClick = {}, label = "Weekly", description = "On Mondays.")
        MonarchRadio(selected = false, onClick = {}, label = "Never", enabled = false)
    }
}
