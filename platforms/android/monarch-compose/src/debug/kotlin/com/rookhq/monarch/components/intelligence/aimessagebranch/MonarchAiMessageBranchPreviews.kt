package com.rookhq.monarch.components.intelligence.aimessagebranch

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiMessageBranchPreview() {
    PreviewFrame {
        MonarchAiMessageBranch(1, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") })
        MonarchAiMessageBranch(0, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") })
        MonarchAiMessageBranch(1, 3, {}, previousIcon = { Text("‹") }, nextIcon = { Text("›") }, enabled = false)
    }
}
