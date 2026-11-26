import Foundation

/// One step of a ``MonarchApprovalTimeline``.
public struct MonarchApprovalTimelineStep: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public let id: String
    /// What the step is, such as "Reporting manager" or "Finance check", already localised.
    public let label: String
    /// Where the step stands.
    public let status: MonarchApprovalStepStatus
    /// Who decided or submitted. Shown with the time on decided steps.
    public let actor: MonarchApprovalPerson?
    /// Who can decide a pending or waiting step.
    public let approvers: [MonarchApprovalPerson]
    /// The person an approver is standing in for.
    public let delegatedFrom: MonarchApprovalPerson?
    /// When it was decided or submitted.
    public let date: Date?
    /// The decider's comment, shown as a quote.
    public let comment: String?
    /// Why the step was skipped or cancelled, or another short note.
    public let note: String?

    /// A step.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - label: What the step is, already localised.
    ///   - status: Where the step stands.
    ///   - actor: Who decided or submitted.
    ///   - approvers: Who can decide a pending or waiting step.
    ///   - delegatedFrom: The person an approver is standing in for.
    ///   - date: When it was decided or submitted.
    ///   - comment: The decider's comment.
    ///   - note: A short note, such as why it was skipped.
    public init(
        id: String,
        label: String,
        status: MonarchApprovalStepStatus,
        actor: MonarchApprovalPerson? = nil,
        approvers: [MonarchApprovalPerson] = [],
        delegatedFrom: MonarchApprovalPerson? = nil,
        date: Date? = nil,
        comment: String? = nil,
        note: String? = nil
    ) {
        self.id = id
        self.label = label
        self.status = status
        self.actor = actor
        self.approvers = approvers
        self.delegatedFrom = delegatedFrom
        self.date = date
        self.comment = comment
        self.note = note
    }
}
