package dev.monarchui.sample.sections.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of FocusTrap: MonarchDialog and MonarchBottomSheet (the Material 3 platform windows)
 * keep TalkBack and keyboard focus inside themselves, and Back closes them.
 */
@OptIn(ExperimentalMaterial3Api::class)
object FocusTrapSection : CataloguePage {
    override val title = "FocusTrap"

    @Composable
    override fun Content() {
        var dialog by rememberSaveable { mutableStateOf(false) }
        var sheet by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A dialog confines focus")
        MonarchButton(text = "Rename project", onClick = { dialog = true })
        CatalogueCaption("A modal bottom sheet confines focus")
        MonarchButton(text = "Share options", onClick = { sheet = true })
        if (dialog) {
            MonarchDialog(
                onDismissRequest = { dialog = false },
                title = "Rename project",
                actions = {
                    MonarchButton(text = "Cancel", onClick = { dialog = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Rename", onClick = { dialog = false }, variant = MonarchButtonVariant.Primary)
                },
            ) {
                MonarchTextField(value = name, onValueChange = { name = it }, label = "Name")
            }
        }
        if (sheet) {
            MonarchBottomSheet(onDismissRequest = { sheet = false }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                ) {
                    MonarchButton(text = "Copy link", onClick = { this@MonarchBottomSheet.dismiss() })
                    MonarchButton(
                        text = "Close",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        variant = MonarchButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}
