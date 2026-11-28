@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRoster
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterMember
import dev.monarchui.components.intelligence.aimultiagentroster.MonarchAiMultiAgentRosterVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val ResearchStates = MonarchAiAgentState.entries

/** The agents of the page after [step] presses of "Change a state". */
private fun membersAt(step: Int): List<MonarchAiMultiAgentRosterMember> = listOf(
    MonarchAiMultiAgentRosterMember(
        MonarchAiAgentRef(name = "Coordinator", id = "coordinator", role = "Plans the work"),
        state = MonarchAiAgentState.Working,
        task = "Splitting the request",
    ),
    MonarchAiMultiAgentRosterMember(
        MonarchAiAgentRef(name = "Research agent", id = "research"),
        state = ResearchStates[step % ResearchStates.size],
        task = "Comparing three sources",
    ),
    MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(name = "Writing agent", id = "writer")),
    MonarchAiMultiAgentRosterMember(
        MonarchAiAgentRef(name = "Review agent", id = "review"),
        state = MonarchAiAgentState.Error,
        task = "Could not open the draft",
    ),
)

/** The catalogue page of AiMultiAgentRoster. */
object MonarchAiMultiAgentRosterSection : CataloguePage {
    override val title = "AiMultiAgentRoster"

    @Composable
    override fun Content() {
        var step by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A list. Changes are announced.")
        MonarchAiMultiAgentRoster(members = membersAt(step))
        MonarchButton(text = "Change a state", onClick = { step++ }, size = MonarchButtonSize.Sm)
        CatalogueCaption("A stack for a header")
        MonarchAiMultiAgentRoster(
            members = membersAt(step),
            variant = MonarchAiMultiAgentRosterVariant.Stack,
            maxVisible = 2,
            announcesChanges = false,
        )
        CatalogueCaption("No agents")
        MonarchAiMultiAgentRoster(members = emptyList())
    }
}
