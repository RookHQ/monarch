import Foundation

/// Who an issue is assigned to, for ``MonarchIssueCard``.
@_spi(Experimental) public enum MonarchIssueCardAssignee: Sendable, Hashable {
    /// A person, shown as an avatar.
    case person(_ name: String, avatarURL: URL? = nil)
    /// Nobody: an empty, dashed avatar slot read as "Unassigned".
    case unassigned
}
