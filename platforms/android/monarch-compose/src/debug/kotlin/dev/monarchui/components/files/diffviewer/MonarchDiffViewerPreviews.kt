package dev.monarchui.components.files.diffviewer

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewBefore = "Ship on Friday.\nTell support.\nKeep the old banner."
private const val PreviewAfter = "Ship on Monday.\nTell support.\nUpdate the status page."

@MonarchPreviews
@Composable
internal fun MonarchDiffViewerPreview() {
    PreviewFrame {
        MonarchDiffViewer(
            before = PreviewBefore,
            after = PreviewAfter,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 2),
            beforeLabel = "Version 3",
            afterLabel = "Version 4",
        ) { MonarchButton(text = "Restore", onClick = {}, size = MonarchButtonSize.Sm) }
        MonarchDiffViewer(
            before = PreviewBefore,
            after = PreviewAfter,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 2),
            mode = MonarchDiffViewerMode.Split,
        )
        MonarchDiffViewer(before = "Same", after = "Same")
    }
}
