import SwiftUI

/// A preview of a shared link: its picture, site, title and description,
/// opened as one link.
///
/// ```swift
/// MonarchLinkCard(
///     "Release notes for March",
///     url: preview.url,
///     description: "Faster search, shared views and a new API.",
///     imageURL: preview.imageURL,
///     imageSize: CGSize(width: 1200, height: 630),
///     faviconURL: preview.faviconURL
/// )
/// ```
///
/// The data comes from the app, which unfurled the link on its server: the
/// card never fetches a page. Pictures follow the Monarch image convention:
/// pass an `Image`, or a URL that SwiftUI's `AsyncImage` loads; a muted
/// placeholder keeps the picture's space while it loads or when it fails.
/// The picture's ratio is kept between near square and a wide banner. Only
/// `http`, `https` and `mailto` links open; any other link shows the card as
/// plain text. With `onDismiss`, a remove button sits in the corner.
///
/// Accessibility: the card is one link read as the site, the title, the
/// description and, for a video, "Video". The picture and the favicon are
/// decorative. The remove button is a separate element named "Remove the
/// preview of" and the title, with a 44-point target.
public struct MonarchLinkCard: View {
    private let title: String
    private let url: URL
    private let description: String?
    private let siteName: String?
    private let image: Image?
    private let imageURL: URL?
    private let hasImage: Bool
    private let imageSize: CGSize?
    private let favicon: Image?
    private let faviconURL: URL?
    private let isVideo: Bool
    private let size: MonarchLinkCardSize
    private let dismissLabel: String?
    private let videoLabel: String?
    private let onDismiss: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var thumbnailSide = MonarchLinkCardTokens.thumbnailSide
    @ScaledMetric(relativeTo: .body) private var playSide = MonarchLinkCardTokens.playSide

    /// A link card with pictures the app already has.
    ///
    /// - Parameters:
    ///   - title: The page title, already localised by the page.
    ///   - url: The link.
    ///   - description: The page description, shown on up to two lines.
    ///   - siteName: The site shown above the title. The host name by default.
    ///   - image: The page's picture, or `nil` for none.
    ///   - imageSize: The picture's natural size, to keep its ratio.
    ///   - favicon: The site's icon, or `nil` for a letter tile.
    ///   - isVideo: Whether the link is a video: a play mark is drawn on the picture.
    ///   - size: A banner picture above the text, or a thumbnail beside it.
    ///   - dismissLabel: Replaces the built-in spoken name of the remove button.
    ///   - videoLabel: Replaces the built-in spoken word "Video".
    ///   - onDismiss: Shows a remove button and runs when it is pressed.
    public init(
        _ title: String,
        url: URL,
        description: String? = nil,
        siteName: String? = nil,
        image: Image?,
        imageSize: CGSize? = nil,
        favicon: Image? = nil,
        isVideo: Bool = false,
        size: MonarchLinkCardSize = .md,
        dismissLabel: String? = nil,
        videoLabel: String? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.url = url
        self.description = description
        self.siteName = siteName
        self.image = image
        self.imageURL = nil
        self.hasImage = image != nil
        self.imageSize = imageSize
        self.favicon = favicon
        self.faviconURL = nil
        self.isVideo = isVideo
        self.size = size
        self.dismissLabel = dismissLabel
        self.videoLabel = videoLabel
        self.onDismiss = onDismiss
    }

    /// A link card with pictures loaded from URLs.
    ///
    /// - Parameters:
    ///   - title: The page title, already localised by the page.
    ///   - url: The link.
    ///   - description: The page description, shown on up to two lines.
    ///   - siteName: The site shown above the title. The host name by default.
    ///   - imageURL: The address of the page's picture, or `nil` for none.
    ///   - imageSize: The picture's natural size, to keep its ratio.
    ///   - faviconURL: The address of the site's icon, or `nil` for a letter tile.
    ///   - isVideo: Whether the link is a video: a play mark is drawn on the picture.
    ///   - size: A banner picture above the text, or a thumbnail beside it.
    ///   - dismissLabel: Replaces the built-in spoken name of the remove button.
    ///   - videoLabel: Replaces the built-in spoken word "Video".
    ///   - onDismiss: Shows a remove button and runs when it is pressed.
    public init(
        _ title: String,
        url: URL,
        description: String? = nil,
        siteName: String? = nil,
        imageURL: URL?,
        imageSize: CGSize? = nil,
        faviconURL: URL? = nil,
        isVideo: Bool = false,
        size: MonarchLinkCardSize = .md,
        dismissLabel: String? = nil,
        videoLabel: String? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.url = url
        self.description = description
        self.siteName = siteName
        self.image = nil
        self.imageURL = imageURL
        self.hasImage = imageURL != nil
        self.imageSize = imageSize
        self.favicon = nil
        self.faviconURL = faviconURL
        self.isVideo = isVideo
        self.size = size
        self.dismissLabel = dismissLabel
        self.videoLabel = videoLabel
        self.onDismiss = onDismiss
    }

    /// The content of the view.
    public var body: some View {
        let site = MonarchLinkCardTokens.siteName(siteName, url: url)
        let label = MonarchLinkCardTokens.accessibilityLabel(
            site: site,
            title: title,
            description: description,
            isVideo: isVideo,
            videoLabel: videoLabel ?? MonarchLinkCardStrings.video
        )
        MonarchCard(padding: isBleed ? .flush : .standard) {
            if MonarchLinkCardTokens.isSafeLink(url) {
                // The standard link keeps its role and activation; only its name is replaced.
                Link(destination: url) { preview(site: site) }
                    .buttonStyle(.plain)
                    .accessibilityLabel(label)
            } else {
                preview(site: site)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(label)
            }
        }
        .overlay(alignment: .topTrailing) {
            if let onDismiss {
                Button(action: onDismiss) {
                    MonarchIcon(systemName: MonarchLinkCardTokens.dismissSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(MonarchLinkCardTokens.textSpacing)
                        .background(Circle().fill(theme.colors.bgSurface))
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(dismissLabel ?? MonarchLinkCardStrings.dismiss(title))
            }
        }
    }

    @ViewBuilder private func preview(site: String) -> some View {
        switch size {
        case .md:
            VStack(alignment: .leading, spacing: MonarchLinkCardTokens.mediaSpacing) {
                if hasImage {
                    picture
                        .aspectRatio(MonarchLinkCardTokens.imageRatio(imageSize), contentMode: .fit)
                        .frame(maxWidth: .infinity)
                }
                text(site: site)
                    .padding(.horizontal, isBleed ? MonarchLinkCardTokens.bleedTextInset : 0)
                    .padding(.bottom, isBleed ? MonarchLinkCardTokens.bleedTextInset : 0)
            }
        case .sm:
            HStack(alignment: .top, spacing: MonarchLinkCardTokens.mediaSpacing) {
                text(site: site)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if hasImage {
                    picture.frame(width: thumbnailSide, height: thumbnailSide)
                }
            }
        }
    }

    /// The picture, clipped, with the play mark for a video.
    /// A medium card with a picture: the picture runs to the card's edges, as on the web, and the text is inset.
    private var isBleed: Bool { size == .md && hasImage }

    private var picture: some View {
        Group {
            if let image {
                MonarchRemoteImage(image: image) { placeholder }
            } else {
                MonarchRemoteImage(url: imageURL) { placeholder }
            }
        }
        .clipShape(isBleed ? AnyShape(Rectangle()) : AnyShape(MonarchShapes.control))
        .overlay {
            if isVideo {
                MonarchIcon(systemName: MonarchLinkCardTokens.playSymbol, size: .md)
                    .foregroundStyle(theme.colors.textInverse)
                    .frame(width: playSide, height: playSide)
                    .background(Circle().fill(theme.colors.bgInverse))
            }
        }
        .accessibilityHidden(true)
    }

    private var placeholder: some View {
        (isBleed ? AnyShape(Rectangle()) : AnyShape(MonarchShapes.control)).fill(theme.colors.bgMuted)
    }

    private func text(site: String) -> some View {
        VStack(alignment: .leading, spacing: MonarchLinkCardTokens.textSpacing) {
            if !site.isEmpty {
                HStack(spacing: MonarchLinkCardTokens.siteSpacing) {
                    siteIcon(site: site)
                    Text(site)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .lineLimit(1)
                }
            }
            Text(title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
                .lineLimit(MonarchLinkCardTokens.titleLines)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .lineLimit(MonarchLinkCardTokens.descriptionLines)
            }
        }
        .multilineTextAlignment(.leading)
        .padding(.trailing, MonarchLinkCardTokens.textTrailingInset(hasDismiss: onDismiss != nil, hasImage: hasImage))
    }

    @ViewBuilder private func siteIcon(site: String) -> some View {
        if let favicon {
            MonarchFavicon(image: favicon, name: site)
        } else {
            MonarchFavicon(url: faviconURL, name: site)
        }
    }
}

#if DEBUG
    #Preview("MonarchLinkCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchLinkCard(
                    "Release notes for March",
                    url: URL(fileURLWithPath: "/"),
                    description: "Faster search, shared views and a new API.",
                    siteName: "Example blog",
                    image: Image(systemName: "photo"),
                    isVideo: true
                )
                MonarchLinkCard(
                    "Pricing",
                    url: URL(fileURLWithPath: "/"),
                    siteName: "example.com",
                    image: nil,
                    size: .sm
                ) {}
            }
        }
    }
#endif
