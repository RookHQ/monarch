package dev.monarchui.sample.sections.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialog
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ResponsiveDialog: a sheet on a phone, a dialog on a tablet or an unfolded foldable. */
object MonarchResponsiveDialogSection : CataloguePage {
    override val title = "ResponsiveDialog"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("Monarch for Android") }
        var draft by rememberSaveable { mutableStateOf(name) }
        CatalogueCaption("Rename a project")
        MonarchText(text = "Project: $name", style = MonarchTextStyle.Body)
        MonarchButton(
            text = "Rename",
            onClick = {
                draft = name
                open = true
            },
        )
        MonarchText(
            text = "On a phone this opens a bottom sheet; on a window 600dp wide or more, a centered dialog.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        if (open) {
            MonarchResponsiveDialog(
                onDismissRequest = { open = false },
                title = "Rename project",
                description = "Everyone in the workspace sees the new name.",
                actions = {
                    MonarchButton(text = "Cancel", onClick = { open = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(
                        text = "Rename",
                        onClick = {
                            name = draft
                            open = false
                        },
                        variant = MonarchButtonVariant.Primary,
                        enabled = draft.isNotBlank(),
                    )
                },
            ) {
                MonarchTextField(value = draft, onValueChange = { draft = it }, label = "Name")
            }
        }
    }
}
