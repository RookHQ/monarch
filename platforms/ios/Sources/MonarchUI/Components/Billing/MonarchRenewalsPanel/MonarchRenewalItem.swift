import Foundation

/// One renewal in a ``MonarchRenewalsPanel``: a subscription, contract,
/// licence or domain reaching the end of its term.
@_spi(Experimental) public struct MonarchRenewalItem: Identifiable, Sendable, Hashable {
    /// A stable id, passed back to the handlers.
    public let id: String
    /// What renews, such as the agreement or asset name.
    public let name: String
    /// The status key, looked up in the panel's statuses (open, in_progress, renewed, lost, void by default).
    public let status: String
    /// The renewal or expiry date. `nil` when it is unknown.
    public let renewalDate: Date?
    /// A secondary line under the name, such as the type and the customer.
    public let description: String?
    /// Whole days until the renewal, negative when past, when the server works it out.
    /// Without it the panel counts the days from `now`.
    public let daysUntil: Int?
    /// The last day to give notice, shown in place of the day count.
    public let noticeDeadline: Date?
    /// What the renewal costs or is worth, in major units. `nil` when it has no price.
    public let amount: Decimal?
    /// The ISO 4217 code of `amount`.
    public let currencyCode: String?
    /// A short note under the amount, such as "per year".
    public let amountNote: String?
    /// Whether it renews by itself. `nil` hides the note.
    public let autoRenews: Bool?
    /// The likelihood of renewing, from 0 to 100.
    public let probability: Int?
    /// Who owns the renewal.
    public let owner: String?
    /// The next step, such as "Send a quote".
    public let nextAction: String?
    /// When the next step is due.
    public let nextActionDate: Date?
    /// Whether the next step is overdue. It is shown with an "Overdue" prefix and icon.
    public let isNextActionOverdue: Bool
    /// How a closed renewal ended, such as "Renewed for two years".
    public let outcome: String?

    /// A renewal.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: What renews, already localised.
    ///   - status: The status key.
    ///   - renewalDate: The renewal or expiry date, or `nil` when unknown.
    ///   - description: A secondary line under the name.
    ///   - daysUntil: Whole days until the renewal, when the server works it out.
    ///   - noticeDeadline: The last day to give notice.
    ///   - amount: What the renewal costs, in major units, or `nil` when it has no price.
    ///   - currencyCode: The ISO 4217 code of `amount`.
    ///   - amountNote: A short note under the amount.
    ///   - autoRenews: Whether it renews by itself.
    ///   - probability: The likelihood of renewing, from 0 to 100.
    ///   - owner: Who owns the renewal.
    ///   - nextAction: The next step.
    ///   - nextActionDate: When the next step is due.
    ///   - isNextActionOverdue: Whether the next step is overdue.
    ///   - outcome: How a closed renewal ended.
    public init(
        id: String,
        name: String,
        status: String,
        renewalDate: Date?,
        description: String? = nil,
        daysUntil: Int? = nil,
        noticeDeadline: Date? = nil,
        amount: Decimal? = nil,
        currencyCode: String? = nil,
        amountNote: String? = nil,
        autoRenews: Bool? = nil,
        probability: Int? = nil,
        owner: String? = nil,
        nextAction: String? = nil,
        nextActionDate: Date? = nil,
        isNextActionOverdue: Bool = false,
        outcome: String? = nil
    ) {
        self.id = id
        self.name = name
        self.status = status
        self.renewalDate = renewalDate
        self.description = description
        self.daysUntil = daysUntil
        self.noticeDeadline = noticeDeadline
        self.amount = amount
        self.currencyCode = currencyCode
        self.amountNote = amountNote
        self.autoRenews = autoRenews
        self.probability = probability
        self.owner = owner
        self.nextAction = nextAction
        self.nextActionDate = nextActionDate
        self.isNextActionOverdue = isNextActionOverdue
        self.outcome = outcome
    }
}
