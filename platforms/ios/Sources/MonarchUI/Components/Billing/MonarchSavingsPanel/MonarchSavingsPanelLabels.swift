/// Replacements for the built-in strings of ``MonarchSavingsPanel``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Category and decision badge labels come from the panel's `registry`.
@_spi(Experimental) public struct MonarchSavingsPanelLabels: Sendable {
    /// The card title. Built in: "Savings".
    public var title: String?
    /// The headline, given the unused count, their formatted renewal cost (or `nil`) and the unpriced count.
    /// Built in: "9 unused items cost $1,200.00 to renew (2 without a price).".
    public var headline: (@Sendable (Int, String?, Int) -> String)?
    /// The spoken name of the categories. Built in: "Unused by reason".
    public var categories: String?
    /// Read before the decision counts. Built in: "Decisions".
    public var decisions: String?
    /// The potential savings, given the formatted amounts. Built in: "Letting the marked items lapse saves <amount>.".
    public var potentialSavings: (@Sendable (String) -> String)?
    /// The spoken name of the item list. Built in: "Unused items".
    public var items: String?
    /// After the shortened list, given how many are hidden. Built in: "and <n> more".
    public var more: (@Sendable (Int) -> String)?
    /// Shown for an item without a price. Built in: "No price".
    public var unpriced: String?
    /// The empty state title. Built in: "Nothing unused".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Everything renewing is in use.".
    public var emptyMessage: String?
    /// The error state title. Built in: "Savings could not be loaded".
    public var errorTitle: String?
    /// Read while loading. Built in: "Loading savings".
    public var loading: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        headline: (@Sendable (Int, String?, Int) -> String)? = nil,
        categories: String? = nil,
        decisions: String? = nil,
        potentialSavings: (@Sendable (String) -> String)? = nil,
        items: String? = nil,
        more: (@Sendable (Int) -> String)? = nil,
        unpriced: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil,
        loading: String? = nil
    ) {
        self.title = title
        self.headline = headline
        self.categories = categories
        self.decisions = decisions
        self.potentialSavings = potentialSavings
        self.items = items
        self.more = more
        self.unpriced = unpriced
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
        self.loading = loading
    }
}
