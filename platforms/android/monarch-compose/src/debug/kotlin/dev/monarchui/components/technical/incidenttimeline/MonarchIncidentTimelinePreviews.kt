package dev.monarchui.components.technical.incidenttimeline

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val Start = 1_772_636_400_000L
private const val Minute = 60_000L

@MonarchPreviews
@Composable
internal fun MonarchIncidentTimelinePreview() {
    PreviewFrame {
        MonarchIncidentTimeline(
            updates = listOf(
                MonarchIncidentUpdate(
                    id = "1",
                    stage = MonarchIncidentStage.Investigating,
                    body = "Checkout is failing for some users.",
                    time = Start,
                    automated = true,
                ),
                MonarchIncidentUpdate(
                    id = "2",
                    stage = MonarchIncidentStage.Identified,
                    body = "A database migration locked the orders table.",
                    time = Start + 15 * Minute,
                    author = "Ada",
                ),
                MonarchIncidentUpdate(
                    id = "3",
                    stage = "partial_outage",
                    body = "Payments are slow in one region.",
                    time = Start + 30 * Minute,
                ),
                MonarchIncidentUpdate(
                    id = "4",
                    stage = MonarchIncidentStage.Resolved,
                    body = "Payments are back to normal.",
                    time = Start + 60 * Minute,
                ),
            ),
        )
        MonarchIncidentTimeline(updates = emptyList(), density = MonarchIncidentTimelineDensity.Compact)
    }
}
