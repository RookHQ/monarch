package dev.monarchui.components.intelligence.aimemorychip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.internal.monarchString

/**
 * The edit dialog of `MonarchAiMemoryChip`: a `MonarchDialog` with a
 * multi-line `MonarchTextField`, Save and Cancel. Save is disabled while the
 * trimmed text is empty or unchanged.
 */
@Composable
internal fun MonarchAiMemoryChipEditDialog(
    original: String,
    labels: MonarchAiMemoryChipLabels,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(original) }
    val edited = MonarchAiMemoryChipTokens.edited(original, draft)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.editTitle, R.string.monarch_aimemorychip_edit_title),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aimemorychip_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_aimemorychip_save),
                onClick = { edited?.let(onSave) },
                variant = MonarchButtonVariant.Primary,
                enabled = edited != null,
            )
        },
    ) {
        MonarchTextField(
            value = draft,
            onValueChange = { draft = it },
            label = monarchString(labels.editField, R.string.monarch_aimemorychip_edit_field),
            singleLine = false,
        )
    }
}
