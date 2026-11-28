package dev.monarchui.components.identity.memberrow

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.components.identity.rolepicker.MonarchRoleOption
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMemberRowPreview() {
    val roles = listOf(MonarchRoleOption("admin", "Admin"), MonarchRoleOption("member", "Member"))
    PreviewFrame {
        MonarchMemberRow(
            name = "Priya Raman",
            email = "priya@example.com",
            presence = MonarchPresenceStatus.Online,
            role = "admin",
            roles = roles,
            self = true,
        )
        MonarchMemberRow(
            name = "Jonas Berg",
            email = "jonas@example.com",
            role = "member",
            roles = roles,
            badges = listOf("Guest"),
            onRoleChange = {},
            onRemove = {},
        )
        MonarchMemberRow(
            name = "ana@example.com",
            meta = "Invite expires in 3 days",
            status = MonarchMemberRowStatus.Invited,
            onResend = {},
            onRemove = {},
        )
    }
}
