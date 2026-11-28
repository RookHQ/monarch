import Foundation

/// A person's own status message, with an optional emoji and an end time.
public struct MonarchMyStatusCustomStatus: Sendable, Hashable {
    /// The message, such as "On holiday", already localised.
    public var text: String?
    /// An emoji shown before the message.
    public var emoji: String?
    /// When the status ends. From then on it is not shown.
    public var until: Date?

    /// A custom status.
    ///
    /// - Parameters:
    ///   - text: The message, already localised.
    ///   - emoji: An emoji shown before the message.
    ///   - until: When the status ends; `nil` keeps it until it is changed.
    public init(_ text: String? = nil, emoji: String? = nil, until: Date? = nil) {
        self.text = text
        self.emoji = emoji
        self.until = until
    }
}
