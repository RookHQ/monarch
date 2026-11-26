/// Where a member of a ``MonarchMemberRow`` is in their lifecycle.
public enum MonarchMemberRowStatus: Sendable, Hashable, CaseIterable {
    /// A member with access. No badge is shown.
    case active
    /// Invited and not joined yet: an Invited badge, Resend invite, and Remove becomes Revoke invite.
    case invited
    /// Access paused: a Suspended badge and a muted name.
    case suspended
}
