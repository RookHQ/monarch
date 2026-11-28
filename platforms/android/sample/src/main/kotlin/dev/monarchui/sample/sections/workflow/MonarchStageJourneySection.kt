package dev.monarchui.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.workflow.stagejourney.MonarchStageHistory
import dev.monarchui.components.workflow.stagejourney.MonarchStageHistoryEntry
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourney
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyStage
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTerminal
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTransition
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyTransitionKind
import dev.monarchui.components.workflow.stagejourney.MonarchStageJourneyVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

private const val MoveDelayMillis = 600L

private val Stages = listOf(
    MonarchStageJourneyStage("filed", "Filed", group = "Application", description = "The application is received."),
    MonarchStageJourneyStage("exam", "Examination", group = "Application", description = "An examiner reviews it."),
    MonarchStageJourneyStage("granted", "Granted", group = "Outcome", description = "The right is registered."),
)

/** The catalogue page of StageJourney. */
object MonarchStageJourneySection : CataloguePage {
    override val title = "StageJourney"

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableStateOf("filed") }
        var withdrawn by rememberSaveable { mutableStateOf(false) }
        var fail by rememberSaveable { mutableStateOf(false) }
        val history = remember {
            mutableStateListOf(MonarchStageHistoryEntry("start", to = "Filed", at = System.currentTimeMillis()))
        }
        val index = Stages.indexOfFirst { it.id == current }
        val next = Stages.getOrNull(index + 1)
        val previous = Stages.getOrNull(index - 1)
        val transitions = listOfNotNull(
            next?.let { MonarchStageJourneyTransition(it.id, it.label) },
            MonarchStageJourneyTransition("withdrawn", "Withdrawn", MonarchStageJourneyTransitionKind.Exit)
                .takeIf { next != null },
            previous?.let { MonarchStageJourneyTransition(it.id, it.label, MonarchStageJourneyTransitionKind.Back) },
        ).takeUnless { withdrawn }.orEmpty()
        MonarchSwitchRow(checked = fail, onCheckedChange = { fail = it }, label = "Make moves fail")
        CatalogueCaption("Full")
        MonarchStageJourney(
            stages = Stages,
            currentId = current,
            terminal = when {
                withdrawn -> MonarchStageJourneyTerminal.Closed
                next == null -> MonarchStageJourneyTerminal.Success
                else -> null
            },
            enteredAt = history.first().at,
            transitions = transitions,
            onTransition = { move ->
                delay(MoveDelayMillis)
                check(!fail) { "The registry did not answer. Try again." }
                val from = Stages.first { it.id == current }.label
                if (move.id == "withdrawn") withdrawn = true else current = move.id
                history.add(
                    0,
                    MonarchStageHistoryEntry(
                        id = "move-${history.size}",
                        to = move.label,
                        at = System.currentTimeMillis(),
                        from = from,
                        actor = "You",
                        backward = move.kind == MonarchStageJourneyTransitionKind.Back,
                    ),
                )
            },
        )
        CatalogueCaption("Strip")
        MonarchStageJourney(stages = Stages, currentId = current, variant = MonarchStageJourneyVariant.Strip)
        CatalogueCaption("History")
        MonarchStageHistory(entries = history)
    }
}
