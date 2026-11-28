package com.rookhq.monarch.components.workflow.escalationpolicyeditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewTargets = listOf(
    MonarchEntityPickerEntity("oncall", "On-call rota", description = "Rota"),
    MonarchEntityPickerEntity("lead", "Team lead", description = "Person"),
)

@MonarchPreviews
@Composable
internal fun MonarchEscalationPolicyEditorPreview() {
    PreviewFrame {
        MonarchEscalationPolicyEditor(
            value = listOf(
                MonarchEscalationLevel("l1", 15, listOf("oncall")),
                MonarchEscalationLevel("l2", 10),
            ),
            onValueChange = {},
            targets = PreviewTargets,
            showErrors = true,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchEscalationPolicyEditorNoTargetsPreview() {
    PreviewFrame {
        MonarchEscalationPolicyEditor(
            value = listOf(MonarchEscalationLevel("l1", 15)),
            onValueChange = {},
            targets = emptyList(),
        )
    }
}
