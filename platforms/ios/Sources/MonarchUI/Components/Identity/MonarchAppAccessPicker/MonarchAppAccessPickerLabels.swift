/// Replacements for the built-in strings of ``MonarchAppAccessPicker`` and
/// ``MonarchAppAccessSummary``. A property that is `nil` keeps the built-in
/// English string.
@_spi(Experimental) public struct MonarchAppAccessPickerLabels: Sendable {
    /// The group's label. Built in: "App access".
    public var label: String?
    /// The bulk button while not everything is selected. Built in: "Select all".
    public var selectAll: String?
    /// The bulk button while everything is selected. Built in: "Clear all".
    public var clearAll: String?
    /// The selection count, from the selected and total numbers. Built in: "2 of 5 selected".
    public var count: (@Sendable (Int, Int) -> String)?
    /// Read while the apps load. Built in: "Loading apps".
    public var loading: String?
    /// Shown when there are no apps. Built in: "No apps are available yet."
    public var empty: String?
    /// The summary when nothing is granted. Built in: "No app access".
    public var none: String?
    /// The summary's visible count of hidden apps. Built in: "+3".
    public var moreCount: (@Sendable (Int) -> String)?
    /// The summary's spoken end, from the number and names of hidden apps. Built in: "and 3 more: Docs, Mail, Sheets".
    public var more: (@Sendable (Int, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        selectAll: String? = nil,
        clearAll: String? = nil,
        count: (@Sendable (Int, Int) -> String)? = nil,
        loading: String? = nil,
        empty: String? = nil,
        none: String? = nil,
        moreCount: (@Sendable (Int) -> String)? = nil,
        more: (@Sendable (Int, String) -> String)? = nil
    ) {
        self.label = label
        self.selectAll = selectAll
        self.clearAll = clearAll
        self.count = count
        self.loading = loading
        self.empty = empty
        self.none = none
        self.moreCount = moreCount
        self.more = more
    }
}
