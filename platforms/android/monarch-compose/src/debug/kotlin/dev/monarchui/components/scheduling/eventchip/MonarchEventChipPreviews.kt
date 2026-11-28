package dev.monarchui.components.scheduling.eventchip

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens

@MonarchPreviews
@Composable
internal fun MonarchEventChipPreview() {
    PreviewFrame {
        MonarchEventChipStatus.entries.forEach { status ->
            MonarchEventChip(title = "Stand-up", time = "09:30", status = status, onClick = {})
        }
        MonarchEventChipStatus.entries.forEach { status ->
            MonarchEventChip(
                title = "Design review",
                variant = MonarchEventChipVariant.Bar,
                status = status,
                time = "14:00 – 15:00",
                detail = "Room 4",
            )
        }
        MonarchEventChip(
            title = "Workshop",
            modifier = Modifier.height(MonarchTokens.Space.space24.dp),
            variant = MonarchEventChipVariant.Block,
            time = "10:00 – 12:00",
            detail = "Lab",
        )
    }
}
