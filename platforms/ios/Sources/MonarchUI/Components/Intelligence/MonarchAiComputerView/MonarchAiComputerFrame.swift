import Foundation
import SwiftUI

/// The latest screenshot of the screen the agent in ``MonarchAiComputerView`` operates.
///
/// Like every Monarch picture it is either an `Image` the app loaded or a
/// URL that is loaded with `AsyncImage`; only http and https URLs are loaded.
@_spi(Experimental) public struct MonarchAiComputerFrame {
    /// The picture, when the app loaded it.
    public let image: Image?
    /// The address of the picture, when Monarch loads it.
    public let url: URL?
    /// The width of the screenshot in pixels; with `height` it sets the aspect ratio and places the markers.
    public let width: Double
    /// The height of the screenshot in pixels.
    public let height: Double
    /// When the screenshot was taken.
    public let capturedAt: Date
    /// What the screenshot shows, in words.
    public let alt: String?
    /// The screenshot may show private information: it is never displayed.
    public let isSensitive: Bool

    /// A screenshot the app loaded.
    ///
    /// - Parameters:
    ///   - image: The picture.
    ///   - width: The width in pixels.
    ///   - height: The height in pixels.
    ///   - capturedAt: When it was taken.
    ///   - alt: What it shows, already localised.
    ///   - isSensitive: Whether it may show private information.
    public init(
        image: Image?,
        width: Double,
        height: Double,
        capturedAt: Date,
        alt: String? = nil,
        isSensitive: Bool = false
    ) {
        self.image = image
        self.url = nil
        self.width = width
        self.height = height
        self.capturedAt = capturedAt
        self.alt = alt
        self.isSensitive = isSensitive
    }

    /// A screenshot loaded from an http or https address.
    ///
    /// - Parameters:
    ///   - url: The address of the picture.
    ///   - width: The width in pixels.
    ///   - height: The height in pixels.
    ///   - capturedAt: When it was taken.
    ///   - alt: What it shows, already localised.
    ///   - isSensitive: Whether it may show private information.
    public init(
        url: URL?,
        width: Double,
        height: Double,
        capturedAt: Date,
        alt: String? = nil,
        isSensitive: Bool = false
    ) {
        self.image = nil
        self.url = url
        self.width = width
        self.height = height
        self.capturedAt = capturedAt
        self.alt = alt
        self.isSensitive = isSensitive
    }
}
