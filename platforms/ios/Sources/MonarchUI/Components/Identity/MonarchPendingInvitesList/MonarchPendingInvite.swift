import Foundation

/// One invitation that has not been accepted yet, shown by ``MonarchPendingInvitesList``.
@_spi(Experimental) public struct MonarchPendingInvite: Identifiable, Sendable, Hashable {
    /// The id passed to the resend and revoke handlers.
    public let id: String
    /// The email address the invitation was sent to.
    public let email: String
    /// The role the person gets on joining, such as "Member", already localised.
    public let role: String?
    /// Who sent the invitation.
    public let invitedBy: String?
    /// When the invitation was last sent.
    public let sentAt: Date?
    /// When the invitation stops working.
    public let expiresAt: Date?
    /// Whether the invitation has expired. `nil` works it out from `expiresAt`.
    public let isExpired: Bool?
    /// The invitation link to copy. Without it the row has no Copy link.
    public let link: String?

    /// An invitation.
    ///
    /// - Parameters:
    ///   - id: The id passed to the resend and revoke handlers.
    ///   - email: The email address the invitation was sent to.
    ///   - role: The role the person gets on joining, already localised.
    ///   - invitedBy: Who sent the invitation.
    ///   - sentAt: When the invitation was last sent.
    ///   - expiresAt: When the invitation stops working.
    ///   - isExpired: Whether the invitation has expired. `nil` works it out from `expiresAt`.
    ///   - link: The invitation link to copy. Without it the row has no Copy link.
    public init(
        id: String,
        email: String,
        role: String? = nil,
        invitedBy: String? = nil,
        sentAt: Date? = nil,
        expiresAt: Date? = nil,
        isExpired: Bool? = nil,
        link: String? = nil
    ) {
        self.id = id
        self.email = email
        self.role = role
        self.invitedBy = invitedBy
        self.sentAt = sentAt
        self.expiresAt = expiresAt
        self.isExpired = isExpired
        self.link = link
    }
}
