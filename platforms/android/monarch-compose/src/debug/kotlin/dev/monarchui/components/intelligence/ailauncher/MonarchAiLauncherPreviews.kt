package dev.monarchui.components.intelligence.ailauncher

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiLauncherPreview() {
    PreviewFrame {
        MonarchAiLauncher(open = false, onOpenChange = {}, icon = { Text("AI") })
        MonarchAiLauncher(
            open = false,
            onOpenChange = {},
            icon = { Text("AI") },
            badge = MonarchAiLauncherBadge.Count(3),
        )
        MonarchAiLauncher(
            open = true,
            onOpenChange = {},
            icon = { Text("AI") },
            variant = MonarchAiLauncherVariant.Subtle,
            badge = MonarchAiLauncherBadge.Unread,
        )
    }
}
