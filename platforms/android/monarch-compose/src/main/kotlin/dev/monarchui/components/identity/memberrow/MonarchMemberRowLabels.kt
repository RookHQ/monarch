package dev.monarchui.components.identity.memberrow

import androidx.compose.runtime.Immutable
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus

/**
 * Replacements for the built-in strings of a `MonarchMemberRow`. Every `null`
 * keeps the built-in string `monarch_memberrow_<key>`. Functions receive the
 * member's name.
 *
 * @property you the marker of the signed-in person's own row, "You".
 * @property invited the badge of an invitation, "Invited".
 * @property suspended the badge of a suspended member, "Suspended".
 * @property presence the name of a presence; `null` uses the strings of `MonarchPresenceDot`.
 * @property roleLabel the label of the role picker: "Role for Priya Raman".
 * @property chooseRole the role picker's text while no role is chosen, "Choose a role".
 * @property resend the visible text of the resend button, "Resend invite".
 * @property resendLabel the name TalkBack reads for the resend button: "Resend invite, Priya Raman".
 * @property resent shown and announced after the invitation was sent again, "Invite sent again.".
 * @property remove the name TalkBack reads for the remove button of a member: "Remove Priya Raman".
 * @property revoke the name TalkBack reads for the remove button of an invitation: "Revoke invite for Priya Raman".
 * @property removeTitle the title of the remove confirmation: "Remove Priya Raman?".
 * @property revokeTitle the title of the revoke confirmation: "Revoke the invite for Priya Raman?".
 * @property removeDescription the text of the remove confirmation.
 * @property revokeDescription the text of the revoke confirmation.
 * @property removeConfirm the remove button and the confirming button, "Remove".
 * @property revokeConfirm the revoke button and the confirming button, "Revoke invite".
 * @property cancel the cancelling button of the confirmation, "Cancel".
 * @property failed shown when an action fails without a message, "That did not work. Try again.".
 */
@Immutable
public class MonarchMemberRowLabels(
    public val you: String? = null,
    public val invited: String? = null,
    public val suspended: String? = null,
    public val presence: ((MonarchPresenceStatus) -> String)? = null,
    public val roleLabel: ((String) -> String)? = null,
    public val chooseRole: String? = null,
    public val resend: String? = null,
    public val resendLabel: ((String) -> String)? = null,
    public val resent: String? = null,
    public val remove: ((String) -> String)? = null,
    public val revoke: ((String) -> String)? = null,
    public val removeTitle: ((String) -> String)? = null,
    public val revokeTitle: ((String) -> String)? = null,
    public val removeDescription: String? = null,
    public val revokeDescription: String? = null,
    public val removeConfirm: String? = null,
    public val revokeConfirm: String? = null,
    public val cancel: String? = null,
    public val failed: String? = null,
) {
    /**
     * A copy with the given strings replaced and the others kept, so an app or
     * a composite component can change a few labels without restating all.
     */
    @Suppress("LongParameterList")
    public fun copy(
        you: String? = this.you,
        invited: String? = this.invited,
        suspended: String? = this.suspended,
        presence: ((MonarchPresenceStatus) -> String)? = this.presence,
        roleLabel: ((String) -> String)? = this.roleLabel,
        chooseRole: String? = this.chooseRole,
        resend: String? = this.resend,
        resendLabel: ((String) -> String)? = this.resendLabel,
        resent: String? = this.resent,
        remove: ((String) -> String)? = this.remove,
        revoke: ((String) -> String)? = this.revoke,
        removeTitle: ((String) -> String)? = this.removeTitle,
        revokeTitle: ((String) -> String)? = this.revokeTitle,
        removeDescription: String? = this.removeDescription,
        revokeDescription: String? = this.revokeDescription,
        removeConfirm: String? = this.removeConfirm,
        revokeConfirm: String? = this.revokeConfirm,
        cancel: String? = this.cancel,
        failed: String? = this.failed,
    ): MonarchMemberRowLabels = MonarchMemberRowLabels(
        you = you,
        invited = invited,
        suspended = suspended,
        presence = presence,
        roleLabel = roleLabel,
        chooseRole = chooseRole,
        resend = resend,
        resendLabel = resendLabel,
        resent = resent,
        remove = remove,
        revoke = revoke,
        removeTitle = removeTitle,
        revokeTitle = revokeTitle,
        removeDescription = removeDescription,
        revokeDescription = revokeDescription,
        removeConfirm = removeConfirm,
        revokeConfirm = revokeConfirm,
        cancel = cancel,
        failed = failed,
    )
}
