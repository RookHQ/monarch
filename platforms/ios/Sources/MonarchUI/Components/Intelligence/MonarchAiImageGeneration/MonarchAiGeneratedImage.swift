import SwiftUI

/// One generated image, or the slot for one that is still generating, for
/// ``MonarchAiImageGeneration``.
///
/// Give a `url` (loaded with `AsyncImage`) or an `Image` the app has
/// already loaded. Leave both out while the image is generating.
public struct MonarchAiGeneratedImage: Sendable, Identifiable {
    /// A stable id. It is the value of the selection and is passed to
    /// `onVariations`.
    public let id: String
    /// The image's address. Only `http`, `https` and `file` addresses are loaded.
    public let url: URL?
    /// An image the app has already loaded.
    public let image: Image?
    /// The alt text, written by the person. Required: the field shows an
    /// error while it is empty.
    public var alt: String

    /// An image loaded from an address.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - url: The image's address, or `nil` while it is generating.
    ///   - alt: The alt text so far.
    public init(id: String, url: URL?, alt: String = "") {
        self.id = id
        self.url = url
        self.image = nil
        self.alt = alt
    }

    /// An image the app has already loaded.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - image: The image, or `nil` while it is generating.
    ///   - alt: The alt text so far.
    public init(id: String, image: Image?, alt: String = "") {
        self.id = id
        self.url = nil
        self.image = image
        self.alt = alt
    }
}
