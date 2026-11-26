import Foundation

/// One request waiting for a decision in a ``MonarchApprovalQueue``.
@_spi(Experimental) public struct MonarchApprovalQueueItem: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public let id: String
    /// The person who asked.
    public let requester: MonarchApprovalPerson
    /// A second line under the name, such as a team or an employee code.
    public let requesterDetail: String?
    /// What is asked, such as "Expense claim" or "Leave, 3 days", already localised.
    public let title: String
    /// Short facts, such as an amount or a period.
    public let summary: String?
    /// When it was submitted.
    public let submittedAt: Date?
    /// Whether it is past its deadline: adds a Past due badge.
    public let isOverdue: Bool
    /// Extra badges, such as "Resubmitted".
    public let badges: [MonarchStatusDefinition]
    /// The text the search matches. The requester's name and the title when `nil`.
    public let searchText: String?
    /// Whether the viewer can decide it. A request they can only see has no approve, send back or selection.
    public let canDecide: Bool

    /// A request.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - requester: The person who asked.
    ///   - requesterDetail: A second line under the name.
    ///   - title: What is asked, already localised.
    ///   - summary: Short facts, such as an amount.
    ///   - submittedAt: When it was submitted.
    ///   - isOverdue: Whether it is past its deadline.
    ///   - badges: Extra badges.
    ///   - searchText: The text the search matches.
    ///   - canDecide: Whether the viewer can decide it.
    public init(
        id: String,
        requester: MonarchApprovalPerson,
        requesterDetail: String? = nil,
        title: String,
        summary: String? = nil,
        submittedAt: Date? = nil,
        isOverdue: Bool = false,
        badges: [MonarchStatusDefinition] = [],
        searchText: String? = nil,
        canDecide: Bool = true
    ) {
        self.id = id
        self.requester = requester
        self.requesterDetail = requesterDetail
        self.title = title
        self.summary = summary
        self.submittedAt = submittedAt
        self.isOverdue = isOverdue
        self.badges = badges
        self.searchText = searchText
        self.canDecide = canDecide
    }
}
