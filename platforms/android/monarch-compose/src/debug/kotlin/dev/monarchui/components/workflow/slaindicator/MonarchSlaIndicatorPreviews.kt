package dev.monarchui.components.workflow.slaindicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewLater = PreviewNow + (2 * 60 + 15) * 60_000L
private const val PreviewSoon = PreviewNow + 40 * 60_000L
private const val PreviewEarlier = PreviewNow - 40 * 60_000L

@OptIn(ExperimentalLayoutApi::class)
@MonarchPreviews
@Composable
internal fun MonarchSlaIndicatorPreview() {
    PreviewFrame {
        MonarchSlaIndicatorVariant.entries.forEach { variant ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            ) {
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.OnTrack,
                    variant = variant,
                    dueAt = PreviewLater,
                    now = PreviewNow,
                )
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.AtRisk,
                    variant = variant,
                    dueAt = PreviewSoon,
                    now = PreviewNow,
                )
                MonarchSlaIndicator(
                    MonarchSlaIndicatorState.Breached,
                    variant = variant,
                    dueAt = PreviewEarlier,
                    now = PreviewNow,
                )
                MonarchSlaIndicator(MonarchSlaIndicatorState.Paused, variant = variant)
                MonarchSlaIndicator(MonarchSlaIndicatorState.Met, variant = variant)
                MonarchSlaIndicator(MonarchSlaIndicatorState.NotApplicable, variant = variant, showNotApplicable = true)
            }
        }
    }
}
