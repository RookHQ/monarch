package dev.monarchui.components.datadisplay.periodselector

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

/** 15 November 2026, UTC midnight. */
private const val Today = 1_794_700_800_000L

@MonarchPreviews
@Composable
internal fun MonarchPeriodSelectorPreview() {
    PreviewFrame {
        MonarchPeriodSelector(
            value = MonarchPeriodValue(MonarchPeriodPreset.Last7Days),
            onValueChange = {},
            showRange = true,
            today = Today,
            locale = Locale.US,
        )
        MonarchPeriodSelector(
            value = MonarchPeriodValue(MonarchPeriodPreset.ThisQuarter),
            onValueChange = {},
            variant = MonarchPeriodSelectorVariant.Select,
            today = Today,
            locale = Locale.US,
        )
        MonarchPeriodSelector(
            value = MonarchPeriodValue(MonarchPeriodPreset.Custom, MonarchPeriodRange(Today - 9 * 86_400_000L, Today)),
            onValueChange = {},
            today = Today,
            locale = Locale.US,
        )
    }
}
