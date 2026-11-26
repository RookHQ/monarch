/// Built-in strings of ``MonarchDownloadReportButton``, from the
/// `MonarchDownloadReportButton` strings table.
enum MonarchDownloadReportButtonStrings {
    private static let table = "MonarchDownloadReportButton"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.downloadReportButton." + key, table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.downloadReportButton." + key, table: table, argument)
    }

    /// The title with several formats.
    static var label: String { string("label") }
    /// The title with one format: "Download CSV".
    static func single(_ format: String) -> String { string("single", format) }
    /// "Preparing CSV…".
    static func preparing(_ format: String) -> String { string("preparing", format) }
    /// "CSV downloaded".
    static func downloaded(_ format: String) -> String { string("downloaded", format) }
    /// The background preparation note.
    static var queued: String { string("queued") }
    /// "The CSV download failed."
    static func error(_ format: String) -> String { string("error", format) }
    /// The name of the progress bar.
    static var progress: String { string("progress") }
    /// The name of the default format.
    static var csv: String { string("csv") }
}
