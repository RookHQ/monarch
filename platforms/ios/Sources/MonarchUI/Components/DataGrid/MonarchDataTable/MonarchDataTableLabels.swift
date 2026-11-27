/// Replacements for the built-in strings of ``MonarchDataTable``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDataTableLabels: Sendable {
    /// The select-all checkbox. Built in: "Select all rows".
    public var selectAll: String?
    /// A row's checkbox, from the row's name. Built in: "Select Ticket 42".
    public var selectRow: (@Sendable (String) -> String)?
    /// The count of selected rows. Built in: "3 selected".
    public var selectedCount: (@Sendable (Int) -> String)?
    /// A row's actions menu, from the row's name. Built in: "Actions for Ticket 42".
    public var rowActions: (@Sendable (String) -> String)?
    /// The header of the actions column. Built in: "Actions".
    public var actionsHeader: String?
    /// The hint on a row that opens. Built in: "Opens the details".
    public var openHint: String?
    /// The sort menu in the list layout. Built in: "Sort".
    public var sort: String?
    /// The spoken state of the sorted column, ascending. Built in: "sorted ascending".
    public var sortedAscending: String?
    /// The spoken state of the sorted column, descending. Built in: "sorted descending".
    public var sortedDescending: String?
    /// Read for an empty cell. Built in: "No value".
    public var noValue: String?
    /// The visible text of an empty cell. Built in: "—". VoiceOver reads ``noValue``.
    public var emptyCell: String?
    /// The SF Symbol before the sorted column in the list layout's sort menu, ascending. Built in: "chevron.up".
    public var ascendingSymbol: String?
    /// The SF Symbol before the sorted column in the list layout's sort menu, descending. Built in: "chevron.down".
    public var descendingSymbol: String?
    /// The spoken name of the first-load placeholder. Built in: "Loading rows".
    public var loading: String?
    /// The empty state title. Built in: "Nothing here yet".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Rows appear here once they are added."
    public var emptyMessage: String?
    /// The title when filters match nothing. Built in: "No rows match these filters".
    public var filteredTitle: String?
    /// The message when filters match nothing. Built in: "Try removing a filter or searching for something else."
    public var filteredMessage: String?
    /// The title when the rows failed to load. Built in: "These rows could not be loaded".
    public var errorTitle: String?
    /// The retry button. Built in: the retry title of ``MonarchErrorState``.
    public var retry: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        selectAll: String? = nil,
        selectRow: (@Sendable (String) -> String)? = nil,
        selectedCount: (@Sendable (Int) -> String)? = nil,
        rowActions: (@Sendable (String) -> String)? = nil,
        actionsHeader: String? = nil,
        openHint: String? = nil,
        sort: String? = nil,
        sortedAscending: String? = nil,
        sortedDescending: String? = nil,
        noValue: String? = nil,
        emptyCell: String? = nil,
        ascendingSymbol: String? = nil,
        descendingSymbol: String? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        filteredTitle: String? = nil,
        filteredMessage: String? = nil,
        errorTitle: String? = nil,
        retry: String? = nil
    ) {
        self.selectAll = selectAll
        self.selectRow = selectRow
        self.selectedCount = selectedCount
        self.rowActions = rowActions
        self.actionsHeader = actionsHeader
        self.openHint = openHint
        self.sort = sort
        self.sortedAscending = sortedAscending
        self.sortedDescending = sortedDescending
        self.noValue = noValue
        self.emptyCell = emptyCell
        self.ascendingSymbol = ascendingSymbol
        self.descendingSymbol = descendingSymbol
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.filteredTitle = filteredTitle
        self.filteredMessage = filteredMessage
        self.errorTitle = errorTitle
        self.retry = retry
    }
}
