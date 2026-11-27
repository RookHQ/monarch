/// Thrown when ``MonarchJsonValue`` cannot parse JSON text.
public struct MonarchJsonParseError: Error, Sendable, Equatable, CustomStringConvertible {
    /// The position of the problem, in UTF-8 bytes from the start.
    public let offset: Int

    /// An error at a position.
    ///
    /// - Parameter offset: The position of the problem, in UTF-8 bytes.
    public init(offset: Int) {
        self.offset = offset
    }

    /// A description for logs, in English.
    public var description: String { "Invalid JSON at byte \(offset)" }
}
