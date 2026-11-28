import SwiftUI

/// The icon of a website at a fixed size, with a letter tile when there is
/// no icon.
///
/// ```swift
/// MonarchFavicon(url: URL(string: "https://example.com/favicon.ico"), name: "example.com")
/// MonarchFavicon(image: nil, name: "Acme")  // the letter tile "A"
/// ```
///
/// The letter tile is shown while the icon loads, when loading fails and
/// when there is no source. Monarch never calls a favicon service: the app
/// supplies the URL or the image. Only `http` and `https` URLs are loaded.
///
/// Accessibility: the favicon is decorative and hidden from VoiceOver unless
/// `label` is given, because the name of the site is normally written next
/// to it. With a label it is read as an image with that label. The tile
/// scales with Dynamic Type.
public struct MonarchFavicon: View {
    private let image: Image?
    private let url: URL?
    private let name: String
    private let label: String?
    private let shape: MonarchFaviconShape

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var side: CGFloat

    /// A favicon from an image the app already has.
    ///
    /// - Parameters:
    ///   - image: The icon, or `nil` for the letter tile.
    ///   - name: The name of the site, its domain or its URL. Its first
    ///     letter or digit is the fallback letter.
    ///   - label: What VoiceOver reads. Without it the favicon is decorative.
    ///   - size: The size of the tile.
    ///   - shape: The corner shape.
    public init(
        image: Image?,
        name: String,
        label: String? = nil,
        size: MonarchFaviconSize = .sm,
        shape: MonarchFaviconShape = .rounded
    ) {
        self.image = image
        self.url = nil
        self.name = name
        self.label = label
        self.shape = shape
        _side = ScaledMetric(wrappedValue: MonarchFaviconTokens.side(size), relativeTo: .body)
    }

    /// A favicon loaded from a URL.
    ///
    /// - Parameters:
    ///   - url: The address of the icon, or `nil` for the letter tile. A URL
    ///     that is not `http` or `https` is not loaded.
    ///   - name: The name of the site, its domain or its URL. Its first
    ///     letter or digit is the fallback letter.
    ///   - label: What VoiceOver reads. Without it the favicon is decorative.
    ///   - size: The size of the tile.
    ///   - shape: The corner shape.
    public init(
        url: URL?,
        name: String,
        label: String? = nil,
        size: MonarchFaviconSize = .sm,
        shape: MonarchFaviconShape = .rounded
    ) {
        self.image = nil
        self.url = MonarchFaviconTokens.loadableURL(url)
        self.name = name
        self.label = label
        self.shape = shape
        _side = ScaledMetric(wrappedValue: MonarchFaviconTokens.side(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let clip = RoundedRectangle(
            cornerRadius: MonarchFaviconTokens.cornerRadius(shape, side: side),
            style: .continuous
        )
        let tile = picture.frame(width: side, height: side).clipShape(clip)
        if let label, !label.isEmpty {
            tile
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
        } else {
            tile.accessibilityHidden(true)
        }
    }

    @ViewBuilder private var picture: some View {
        if let image {
            MonarchRemoteImage(image: image) { letterTile }
        } else {
            MonarchRemoteImage(url: url) { letterTile }
        }
    }

    /// The fallback. The letter is sized as a part of the tile, which is a
    /// scaled metric, so it follows Dynamic Type; a type role would not fit
    /// the tile at every size.
    private var letterTile: some View {
        let pointSize = side * MonarchFaviconTokens.letterScale
        let font: Font =
            if let family = theme.typography.fontFamily {
                .custom(family, fixedSize: pointSize).weight(MonarchFaviconTokens.letterWeight)
            } else {
                .system(size: pointSize, weight: MonarchFaviconTokens.letterWeight)
            }
        return ZStack {
            theme.colors[role: MonarchFaviconTokens.tile]
            Text(MonarchFaviconTokens.letter(for: name))
                .font(font)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(theme.colors[role: MonarchFaviconTokens.letter])
        }
    }
}

#if DEBUG
    #Preview("MonarchFavicon") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    ForEach(MonarchFaviconSize.allCases, id: \.self) { size in
                        MonarchFavicon(image: nil, name: "example.com", size: size)
                    }
                }
                HStack {
                    ForEach(MonarchFaviconShape.allCases, id: \.self) { shape in
                        MonarchFavicon(image: Image(systemName: "globe"), name: "Globe", size: .lg, shape: shape)
                    }
                }
                MonarchFavicon(image: nil, name: "", label: "Unknown site", size: .lg)
            }
        }
    }
#endif
