package dev.monarchui.components.identity.memberrow

/** Where a member stands, for `MonarchMemberRow`. */
public enum class MonarchMemberRowStatus {
    /** A member with access. No status badge. */
    Active,

    /** An invitation not yet accepted: an info badge, Resend invite, and Revoke invite instead of Remove. */
    Invited,

    /** A member whose access is paused: a warning badge and the name in the muted colour. */
    Suspended,
}
