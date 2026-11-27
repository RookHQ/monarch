/// Replacements for the built-in strings of ``MonarchRenewalsPanel``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Status labels are replaced through the panel's `registry`.
@_spi(Experimental) public struct MonarchRenewalsPanelLabels: Sendable {
    /// The card title. Built in: "Renewals".
    public var title: String?
    /// The spoken name of the list. Built in: "Upcoming renewals".
    public var list: String?
    /// The name of the filter picker. Built in: "Filter renewals".
    public var filters: String?
    /// The spoken name of the totals. Built in: "Totals by currency".
    public var totals: String?
    /// The day count: "today", "in 12 days", "3 days ago".
    public var daysUntil: (@Sendable (Int) -> String)?
    /// The notice deadline, given the formatted date. Built in: "Notice by <date>".
    public var noticeBy: (@Sendable (String) -> String)?
    /// Shown for a missing date. Built in: "Unknown date".
    public var unknownDate: String?
    /// Shown for a missing amount. Built in: "No price".
    public var unpriced: String?
    /// Built in: "Auto-renews".
    public var autoRenews: String?
    /// Built in: "Manual renewal".
    public var manualRenewal: String?
    /// Built in: "No owner".
    public var noOwner: String?
    /// Built in: "No next action".
    public var noNextAction: String?
    /// Before an overdue next step. Built in: "Overdue".
    public var overdue: String?
    /// The spoken name of the urgent icon. Built in: "Due soon".
    public var dueSoon: String?
    /// The probability. Built in: "<n>% likely".
    public var probability: (@Sendable (Int) -> String)?
    /// The heading of a total, given the currency code and the count. Built in: "USD · 4 renewals".
    public var totalIn: (@Sendable (String, Int) -> String)?
    /// The estimated part of a total, given the formatted amount. Built in: "Includes <amount> estimated".
    public var estimated: (@Sendable (String) -> String)?
    /// The heading of the unpriced count. Built in: "Without a price".
    public var unpricedCount: String?
    /// The line under the unpriced count. Built in: "Not included in the totals.".
    public var unpricedHint: String?
    /// The heading of the item column of the table. Built in: "Item".
    public var columnItem: String?
    /// The heading of the renewal date column. Built in: "Renewal".
    public var columnRenewal: String?
    /// The heading of the amount column. Built in: "Amount".
    public var columnAmount: String?
    /// The heading of the status column. Built in: "Status".
    public var columnStatus: String?
    /// The row action. Built in: "Update".
    public var action: String?
    /// The spoken name of a row action, given the action and the name. Built in: "<action> <name>".
    public var actionFor: (@Sendable (String, String) -> String)?
    /// The empty state title. Built in: "No renewals coming up".
    public var emptyTitle: String?
    /// The empty state message.
    public var emptyMessage: String?
    /// The error state title. Built in: "Renewals could not be loaded".
    public var errorTitle: String?
    /// Read while loading. Built in: "Loading renewals".
    public var loading: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        list: String? = nil,
        filters: String? = nil,
        totals: String? = nil,
        daysUntil: (@Sendable (Int) -> String)? = nil,
        noticeBy: (@Sendable (String) -> String)? = nil,
        unknownDate: String? = nil,
        unpriced: String? = nil,
        autoRenews: String? = nil,
        manualRenewal: String? = nil,
        noOwner: String? = nil,
        noNextAction: String? = nil,
        overdue: String? = nil,
        dueSoon: String? = nil,
        probability: (@Sendable (Int) -> String)? = nil,
        totalIn: (@Sendable (String, Int) -> String)? = nil,
        estimated: (@Sendable (String) -> String)? = nil,
        unpricedCount: String? = nil,
        unpricedHint: String? = nil,
        columnItem: String? = nil,
        columnRenewal: String? = nil,
        columnAmount: String? = nil,
        columnStatus: String? = nil,
        action: String? = nil,
        actionFor: (@Sendable (String, String) -> String)? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil,
        loading: String? = nil
    ) {
        self.title = title
        self.list = list
        self.filters = filters
        self.totals = totals
        self.daysUntil = daysUntil
        self.noticeBy = noticeBy
        self.unknownDate = unknownDate
        self.unpriced = unpriced
        self.autoRenews = autoRenews
        self.manualRenewal = manualRenewal
        self.noOwner = noOwner
        self.noNextAction = noNextAction
        self.overdue = overdue
        self.dueSoon = dueSoon
        self.probability = probability
        self.totalIn = totalIn
        self.estimated = estimated
        self.unpricedCount = unpricedCount
        self.unpricedHint = unpricedHint
        self.columnItem = columnItem
        self.columnRenewal = columnRenewal
        self.columnAmount = columnAmount
        self.columnStatus = columnStatus
        self.action = action
        self.actionFor = actionFor
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
        self.loading = loading
    }
}
