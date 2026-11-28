/// Replacements for the built-in strings of ``MonarchPendingInvitesList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Closures receive the invitation's email address.
@_spi(Experimental) public struct MonarchPendingInvitesListLabels: Sendable {
    /// The card title. Built in: "Pending invitations".
    public var title: String?
    /// The spoken name of the list. Built in: "Pending invitations".
    public var listLabel: String?
    /// Before the sender. Built in: "Invited by".
    public var invitedBy: String?
    /// Before the sent time. Built in: "Sent".
    public var sent: String?
    /// Before the expiry time. Built in: "Expires".
    public var expires: String?
    /// The badge of an expired invitation. Built in: "Expired".
    public var expired: String?
    /// The copy button. Built in: "Copy link".
    public var copyLink: String?
    /// Added to the copy button's spoken name. Built in: "for <email>".
    public var copyTarget: (@Sendable (String) -> String)?
    /// Announced after an invitation is revoked. Built in: "Invitation to <email> revoked".
    public var revoked: (@Sendable (String) -> String)?
    /// Read while loading. Built in: "Loading invitations".
    public var loading: String?
    /// The empty state title. Built in: "No pending invitations".
    public var emptyTitle: String?
    /// The error state title. Built in: "Couldn’t load invitations".
    public var errorTitle: String?
    /// The resend button. Built in: "Resend".
    public var resend: String?
    /// The revoke button. Built in: "Revoke".
    public var revoke: String?
    /// The strings of each row: Invited, Resend invite, the revoke confirmation and the failure text.
    public var row: MonarchMemberRowLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        listLabel: String? = nil,
        invitedBy: String? = nil,
        sent: String? = nil,
        expires: String? = nil,
        expired: String? = nil,
        copyLink: String? = nil,
        copyTarget: (@Sendable (String) -> String)? = nil,
        revoked: (@Sendable (String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        errorTitle: String? = nil,
        resend: String? = nil,
        revoke: String? = nil,
        row: MonarchMemberRowLabels = MonarchMemberRowLabels()
    ) {
        self.title = title
        self.listLabel = listLabel
        self.invitedBy = invitedBy
        self.sent = sent
        self.expires = expires
        self.expired = expired
        self.copyLink = copyLink
        self.copyTarget = copyTarget
        self.revoked = revoked
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.errorTitle = errorTitle
        self.resend = resend
        self.revoke = revoke
        self.row = row
    }
}
