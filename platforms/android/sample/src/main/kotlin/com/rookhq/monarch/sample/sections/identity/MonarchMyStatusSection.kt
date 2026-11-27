package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardGroup
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.identity.mystatus.MonarchMyStatus
import com.rookhq.monarch.components.identity.mystatus.MonarchMyStatusCustomStatus
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of MyStatus: the row opens a sheet that changes the presence and the message. */
object MonarchMyStatusSection : CataloguePage {
    override val title = "MyStatus"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var presence by rememberSaveable { mutableStateOf(MonarchPresenceStatus.Online) }
        var message by rememberSaveable { mutableStateOf("On holiday") }
        var open by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Tap the row to change the status")
        MonarchMyStatus(
            name = "Priya Raman",
            presence = presence,
            onClick = { open = true },
            customStatus = MonarchMyStatusCustomStatus(text = message, emoji = "🌴"),
        )
        CatalogueCaption("Without a status")
        MonarchMyStatus(name = "Priya Raman", presence = MonarchPresenceStatus.Away, onClick = {})
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, title = "Your status") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchTextField(value = message, onValueChange = { message = it }, label = "Message")
                    MonarchRadioCardGroup(
                        options = MonarchPresenceStatus.entries.map { MonarchRadioCardOption(it, title = it.name) },
                        selected = presence,
                        onSelect = { presence = it },
                        label = "Presence",
                    )
                }
            }
        }
    }
}
