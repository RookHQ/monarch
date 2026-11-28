package com.rookhq.monarch.components.search.savedviews

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The resolved strings of the naming dialog. */
internal class SavedViewsNameText(
    val title: String,
    val name: String,
    val save: String,
    val cancel: String,
    val required: String,
    val taken: String,
    val failed: String,
)

/**
 * The dialog that names a new view or renames one. Save checks the name;
 * while [onSubmit] runs the dialog stays open with a busy Save button and
 * cannot be dismissed, and a thrown error is shown in it.
 */
@Composable
internal fun SavedViewsNameDialog(
    initial: String,
    views: List<MonarchSavedView>,
    renamingId: String?,
    maxLength: Int,
    text: SavedViewsNameText,
    onDismiss: () -> Unit,
    onSubmit: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val submit by rememberUpdatedState(onSubmit)
    var name by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val save: () -> Unit = {
        when (MonarchSavedViewsTokens.validate(name, views, renamingId)) {
            SavedViewsNameError.Required -> error = text.required
            SavedViewsNameError.Taken -> error = text.taken
            null -> {
                busy = true
                error = null
                scope.launch {
                    val outcome = runCatching { submit(name.trim()) }
                    busy = false
                    (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                    if (outcome.isSuccess) onDismiss() else error = text.failed
                }
            }
        }
    }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.title,
        dismissible = !busy,
        // A tap outside must not lose what was typed; back still closes it.
        dismissOnClickOutside = false,
        actions = {
            MonarchButton(
                text = text.cancel,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !busy,
            )
            MonarchButton(text = text.save, onClick = save, variant = MonarchButtonVariant.Primary, loading = busy)
        },
    ) {
        MonarchTextField(
            value = name,
            onValueChange = {
                name = it.take(maxLength)
                error = null
            },
            label = text.name,
            errorText = error,
            enabled = !busy,
        )
    }
}

/** The resolved strings of the delete confirmation. */
internal class SavedViewsDeleteText(
    val title: String,
    val message: String,
    val confirm: String,
    val cancel: String,
    val failed: String,
)

/** Asks before deleting a view; stays open while [onConfirm] runs and shows a thrown error. */
@Composable
internal fun SavedViewsDeleteDialog(text: SavedViewsDeleteText, onDismiss: () -> Unit, onConfirm: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    val confirm by rememberUpdatedState(onConfirm)
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.title,
        description = text.message,
        dismissible = !busy,
        actions = {
            MonarchButton(
                text = text.cancel,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !busy,
            )
            MonarchButton(
                text = text.confirm,
                onClick = {
                    busy = true
                    failed = false
                    scope.launch {
                        val outcome = runCatching { confirm() }
                        busy = false
                        (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                        if (outcome.isSuccess) onDismiss() else failed = true
                    }
                },
                variant = MonarchButtonVariant.Danger,
                loading = busy,
            )
        },
    ) {
        if (failed) {
            Text(
                text = text.failed,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MonarchSavedViewsTokens.dangerText(MonarchTheme.colors),
            )
        }
    }
}
