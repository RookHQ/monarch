package dev.monarchui.components.files.filecard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchFileCardPreview() {
    PreviewFrame {
        Row {
            MonarchFileCard(
                name = "Q3 report.pdf",
                modifier = Modifier.width(160.dp),
                sizeBytes = 2_400_000,
                meta = "Edited yesterday",
                badges = listOf("Shared"),
                selected = true,
                onSelectedChange = {},
                onOpen = {},
            ) {
                MonarchIconButton(label = "Actions for Q3 report.pdf", onClick = {}) {
                    Icon(PreviewIcon, contentDescription = null)
                }
            }
            MonarchFileCard(
                name = "archive.zip",
                modifier = Modifier.width(160.dp),
                sizeBytes = 9_800_000,
                enabled = false,
            )
        }
        MonarchFileCard(name = "notes.txt", sizeBytes = 900, layout = MonarchFileCardLayout.Row, onOpen = {})
        MonarchFileCard(
            name = "budget-2026-final-final.xlsx",
            meta = "Shared by Sam",
            layout = MonarchFileCardLayout.Row,
            onSelectedChange = {},
            onOpen = {},
        )
    }
}
