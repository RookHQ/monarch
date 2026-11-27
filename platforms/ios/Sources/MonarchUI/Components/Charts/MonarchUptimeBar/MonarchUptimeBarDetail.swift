/// An extra line in the details of a ``MonarchUptimeBarSegment``, such as
/// the number of checks or the p95 response time.
public struct MonarchUptimeBarDetail: Sendable, Hashable {
    /// The name, already localised, such as "Checks".
    public let label: String
    /// The value, already formatted, such as "1,440".
    public let value: String

    /// A detail line.
    ///
    /// - Parameters:
    ///   - label: The name, already localised.
    ///   - value: The value, already formatted.
    public init(_ label: String, value: String) {
        self.label = label
        self.value = value
    }
}
