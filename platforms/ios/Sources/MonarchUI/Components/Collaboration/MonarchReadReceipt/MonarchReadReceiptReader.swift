import Foundation

/// A person who has read a message, for ``MonarchReadReceipt``.
public struct MonarchReadReceiptReader: Identifiable, Sendable, Hashable {
    /// A stable id. Also picks the colour of the avatar's fallback.
    public let id: String
    /// The display name, already localised. Read in the full "Seen by" sentence.
    public let name: String
    /// The picture shown with ``MonarchReadReceiptDisplay/avatars``. Initials without it.
    public let avatarURL: URL?

    /// A reader.
    ///
    /// - Parameters:
    ///   - id: A stable id. The name by default.
    ///   - name: The display name, already localised.
    ///   - avatarURL: The picture, loaded with `AsyncImage`.
    public init(id: String? = nil, name: String, avatarURL: URL? = nil) {
        self.id = id ?? name
        self.name = name
        self.avatarURL = avatarURL
    }
}
