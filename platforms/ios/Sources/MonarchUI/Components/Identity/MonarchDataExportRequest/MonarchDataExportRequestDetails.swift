/// What a person chose when requesting a data export in a ``MonarchDataExportRequest``.
@_spi(Experimental) public struct MonarchDataExportRequestDetails: Sendable, Hashable {
    /// The ids of the chosen kinds of data, in the order they are listed. Empty when there is no scope.
    public let scope: [String]
    /// The chosen format's id, or `nil` when there are no formats.
    public let format: String?

    /// The details of a request.
    ///
    /// - Parameters:
    ///   - scope: The ids of the chosen kinds of data, in order.
    ///   - format: The chosen format's id.
    public init(scope: [String], format: String?) {
        self.scope = scope
        self.format = format
    }
}
