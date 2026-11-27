import SwiftUI

/// A poster for a video, with a play mark and the length of the video.
///
/// Pass an image the app already has, or the URL of a poster image. With
/// `onPlay` the whole thumbnail is a button. To use a frame of the video
/// itself, capture it with ``frame(of:at:)`` and pass the image.
///
/// ```swift
/// MonarchVideoThumbnail("Product tour", url: posterURL, duration: 65) {
///     isPlayerPresented = true
/// }
/// ```
///
/// Accessibility: with `onPlay` it is one button named "Play <title>"; without
/// it, one image named by the title. The length is the value, spoken in
/// words ("1 minute, 5 seconds"). The play mark and the badge are not read
/// separately. The badge text and the play mark scale with Dynamic Type.
public struct MonarchVideoThumbnail: View {
    private let title: String
    private let image: Image?
    private let url: URL?
    private let duration: TimeInterval?
    private let aspectRatio: CGFloat?
    private let contentMode: ContentMode
    private let showsPlayIcon: Bool
    private let playLabel: String?
    private let onPlay: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var playSize = MonarchVideoThumbnailTokens.playSize

    /// A thumbnail with a poster image the app already has.
    ///
    /// - Parameters:
    ///   - title: What the video shows, already localised. Read by VoiceOver; not drawn.
    ///   - image: The poster. A placeholder is shown while it is `nil`.
    ///   - duration: The length of the video in seconds, shown as a badge.
    ///   - aspectRatio: The width divided by the height. 16 to 9 when `nil`.
    ///   - contentMode: `.fill` crops the poster to the frame; `.fit` letterboxes it.
    ///   - showsPlayIcon: Whether the round play mark is drawn over the poster.
    ///   - playLabel: Replaces the built-in "Play <title>" spoken name of the button.
    ///   - onPlay: Makes the thumbnail a button that runs this.
    public init(
        _ title: String,
        image: Image?,
        duration: TimeInterval? = nil,
        aspectRatio: CGFloat? = nil,
        contentMode: ContentMode = .fill,
        showsPlayIcon: Bool = true,
        playLabel: String? = nil,
        onPlay: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.image = image
        self.url = nil
        self.duration = duration
        self.aspectRatio = aspectRatio
        self.contentMode = contentMode
        self.showsPlayIcon = showsPlayIcon
        self.playLabel = playLabel
        self.onPlay = onPlay
    }

    /// A thumbnail with a poster image loaded from a URL.
    ///
    /// - Parameters:
    ///   - title: What the video shows, already localised. Read by VoiceOver; not drawn.
    ///   - url: The URL of the poster image, not of the video. A placeholder is
    ///     shown while it loads, when it fails and when it is `nil`.
    ///   - duration: The length of the video in seconds, shown as a badge.
    ///   - aspectRatio: The width divided by the height. 16 to 9 when `nil`.
    ///   - contentMode: `.fill` crops the poster to the frame; `.fit` letterboxes it.
    ///   - showsPlayIcon: Whether the round play mark is drawn over the poster.
    ///   - playLabel: Replaces the built-in "Play <title>" spoken name of the button.
    ///   - onPlay: Makes the thumbnail a button that runs this.
    public init(
        _ title: String,
        url: URL?,
        duration: TimeInterval? = nil,
        aspectRatio: CGFloat? = nil,
        contentMode: ContentMode = .fill,
        showsPlayIcon: Bool = true,
        playLabel: String? = nil,
        onPlay: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.image = nil
        self.url = url
        self.duration = duration
        self.aspectRatio = aspectRatio
        self.contentMode = contentMode
        self.showsPlayIcon = showsPlayIcon
        self.playLabel = playLabel
        self.onPlay = onPlay
    }

    /// The content of the view.
    public var body: some View {
        let label = MonarchVideoThumbnailTokens.accessibilityLabel(
            title: title,
            isButton: onPlay != nil,
            playLabel: playLabel
        )
        let value = MonarchVideoThumbnailTokens.spokenDuration(duration, locale: locale) ?? ""
        if let onPlay {
            Button(action: onPlay) { frame }
                .buttonStyle(.plain)
                .accessibilityLabel(label)
                .accessibilityValue(value)
        } else {
            frame
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(value)
                .accessibilityAddTraits(.isImage)
        }
    }

    private var hasSource: Bool { image != nil || url != nil }

    private var frame: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchVideoThumbnailTokens.background])
            .aspectRatio(MonarchVideoThumbnailTokens.aspectRatio(aspectRatio), contentMode: .fit)
            .overlay { poster }
            .overlay {
                if showsPlayIcon, hasSource { playMark }
            }
            .overlay(alignment: .bottomTrailing) {
                if let text = MonarchVideoThumbnailTokens.durationText(duration, locale: locale) {
                    MonarchBadge(text, variant: .solid)
                        .padding(MonarchVideoThumbnailTokens.badgeInset)
                }
            }
            .clipShape(MonarchShapes.container)
            .contentShape(MonarchShapes.container)
    }

    /// The poster, by the image convention of `MonarchRemoteImage`, letterboxed
    /// or filling as `contentMode` says; the placeholder stands in while
    /// there is no image.
    @ViewBuilder private var poster: some View {
        if let image {
            MonarchRemoteImage(image: image, contentMode: contentMode) { placeholder }
        } else {
            MonarchRemoteImage(url: url, contentMode: contentMode) { placeholder }
        }
    }

    private var placeholder: some View {
        MonarchIcon(systemName: MonarchVideoThumbnailTokens.playSymbol, size: .lg)
            .foregroundStyle(theme.colors[role: MonarchVideoThumbnailTokens.placeholderGlyph])
    }

    private var playMark: some View {
        MonarchIcon(systemName: MonarchVideoThumbnailTokens.playSymbol, size: .md)
            .foregroundStyle(theme.colors[role: MonarchVideoThumbnailTokens.playGlyph])
            .frame(width: playSize, height: playSize)
            .background(Circle().fill(theme.colors[role: MonarchVideoThumbnailTokens.playBackground]))
    }
}

#if DEBUG
    #Preview("MonarchVideoThumbnail") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchVideoThumbnail("Product tour", image: Image(systemName: "mountain.2.fill"), duration: 65) {}
                MonarchVideoThumbnail("Keynote", image: nil, duration: 3725)
                MonarchVideoThumbnail(
                    "Portrait clip",
                    image: Image(systemName: "person.fill"),
                    aspectRatio: 1,
                    contentMode: .fit,
                    showsPlayIcon: false
                )
            }
        }
    }
#endif
