import SwiftUI

/// An image from one of the two sources a component accepts, with a
/// placeholder while there is no image.
///
/// The image convention for every component that shows a picture:
///
/// - The component has one initialiser that takes `image: Image?` and one
///   that takes `url: URL?`. Nothing else.
/// - A URL is loaded with SwiftUI's `AsyncImage`, which uses the shared
///   `URLSession`. Monarch adds no networking, caching or image dependency.
///   An app with its own image pipeline loads the image itself and passes
///   the `Image`.
/// - The placeholder (initials, a symbol, a letter tile) is shown while the
///   image loads, when it fails, and when there is no source. A failure is
///   never an error state: the placeholder is the result.
/// - The picture is decorative unless the component gives it a label. The
///   component, not this view, decides what VoiceOver reads.
///
/// By default the image fills the frame the component gives it and is
/// clipped by the component; with `contentMode: .fit` it is letterboxed
/// inside the frame instead (a video thumbnail, a logo).
struct MonarchRemoteImage<Placeholder: View>: View {
    private let image: Image?
    private let url: URL?
    private let contentMode: ContentMode
    private let placeholder: Placeholder

    /// An image the app already has, or the placeholder when it is `nil`.
    init(image: Image?, contentMode: ContentMode = .fill, @ViewBuilder placeholder: () -> Placeholder) {
        self.image = image
        self.url = nil
        self.contentMode = contentMode
        self.placeholder = placeholder()
    }

    /// An image loaded from a URL, or the placeholder when it is `nil`.
    init(url: URL?, contentMode: ContentMode = .fill, @ViewBuilder placeholder: () -> Placeholder) {
        self.image = nil
        self.url = url
        self.contentMode = contentMode
        self.placeholder = placeholder()
    }

    var body: some View {
        if let image {
            image.resizable().aspectRatio(contentMode: contentMode)
        } else if let url {
            AsyncImage(url: url) { phase in
                if let loaded = phase.image {
                    loaded.resizable().aspectRatio(contentMode: contentMode)
                } else {
                    placeholder
                }
            }
        } else {
            placeholder
        }
    }
}
