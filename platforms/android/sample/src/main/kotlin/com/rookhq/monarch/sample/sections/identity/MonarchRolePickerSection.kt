package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePicker
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePickerLayout
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val Roles = listOf(
    MonarchRoleOption("viewer", title = "Viewer", description = "Can see everything, change nothing."),
    MonarchRoleOption(
        "editor",
        title = "Editor",
        description = "Can create and edit content.",
        capabilities = listOf("Invite guests", "Publish pages"),
    ),
    MonarchRoleOption(
        "admin",
        title = "Admin",
        description = "Can manage members and billing.",
        enabled = false,
        disabledReason = "Only owners can make someone an admin.",
    ),
)

/** The catalogue page of RolePicker. */
object MonarchRolePickerSection : CataloguePage {
    override val title = "RolePicker"

    @Composable
    override fun Content() {
        var role by rememberSaveable { mutableStateOf<String?>("viewer") }
        var compactRole by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Cards")
        MonarchRolePicker(
            roles = Roles,
            selected = role,
            onSelect = { role = it },
            helperText = "You can change the role later.",
        )
        CatalogueCaption("Compact")
        MonarchRolePicker(
            roles = Roles,
            selected = compactRole,
            onSelect = { compactRole = it },
            layout = MonarchRolePickerLayout.Compact,
            errorText = if (compactRole == null) "Choose a role to send the invitation." else null,
        )
    }
}
