package com.rookhq.monarch.components.files.editortoolbar

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.monarchString

/**
 * The link form of a `MonarchEditorToolbar`: a `MonarchDialog` with
 * an address field, Apply (enabled only for a safe address), Remove link when
 * a link is active, and Cancel.
 */
@Composable
internal fun MonarchEditorToolbarLinkForm(
    linkActive: Boolean,
    labels: MonarchEditorToolbarLabels,
    onDone: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var address by rememberSaveable { mutableStateOf("") }
    val safe = MonarchEditorToolbarTokens.safeLink(address)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.linkTitle, R.string.monarch_editortoolbar_link_title),
        actions = {
            MonarchButton(
                text = monarchString(labels.linkCancel, R.string.monarch_editortoolbar_link_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            if (linkActive) {
                MonarchButton(
                    text = monarchString(labels.linkRemove, R.string.monarch_editortoolbar_link_remove),
                    onClick = { onDone(null) },
                    variant = MonarchButtonVariant.Ghost,
                )
            }
            MonarchButton(
                text = monarchString(labels.linkApply, R.string.monarch_editortoolbar_link_apply),
                onClick = { safe?.let(onDone) },
                variant = MonarchButtonVariant.Primary,
                enabled = safe != null,
            )
        },
    ) {
        MonarchTextField(
            value = address,
            onValueChange = { address = it },
            label = monarchString(labels.linkAddress, R.string.monarch_editortoolbar_link_address),
            helperText = monarchString(labels.linkHelp, R.string.monarch_editortoolbar_link_help),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
    }
}
