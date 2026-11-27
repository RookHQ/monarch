/// Replacements for the built-in strings of ``MonarchFacetedFilterPanel``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFacetedFilterPanelLabels: Sendable {
    /// The panel heading. Built in: "Filters".
    public var title: String?
    /// The button that clears every facet. Built in: "Clear all".
    public var clearAll: String?
    /// The visible text of a group's clear button. Built in: "Clear".
    public var clearGroup: String?
    /// The spoken name of a group's clear button, from the group's label. Built in: "Clear Status filter".
    public var clearGroupLabel: (@Sendable (String) -> String)?
    /// How many filters a group has, shown under its heading. Built in: "2 selected".
    public var activeCount: (@Sendable (Int) -> String)?
    /// The button that shows the hidden options. Built in: "Show 4 more".
    public var showMore: (@Sendable (Int) -> String)?
    /// The button that hides them again. Built in: "Show fewer".
    public var showFewer: String?
    /// Shown in an options facet with no options. Built in: "No options".
    public var noOptions: String?
    /// Replacements for the range slider's spoken "Minimum" and "Maximum".
    public var range: MonarchRangeSliderLabels

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - title: The panel heading.
    ///   - clearAll: The button that clears every facet.
    ///   - clearGroup: The visible text of a group's clear button.
    ///   - clearGroupLabel: The spoken name of a group's clear button, from the group's label.
    ///   - activeCount: How many filters a group has.
    ///   - showMore: The button that shows the hidden options, from their number.
    ///   - showFewer: The button that hides them again.
    ///   - noOptions: Shown in an options facet with no options.
    ///   - range: Replacements for the range slider's strings.
    public init(
        title: String? = nil,
        clearAll: String? = nil,
        clearGroup: String? = nil,
        clearGroupLabel: (@Sendable (String) -> String)? = nil,
        activeCount: (@Sendable (Int) -> String)? = nil,
        showMore: (@Sendable (Int) -> String)? = nil,
        showFewer: String? = nil,
        noOptions: String? = nil,
        range: MonarchRangeSliderLabels = MonarchRangeSliderLabels()
    ) {
        self.title = title
        self.clearAll = clearAll
        self.clearGroup = clearGroup
        self.clearGroupLabel = clearGroupLabel
        self.activeCount = activeCount
        self.showMore = showMore
        self.showFewer = showFewer
        self.noOptions = noOptions
        self.range = range
    }
}
