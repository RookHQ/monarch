package com.rookhq.monarch.components.identity.pendinginviteslist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.identity.memberrow.MonarchMemberRowLabels

/**
 * Replacements for the strings `MonarchPendingInvitesList` supplies. Each
 * `null` keeps the built-in English string `monarch_pendinginviteslist_<key>`.
 *
 * @property title the card title. Default "Pending invitations".
 * @property listLabel the TalkBack name of the rows. Default "Pending invitations".
 * @property invitedBy the sender part of the detail line, from the sender. Default "Invited by Sam Rivera".
 * @property sent the sent part of the detail line, from a relative time. Default "Sent 2 days ago".
 * @property expires the expiry part of the detail line, from a relative time. Default "Expires in 5 days".
 * @property expired the status badge of an expired invitation. Default "Expired".
 * @property copyLink the visible text of the copy button. Default "Copy link".
 * @property copyTarget what the copy button copies, from the email, added to its spoken name. Default
 * "for ana@example.com", read as "Copy link for ana@example.com".
 * @property revoked announced after a revoke, from the email. Default "Invitation to ana@example.com revoked".
 * @property loading read while the list loads. Default "Loading invitations".
 * @property emptyTitle the empty state. Default "No pending invitations".
 * @property errorTitle the error state. Default "Couldn’t load invitations".
 * @property row the strings of each `MonarchMemberRow`: resend, revoke, the confirmation and the failure.
 */
@Immutable
public class MonarchPendingInvitesListLabels(
    public val title: String? = null,
    public val listLabel: String? = null,
    public val invitedBy: ((name: String) -> String)? = null,
    public val sent: ((relativeTime: String) -> String)? = null,
    public val expires: ((relativeTime: String) -> String)? = null,
    public val expired: String? = null,
    public val copyLink: String? = null,
    public val copyTarget: ((email: String) -> String)? = null,
    public val revoked: ((email: String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val errorTitle: String? = null,
    public val row: MonarchMemberRowLabels = MonarchMemberRowLabels(),
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchPendingInvitesListLabels && other.title == title && other.listLabel == listLabel &&
            other.invitedBy == invitedBy && other.sent == sent && other.expires == expires &&
            other.expired == expired && other.copyLink == copyLink && other.copyTarget == copyTarget &&
            other.revoked == revoked && other.loading == loading && other.emptyTitle == emptyTitle &&
            other.errorTitle == errorTitle && other.row == row

    override fun hashCode(): Int = listOf(
        title, listLabel, invitedBy, sent, expires, expired, copyLink, copyTarget, revoked, loading, emptyTitle,
        errorTitle, row,
    ).hashCode()
}
