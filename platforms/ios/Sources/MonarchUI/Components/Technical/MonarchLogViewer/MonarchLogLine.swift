import Foundation

/// One line of a ``MonarchLogViewer``.
public struct MonarchLogLine: Identifiable, Sendable, Hashable {
    /// A stable, unique id. Lines are told apart and scrolled to by it.
    public let id: String
    /// The text of the line, shown as plain text.
    public let message: String
    /// The severity.
    public let level: MonarchLogLevel
    /// When the line was written.
    public let time: Date?
    /// Where it came from, such as a service, a pod or a build step. Shown before the message.
    public let source: String?

    /// A log line.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - message: The text of the line.
    ///   - level: The severity. Info by default.
    ///   - time: When the line was written. No time column entry without it.
    ///   - source: Where it came from, shown before the message.
    public init(
        id: String,
        message: String,
        level: MonarchLogLevel = .info,
        time: Date? = nil,
        source: String? = nil
    ) {
        self.id = id
        self.message = message
        self.level = level
        self.time = time
        self.source = source
    }
}
