package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimeline
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActorKind
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineMarker
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ActivityTimeline. */
object MonarchActivityTimelineSection : CataloguePage {
    override val title = "ActivityTimeline"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_772_636_400_000L
    private const val HOUR = 3_600_000L
    private const val YESTERDAY = 26
    private const val TWO_DAYS = 50
    private const val HEIGHT_STEPS = 4

    private val ana = MonarchActivityTimelineActor("Ana Ruiz")
    private val ben = MonarchActivityTimelineActor("Ben Okafor")
    private val rule = MonarchActivityTimelineActor(
        name = "Follow-up rule",
        kind = MonarchActivityTimelineActorKind.Automation,
    )
    private val events = listOf(
        MonarchActivityTimelineEvent(
            id = "1",
            verb = "moved",
            time = NOW - HOUR,
            actor = ana,
            target = "Acme renewal",
            tone = MonarchTone.Info,
            tags = listOf("Negotiation"),
            details = "Stage: Proposal → Negotiation",
        ),
        MonarchActivityTimelineEvent(
            id = "2",
            verb = "sent a reminder to",
            time = NOW - 2 * HOUR,
            actor = rule,
            target = "Ben Okafor",
        ),
        MonarchActivityTimelineEvent(
            id = "3",
            verb = "commented on",
            time = NOW - YESTERDAY * HOUR,
            actor = ben,
            target = "Acme renewal",
            body = "Sent the revised draft to their legal team.",
        ),
        MonarchActivityTimelineEvent(
            id = "4",
            verb = "Ticket created from email",
            time = NOW - TWO_DAYS * HOUR,
            tone = MonarchTone.Success,
        ),
    )

    @Composable
    override fun Content() {
        var marker by rememberSaveable { mutableIntStateOf(0) }
        var density by rememberSaveable { mutableIntStateOf(0) }
        var loadingMore by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Marker")
        Choice(MonarchActivityTimelineMarker.entries.map { it.name }, marker) { marker = it }
        CatalogueCaption("Density")
        Choice(MonarchActivityTimelineDensity.entries.map { it.name }, density) { density = it }
        MonarchActivityTimeline(
            events = events,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            marker = MonarchActivityTimelineMarker.entries[marker],
            density = MonarchActivityTimelineDensity.entries[density],
            hasMore = true,
            loadingMore = loadingMore,
            now = NOW,
            onLoadMore = { loadingMore = true },
        )
        CatalogueCaption("Loading, failed and empty")
        MonarchActivityTimeline(events = emptyList(), loading = true)
        MonarchActivityTimeline(events = emptyList(), failed = true, onRetry = {})
        MonarchActivityTimeline(events = emptyList())
    }

    @Composable
    private fun Choice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        MonarchSegmentedControl {
            options.forEachIndexed { index, option ->
                Segment(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    label = { Text(option) },
                )
            }
        }
    }
}
