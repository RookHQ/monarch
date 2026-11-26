package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.intelligence.aiconversationlist.MonarchAiConversationList
import com.rookhq.monarch.components.intelligence.aiconversationlist.MonarchAiConversationSummary
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private const val HourMillis = 3_600_000L
private const val DayMillis = 86_400_000L
private const val HeightSteps = 5
private const val PlanAgeDays = 40
private const val InvoicesAgeDays = 3
private const val DraftAgeDays = 30

/** The catalogue page of AiConversationList. */
object MonarchAiConversationListSection : CataloguePage {
    override val title = "AiConversationList"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        // The summaries are a list of data classes; the page keeps them while it is shown.
        var conversations by remember {
            mutableStateOf(
                listOf(
                    MonarchAiConversationSummary("plan", "Quarterly plan", now - DayMillis * PlanAgeDays, true),
                    MonarchAiConversationSummary("trip", "Trip to Lisbon", now - HourMillis, preview = "Day two"),
                    MonarchAiConversationSummary("invoices", "Invoice questions", now - DayMillis * InvoicesAgeDays),
                    MonarchAiConversationSummary("draft", "", now - DayMillis * DraftAgeDays),
                ),
            )
        }
        var active by rememberSaveable { mutableStateOf<String?>("trip") }
        CatalogueCaption("Long-press a row, or use the TalkBack actions")
        MonarchAiConversationList(
            conversations = conversations,
            onSelect = { active = it },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HeightSteps),
            activeId = active,
            nowMillis = now,
            onNew = {
                val id = "new-${conversations.size}"
                conversations = conversations + MonarchAiConversationSummary(id, "", now)
                active = id
            },
            onRename = { id, title ->
                conversations = conversations.map { if (it.id == id) it.copy(title = title) else it }
            },
            onPin = { id, pinned ->
                conversations = conversations.map { if (it.id == id) it.copy(pinned = pinned) else it }
            },
            onDelete = { id -> conversations = conversations.filterNot { it.id == id } },
        )
    }
}
