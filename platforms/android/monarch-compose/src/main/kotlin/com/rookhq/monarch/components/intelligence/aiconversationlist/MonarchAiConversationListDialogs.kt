package com.rookhq.monarch.components.intelligence.aiconversationlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.monarchString

/** The dialog that asks for a conversation's new title. Save stays disabled until the title is new and not empty. */
@Composable
internal fun MonarchAiConversationListRenameDialog(
    current: String,
    labels: MonarchAiConversationListLabels,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(current) }
    val renamed = MonarchAiConversationListTokens.renamed(current, text)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.renameTitle, R.string.monarch_aiconversationlist_rename_title),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aiconversationlist_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_aiconversationlist_save),
                onClick = { renamed?.let(onSave) },
                variant = MonarchButtonVariant.Primary,
                enabled = renamed != null,
            )
        },
    ) {
        MonarchTextField(
            value = text,
            onValueChange = { text = it },
            label = monarchString(labels.renameField, R.string.monarch_aiconversationlist_rename_field),
        )
    }
}

/** The dialog that confirms deleting a conversation, with a danger Delete button. */
@Composable
internal fun MonarchAiConversationListDeleteDialog(
    title: String,
    labels: MonarchAiConversationListLabels,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = labels.deleteTitle?.invoke(title)
            ?: stringResource(R.string.monarch_aiconversationlist_delete_title, title),
        description = monarchString(labels.deleteMessage, R.string.monarch_aiconversationlist_delete_message),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aiconversationlist_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.deleteConfirm, R.string.monarch_aiconversationlist_delete_confirm),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
    )
}
