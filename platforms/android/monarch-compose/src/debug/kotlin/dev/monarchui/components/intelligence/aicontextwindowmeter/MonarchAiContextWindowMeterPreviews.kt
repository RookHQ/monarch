package dev.monarchui.components.intelligence.aicontextwindowmeter

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiContextWindowMeterPreview() {
    PreviewFrame {
        MonarchAiContextWindowMeter(used = 45_000, limit = 128_000, onSummarize = {})
        MonarchAiContextWindowMeter(
            used = 110_000,
            limit = 128_000,
            breakdown = listOf(
                MonarchAiContextWindowPart("Instructions", 4_000),
                MonarchAiContextWindowPart("Files", 36_000),
                MonarchAiContextWindowPart("Messages", 62_000),
            ),
            approximate = true,
            onSummarize = {},
        )
        MonarchAiContextWindowMeter(used = 130_000, limit = 128_000)
        MonarchAiContextWindowMeter(used = 90_000, limit = 128_000, compact = true)
    }
}
