import Foundation

/// Replacements for the built-in strings of ``MonarchJsonViewer``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchJsonViewerLabels: Sendable {
    /// The spoken name of the tree. Built in: "JSON".
    public var label: String?
    /// The spoken name of the search field. Built in: "Search keys and values".
    public var search: String?
    /// The placeholder of the search field. Built in: "Search".
    public var searchPrompt: String?
    /// The spoken name of the button that clears the search. Built in: "Clear search".
    public var clearSearch: String?
    /// The match count. Receives the count and whether the search stopped
    /// early. Built in: "No matches", "1 match", "12 matches", "500+ matches".
    public var matches: (@Sendable (Int, Bool) -> String)?
    /// The button that closes every node. Built in: "Collapse all".
    public var collapseAll: String?
    /// The button that copies the selected path. Built in: "Copy path".
    public var copyPath: String?
    /// The button that copies the selected value. Built in: "Copy value".
    public var copyValue: String?
    /// Spoken before the selected path. Built in: "Selected".
    public var selectedPath: String?
    /// The summary of an object. Receives the number of keys. Built in: "3 keys".
    public var keys: (@Sendable (Int) -> String)?
    /// The summary of an array. Receives the number of items. Built in: "2 items".
    public var items: (@Sendable (Int) -> String)?
    /// The "Show more" row. Receives how many it adds and how many are
    /// hidden. Built in: "Show 100 more (250 hidden)".
    public var showMore: (@Sendable (Int, Int) -> String)?
    /// Added after a cut string. Receives the full length. Built in:
    /// "… (1,200 characters)".
    public var truncated: (@Sendable (Int) -> String)?
    /// The spoken state of an open container. Built in: "expanded".
    public var expanded: String?
    /// The spoken state of a closed container. Built in: "collapsed".
    public var collapsed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        search: String? = nil,
        searchPrompt: String? = nil,
        clearSearch: String? = nil,
        matches: (@Sendable (Int, Bool) -> String)? = nil,
        collapseAll: String? = nil,
        copyPath: String? = nil,
        copyValue: String? = nil,
        selectedPath: String? = nil,
        keys: (@Sendable (Int) -> String)? = nil,
        items: (@Sendable (Int) -> String)? = nil,
        showMore: (@Sendable (Int, Int) -> String)? = nil,
        truncated: (@Sendable (Int) -> String)? = nil,
        expanded: String? = nil,
        collapsed: String? = nil
    ) {
        self.label = label
        self.search = search
        self.searchPrompt = searchPrompt
        self.clearSearch = clearSearch
        self.matches = matches
        self.collapseAll = collapseAll
        self.copyPath = copyPath
        self.copyValue = copyValue
        self.selectedPath = selectedPath
        self.keys = keys
        self.items = items
        self.showMore = showMore
        self.truncated = truncated
        self.expanded = expanded
        self.collapsed = collapsed
    }

    private static func count(_ value: Int) -> String {
        value.formatted(.number)
    }

    var resolvedLabel: String { label ?? MonarchJsonViewerStrings.string("label") }
    var resolvedSearch: String { search ?? MonarchJsonViewerStrings.string("search") }
    var resolvedSearchPrompt: String { searchPrompt ?? MonarchJsonViewerStrings.string("searchPrompt") }
    var resolvedClearSearch: String { clearSearch ?? MonarchJsonViewerStrings.string("clearSearch") }
    var resolvedCollapseAll: String { collapseAll ?? MonarchJsonViewerStrings.string("collapseAll") }
    var resolvedCopyPath: String { copyPath ?? MonarchJsonViewerStrings.string("copyPath") }
    var resolvedCopyValue: String { copyValue ?? MonarchJsonViewerStrings.string("copyValue") }
    var resolvedSelectedPath: String { selectedPath ?? MonarchJsonViewerStrings.string("selectedPath") }
    var resolvedExpanded: String { expanded ?? MonarchJsonViewerStrings.string("expanded") }
    var resolvedCollapsed: String { collapsed ?? MonarchJsonViewerStrings.string("collapsed") }

    func matchesText(_ value: Int, isCapped: Bool) -> String {
        if let matches { return matches(value, isCapped) }
        if value == 0 { return MonarchJsonViewerStrings.string("noMatches") }
        let text = Self.count(value) + (isCapped ? "+" : "")
        return value == 1 && !isCapped
            ? MonarchJsonViewerStrings.string("oneMatch", text)
            : MonarchJsonViewerStrings.string("matches", text)
    }

    func keysText(_ value: Int) -> String {
        if let keys { return keys(value) }
        return value == 1
            ? MonarchJsonViewerStrings.string("oneKey") : MonarchJsonViewerStrings.string("keys", Self.count(value))
    }

    func itemsText(_ value: Int) -> String {
        if let items { return items(value) }
        return value == 1
            ? MonarchJsonViewerStrings.string("oneItem") : MonarchJsonViewerStrings.string("items", Self.count(value))
    }

    func showMoreText(next: Int, remaining: Int) -> String {
        showMore?(next, remaining)
            ?? MonarchJsonViewerStrings.string("showMore", Self.count(next), Self.count(remaining))
    }

    func truncatedText(_ length: Int) -> String {
        truncated?(length) ?? MonarchJsonViewerStrings.string("truncated", Self.count(length))
    }
}
