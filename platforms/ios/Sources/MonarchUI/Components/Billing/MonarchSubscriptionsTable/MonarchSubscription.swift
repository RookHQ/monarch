import Foundation

/// One recurring agreement, a row of ``MonarchSubscriptionsTable``.
public struct MonarchSubscription: Sendable, Hashable, Identifiable {
    /// A stable id.
    public let id: String
    /// The subscription's name, shown first.
    public let name: String
    /// The plan or product, such as "Standard plan". `nil` shows "Custom price".
    public let plan: String?
    /// The number of seats or units, shown as "3 ×" before the plan when above 1.
    public let quantity: Int
    /// The customer it belongs to.
    public let customer: String?
    /// The status key, such as `active` or `past_due`, looked up in the
    /// table's statuses.
    public let status: String
    /// The price per billing period in major units, or `nil` for none.
    public let amount: Decimal?
    /// The ISO 4217 code of `amount`.
    public let currencyCode: String
    /// The billing period unit; `nil` is a one-time charge.
    public let interval: MonarchSubscriptionInterval?
    /// The number of intervals per billing period, such as 3 months.
    public let intervalCount: Int
    /// The price normalised to one month; `nil` shows a dash.
    public let monthly: MonarchSubscriptionMonthly?
    /// The next renewal or end date; `nil` has no end ("Evergreen").
    public let renewalDate: Date?
    /// Whether it renews on `renewalDate` (true) or ends on it (false).
    public let renewsAutomatically: Bool
    /// A scheduled cancellation date, noted under the name.
    public let cancelsAt: Date?
    /// A scheduled plan or price change date, noted under the name.
    public let changesAt: Date?

    /// A subscription.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The subscription's name.
    ///   - plan: The plan or product. `nil` shows "Custom price".
    ///   - quantity: The number of seats or units.
    ///   - customer: The customer it belongs to.
    ///   - status: The status key.
    ///   - amount: The price per billing period in major units.
    ///   - currencyCode: The ISO 4217 code of `amount`.
    ///   - interval: The billing period unit; `nil` is a one-time charge.
    ///   - intervalCount: The number of intervals per billing period.
    ///   - monthly: The price normalised to one month.
    ///   - renewalDate: The next renewal or end date.
    ///   - renewsAutomatically: Whether it renews on `renewalDate` or ends on it.
    ///   - cancelsAt: A scheduled cancellation date.
    ///   - changesAt: A scheduled plan or price change date.
    public init(
        _ id: String,
        name: String,
        plan: String? = nil,
        quantity: Int = 1,
        customer: String? = nil,
        status: String,
        amount: Decimal?,
        currencyCode: String,
        interval: MonarchSubscriptionInterval? = .month,
        intervalCount: Int = 1,
        monthly: MonarchSubscriptionMonthly? = nil,
        renewalDate: Date? = nil,
        renewsAutomatically: Bool = true,
        cancelsAt: Date? = nil,
        changesAt: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.plan = plan
        self.quantity = quantity
        self.customer = customer
        self.status = status
        self.amount = amount
        self.currencyCode = currencyCode
        self.interval = interval
        self.intervalCount = intervalCount
        self.monthly = monthly
        self.renewalDate = renewalDate
        self.renewsAutomatically = renewsAutomatically
        self.cancelsAt = cancelsAt
        self.changesAt = changesAt
    }
}
