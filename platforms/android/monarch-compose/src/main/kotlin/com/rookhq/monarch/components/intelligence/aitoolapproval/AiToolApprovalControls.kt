package com.rookhq.monarch.components.intelligence.aitoolapproval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.internal.monarchString

/** The editor, the reason field, Always allow and the buttons, while the call waits. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiToolApprovalControls(
    part: MonarchAiToolCallPart,
    state: AiToolApprovalState,
    config: AiToolApprovalConfig,
    labels: MonarchAiToolApprovalLabels,
) {
    val tokens = MonarchAiToolApprovalTokens
    val original = MonarchAiToolApprovalFormat.editableText(part.input)
    val offerAlways = tokens.offersAlwaysAllow(config.onAlwaysAllow != null, config.risk)
    val approve = {
        when (val edit = MonarchAiToolApprovalFormat.edit(state.editing.value, state.json.value, original)) {
            AiToolApprovalEdit.Invalid -> state.jsonError.value = true
            else -> {
                if (state.always.value && offerAlways) config.onAlwaysAllow?.invoke(part.name)
                state.decision.value = AiToolApprovalDecision.Approved
                config.onApprove((edit as? AiToolApprovalEdit.Edited)?.input)
            }
        }
    }
    val deny = {
        state.decision.value = AiToolApprovalDecision.Denied
        config.onDeny(MonarchAiToolApprovalFormat.reason(state.reason.value))
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (state.editing.value) {
            MonarchTextField(
                value = state.json.value,
                onValueChange = {
                    state.json.value = it
                    state.jsonError.value = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = monarchString(labels.argumentsJson, R.string.monarch_aitoolapproval_arguments_json),
                errorText = if (state.jsonError.value) {
                    monarchString(labels.invalidJson, R.string.monarch_aitoolapproval_invalid_json)
                } else {
                    null
                },
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
            )
        }
        if (state.reasonOpen.value) {
            MonarchTextField(
                value = state.reason.value,
                onValueChange = { state.reason.value = it },
                modifier = Modifier.fillMaxWidth(),
                label = monarchString(labels.reason, R.string.monarch_aitoolapproval_reason),
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        if (offerAlways) {
            MonarchCheckboxRow(
                checked = state.always.value,
                onCheckedChange = { state.always.value = it },
                label = monarchString(labels.alwaysAllow, R.string.monarch_aitoolapproval_always_allow),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
            verticalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
        ) {
            if (config.editable) {
                MonarchButton(
                    text = if (state.editing.value) {
                        monarchString(labels.discardEdits, R.string.monarch_aitoolapproval_discard_edits)
                    } else {
                        monarchString(labels.editArguments, R.string.monarch_aitoolapproval_edit_arguments)
                    },
                    onClick = {
                        if (state.editing.value) {
                            state.json.value = original
                            state.jsonError.value = false
                        }
                        state.editing.value = !state.editing.value
                    },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
            if (!state.reasonOpen.value) {
                MonarchButton(
                    text = monarchString(labels.addReason, R.string.monarch_aitoolapproval_add_reason),
                    onClick = { state.reasonOpen.value = true },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
            MonarchButton(
                text = monarchString(labels.deny, R.string.monarch_aitoolapproval_deny),
                onClick = deny,
                size = MonarchButtonSize.Sm,
            )
            MonarchButton(
                text = monarchString(labels.approve, R.string.monarch_aitoolapproval_approve),
                onClick = approve,
                variant = tokens.approveVariant(config.risk),
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
