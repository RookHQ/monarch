package dev.monarchui.components.files.calloutblock

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchCalloutBlockPreview() {
    PreviewFrame {
        MonarchTone.entries.forEach { tone ->
            MonarchCalloutBlock(
                text = "A paragraph inside a ${tone.name.lowercase()} callout block.",
                tone = tone,
                icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            )
        }
        MonarchCalloutBlock(tone = MonarchTone.Info) {
            Text("Tip", style = MonarchTheme.typography.bodyStrong)
            Text("Callouts can hold more than one paragraph.")
        }
    }
}
