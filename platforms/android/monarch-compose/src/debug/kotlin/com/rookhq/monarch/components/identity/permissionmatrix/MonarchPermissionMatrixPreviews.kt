package com.rookhq.monarch.components.identity.permissionmatrix

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewRoles = listOf(
    MonarchPermissionMatrixRole("owner", "Owner", "Everything, including billing."),
    MonarchPermissionMatrixRole("member", "Member", "Works in projects."),
)
private val PreviewPermissions = listOf(
    MonarchPermissionMatrixPermission("invite", "Invite members", group = "People"),
    MonarchPermissionMatrixPermission("billing", "Manage billing", group = "Billing"),
)
private val PreviewValue = mapOf("owner" to setOf("invite", "billing"), "member" to setOf("invite"))

@MonarchPreviews
@Composable
internal fun MonarchPermissionMatrixPreview() {
    PreviewFrame {
        MonarchPermissionMatrix(
            roles = PreviewRoles,
            permissions = PreviewPermissions,
            value = PreviewValue,
            onValueChange = {},
            title = "Workspace roles",
            lockReason = { role, _ -> if (role == "owner") "Owners can do everything." else null },
        )
        MonarchPermissionMatrix(
            roles = PreviewRoles,
            permissions = PreviewPermissions,
            value = PreviewValue,
            onValueChange = {},
            layout = MonarchPermissionMatrixLayout.ByRole,
            readOnly = true,
        )
    }
}
