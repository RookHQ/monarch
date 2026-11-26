@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSession
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionActivity
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionAnswering
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionKind
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionParticipant
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionPresence
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionRequest
import com.rookhq.monarch.components.intelligence.aisharedsession.MonarchAiSharedSessionRole
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
