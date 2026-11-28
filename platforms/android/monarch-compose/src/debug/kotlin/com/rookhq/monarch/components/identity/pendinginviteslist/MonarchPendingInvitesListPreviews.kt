@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity.pendinginviteslist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val Now = 1_793_750_400_000L
private const val Day = 86_400_000L

@MonarchPreviews
@Composable
internal fun MonarchPendingInvitesListPreview() {
    PreviewFrame {
        MonarchPendingInvitesList(
            invites = listOf(
                MonarchPendingInvite(
                    id = "1",
                    email = "ana@example.com",
                    role = "Member",
                    invitedBy = "Sam Rivera",
                    sentAt = Now - 2 * Day,
                    expiresAt = Now + 5 * Day,
                    link = "https://example.com/invite/1",
                ),
                MonarchPendingInvite(id = "2", email = "ben@example.com", sentAt = Now - 9 * Day, expired = true),
            ),
            now = Now,
            onResend = {},
            onRevoke = {},
        )
        MonarchPendingInvitesList(invites = emptyList(), now = Now)
        MonarchPendingInvitesList(invites = emptyList(), state = MonarchSectionCardState.Error, onRetry = {})
    }
}
