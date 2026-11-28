package dev.monarchui.components.intelligence.aistreamingtext

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiStreamingTextPreview() {
    PreviewFrame {
        MonarchAiStreamingText(
            text = "## Summary\n\nThe project has **three open tasks**. Review the *migration",
            streaming = true,
            announces = false,
        )
        MonarchAiStreamingText(
            text = "1. Review the migration plan.\n2. Update the changelog.\n\n```bash\nnpm run release\n```",
            streaming = false,
        )
    }
}
