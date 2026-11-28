package dev.monarchui.components.workflow.escalationpolicyeditor

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
