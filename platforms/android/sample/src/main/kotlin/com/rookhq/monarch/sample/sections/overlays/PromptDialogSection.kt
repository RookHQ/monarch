package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PromptDialog: MonarchDialog with a MonarchTextField. */
object PromptDialogSection : CataloguePage {
    override val title = "PromptDialog"

    private const val MAX_LENGTH = 40

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("") }
        var created by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Ask for one value")
        MonarchButton(text = "New folder", onClick = { open = true })
        created?.let { MonarchText(text = "Created \"$it\"") }
        if (open) {
            val focus = remember { FocusRequester() }
            val tooLong = name.length > MAX_LENGTH
            val valid = name.isNotBlank() && !tooLong
            val create = {
                created = name
                name = ""
                open = false
            }
            MonarchDialog(
                onDismissRequest = { open = false },
                title = "New folder",
                content = {
                    MonarchTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.focusRequester(focus),
                        label = "Folder name",
                        errorText = if (tooLong) "Use $MAX_LENGTH characters or fewer." else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (valid) create() }),
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                },
                actions = {
                    MonarchButton(text = "Cancel", onClick = { open = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(
                        text = "Create",
                        onClick = create,
                        variant = MonarchButtonVariant.Primary,
                        enabled = valid,
                    )
                },
            )
        }
    }
}
