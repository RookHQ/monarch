import SwiftUI

/// One image or video in a ``MonarchLightbox``.
///
/// ```swift
/// MonarchLightboxItem(image: Image("whiteboard"), alt: "Whiteboard sketch of the checkout flow")
/// MonarchLightboxItem(url: photoURL, alt: "Team photo", caption: "Lisbon, 2026")
/// ```
///
/// Every item needs `alt`: what the picture shows, for people who cannot see
/// it.
public struct MonarchLightboxItem {
    let image: Image?
    let url: URL?

    /// Whether the item is an image or a video.
    public let kind: MonarchLightboxItemKind
    /// What the media shows. VoiceOver reads it as the name of the item.
    public let alt: String
    /// The visible caption under the media.
    public let caption: String?
    /// A file the person can share or save, such as the original.
    public let shareURL: URL?

    /// An image the app already has.
    ///
    /// - Parameters:
    ///   - image: The picture. `nil` shows the "can't be shown" text.
    ///   - alt: What the picture shows, already localised.
    ///   - caption: The visible caption under the picture.
    ///   - shareURL: A file to share or save, shown as a Share button.
    public init(image: Image?, alt: String, caption: String? = nil, shareURL: URL? = nil) {
        self.image = image
        self.url = nil
        self.kind = .image
        self.alt = alt
        self.caption = caption
        self.shareURL = shareURL
    }

    /// An image or a video loaded from a URL.
    ///
    /// An image is loaded with SwiftUI's `AsyncImage`; a video is played with
    /// AVKit's `VideoPlayer`. An app with its own image pipeline loads the
    /// image itself and uses the other initialiser.
    ///
    /// - Parameters:
    ///   - url: The address of the media. `nil` shows the "can't be shown"
    ///     text.
    ///   - alt: What the media shows, already localised.
    ///   - caption: The visible caption under the media.
    ///   - kind: Image or video.
    ///   - shareURL: A file to share or save, shown as a Share button.
    public init(
        url: URL?,
        alt: String,
        caption: String? = nil,
        kind: MonarchLightboxItemKind = .image,
        shareURL: URL? = nil
    ) {
        self.image = nil
        self.url = url
        self.kind = kind
        self.alt = alt
        self.caption = caption
        self.shareURL = shareURL
    }
}
