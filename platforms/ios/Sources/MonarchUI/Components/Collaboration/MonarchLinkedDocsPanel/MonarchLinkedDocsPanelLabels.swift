/// Replacements for the built-in strings of ``MonarchLinkedDocsPanel``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchLinkedDocsPanelLabels: Sendable {
    /// The card title. Built in: "Docs".
    public var title: String?
    /// The menu item and picker title that links a doc. Built in: "Link doc".
    public var linkDoc: String?
    /// The menu item and form title that adds a link. Built in: "Add link".
    public var addLink: String?
    /// The picker button that creates a doc. Built in: "New doc".
    public var newDoc: String?
    /// The search prompt of the picker. Built in: "Search docs".
    public var searchDocs: String?
    /// Shown when no doc matches. Built in: "No docs match".
    public var noDocs: String?
    /// The empty text when linking is possible.
    public var empty: String?
    /// The empty text of a read-only panel. Built in: "Nothing linked yet.".
    public var emptyReadOnly: String?
    /// Read while loading. Built in: "Loading linked docs".
    public var loading: String?
    /// The name of the add menu. Built in: "Add".
    public var add: String?
    /// The name of the space filter. Built in: "Filter by space".
    public var filterBySpace: String?
    /// The filter option for every space. Built in: "All".
    public var all: String?
    /// The filter option and group for docs without a space. Built in: "No space".
    public var noSpace: String?
    /// How many rows the filter hides. Built in: "<n> more in other spaces.".
    public var hiddenByFilter: (@Sendable (Int) -> String)?
    /// The button that clears the filter. Built in: "Show all".
    public var showAll: String?
    /// The spoken name of a row's unlink button. Built in: "Unlink <title>".
    public var unlink: (@Sendable (String) -> String)?
    /// Shown when unlinking fails. Built in: "That didn’t unlink. Try again.".
    public var unlinkFailed: String?
    /// Shown when linking fails. Built in: "That didn’t link. Try again.".
    public var linkFailed: String?
    /// The URL field. Built in: "Link".
    public var urlLabel: String?
    /// Shown under an invalid URL. Built in: "Enter a full web address that starts with https://".
    public var urlInvalid: String?
    /// The title field. Built in: "Title (optional)".
    public var titleLabel: String?
    /// The confirm button of the link form. Built in: "Add".
    public var confirmAdd: String?
    /// Built in: "Cancel".
    public var cancel: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        linkDoc: String? = nil,
        addLink: String? = nil,
        newDoc: String? = nil,
        searchDocs: String? = nil,
        noDocs: String? = nil,
        empty: String? = nil,
        emptyReadOnly: String? = nil,
        loading: String? = nil,
        add: String? = nil,
        filterBySpace: String? = nil,
        all: String? = nil,
        noSpace: String? = nil,
        hiddenByFilter: (@Sendable (Int) -> String)? = nil,
        showAll: String? = nil,
        unlink: (@Sendable (String) -> String)? = nil,
        unlinkFailed: String? = nil,
        linkFailed: String? = nil,
        urlLabel: String? = nil,
        urlInvalid: String? = nil,
        titleLabel: String? = nil,
        confirmAdd: String? = nil,
        cancel: String? = nil
    ) {
        self.title = title
        self.linkDoc = linkDoc
        self.addLink = addLink
        self.newDoc = newDoc
        self.searchDocs = searchDocs
        self.noDocs = noDocs
        self.empty = empty
        self.emptyReadOnly = emptyReadOnly
        self.loading = loading
        self.add = add
        self.filterBySpace = filterBySpace
        self.all = all
        self.noSpace = noSpace
        self.hiddenByFilter = hiddenByFilter
        self.showAll = showAll
        self.unlink = unlink
        self.unlinkFailed = unlinkFailed
        self.linkFailed = linkFailed
        self.urlLabel = urlLabel
        self.urlInvalid = urlInvalid
        self.titleLabel = titleLabel
        self.confirmAdd = confirmAdd
        self.cancel = cancel
    }
}
