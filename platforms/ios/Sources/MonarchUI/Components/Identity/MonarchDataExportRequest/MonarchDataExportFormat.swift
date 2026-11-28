/// A file format a person can choose in a ``MonarchDataExportRequest``.
@_spi(Experimental) public struct MonarchDataExportFormat: Sendable, Hashable, Identifiable {
    /// The value passed to the request, such as "json", "csv" or "zip".
    public let id: String
    /// The visible label, already localised, such as "JSON (machine-readable)".
    public let label: String

    /// A file format.
    ///
    /// - Parameters:
    ///   - id: The value passed to the request.
    ///   - label: The visible label, already localised.
    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}
