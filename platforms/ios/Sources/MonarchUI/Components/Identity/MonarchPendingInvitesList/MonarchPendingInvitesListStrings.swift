/// Built-in strings of the pending invitations list, from the `MonarchPendingInvitesList` strings table.
enum MonarchPendingInvitesListStrings {
    private static let table = "MonarchPendingInvitesList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.pendingInvitesList.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.pendingInvitesList.\(key)", table: table, argument)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The spoken name of the list.
    static var listLabel: String { string("listLabel") }
    /// Before the sender.
    static var invitedBy: String { string("invitedBy") }
    /// Before the sent time.
    static var sent: String { string("sent") }
    /// Before the expiry time.
    static var expires: String { string("expires") }
    /// The badge of an expired invitation.
    static var expired: String { string("expired") }
    /// The copy button.
    static var copyLink: String { string("copyLink") }
    /// Added to the copy button's spoken name.
    static func copyTarget(_ email: String) -> String { string("copyTarget", email) }
    /// Announced after a revoke.
    static func revoked(_ email: String) -> String { string("revoked", email) }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The resend button.
    static var resend: String { string("resend") }
    /// The revoke button.
    static var revoke: String { string("revoke") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
}
