@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiartifactcanvas

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens

@MonarchPreviews
@Composable
internal fun MonarchAiArtifactCanvasPreview() {
    PreviewFrame {
        MonarchAiArtifactCanvas(
            artifact = MonarchAiCanvasArtifact(
                id = "summary",
                kind = MonarchAiArtifactKind.Document,
                title = "Q3 summary",
                version = 3,
                content = "## Revenue\n\nRevenue grew **12%** over the quarter.\n\n- New accounts: 48\n- Churn: 2%",
            ),
            open = true,
            onDismissRequest = {},
            modifier = Modifier.height(MonarchTokens.Breakpoint.sm.dp),
            placement = MonarchAiArtifactCanvasPlacement.Inline,
            onDownload = {},
            onSave = {},
        )
    }
}
