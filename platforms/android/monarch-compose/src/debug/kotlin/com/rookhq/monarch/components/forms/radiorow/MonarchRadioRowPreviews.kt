package com.rookhq.monarch.components.forms.radiorow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
