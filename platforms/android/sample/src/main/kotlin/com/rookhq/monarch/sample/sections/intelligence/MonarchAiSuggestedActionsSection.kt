@file:OptIn(com.rookhq.monarch.ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedAction
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActions
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActionsOrigin
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private val SectionActions = listOf(
    MonarchAiSuggestedAction(
        id = "reminder",
        label = "Send the renewal reminder",
        description = "The contract ends in 14 days and no renewal is recorded.",
    ),
    MonarchAiSuggestedAction(
        id = "task",
        label = "Create a follow-up task for next Monday",
        risk = MonarchAiToolCallRisk.Medium,
    ),
    MonarchAiSuggestedAction(
        id = "email",
        label = "Email the account team a status update",
        description = "Goes to 3 people.",
        requiresApproval = true,
        consequences = listOf("3 people will get an email", "The email is logged on the account"),
        confirmLabel = "Send email",
    ),
    MonarchAiSuggestedAction(
        id = "close",
        label = "Close 4 stale tickets",
        description = "No reply in 60 days.",
        risk = MonarchAiToolCallRisk.Destructive,
        consequences = listOf("4 tickets will be closed", "Requesters are notified"),
        confirmLabel = "Close tickets",
    ),
)

private const val RunMillis = 800L

/** The catalogue page of AiSuggestedActions. */
object MonarchAiSuggestedActionsSection : CataloguePage {
    override val title = "AiSuggestedActions"

    @Composable
    override fun Content() {
        var dismissed by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("Run, approve and dismiss")
        MonarchAiSuggestedActions(
            actions = SectionActions.filterNot { it.id in dismissed },
            onRun = { delay(RunMillis) },
            onDismiss = { id -> dismissed = dismissed + id },
        )
        if (dismissed.isNotEmpty()) {
            MonarchButton(text = "Bring back the suggestions", onClick = { dismissed = emptyList() })
        }
        CatalogueCaption("Rule-based, nothing to suggest")
        MonarchAiSuggestedActions(actions = emptyList(), onRun = {}, origin = MonarchAiSuggestedActionsOrigin.Rule)
    }
}
