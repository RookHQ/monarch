/// A column of the imported data, such as a CSV header, with a few example
/// values from the first rows.
public struct MonarchColumnMappingSource: Sendable, Hashable {
    /// The header text. Unique among the sources.
    public let name: String
    /// Example values from the first rows. The first three that are not
    /// blank are shown.
    public let samples: [String]

    /// A source column.
    ///
    /// - Parameters:
    ///   - name: The header text.
    ///   - samples: Example values from the first rows.
    public init(_ name: String, samples: [String] = []) {
        self.name = name
        self.samples = samples
    }
}
