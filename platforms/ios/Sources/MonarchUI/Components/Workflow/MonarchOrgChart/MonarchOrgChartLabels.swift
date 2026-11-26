/// Replacements for the built-in strings of ``MonarchOrgChart``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchOrgChartLabels: Sendable {
    /// The spoken name of the chart. Built in: "Organisation chart".
    public var label: String?
    /// Read for an open position. Built in: "Open position".
    public var vacant: String?
    /// Read for a temporary reporting line. Built in: "Temporary reporting line".
    public var dashed: String?
    /// Read for a dotted-line manager, from the manager's name. Built in: "Also reports to Ana Silva".
    public var alsoReportsTo: (@Sendable (String) -> String)?
    /// The number of direct reports, from the count. Built in: "3 direct reports".
    public var reports: (@Sendable (Int) -> String)?
    /// The state of a person whose team is shown. Built in: "Team shown".
    public var expanded: String?
    /// The state of a person whose team is hidden. Built in: "Team hidden".
    public var collapsed: String?
    /// The state of a person whose team is loading. Built in: "Loading team".
    public var loading: String?
    /// The accessibility action that shows a team. Built in: "Show team".
    public var expand: String?
    /// The accessibility action that hides a team. Built in: "Hide team".
    public var collapse: String?
    /// The menu item and accessibility action that asks to move a person. Built in: "Move…".
    public var move: String?
    /// The visible hint when people can be moved. Built in: "Drag a person onto their new manager, …".
    public var moveHint: String?
    /// Announced when a team fails to load, from the manager's name. Built in: "Could not load Ana Silva’s team."
    public var loadFailed: (@Sendable (String) -> String)?
    /// Built in: "Zoom in".
    public var zoomIn: String?
    /// Built in: "Zoom out".
    public var zoomOut: String?
    /// Built in: "Reset zoom".
    public var zoomReset: String?
    /// The zoom level, shown and announced, from the percentage. Built in: "120%".
    public var zoomLevel: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        vacant: String? = nil,
        dashed: String? = nil,
        alsoReportsTo: (@Sendable (String) -> String)? = nil,
        reports: (@Sendable (Int) -> String)? = nil,
        expanded: String? = nil,
        collapsed: String? = nil,
        loading: String? = nil,
        expand: String? = nil,
        collapse: String? = nil,
        move: String? = nil,
        moveHint: String? = nil,
        loadFailed: (@Sendable (String) -> String)? = nil,
        zoomIn: String? = nil,
        zoomOut: String? = nil,
        zoomReset: String? = nil,
        zoomLevel: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.vacant = vacant
        self.dashed = dashed
        self.alsoReportsTo = alsoReportsTo
        self.reports = reports
        self.expanded = expanded
        self.collapsed = collapsed
        self.loading = loading
        self.expand = expand
        self.collapse = collapse
        self.move = move
        self.moveHint = moveHint
        self.loadFailed = loadFailed
        self.zoomIn = zoomIn
        self.zoomOut = zoomOut
        self.zoomReset = zoomReset
        self.zoomLevel = zoomLevel
    }
}
