package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ConfirmDialog: MonarchDialog with a danger MonarchButton. */
object ConfirmDialogSection : CataloguePage {
    override val title = "ConfirmDialog"

    @Composable
    override fun Content() {
        var confirming by rememberSaveable { mutableStateOf(false) }
        var deleted by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Confirm a deletion")
        if (deleted) {
            MonarchText(
                text = "The project was deleted.",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
            MonarchButton(text = "Restore the example", onClick = { deleted = false })
        } else {
            MonarchButton(
                text = "Delete project",
                onClick = { confirming = true },
                variant = MonarchButtonVariant.Danger,
            )
        }
        if (confirming) {
            MonarchDialog(
                onDismissRequest = { confirming = false },
                title = "Delete the project?",
                description = "Its 12 tickets are deleted with it. This cannot be undone.",
                actions = {
                    MonarchButton(
                        text = "Cancel",
                        onClick = { confirming = false },
                        variant = MonarchButtonVariant.Ghost,
                    )
                    MonarchButton(
                        text = "Delete",
                        onClick = {
                            deleted = true
                            confirming = false
                        },
                        variant = MonarchButtonVariant.Danger,
                    )
                },
            )
        }
    }
}
