@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvite
import com.rookhq.monarch.components.identity.pendinginviteslist.MonarchPendingInvitesList
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardState
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of PendingInvitesList. */
object MonarchPendingInvitesListSection : CataloguePage {
    override val title = "PendingInvitesList"

    private const val DAY = 86_400_000L
    private const val WORK_MILLIS = 800L

    private fun invites(now: Long) = listOf(
        MonarchPendingInvite(
            id = "1",
            email = "ana@example.com",
            role = "Member",
            invitedBy = "Sam Rivera",
            sentAt = now - 2 * DAY,
            expiresAt = now + 5 * DAY,
            link = "https://example.com/invite/ana",
        ),
        MonarchPendingInvite(
            id = "2",
            email = "ben@example.com",
            role = "Admin",
            invitedBy = "Sam Rivera",
            sentAt = now - 9 * DAY,
            expiresAt = now - 2 * DAY,
            link = "https://example.com/invite/ben",
        ),
    )

    @Composable
    override fun Content() {
        val now = rememberSaveable { System.currentTimeMillis() }
        var revoked by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var failing by rememberSaveable { mutableStateOf(false) }
        MonarchSwitchRow(checked = failing, onCheckedChange = { failing = it }, label = "Make actions fail")
        CatalogueCaption("Resend, copy the link or revoke; the expired invitation has no link")
        MonarchPendingInvitesList(
            invites = invites(now).filterNot { it.id in revoked },
            now = now,
            onResend = {
                delay(WORK_MILLIS)
                check(!failing) { "The mail server did not answer." }
            },
            onRevoke = { id ->
                delay(WORK_MILLIS)
                check(!failing) { "The invitation could not be revoked." }
                revoked = revoked + id
            },
        )
        CatalogueCaption("Loading and error")
        MonarchPendingInvitesList(invites = emptyList(), state = MonarchSectionCardState.Loading)
        MonarchPendingInvitesList(invites = emptyList(), state = MonarchSectionCardState.Error, onRetry = {})
    }
}
