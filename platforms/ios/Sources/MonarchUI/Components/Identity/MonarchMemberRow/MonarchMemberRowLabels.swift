/// Replacements for the built-in strings of ``MonarchMemberRow``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Closures receive the member's name.
public struct MonarchMemberRowLabels: Sendable {
    /// After the name of the signed-in person. Built in: "You".
    public var you: String?
    /// The badge of an invitation. Built in: "Invited".
    public var invited: String?
    /// The badge of a suspended member. Built in: "Suspended".
    public var suspended: String?
    /// The spoken presence. Built in: the presence dot's names, such as "Online".
    public var presence: (@Sendable (MonarchPresenceStatus) -> String)?
    /// The spoken name of the role picker. Built in: "Role for <name>".
    public var roleLabel: (@Sendable (String) -> String)?
    /// The role picker's choice while no role is set. Built in: "Choose a role".
    public var chooseRole: String?
    /// The resend button. Built in: "Resend invite".
    public var resend: String?
    /// The spoken name of the resend button. Built in: "Resend invite, <name>".
    public var resendLabel: (@Sendable (String) -> String)?
    /// Shown and announced after a resend. Built in: "Invite sent again."
    public var resent: String?
    /// The spoken name of the remove button of a member. Built in: "Remove <name>".
    public var remove: (@Sendable (String) -> String)?
    /// The spoken name of the remove button of an invitation. Built in: "Revoke invite for <name>".
    public var revoke: (@Sendable (String) -> String)?
    /// The confirmation title for a member. Built in: "Remove <name>?".
    public var removeTitle: (@Sendable (String) -> String)?
    /// The confirmation title for an invitation. Built in: "Revoke the invite for <name>?".
    public var revokeTitle: (@Sendable (String) -> String)?
    /// The confirmation message for a member. Built in: "They lose access straight away. You can invite them again later."
    public var removeDescription: String?
    /// The confirmation message for an invitation. Built in: "The invite link stops working. You can invite them again later."
    public var revokeDescription: String?
    /// The confirm button for a member. Built in: "Remove".
    public var removeConfirm: String?
    /// The confirm button for an invitation. Built in: "Revoke invite".
    public var revokeConfirm: String?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// Shown when an action throws an error without a description. Built in: "That did not work. Try again."
    public var failed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        you: String? = nil,
        invited: String? = nil,
        suspended: String? = nil,
        presence: (@Sendable (MonarchPresenceStatus) -> String)? = nil,
        roleLabel: (@Sendable (String) -> String)? = nil,
        chooseRole: String? = nil,
        resend: String? = nil,
        resendLabel: (@Sendable (String) -> String)? = nil,
        resent: String? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        revoke: (@Sendable (String) -> String)? = nil,
        removeTitle: (@Sendable (String) -> String)? = nil,
        revokeTitle: (@Sendable (String) -> String)? = nil,
        removeDescription: String? = nil,
        revokeDescription: String? = nil,
        removeConfirm: String? = nil,
        revokeConfirm: String? = nil,
        cancel: String? = nil,
        failed: String? = nil
    ) {
        self.you = you
        self.invited = invited
        self.suspended = suspended
        self.presence = presence
        self.roleLabel = roleLabel
        self.chooseRole = chooseRole
        self.resend = resend
        self.resendLabel = resendLabel
        self.resent = resent
        self.remove = remove
        self.revoke = revoke
        self.removeTitle = removeTitle
        self.revokeTitle = revokeTitle
        self.removeDescription = removeDescription
        self.revokeDescription = revokeDescription
        self.removeConfirm = removeConfirm
        self.revokeConfirm = revokeConfirm
        self.cancel = cancel
        self.failed = failed
    }
}
