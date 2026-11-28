package dev.monarchui.components.forms.datefield

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

/** 2026-11-24 at midnight UTC, as the platform picker gives it. */
private const val Nov24of2026 = 1_795_478_400_000L

@MonarchPreviews
@Composable
internal fun MonarchDateFieldPreview() {
    PreviewFrame {
        MonarchDateField(value = Nov24of2026, onValueChange = {}, label = "Due date", clearable = true)
        MonarchDateField(value = null, onValueChange = {}, label = "Start date", required = true)
        MonarchDateField(value = null, onValueChange = {}, label = "End date", errorText = "Choose an end date.")
        MonarchDateField(value = Nov24of2026, onValueChange = {}, label = "Signed on", enabled = false)
    }
}
