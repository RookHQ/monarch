import Foundation

/// A short-lived emoji reaction on a ``MonarchCallTile``. A new `sentAt` replays it.
public struct MonarchCallReaction: Sendable, Hashable {
    /// The emoji, such as "👏".
    public let emoji: String
    /// When it was sent. The reaction floats for three seconds after this time.
    public let sentAt: Date

    /// A reaction.
    ///
    /// - Parameters:
    ///   - emoji: The emoji.
    ///   - sentAt: When it was sent.
    public init(_ emoji: String, sentAt: Date) {
        self.emoji = emoji
        self.sentAt = sentAt
    }
}
