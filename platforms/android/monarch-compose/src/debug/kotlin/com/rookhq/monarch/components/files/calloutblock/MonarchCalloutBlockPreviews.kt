package com.rookhq.monarch.components.files.calloutblock

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchTheme

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
