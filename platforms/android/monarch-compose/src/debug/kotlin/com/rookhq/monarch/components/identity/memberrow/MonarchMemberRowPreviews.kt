package com.rookhq.monarch.components.identity.memberrow

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
