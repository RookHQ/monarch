/// Built-in strings of the breadcrumb, from the `MonarchBreadcrumb` strings table.
enum MonarchBreadcrumbStrings {
    private static let table = "MonarchBreadcrumb"

    /// The spoken name of the trail.
    static var label: String { MonarchStrings.string("monarch.breadcrumb.label", table: table) }
    /// "Show 3 more levels".
    static func more(_ count: String) -> String {
        MonarchStrings.string("monarch.breadcrumb.more", table: table, count)
    }
    /// The value of the last level.
    static var current: String { MonarchStrings.string("monarch.breadcrumb.current", table: table) }
}
