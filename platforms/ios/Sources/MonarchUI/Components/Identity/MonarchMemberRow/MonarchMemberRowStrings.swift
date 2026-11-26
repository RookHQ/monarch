/// Built-in strings of the member row, from the `MonarchMemberRow` strings table.
enum MonarchMemberRowStrings {
    private static let table = "MonarchMemberRow"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.memberRow.\(key)", table: table)
    }

    private static func string(_ key: String, _ name: String) -> String {
        MonarchStrings.string("monarch.memberRow.\(key)", table: table, name)
    }

    /// After the signed-in person's name.
    static var you: String { string("you") }
    /// The badge of an invitation.
    static var invited: String { string("invited") }
    /// The badge of a suspended member.
    static var suspended: String { string("suspended") }
    /// The spoken name of the role picker.
    static func roleLabel(_ name: String) -> String { string("roleLabel", name) }
    /// The role picker's choice while no role is set.
    static var chooseRole: String { string("chooseRole") }
    /// The resend button.
    static var resend: String { string("resend") }
    /// The spoken name of the resend button.
    static func resendLabel(_ name: String) -> String { string("resendLabel", name) }
    /// After a resend.
    static var resent: String { string("resent") }
    /// The spoken name of the remove button of a member.
    static func remove(_ name: String) -> String { string("remove", name) }
    /// The spoken name of the remove button of an invitation.
    static func revoke(_ name: String) -> String { string("revoke", name) }
    /// The confirmation title for a member.
    static func removeTitle(_ name: String) -> String { string("removeTitle", name) }
    /// The confirmation title for an invitation.
    static func revokeTitle(_ name: String) -> String { string("revokeTitle", name) }
    /// The confirmation message for a member.
    static var removeDescription: String { string("removeDescription") }
    /// The confirmation message for an invitation.
    static var revokeDescription: String { string("revokeDescription") }
    /// The confirm button for a member.
    static var removeConfirm: String { string("removeConfirm") }
    /// The confirm button for an invitation.
    static var revokeConfirm: String { string("revokeConfirm") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// A failure without a description.
    static var failed: String { string("failed") }
}
