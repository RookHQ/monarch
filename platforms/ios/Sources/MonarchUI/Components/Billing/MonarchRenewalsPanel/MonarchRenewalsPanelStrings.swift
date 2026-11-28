/// Built-in strings of the renewals panel, from the `MonarchRenewalsPanel` strings table.
enum MonarchRenewalsPanelStrings {
    private static let table = "MonarchRenewalsPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.renewalsPanel.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.renewalsPanel.\(key)", table: table, argument)
    }

    private static func string(_ key: String, count: Int) -> String {
        MonarchStrings.string("monarch.renewalsPanel.\(key).\(count == 1 ? "one" : "other")", table: table, count)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The spoken name of the list.
    static var list: String { string("list") }
    /// The name of the filter picker.
    static var filters: String { string("filters") }
    /// The spoken name of the totals.
    static var totals: String { string("totals") }
    /// The day count.
    static func daysUntil(_ days: Int) -> String {
        if days == 0 { return string("today") }
        return days > 0 ? string("inDays", count: days) : string("daysAgo", count: -days)
    }
    /// The notice deadline.
    static func noticeBy(_ date: String) -> String { string("noticeBy", date) }
    /// A missing date.
    static var unknownDate: String { string("unknownDate") }
    /// The heading of the item column.
    static var columnItem: String { string("columnItem") }
    /// The heading of the renewal column.
    static var columnRenewal: String { string("columnRenewal") }
    /// The heading of the amount column.
    static var columnAmount: String { string("columnAmount") }
    /// The heading of the status column.
    static var columnStatus: String { string("columnStatus") }
    /// A missing amount.
    static var unpriced: String { string("unpriced") }
    /// Renews by itself.
    static var autoRenews: String { string("autoRenews") }
    /// Renews by hand.
    static var manualRenewal: String { string("manualRenewal") }
    /// No owner.
    static var noOwner: String { string("noOwner") }
    /// No next step.
    static var noNextAction: String { string("noNextAction") }
    /// Before an overdue next step.
    static var overdue: String { string("overdue") }
    /// The spoken name of the urgent icon.
    static var dueSoon: String { string("dueSoon") }
    /// The probability.
    static func probability(_ percent: Int) -> String {
        MonarchStrings.string("monarch.renewalsPanel.probability", table: table, percent)
    }
    /// The heading of a total.
    static func totalIn(_ currency: String, _ count: Int) -> String {
        MonarchStrings.string(
            "monarch.renewalsPanel.totalIn.\(count == 1 ? "one" : "other")",
            table: table,
            currency,
            count
        )
    }
    /// The estimated part of a total.
    static func estimated(_ amount: String) -> String { string("estimated", amount) }
    /// The heading of the unpriced count.
    static var unpricedCount: String { string("unpricedCount") }
    /// The line under the unpriced count.
    static var unpricedHint: String { string("unpricedHint") }
    /// The row action.
    static var action: String { string("action") }
    /// The spoken name of a row action.
    static func actionFor(_ action: String, _ name: String) -> String {
        MonarchStrings.string("monarch.renewalsPanel.actionFor", table: table, action, name)
    }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The built-in status labels.
    static func status(_ key: String) -> String { string("status.\(key)") }
}
