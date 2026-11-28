/// One of the records a ``MonarchMergeDialog`` merges.
public struct MonarchMergeRecord: Identifiable, Hashable, Sendable {
    /// A stable identifier.
    public let id: String
    /// The record's name, such as the company name.
    public let title: String
    /// A secondary line, such as "Created 3 Mar 2025 · 12 invoices".
    public let description: String?
    /// The field values by field identifier, already written for display.
    /// A missing value, or one that is empty after trimming, counts as empty.
    public let values: [String: String]

    /// A record.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - title: The record's name.
    ///   - description: A secondary line.
    ///   - values: The field values by field identifier.
    public init(id: String, title: String, description: String? = nil, values: [String: String]) {
        self.id = id
        self.title = title
        self.description = description
        self.values = values
    }
}
