package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentStage
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentTimeline
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentTimelineDensity
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentUpdate
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of IncidentTimeline. */
object MonarchIncidentTimelineSection : CataloguePage {
    override val title = "IncidentTimeline"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val START = 1_772_636_400_000L
    private const val QUARTER = 900_000L

    private val story = listOf(
        MonarchIncidentUpdate(
            id = "1",
            stage = MonarchIncidentStage.Investigating,
            body = "Checkout is failing for some users.",
            time = START,
            automated = true,
        ),
        MonarchIncidentUpdate(
            id = "2",
            stage = MonarchIncidentStage.Identified,
            body = "A database migration locked the orders table.",
            time = START + QUARTER,
            author = "Ada",
        ),
        MonarchIncidentUpdate(
            id = "3",
            stage = MonarchIncidentStage.Monitoring,
            body = "The migration was rolled back. We are watching error rates.",
            time = START + 2 * QUARTER,
        ),
        MonarchIncidentUpdate(
            id = "4",
            stage = MonarchIncidentStage.Resolved,
            body = "Payments are back to normal.",
            time = START + 4 * QUARTER,
        ),
    )

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableIntStateOf(2) }
        var compact by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A live incident")
        MonarchSwitchRow(checked = compact, onCheckedChange = { compact = it }, label = "Compact")
        MonarchButton(
            text = "Post the next update",
            onClick = { shown = if (shown < story.size) shown + 1 else 1 },
        )
        MonarchIncidentTimeline(
            updates = story.take(shown),
            density = if (compact) MonarchIncidentTimelineDensity.Compact else MonarchIncidentTimelineDensity.Regular,
            live = true,
        )
        CatalogueCaption("A product stage")
        MonarchIncidentTimeline(
            updates = listOf(
                MonarchIncidentUpdate(
                    id = "m",
                    stage = "maintenance",
                    body = "The database moves to new hardware.",
                    time = START,
                ),
            ),
            stages = mapOf("maintenance" to MonarchStatusDefinition("Scheduled maintenance", MonarchTone.Info)),
        )
        CatalogueCaption("Empty")
        MonarchIncidentTimeline(updates = emptyList())
    }
}
