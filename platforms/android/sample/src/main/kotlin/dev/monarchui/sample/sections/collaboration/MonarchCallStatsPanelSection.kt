package dev.monarchui.sample.sections.collaboration

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.collaboration.callstatspanel.MonarchCallStatsPanel
import dev.monarchui.components.collaboration.callstatspanel.MonarchCallStatsPanelPeer
import dev.monarchui.components.collaboration.callstatspanel.MonarchCallStatsPanelQuality
import dev.monarchui.components.collaboration.callstatspanel.MonarchCallStatsPanelUsage
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of CallStatsPanel. */
object MonarchCallStatsPanelSection : CataloguePage {
    override val title = "CallStatsPanel"

    private val usage = MonarchCallStatsPanelUsage(
        sentBytes = 6_200_000,
        receivedBytes = 9_400_000,
        upBitsPerSecond = 420_000.0,
        downBitsPerSecond = 1_250_000.0,
    )
    private val peers = listOf(
        MonarchCallStatsPanelPeer("a", "Robin Park", 38.0, 0.2, 0.1, MonarchCallStatsPanelQuality.Good),
        MonarchCallStatsPanelPeer("b", "Alex Chen", 186.0, 3.4, 1.2, MonarchCallStatsPanelQuality.Fair),
        MonarchCallStatsPanelPeer("c", "Sam Lee", 640.0, 12.0, 8.0, MonarchCallStatsPanelQuality.Poor),
        MonarchCallStatsPanelPeer("d", "Jordan Diaz"),
    )

    private val smallUsage = MonarchCallStatsPanelUsage(sentBytes = 120_000, receivedBytes = 80_000)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var showStats by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("In the page")
        MonarchCallStatsPanel(
            usage = usage,
            peers = peers,
            footnote = "Example footnote: say here how your product routes and protects call media.",
        )
        CatalogueCaption("Nobody else, low data")
        MonarchCallStatsPanel(usage = smallUsage, peers = emptyList(), lowData = true)
        CatalogueCaption("In a bottom sheet from the call controls")
        MonarchButton(text = "Show connection", onClick = { showStats = true })
        if (showStats) {
            MonarchBottomSheet(onDismissRequest = { showStats = false }) {
                MonarchCallStatsPanel(usage = usage, peers = peers)
            }
        }
    }
}
