package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Sheet: MonarchBottomSheet with a title, a body and a footer. */
object SheetSection : CataloguePage {
    override val title = "Sheet"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var note by rememberSaveable { mutableStateOf("") }
        var saved by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A modal sheet from the bottom")
        MonarchButton(text = "Add a note", onClick = { open = true })
        if (saved.isNotEmpty()) MonarchText(text = "Note: $saved")
        MonarchText(
            text = "A side panel is a ModalNavigationDrawer at the start, or a supporting pane at expanded width.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }, title = "Add a note") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchTextField(value = note, onValueChange = { note = it }, label = "Note", singleLine = false)
                    MonarchButton(
                        text = "Save",
                        onClick = {
                            saved = note
                            this@MonarchBottomSheet.dismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
        }
    }
}
