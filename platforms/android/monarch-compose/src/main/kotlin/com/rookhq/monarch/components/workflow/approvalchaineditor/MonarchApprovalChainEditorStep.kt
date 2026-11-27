package com.rookhq.monarch.components.workflow.approvalchaineditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPicker
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchRemoveButton

private typealias StepTokens = MonarchApprovalChainEditorTokens

/** One step of a MonarchApprovalChainEditor, after the drag handle: heading, remove button and the fields. */
@Composable
@Suppress("LongParameterList")
internal fun RowScope.MonarchApprovalChainEditorStep(
    step: MonarchApprovalChainStep,
    position: Int,
    approverTypes: List<MonarchApprovalApproverType>,
    removable: Boolean,
    showLabels: Boolean,
    missingTarget: Boolean,
    enabled: Boolean,
    text: MonarchApprovalChainEditorText,
    removeIcon: (@Composable () -> Unit)?,
    onChange: (MonarchApprovalChainStep) -> Unit,
    onRemove: () -> Unit,
) {
    val type = approverTypes.firstOrNull { it.value == step.approver }
    Column(
        modifier = Modifier
            .weight(1f)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(StepTokens.ControlGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonarchText(
                text = text.step(position),
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.BodyStrong,
                heading = true,
            )
            MonarchRemoveButton(
                name = text.removeStep(position),
                text = text.remove,
                onClick = onRemove,
                enabled = enabled && removable,
                icon = removeIcon,
            )
        }
        MonarchSelect(
            options = approverTypes.map { MonarchSelectOption(it.value, it.label) },
            selected = step.approver.takeIf { it.isNotEmpty() },
            onSelect = { onChange(step.copy(approver = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = text.approver,
            enabled = enabled,
        )
        type?.targets?.let { targets ->
            MonarchEntityPicker(
                label = type.targetLabel ?: text.target,
                entities = targets,
                selected = step.target,
                onSelectedChange = { onChange(step.copy(target = it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                required = true,
                placeholder = text.targetPlaceholder,
                errorText = text.targetRequired.takeIf { missingTarget },
            )
        }
        if (showLabels) {
            MonarchTextField(
                value = step.label.orEmpty(),
                onValueChange = { onChange(step.copy(label = it.take(StepTokens.LABEL_MAX_LENGTH))) },
                modifier = Modifier.fillMaxWidth(),
                label = text.label,
                placeholder = text.labelPlaceholder,
                enabled = enabled,
            )
        }
    }
}
