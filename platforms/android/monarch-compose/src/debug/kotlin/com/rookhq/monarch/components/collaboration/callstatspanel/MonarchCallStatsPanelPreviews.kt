package com.rookhq.monarch.components.collaboration.callstatspanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCallStatsPanelPreview() {
    PreviewFrame {
        MonarchCallStatsPanel(
            usage = MonarchCallStatsPanelUsage(6_200_000, 9_400_000, 420_000.0, 1_250_000.0),
            peers = listOf(
                MonarchCallStatsPanelPeer("a", "Robin Park", 38.0, 0.2, 0.1, MonarchCallStatsPanelQuality.Good),
                MonarchCallStatsPanelPeer("b", "Alex Chen", 186.0, 3.4, 1.0, MonarchCallStatsPanelQuality.Fair),
                MonarchCallStatsPanelPeer("c", "Jordan Diaz"),
            ),
            footnote = "Example footnote: say here how your product routes and protects call media.",
        )
        MonarchCallStatsPanel(
            usage = MonarchCallStatsPanelUsage(120_000, 80_000),
            peers = emptyList(),
            lowData = true,
        )
    }
}
