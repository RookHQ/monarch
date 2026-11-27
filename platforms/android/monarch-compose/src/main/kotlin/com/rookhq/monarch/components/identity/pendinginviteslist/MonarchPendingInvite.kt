package com.rookhq.monarch.components.identity.pendinginviteslist

import androidx.compose.runtime.Immutable

/**
 * One invitation that has not been accepted yet, as `MonarchPendingInvitesList`
 * shows it.
 *
 * @property id a stable id, passed to `onResend` and `onRevoke`.
 * @property email who was invited.
 * @property role the role they will get, such as "Member", already localised.
 * @property invitedBy who sent it.
 * @property sentAt when it was last sent, in epoch milliseconds (UTC).
 * @property expiresAt when it stops working, in epoch milliseconds (UTC).
 * @property expired whether it has expired. `null` works it out from [expiresAt] and the list's time.
 * @property link the invitation link; shows Copy link while the invitation has not expired.
 */
@Immutable
public data class MonarchPendingInvite(
    public val id: String,
    public val email: String,
    public val role: String? = null,
    public val invitedBy: String? = null,
    public val sentAt: Long? = null,
    public val expiresAt: Long? = null,
    public val expired: Boolean? = null,
    public val link: String? = null,
)
