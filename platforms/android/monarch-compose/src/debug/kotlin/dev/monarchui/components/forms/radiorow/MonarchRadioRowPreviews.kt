package dev.monarchui.components.forms.radiorow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRadioRowPreview() {
    PreviewFrame {
        Column(modifier = Modifier.selectableGroup()) {
            MonarchRadioRow(selected = true, onClick = {}, label = "Daily")
            MonarchRadioRow(selected = false, onClick = {}, label = "Weekly")
            MonarchRadioRow(selected = false, onClick = {}, label = "Never", enabled = false)
        }
    }
}
