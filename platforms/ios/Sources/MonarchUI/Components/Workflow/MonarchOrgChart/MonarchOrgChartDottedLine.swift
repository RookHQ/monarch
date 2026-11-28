/// A secondary reporting line in a ``MonarchOrgChart``, drawn as a dashed curve.
@_spi(Experimental) public struct MonarchOrgChartDottedLine: Hashable, Sendable {
    /// The id of the person who also reports.
    public var from: String
    /// The id of the secondary manager.
    public var to: String

    /// A secondary reporting line.
    ///
    /// - Parameters:
    ///   - from: The id of the person who also reports.
    ///   - to: The id of the secondary manager.
    public init(from: String, to: String) {
        self.from = from
        self.to = to
    }
}
