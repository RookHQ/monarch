package dev.monarchui.components.files.pagetree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.components.typography.text.MonarchText

/** The open dialog, if any: `MonarchDialog`s that ask for a name or confirm a delete. */
@Suppress("LongParameterList")
@Composable
internal fun PageTreeDialogs(
    dialog: PageTreeDialog?,
    texts: PageTreeTexts,
    onDismiss: () -> Unit,
    onAddGroup: ((String) -> Unit)?,
    onRenameGroup: ((String, String) -> Unit)?,
    onDeleteGroup: ((String) -> Unit)?,
    onDeletePage: ((String) -> Unit)?,
) {
    when (dialog) {
        null -> Unit
        PageTreeDialog.AddGroup -> PageTreeNameDialog(
            title = texts.addGroupTitle,
            initial = texts.newGroupName,
            submit = texts.addGroup,
            texts = texts,
            accept = { it.trim().takeIf(String::isNotEmpty) },
            onDismiss = onDismiss,
        ) { name -> onAddGroup?.invoke(name) }
        is PageTreeDialog.RenameGroup -> PageTreeNameDialog(
            title = texts.renameGroup,
            initial = dialog.group.label,
            submit = texts.renameSubmit,
            texts = texts,
            accept = { pageTreeRenamed(it, dialog.group.label) },
            onDismiss = onDismiss,
        ) { name -> onRenameGroup?.invoke(dialog.group.id, name) }
        is PageTreeDialog.DeleteGroup -> PageTreeDeleteGroupDialog(dialog.group, texts, onDismiss) {
            onDeleteGroup?.invoke(dialog.group.id)
        }
        is PageTreeDialog.DeletePage -> PageTreeConfirmDialog(
            title = texts.deletePageTitle(dialog.title),
            confirm = texts.deletePage,
            texts = texts,
            confirmEnabled = true,
            onDismiss = onDismiss,
            onConfirm = { onDeletePage?.invoke(dialog.page.id) },
        ) { MonarchText(text = texts.cannotUndo) }
    }
}

/** Asks for a group name. The confirm button is enabled while [accept] gives a name. */
@Suppress("LongParameterList")
@Composable
private fun PageTreeNameDialog(
    title: String,
    initial: String,
    submit: String,
    texts: PageTreeTexts,
    accept: (String) -> String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var name by rememberSaveable(initial) { mutableStateOf(initial) }
    val accepted = accept(name)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = title,
        actions = {
            MonarchButton(text = texts.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(
                text = submit,
                onClick = {
                    onDismiss()
                    accepted?.let(onSubmit)
                },
                variant = MonarchButtonVariant.Primary,
                enabled = accepted != null,
            )
        },
    ) {
        MonarchTextField(value = name, onValueChange = { name = it }, label = texts.groupName)
    }
}

/** Confirms deleting a group; a group with pages also asks for its name. */
@Composable
private fun PageTreeDeleteGroupDialog(
    group: MonarchPageTreeGroup,
    texts: PageTreeTexts,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var typed by rememberSaveable(group.id) { mutableStateOf("") }
    val needsName = group.pages.isNotEmpty()
    PageTreeConfirmDialog(
        title = texts.deleteGroupTitle(group.label),
        confirm = texts.deleteGroup,
        texts = texts,
        confirmEnabled = !needsName || MonarchTypeToConfirmInputDefaults.matches(typed, group.label),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchPageTreeTokens.Gap)) {
            if (needsName) MonarchText(text = texts.deleteGroupConsequence(group.pages.size))
            MonarchText(text = texts.cannotUndo)
            if (needsName) {
                MonarchTypeToConfirmInput(
                    phrase = group.label,
                    value = typed,
                    onValueChange = { typed = it },
                    label = texts.typeToConfirm(group.label),
                )
            }
        }
    }
}

/** A delete confirmation with a danger button. */
@Suppress("LongParameterList")
@Composable
private fun PageTreeConfirmDialog(
    title: String,
    confirm: String,
    texts: PageTreeTexts,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = title,
        actions = {
            MonarchButton(text = texts.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(
                text = confirm,
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                variant = MonarchButtonVariant.Danger,
                enabled = confirmEnabled,
            )
        },
    ) { content() }
}
