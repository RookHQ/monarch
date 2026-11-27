package com.rookhq.monarch.components.workflow.approvalchaineditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
