@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aimemorypanel

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val Day = 86_400_000L
private const val Now = 1_793_750_400_000L

private val Memories = listOf(
    MonarchAiMemoryItem(
        "m1",
        "Prefers summaries under 200 words",
        MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Person, "Ada Lovelace"),
        Now,
    ),
    MonarchAiMemoryItem(
        "m2",
        "Renewal is due in June",
        MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Organization, "Acme"),
        Now - Day,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchAiMemoryPanelPreview() {
    PreviewFrame {
        // The list is lazy, so the preview gives it a height.
        MonarchAiMemoryPanel(
            memories = Memories,
            onEdit = { _, _ -> },
            onForget = {},
            onForgetMany = {},
            modifier = Modifier.height(480.dp),
        )
    }
}
