/// Built-in strings of ``MonarchOrgChart``, from the `MonarchOrgChart` strings table.
enum MonarchOrgChartStrings {
    private static let table = "MonarchOrgChart"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.orgChart.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static var label: String { string("label") }
    static var vacant: String { string("vacant") }
    static var dashed: String { string("dashed") }
    static func alsoReportsTo(_ manager: String) -> String { string("alsoReportsTo", manager) }
    static func reports(_ count: Int) -> String {
        count == 1 ? string("reports.one") : string("reports.other", count)
    }
    static var expanded: String { string("expanded") }
    static var collapsed: String { string("collapsed") }
    static var loading: String { string("loading") }
    static var expand: String { string("expand") }
    static var collapse: String { string("collapse") }
    static var move: String { string("move") }
    static var moveHint: String { string("moveHint") }
    static func loadFailed(_ name: String) -> String { string("loadFailed", name) }
    static var zoomIn: String { string("zoomIn") }
    static var zoomOut: String { string("zoomOut") }
    static var zoomReset: String { string("zoomReset") }
    static func zoomLevel(_ percent: Int) -> String { string("zoomLevel", percent) }
}
