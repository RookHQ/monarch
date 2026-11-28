/// A file format a report can be downloaded in.
public struct MonarchDownloadReportFormat: Sendable, Hashable, Identifiable {
    /// A stable id that the app's download reads, such as `csv`, `xlsx`, `pdf`.
    public let id: String
    /// The short name shown in the button or the menu, such as "CSV",
    /// already localised.
    public let label: String
    /// A hint under the name in the menu, such as "Opens in any spreadsheet".
    public let description: String?

    /// A format.
    ///
    /// - Parameters:
    ///   - label: The short name, already localised.
    ///   - id: A stable id that the app's download reads.
    ///   - description: A hint under the name in the menu.
    public init(_ label: String, id: String, description: String? = nil) {
        self.id = id
        self.label = label
        self.description = description
    }

    /// Comma-separated values, the default format.
    public static var csv: MonarchDownloadReportFormat {
        MonarchDownloadReportFormat(MonarchDownloadReportButtonStrings.csv, id: "csv")
    }
}
