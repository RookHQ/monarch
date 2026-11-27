package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuard
import com.rookhq.monarch.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuardLeaveReason
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

private const val SimulatedSaveMillis = 800L

/** The catalogue page of UnsavedChangesGuard. */
object MonarchUnsavedChangesGuardSection : CataloguePage {
    override val title = "UnsavedChangesGuard"

    @Composable
    override fun Content() {
        var saved by rememberSaveable { mutableStateOf("Ana Flores") }
        var draft by rememberSaveable { mutableStateOf(saved) }
        var asking by rememberSaveable { mutableStateOf(false) }
        var outcome by rememberSaveable { mutableStateOf("") }
        val hasChanges = draft != saved
        CatalogueCaption("Edit the name, then press Close or the system back")
        MonarchTextField(value = draft, onValueChange = { draft = it }, label = "Display name")
        MonarchButton(
            text = "Close",
            onClick = { if (hasChanges) asking = true else outcome = "Closed with nothing to save." },
            variant = MonarchButtonVariant.Ghost,
        )
        if (outcome.isNotEmpty()) {
            MonarchText(text = outcome, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        }
        MonarchUnsavedChangesGuard(
            hasChanges = hasChanges,
            open = asking,
            onOpenChange = { asking = it },
            onLeave = { reason ->
                if (reason == MonarchUnsavedChangesGuardLeaveReason.Discard) draft = saved
                outcome = "Left the screen: $reason."
            },
            onSave = {
                delay(SimulatedSaveMillis)
                saved = draft
            },
        )
    }
}
