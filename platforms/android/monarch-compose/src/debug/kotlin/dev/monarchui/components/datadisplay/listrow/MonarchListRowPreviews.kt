package dev.monarchui.components.datadisplay.listrow

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchListRowPreview() {
    PreviewFrame {
        MonarchListRow(
            headlineContent = { Text("Invoice 1042") },
            supportingContent = { Text("Paid on 3 March") },
            leadingContent = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            trailingContent = { Text("$420.00") },
            onClick = {},
            divider = true,
        )
        MonarchListRow(headlineContent = { Text("Selected row") }, onClick = {}, selected = true)
        MonarchListRow(headlineContent = { Text("Disabled row") }, onClick = {}, enabled = false)
    }
}
