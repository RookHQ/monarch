@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSession
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionActivity
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionAnswering
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionKind
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionParticipant
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionPresence
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionRequest
import dev.monarchui.components.intelligence.aisharedsession.MonarchAiSharedSessionRole
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Owner = MonarchAiSharedSessionParticipant(
    id = "maya",
    name = "Maya",
    role = MonarchAiSharedSessionRole.Owner,
    you = true,
)

private val Others = listOf(
    MonarchAiSharedSessionParticipant(id = "sam", name = "Sam Lee", activity = MonarchAiSharedSessionActivity.Typing),
    MonarchAiSharedSessionParticipant(
        id = "ana",
        name = "Ana Silva",
        presence = MonarchAiSharedSessionPresence.Idle,
        activity = MonarchAiSharedSessionActivity.Reviewing,
    ),
    MonarchAiSharedSessionParticipant(id = "ra", name = "Research agent", kind = MonarchAiSharedSessionKind.Agent),
)

private val StartQueue = listOf(
    MonarchAiSharedSessionRequest("q1", "sam", "Summarize the Q3 notes"),
    MonarchAiSharedSessionRequest("q2", "maya", "Draft the reply to the customer"),
    MonarchAiSharedSessionRequest("q3", "ana", "Check the numbers in the table"),
)

/** The catalogue page of AiSharedSession. */
object MonarchAiSharedSessionSection : CataloguePage {
    override val title = "AiSharedSession"

    @Composable
    override fun Content() {
        var order by rememberSaveable { mutableStateOf(StartQueue.map { it.id }) }
        var following by rememberSaveable { mutableStateOf(listOf<String>()) }
        var requested by rememberSaveable { mutableStateOf(false) }
        val queue = remember(order) { order.mapNotNull { id -> StartQueue.firstOrNull { it.id == id } } }
        CatalogueCaption("As the owner: reorder, remove and withdraw requests")
        MonarchAiSharedSession(
            participants = listOf(Owner) + Others,
            answering = MonarchAiSharedSessionAnswering("ra", "sam", "Q3 notes"),
            queue = queue,
            following = following.toSet(),
            onFollowingChange = { following = it.toList() },
            onWithdraw = { id -> order = order - id },
            onRemove = { id -> order = order - id },
            onReorder = { ids, _ -> order = ids },
            onInvite = {},
        )
        CatalogueCaption("As a viewer")
        MonarchAiSharedSession(
            participants = listOf(
                MonarchAiSharedSessionParticipant(
                    id = "vic",
                    name = "Vic",
                    role = MonarchAiSharedSessionRole.Viewer,
                    you = true,
                ),
            ) + Others,
            accessRequested = requested,
            onRequestAccess = { requested = true },
            announcesChanges = false,
        )
    }
}
