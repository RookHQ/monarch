/// Built-in strings of ``MonarchDataExportRequest``, from the `MonarchDataExportRequest` strings table.
enum MonarchDataExportRequestStrings {
    private static let table = "MonarchDataExportRequest"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dataExportRequest.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.dataExportRequest.\(key)", table: table, argument)
    }

    /// The card's title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The badge text of a status.
    static func status(_ status: MonarchDataExportStatus) -> String { string("status.\(status.rawValue)") }
    /// The first request button.
    static var request: String { string("request") }
    /// The request button after an expired or failed export.
    static var requestAgain: String { string("requestAgain") }
    /// The download button.
    static var download: String { string("download") }
    /// The label of the scope checklist.
    static var scopeLegend: String { string("scopeLegend") }
    /// Shown when no kind of data is chosen.
    static var scopeRequired: String { string("scopeRequired") }
    /// The label of the format menu.
    static var formatLabel: String { string("formatLabel") }
    /// The spoken name of the progress bar.
    static var preparingProgress: String { string("preparingProgress") }
    /// The status line when failed.
    static var failed: String { string("failed") }
    /// Shown when a request fails without a description.
    static var requestFailed: String { string("requestFailed") }

    /// The status line while requested.
    static func requested(_ date: String?) -> String {
        date.map { string("requestedOn", $0) } ?? string("requested")
    }
    /// The status line while preparing.
    static func preparing(_ date: String?) -> String {
        date.map { string("preparingSince", $0) } ?? string("preparing")
    }
    /// The status line when ready.
    static func ready(_ readyAt: String?, _ expiresAt: String?) -> String {
        [string("ready"), readyAt.map { string("readyOn", $0) }, expiresAt.map { string("readyUntil", $0) }]
            .compactMap { $0 }
            .joined(separator: " ")
    }
    /// The status line when expired.
    static func expired(_ date: String?) -> String {
        date.map { string("expiredOn", $0) } ?? string("expired")
    }
}
