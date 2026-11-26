package com.rookhq.monarch.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of SectionCard: a card that cycles through its states, a collapsible card and a danger zone. */
object MonarchSectionCardSection : CataloguePage {
    override val title = "SectionCard"

    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableStateOf(MonarchSectionCardState.Ready) }
        var expanded by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Tap Next state to see loading, empty and error")
        MonarchSectionCard(
            title = "Open tickets",
            meta = "3 open",
            state = state,
            emptyMessage = "New tickets appear here.",
            errorMessage = "The server did not answer.",
            onRetry = { state = MonarchSectionCardState.Ready },
            actions = {
                MonarchButton(
                    text = "Next state",
                    onClick = {
                        val states = MonarchSectionCardState.entries
                        state = states[(state.ordinal + 1) % states.size]
                    },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
            footer = { MonarchText(text = "Updated 5 min ago") },
        ) {
            MonarchText(text = "Printer offline on floor 3")
            MonarchText(text = "Invoice shows the wrong address")
            MonarchText(text = "Reset two-factor")
        }
        CatalogueCaption("Collapsible")
        MonarchSectionCard(
            title = "Advanced settings",
            description = "Webhooks and API access.",
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) { MonarchText(text = "Webhooks are off.") }
        CatalogueCaption("Danger zone")
        MonarchSectionCard(
            title = "Danger zone",
            description = "Deleting the workspace removes every project.",
            tone = MonarchTone.Danger,
        ) { MonarchButton(text = "Delete workspace", onClick = {}, variant = MonarchButtonVariant.Danger) }
    }
}
