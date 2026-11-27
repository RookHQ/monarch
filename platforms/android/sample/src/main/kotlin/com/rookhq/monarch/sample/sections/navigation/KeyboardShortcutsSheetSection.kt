package com.rookhq.monarch.sample.sections.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/**
 * The catalogue page of KeyboardShortcutsSheet: the system Keyboard Shortcuts
 * Helper. The catalogue's own activity declares no shortcut groups, so the
 * system list shows only the system's shortcuts here; an app returns its groups
 * from Activity.onProvideKeyboardShortcuts, as the inventory example shows.
 */
object KeyboardShortcutsSheetSection : CataloguePage {
    override val title = "KeyboardShortcutsSheet"

    @Composable
    override fun Content() {
        val activity = LocalActivity.current
        var created by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Ctrl+N handled with onPreviewKeyEvent")
        Column(
            modifier = Modifier.onPreviewKeyEvent { event ->
                val newTicket = event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.N
                if (newTicket) created++
                newTicket
            },
        ) {
            MonarchButton(text = "New ticket", onClick = { created++ })
            MonarchText(text = "$created tickets created")
        }
        CatalogueCaption("The system list, opened with Meta+/ or from code")
        MonarchButton(text = "Keyboard shortcuts", onClick = { activity?.requestShowKeyboardShortcuts() })
        MonarchText(text = "Needs a hardware keyboard.", style = MonarchTextStyle.Caption)
    }
}
