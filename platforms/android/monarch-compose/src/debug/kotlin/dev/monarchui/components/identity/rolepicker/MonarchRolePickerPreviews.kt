package dev.monarchui.components.identity.rolepicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRolePickerPreview() {
    val roles = listOf(
        MonarchRoleOption("viewer", title = "Viewer", description = "Can see everything, change nothing."),
        MonarchRoleOption("editor", title = "Editor", capabilities = listOf("Invite guests")),
        MonarchRoleOption("owner", title = "Owner", enabled = false, disabledReason = "Only owners can add owners."),
    )
    PreviewFrame {
        MonarchRolePicker(roles = roles, selected = "editor", onSelect = {}, helperText = "You can change it later.")
        MonarchRolePicker(
            roles = roles,
            selected = null,
            onSelect = {},
            layout = MonarchRolePickerLayout.Compact,
            errorText = "Choose a role.",
        )
    }
}
