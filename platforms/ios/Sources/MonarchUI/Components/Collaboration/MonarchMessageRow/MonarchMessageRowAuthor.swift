import SwiftUI

/// The author of a ``MonarchMessageRow``: a name and an avatar picture.
///
/// Like every Monarch component that shows a picture, the author takes the
/// picture as an `Image` or as a URL loaded with `AsyncImage`. The initials
/// of the name are shown while it loads, when it fails and when there is none.
public struct MonarchMessageRowAuthor {
    /// The display name, already localised. Shown above the first message of
    /// a group and read by VoiceOver on every message.
    public let name: String
    /// The avatar picture, when the app loaded it.
    public let image: Image?
    /// The avatar picture's address, loaded with `AsyncImage`.
    public let url: URL?
    /// Picks the avatar's fallback colour, so a renamed person keeps it.
    public let seed: String?

    /// An author whose avatar is loaded from a URL.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised.
    ///   - url: The avatar picture, loaded with `AsyncImage`. Initials without it.
    ///   - seed: Picks the avatar's fallback colour. The name by default; pass a stable id.
    public init(_ name: String, url: URL? = nil, seed: String? = nil) {
        self.name = name
        self.image = nil
        self.url = url
        self.seed = seed
    }

    /// An author whose avatar picture the app has already loaded.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised.
    ///   - image: The avatar picture. Initials when it is `nil`.
    ///   - seed: Picks the avatar's fallback colour. The name by default; pass a stable id.
    public init(_ name: String, image: Image?, seed: String? = nil) {
        self.name = name
        self.image = image
        self.url = nil
        self.seed = seed
    }
}
