package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimeline
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineActorKind
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import com.rookhq.monarch.components.collaboration.activitytimeline.MonarchActivityTimelineLabels
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidgetState
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ActivityWidget: a MonarchWidget around a compact MonarchActivityTimeline. */
object ActivityWidgetSection : CataloguePage {
    override val title = "ActivityWidget"

    private val states = listOf("Ready", "Loading", "Empty", "Failed")
    private const val LIMIT = 6
    private const val HEIGHT_STEPS = 3
    private const val MINUTE = 60_000L

    private fun events(now: Long): List<MonarchActivityTimelineEvent> = listOf(
        MonarchActivityTimelineEvent(
            id = "1",
            verb = "closed",
            time = now - 20 * MINUTE,
            actor = MonarchActivityTimelineActor("Ada Moss"),
            target = "Request 1042",
        ),
        MonarchActivityTimelineEvent(
            id = "2",
            verb = "flagged",
            time = now - 115 * MINUTE,
            actor = MonarchActivityTimelineActor("Renewal rule", kind = MonarchActivityTimelineActorKind.Automation),
            target = "Contract 88",
            tone = MonarchTone.Warning,
        ),
        MonarchActivityTimelineEvent(
            id = "3",
            verb = "commented on",
            time = now - 225 * MINUTE,
            actor = MonarchActivityTimelineActor("Bea Lund"),
            target = "Request 1039",
        ),
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var retrying by rememberSaveable { mutableStateOf(false) }
        var viewAll by rememberSaveable { mutableIntStateOf(0) }
        val now = rememberSaveable { System.currentTimeMillis() }
        CatalogueCaption("Choose a state")
        MonarchSegmentedControl {
            states.forEachIndexed { index, name ->
                Segment(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        retrying = false
                    },
                    label = { Text(name) },
                )
            }
        }
        MonarchWidget(
            title = "Recent activity",
            state = if (selected == 3) MonarchWidgetState.Failed() else MonarchWidgetState.Ready,
            retrying = retrying,
            onRetry = { retrying = true },
            onViewAll = { viewAll += 1 },
        ) {
            MonarchActivityTimeline(
                events = if (selected == 0) events(now).take(LIMIT) else emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
                groupByDay = false,
                density = MonarchActivityTimelineDensity.Compact,
                loading = selected == 1,
                now = now,
                labels = MonarchActivityTimelineLabels(list = "Recent activity", empty = "No activity yet"),
            )
        }
        MonarchText(text = "View all pressed $viewAll times", color = MonarchTheme.colors.textMuted)
    }
}
