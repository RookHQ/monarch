package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aidraftqueue.MonarchAiDraft
import com.rookhq.monarch.components.intelligence.aidraftqueue.MonarchAiDraftQueue
import com.rookhq.monarch.components.intelligence.aidraftqueue.MonarchAiDraftStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiDraftQueue. */
object MonarchAiDraftQueueSection : CataloguePage {
    override val title = "AiDraftQueue"

    private val initial = listOf(
        MonarchAiDraft(
            id = "1",
            title = "Reply to Alex Morgan",
            body = "Thanks for flagging this, Alex. The refund was issued today.",
            context = "Alex: I was charged twice for my order.",
        ),
        MonarchAiDraft("2", "Follow-up to Priya", "Here are the notes from Tuesday's call."),
        MonarchAiDraft("3", "Note for Sam", "Moved the call to Friday.", status = MonarchAiDraftStatus.Approved),
    )

    @Composable
    override fun Content() {
        val drafts = remember { mutableStateListOf<MonarchAiDraft>().apply { addAll(initial) } }
        fun update(id: String, change: (MonarchAiDraft) -> MonarchAiDraft) {
            val index = drafts.indexOfFirst { it.id == id }
            if (index >= 0) drafts[index] = change(drafts[index])
        }
        CatalogueCaption("Review queue")
        MonarchAiDraftQueue(
            drafts = drafts,
            onApprove = { id -> update(id) { it.copy(status = MonarchAiDraftStatus.Approved) } },
            onEdit = { id, body -> update(id) { it.copy(body = body) } },
            onRegenerate = { id -> update(id) { it.copy(status = MonarchAiDraftStatus.Regenerating) } },
            onSkip = { id -> update(id) { it.copy(status = MonarchAiDraftStatus.Skipped) } },
            title = "Replies to review",
            onSend = { id -> update(id) { it.copy(status = MonarchAiDraftStatus.Sent) } },
        )
        MonarchButton(
            text = "Finish writing and reset",
            onClick = {
                drafts.clear()
                drafts.addAll(initial)
            },
        )
    }
}
