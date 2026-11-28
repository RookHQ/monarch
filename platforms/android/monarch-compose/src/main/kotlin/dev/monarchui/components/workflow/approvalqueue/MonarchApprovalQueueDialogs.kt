package dev.monarchui.components.workflow.approvalqueue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.internal.LabelledControlRow
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/** Select all over the decidable visible requests, and the failure of the last bulk approval. */
@Composable
internal fun MonarchApprovalQueueSelectAll(
    visible: List<MonarchApprovalQueueItem>,
    selection: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    failed: Boolean,
    text: MonarchApprovalQueueText,
    idle: Boolean = false,
) {
    val tokens = MonarchApprovalQueueTokens
    val state = tokens.selectAllState(visible, selection)
    val enabled = visible.any { it.canDecide }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelledControlRow(
                label = text.selectAll,
                description = null,
                enabled = enabled,
                controlAtEnd = false,
                modifier = Modifier
                    .weight(1f)
                    .monarchFocusRing(MonarchTheme.shapes.sm)
                    .triStateToggleable(
                        state = state,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onClick = { onSelectionChange(tokens.toggleAll(visible, selection)) },
                    ),
            ) {
                MonarchCheckbox(state = state, onClick = null, enabled = enabled)
            }
            // As the web: the bulk action sits here, off, until a request is selected (then the bar under the
            // list shows).
            if (idle) {
                Text(
                    text = pluralStringResource(R.plurals.monarch_bulkactionbar_selected, 0, 0),
                    style = MonarchTheme.typography.caption,
                    color = MonarchTheme.colors.textMuted,
                )
                MonarchButton(
                    text = text.approveSelected,
                    onClick = {},
                    variant = MonarchButtonVariant.Success,
                    size = MonarchButtonSize.Sm,
                    enabled = false,
                    leadingIcon = MonarchDefaultIcons.Check,
                )
            }
        }
        if (failed) MonarchApprovalQueueFailure(text.failed)
    }
}

/** The dialog that sends [item] back, with a reason field unless [requirement] is `None`. */
@Suppress("LongParameterList")
@Composable
internal fun MonarchApprovalQueueRejectDialog(
    item: MonarchApprovalQueueItem,
    requirement: MonarchApprovalQueueRejectReason,
    onReject: suspend (MonarchApprovalQueueItem, String?) -> Unit,
    onDone: () -> Unit,
    onFailed: () -> Unit,
    onDismiss: () -> Unit,
    text: MonarchApprovalQueueText,
) {
    val tokens = MonarchApprovalQueueTokens
    val scope = rememberCoroutineScope()
    var reason by rememberSaveable(item.id) { mutableStateOf("") }
    var pending by remember(item.id) { mutableStateOf(false) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    val submit = {
        if (!pending) {
            pending = true
            error = null
            scope.launch {
                val sent = monarchApprovalQueueAttempt { onReject(item, tokens.submittedReason(reason, requirement)) }
                pending = false
                if (sent) {
                    onDone()
                } else {
                    error = text.failed
                    onFailed()
                }
            }
        }
    }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.rejectTitle(item.requesterName),
        description = text.rejectDescription,
        dismissible = !pending,
        actions = {
            MonarchButton(
                text = text.cancel,
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !pending,
            )
            MonarchButton(
                text = text.reject,
                onClick = { submit() },
                variant = MonarchButtonVariant.Danger,
                enabled = tokens.canConfirmReject(reason, requirement),
                loading = pending,
            )
        },
    ) {
        if (requirement != MonarchApprovalQueueRejectReason.None) {
            MonarchTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = text.rejectReason,
                placeholder = text.rejectReasonPrompt,
                errorText = error,
                enabled = !pending,
                singleLine = false,
            )
        } else {
            error?.let { MonarchApprovalQueueFailure(it) }
        }
    }
}

/** The confirmation of a bulk approval of [count] requests. */
@Composable
internal fun MonarchApprovalQueueBulkDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    text: MonarchApprovalQueueText,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = text.bulkTitle(count),
        description = text.bulkDescription,
        actions = {
            MonarchButton(text = text.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(
                text = text.bulkConfirm,
                onClick = onConfirm,
                variant = MonarchButtonVariant.Primary,
                enabled = count > 0,
            )
        },
    )
}

