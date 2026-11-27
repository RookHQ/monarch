package com.rookhq.monarch.components.datadisplay.leaderboard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.charts.barlist.MonarchBarListSort
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewEntries = listOf(
    MonarchLeaderboardEntry(name = "Ana Lima", value = 42.0, change = 0.12, current = true),
    MonarchLeaderboardEntry(name = "Ben Okafor", value = 42.0, secondary = "Tier 2", change = -0.05),
    MonarchLeaderboardEntry(name = "Chloé Martin", value = 31.0, badge = "3 breached", badgeTone = MonarchTone.Danger),
    MonarchLeaderboardEntry(name = "Dev Patel", value = 12.0),
)

@MonarchPreviews
@Composable
internal fun MonarchLeaderboardPreview() {
    PreviewFrame {
        MonarchLeaderboard(
            entries = PreviewEntries,
            sort = MonarchBarListSort.Descending,
            valueLabel = "tickets solved",
            changeComparison = "vs last week",
            highlightTop = true,
            total = 12,
        )
        MonarchLeaderboard(entries = emptyList())
        MonarchLeaderboard(entries = emptyList(), errorMessage = "The ranking could not be loaded.", onRetry = {})
    }
}
