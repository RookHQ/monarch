package dev.monarchui.components.intelligence.aimessagebranch

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiMessageBranchPreview() {
    PreviewFrame {
        MonarchAiMessageBranch(1, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") })
        MonarchAiMessageBranch(0, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") })
        MonarchAiMessageBranch(1, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") }, enabled = false)
    }
}
