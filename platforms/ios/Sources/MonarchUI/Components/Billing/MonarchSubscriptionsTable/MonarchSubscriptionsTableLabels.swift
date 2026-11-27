/// Replacements for the built-in strings of ``MonarchSubscriptionsTable``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Status labels are replaced through the table's `registry`.
public struct MonarchSubscriptionsTableLabels: Sendable {
    /// The spoken name of the table. Built in: "Subscriptions".
    public var caption: String?
    /// The first column. Built in: "Subscription".
    public var subscription: String?
    /// Built in: "Customer".
    public var customer: String?
    /// Built in: "Status".
    public var status: String?
    /// Built in: "Price".
    public var price: String?
    /// The normalised monthly column. Built in: "Monthly".
    public var monthly: String?
    /// Built in: "Renewal".
    public var renewal: String?
    /// Shown in place of a missing plan. Built in: "Custom price".
    public var customPrice: String?
    /// Before the plan when the quantity is above 1. Built in: "<n> ×".
    public var quantity: (@Sendable (Int) -> String)?
    /// The billing period: "month", "3 months", "quarter", "one time".
    public var interval: (@Sendable (MonarchSubscriptionInterval?, Int) -> String)?
    /// After the price, with the billing period. Built in: "/ <period>".
    public var priceSuffix: (@Sendable (String) -> String)?
    /// Spoken price of a recurring charge. Built in: "<price> per <period>".
    public var spokenPrice: (@Sendable (String, String) -> String)?
    /// Spoken price of a one-time charge. Built in: "<price>, one time".
    public var spokenOneTimePrice: (@Sendable (String) -> String)?
    /// A scheduled cancellation, under the name. Built in: "cancels <date>".
    public var cancels: (@Sendable (String) -> String)?
    /// A scheduled change, under the name. Built in: "changes <date>".
    public var changes: (@Sendable (String) -> String)?
    /// Built in: "Renews <date>".
    public var renews: (@Sendable (String) -> String)?
    /// Built in: "Ends <date>".
    public var ends: (@Sendable (String) -> String)?
    /// No renewal date. Built in: "Evergreen".
    public var evergreen: String?
    /// A live subscription without a monthly figure. Built in: "Not converted".
    public var notConverted: String?
    /// The hint that explains "Not converted". Built in: "No exchange rate
    /// is available, so this is left out of the monthly total."
    public var notConvertedHint: String?
    /// A missing customer. Built in: "—".
    public var noValue: String?
    /// The empty state title. Built in: "No subscriptions".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Subscriptions appear here once they are created."
    public var emptyMessage: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        caption: String? = nil,
        subscription: String? = nil,
        customer: String? = nil,
        status: String? = nil,
        price: String? = nil,
        monthly: String? = nil,
        renewal: String? = nil,
        customPrice: String? = nil,
        quantity: (@Sendable (Int) -> String)? = nil,
        interval: (@Sendable (MonarchSubscriptionInterval?, Int) -> String)? = nil,
        priceSuffix: (@Sendable (String) -> String)? = nil,
        spokenPrice: (@Sendable (String, String) -> String)? = nil,
        spokenOneTimePrice: (@Sendable (String) -> String)? = nil,
        cancels: (@Sendable (String) -> String)? = nil,
        changes: (@Sendable (String) -> String)? = nil,
        renews: (@Sendable (String) -> String)? = nil,
        ends: (@Sendable (String) -> String)? = nil,
        evergreen: String? = nil,
        notConverted: String? = nil,
        notConvertedHint: String? = nil,
        noValue: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil
    ) {
        self.caption = caption
        self.subscription = subscription
        self.customer = customer
        self.status = status
        self.price = price
        self.monthly = monthly
        self.renewal = renewal
        self.customPrice = customPrice
        self.quantity = quantity
        self.interval = interval
        self.priceSuffix = priceSuffix
        self.spokenPrice = spokenPrice
        self.spokenOneTimePrice = spokenOneTimePrice
        self.cancels = cancels
        self.changes = changes
        self.renews = renews
        self.ends = ends
        self.evergreen = evergreen
        self.notConverted = notConverted
        self.notConvertedHint = notConvertedHint
        self.noValue = noValue
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
    }
}
