package dev.monarchui.components.workflow.approvalchaineditor

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewTypes = listOf(
    MonarchApprovalApproverType("manager", "Reporting manager"),
    MonarchApprovalApproverType(
        "role",
        "Anyone with a role",
        targets = listOf(MonarchEntityPickerEntity("finance", "Finance team")),
        targetLabel = "Role",
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchApprovalChainEditorPreview() {
    PreviewFrame {
        MonarchApprovalChainEditor(
            value = listOf(
                MonarchApprovalChainStep("s1", "manager"),
                MonarchApprovalChainStep("s2", "role", label = "Finance check"),
            ),
            onValueChange = {},
            approverTypes = PreviewTypes,
            showErrors = true,
        )
    }
}
