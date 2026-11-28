package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.identity.permissionmatrix.MonarchPermissionMatrix
import dev.monarchui.components.identity.permissionmatrix.MonarchPermissionMatrixLayout
import dev.monarchui.components.identity.permissionmatrix.MonarchPermissionMatrixPermission
import dev.monarchui.components.identity.permissionmatrix.MonarchPermissionMatrixRole
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Roles = listOf(
    MonarchPermissionMatrixRole("owner", "Owner", "Everything, including billing and deleting the workspace."),
    MonarchPermissionMatrixRole("admin", "Admin", "Manages people and settings."),
    MonarchPermissionMatrixRole("member", "Member", "Works in projects."),
)
private val Permissions = listOf(
    MonarchPermissionMatrixPermission("invite", "Invite members", group = "People"),
    MonarchPermissionMatrixPermission("remove", "Remove members", group = "People"),
    MonarchPermissionMatrixPermission("billing", "Manage billing", "Invoices, plan and payment method", "Billing"),
)

/** The catalogue page of PermissionMatrix. */
object MonarchPermissionMatrixSection : CataloguePage {
    override val title = "PermissionMatrix"

    @Composable
    override fun Content() {
        // A map of sets is not saveable as is; the page keeps it for as long as it is shown.
        var grants by remember {
            mutableStateOf(
                mapOf(
                    "owner" to setOf("invite", "remove", "billing"),
                    "admin" to setOf("invite", "remove"),
                    "member" to emptySet(),
                ),
            )
        }
        CatalogueCaption("Automatic: one role at a time on a phone")
        MonarchPermissionMatrix(
            roles = Roles,
            permissions = Permissions,
            value = grants,
            onValueChange = { grants = it },
            title = "Workspace roles",
            lockReason = { role, _ -> if (role == "owner") "Owners can do everything." else null },
        )
        CatalogueCaption("Grid, read only")
        MonarchPermissionMatrix(
            roles = Roles,
            permissions = Permissions,
            value = grants,
            onValueChange = {},
            layout = MonarchPermissionMatrixLayout.Grid,
            readOnly = true,
        )
    }
}
