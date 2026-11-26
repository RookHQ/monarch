@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.intelligence.aiactionlog.MonarchAiActionLog
import com.rookhq.monarch.components.intelligence.aiactionlog.MonarchAiActionLogEntry
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of AiActionLog. */
object MonarchAiActionLogSection : CataloguePage {
    override val title = "AiActionLog"

    private const val MINUTE = 60_000L
    private const val UNDO_MILLIS = 800L
    private val research = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Analyst")
    private val triage = MonarchAiAgentRef(name = "Triage agent", id = "triage", role = "Support")

    private fun entries(now: Long) = listOf(
        MonarchAiActionLogEntry(
            id = "1",
            date = now - 4 * MINUTE,
            agent = research,
            action = "added a summary to",
            target = "Quarterly report",
            url = "https://example.com/report",
            undoable = true,
        ),
        MonarchAiActionLogEntry(
            id = "2",
            date = now - 95 * MINUTE,
            agent = triage,
            action = "closed",
            target = "Ticket 4821",
            undoable = true,
        ),
        MonarchAiActionLogEntry(
            id = "3",
            date = now - 26 * 60 * MINUTE,
            agent = triage,
            action = "assigned",
            target = "Ticket 4790",
            undone = true,
        ),
        MonarchAiActionLogEntry(
            id = "4",
            date = now - 3 * 24 * 60 * MINUTE,
            agent = research,
            action = "renamed",
            target = "Board deck",
        ),
    )

    @Composable
    override fun Content() {
        var failing by rememberSaveable { mutableStateOf(false) }
        val now = rememberSaveable { System.currentTimeMillis() }
        MonarchSwitchRow(checked = failing, onCheckedChange = { failing = it }, label = "Make undo fail")
        CatalogueCaption("Two entries at first; Show more adds two")
        MonarchAiActionLog(
            entries = entries(now),
            now = now,
            pageSize = 2,
            onUndo = {
                delay(UNDO_MILLIS)
                check(!failing) { "The server did not answer." }
            },
        )
        CatalogueCaption("Empty")
        MonarchAiActionLog(entries = emptyList(), now = now)
    }
}
