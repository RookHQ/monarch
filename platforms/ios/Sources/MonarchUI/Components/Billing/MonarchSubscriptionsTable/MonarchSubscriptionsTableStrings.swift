/// Built-in strings of the subscriptions table, from the `MonarchSubscriptionsTable` strings table.
enum MonarchSubscriptionsTableStrings {
    private static let table = "MonarchSubscriptionsTable"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.subscriptionsTable.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The spoken name of the table.
    static var caption: String { string("caption") }
    /// The first column.
    static var subscription: String { string("subscription") }
    /// The customer column.
    static var customer: String { string("customer") }
    /// The status column.
    static var status: String { string("status") }
    /// The price column.
    static var price: String { string("price") }
    /// The monthly column.
    static var monthly: String { string("monthly") }
    /// The renewal column.
    static var renewal: String { string("renewal") }
    /// In place of a missing plan.
    static var customPrice: String { string("customPrice") }
    /// Before the plan.
    static func quantity(_ count: Int) -> String { string("quantity", count) }
    /// One unit of a billing period.
    static func interval(_ interval: MonarchSubscriptionInterval) -> String { string("interval.\(interval.rawValue)") }
    /// Several units of a billing period.
    static func intervals(_ interval: MonarchSubscriptionInterval, count: Int) -> String {
        string("intervals.\(interval.rawValue)", count)
    }
    /// Three months.
    static var quarter: String { string("quarter") }
    /// A one-time charge.
    static var oneTime: String { string("oneTime") }
    /// After the price.
    static func priceSuffix(_ period: String) -> String { string("priceSuffix", period) }
    /// The spoken price of a recurring charge.
    static func spokenPrice(_ price: String, _ period: String) -> String { string("spokenPrice", price, period) }
    /// The spoken price of a one-time charge.
    static func spokenOneTimePrice(_ price: String) -> String { string("spokenOneTimePrice", price) }
    /// A scheduled cancellation.
    static func cancels(_ date: String) -> String { string("cancels", date) }
    /// A scheduled change.
    static func changes(_ date: String) -> String { string("changes", date) }
    /// The next renewal.
    static func renews(_ date: String) -> String { string("renews", date) }
    /// The end date.
    static func ends(_ date: String) -> String { string("ends", date) }
    /// No renewal date.
    static var evergreen: String { string("evergreen") }
    /// A live subscription without a monthly figure.
    static var notConverted: String { string("notConverted") }
    /// Explains "Not converted".
    static var notConvertedHint: String { string("notConvertedHint") }
    /// A missing value.
    static var noValue: String { string("noValue") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The label of a built-in subscription status.
    static func status(_ key: String) -> String { string("status.\(key)") }
}
