package dev.monarchui.components.overlays.unsavedchangesguard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.internal.MonarchActionState
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchActionRunner
import dev.monarchui.theme.MonarchTheme

/**
 * Warns before leaving a screen throws away unsaved changes, with Keep
 * editing, Discard changes and an optional Save and leave.
 *
 * Place it once in the screen. While [hasChanges] is true it intercepts the
 * system back gesture and button with an androidx.activity `BackHandler` and
 * opens the question, a Material 3 `AlertDialog`. The app's own Close or
 * Cancel button opens it too, by setting [open] when there are changes.
 *
 * Save and leave runs [onSave] in a coroutine of the guard. While it runs the
 * dialog stays open with a busy button, cannot be dismissed, and back does
 * nothing; when it throws, the error is shown in the dialog and announced,
 * and the person stays.
 *
 * @param hasChanges whether there are unsaved changes. While false, back is not intercepted.
 * @param open whether the question is shown.
 * @param onOpenChange called with true when back asks, and with false when the person chooses to stay (Keep
 * editing, back or a tap outside) and before [onLeave].
 * @param onLeave leaves the screen (pop the back stack, close the sheet), with
 * [MonarchUnsavedChangesGuardLeaveReason.Discard] at once or [MonarchUnsavedChangesGuardLeaveReason.Save] after
 * [onSave] returned.
 * @param modifier the modifier of the dialog.
 * @param guardsBack whether the system back is intercepted while there are changes. Set it to false when the
 * screen handles back itself and opens the guard.
 * @param onSave saves the changes. When given, Save and leave is offered. Throw to report a failure.
 * @param details extra content under the message, for example the list of unsaved fields.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchUnsavedChangesGuard(
    hasChanges: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onLeave: (MonarchUnsavedChangesGuardLeaveReason) -> Unit,
    modifier: Modifier = Modifier,
    guardsBack: Boolean = true,
    onSave: (suspend () -> Unit)? = null,
    details: (@Composable () -> Unit)? = null,
    labels: MonarchUnsavedChangesGuardLabels = MonarchUnsavedChangesGuardLabels(),
) {
    val tokens = MonarchUnsavedChangesGuardTokens
    val runner = rememberMonarchActionRunner()
    val saving = runner.busy
    val failed = runner.state == MonarchActionState.Failed
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)
    val currentOnLeave by rememberUpdatedState(onLeave)
    val currentOnSave by rememberUpdatedState(onSave)
    BackHandler(enabled = tokens.interceptsBack(guardsBack, hasChanges, saving)) {
        if (tokens.backAsks(saving)) currentOnOpenChange(true)
    }
    if (!open) return
    val stay = {
        if (!saving) {
            runner.reset()
            currentOnOpenChange(false)
        }
    }
    val discard = {
        runner.reset()
        currentOnOpenChange(false)
        currentOnLeave(MonarchUnsavedChangesGuardLeaveReason.Discard)
    }
    val save: () -> Unit = {
        val action = currentOnSave
        // One save at a time; a save cancelled with the screen is not a failure.
        if (action != null) {
            runner.run {
                action()
                currentOnOpenChange(false)
                currentOnLeave(MonarchUnsavedChangesGuardLeaveReason.Save)
            }
        }
    }
    MonarchDialog(
        onDismissRequest = stay,
        title = monarchString(labels.title, R.string.monarch_unsavedchangesguard_title),
        modifier = modifier,
        dismissible = !saving,
        actions = {
            GuardDiscardButton(saving, discard, labels)
            MonarchButton(
                text = monarchString(labels.keepEditing, R.string.monarch_unsavedchangesguard_keep_editing),
                onClick = stay,
                variant = MonarchButtonVariant.Secondary,
                enabled = !saving,
            )
            GuardSaveButton(saving, onSave != null, save, labels)
        },
        content = { GuardMessage(failed, details, labels) },
    )
}

@Composable
private fun GuardMessage(
    failed: Boolean,
    details: (@Composable () -> Unit)?,
    labels: MonarchUnsavedChangesGuardLabels,
) {
    val tokens = MonarchUnsavedChangesGuardTokens
    val error = monarchString(labels.saveFailed, R.string.monarch_unsavedchangesguard_save_failed)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
        Text(
            text = monarchString(labels.message, R.string.monarch_unsavedchangesguard_message),
            style = MonarchTheme.typography.body,
            color = tokens.messageColor(MonarchTheme.colors),
        )
        details?.invoke()
        // Always present, so the failure is announced when it appears.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (failed) contentDescription = error
            },
        ) {
            if (failed) {
                Text(
                    text = error,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.body,
                    color = tokens.errorColor(MonarchTheme.colors),
                )
            }
        }
    }
}

/** Discard changes and, with a save, Save and leave, which wrap when there is no room. */
/** The two choices as one row: Discard, then Save, which wraps with the other buttons of the dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchUnsavedChangesGuardChoices(
    saving: Boolean,
    canSave: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    labels: MonarchUnsavedChangesGuardLabels,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchUnsavedChangesGuardTokens.ActionGap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(MonarchUnsavedChangesGuardTokens.ActionGap),
    ) {
        GuardChoiceButtons(saving, canSave, onDiscard, onSave, labels)
    }
}

/** Discard is the destructive button; Save and leave stays the primary action of the dialog. */
@Composable
private fun GuardDiscardButton(saving: Boolean, onDiscard: () -> Unit, labels: MonarchUnsavedChangesGuardLabels) {
    MonarchButton(
        text = monarchString(labels.discard, R.string.monarch_unsavedchangesguard_discard),
        onClick = onDiscard,
        variant = MonarchButtonVariant.Danger,
        enabled = !saving,
    )
}

@Composable
private fun GuardSaveButton(
    saving: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    labels: MonarchUnsavedChangesGuardLabels,
) {
    if (canSave) {
        MonarchButton(
            text = monarchString(labels.save, R.string.monarch_unsavedchangesguard_save),
            onClick = onSave,
            variant = MonarchButtonVariant.Primary,
            loading = saving,
        )
    }
}

@Composable
private fun GuardChoiceButtons(
    saving: Boolean,
    canSave: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    labels: MonarchUnsavedChangesGuardLabels,
) {
    GuardDiscardButton(saving, onDiscard, labels)
    GuardSaveButton(saving, canSave, onSave, labels)
}
