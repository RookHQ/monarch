import SwiftUI

/// Shows a screen that someone shares in a call: who is presenting, the
/// picture with zoom and pan, and controls.
///
/// The picture is a slot. Put the view of the product's call SDK in it; the
/// component does not capture or decode video.
///
/// ```swift
/// MonarchScreenShareView(
///     "Ana",
///     onPictureInPicture: { startPictureInPicture() },
///     video: { RemoteVideoView(track: screenTrack) }
/// )
/// ```
///
/// Pinch, double tap or use the buttons to zoom from "fit" to three times;
/// drag to pan while zoomed. Picture in picture and full screen belong to
/// the product, because they need the video layer and the presentation: the
/// buttons are shown when `onPictureInPicture` and `onFullScreen` are given.
/// When the local person is the presenter (`isLocal`), the picture is hidden
/// until they ask for it, to avoid a mirror effect, and Stop sharing is
/// offered when `onStopSharing` is given.
///
/// Accessibility: the picture is one element named "Screen shared by Ana"
/// whose value is the zoom level; it responds to the VoiceOver zoom gesture
/// and to swipe up and down. The controls are a named group of buttons with
/// 44-point touch targets, and a new zoom level chosen with a button is
/// announced. The content of the shared screen itself is not described.
public struct MonarchScreenShareView<Video: View>: View {
    private let presenterName: String
    private let externalZoom: Binding<Double>?
    private let isLocal: Bool
    private let isWaiting: Bool
    private let isPictureInPictureActive: Bool
    private let isFullScreen: Bool
    private let labels: MonarchScreenShareViewLabels
    private let showsControls: Bool
    private let onPictureInPicture: (@MainActor () -> Void)?
    private let onFullScreen: (@MainActor () -> Void)?
    private let onStopSharing: (@MainActor () -> Void)?
    private let video: Video

    @State private var internalZoom = MonarchScreenShareViewTokens.fitZoom
    @State private var showsLocalPreview = false
    @Environment(\.monarchTheme) private var theme

    /// A screen share view that keeps the zoom itself.
    ///
    /// - Parameters:
    ///   - presenterName: Who is sharing, as it should be read.
    ///   - isLocal: Whether the person using the app is the one sharing.
    ///   - isWaiting: Whether the shared screen has not arrived yet. A
    ///     waiting text is shown instead of the picture.
    ///   - isPictureInPictureActive: Whether picture in picture is active.
    ///   - isFullScreen: Whether the view is shown full screen.
    ///   - labels: Replacements for the built-in strings.
    ///   - showsControls: Whether the zoom, picture-in-picture and
    ///     full-screen controls are shown.
    ///   - onPictureInPicture: Starts or ends picture in picture. Adds the
    ///     button.
    ///   - onFullScreen: Enters or leaves full screen. Adds the button.
    ///   - onStopSharing: Stops sharing. Adds the button when `isLocal`.
    ///   - video: The picture, from the product's call SDK.
    public init(
        _ presenterName: String,
        isLocal: Bool = false,
        isWaiting: Bool = false,
        isPictureInPictureActive: Bool = false,
        isFullScreen: Bool = false,
        labels: MonarchScreenShareViewLabels = MonarchScreenShareViewLabels(),
        showsControls: Bool = true,
        onPictureInPicture: (@MainActor () -> Void)? = nil,
        onFullScreen: (@MainActor () -> Void)? = nil,
        onStopSharing: (@MainActor () -> Void)? = nil,
        @ViewBuilder video: () -> Video
    ) {
        self.presenterName = presenterName
        self.externalZoom = nil
        self.isLocal = isLocal
        self.isWaiting = isWaiting
        self.isPictureInPictureActive = isPictureInPictureActive
        self.isFullScreen = isFullScreen
        self.labels = labels
        self.showsControls = showsControls
        self.onPictureInPicture = onPictureInPicture
        self.onFullScreen = onFullScreen
        self.onStopSharing = onStopSharing
        self.video = video()
    }

    /// A screen share view whose zoom the app holds.
    ///
    /// - Parameters:
    ///   - presenterName: Who is sharing, as it should be read.
    ///   - zoom: The zoom factor. 1 fits the whole screen in the view; the
    ///     largest is 3.
    ///   - isLocal: Whether the person using the app is the one sharing.
    ///   - isWaiting: Whether the shared screen has not arrived yet. A
    ///     waiting text is shown instead of the picture.
    ///   - isPictureInPictureActive: Whether picture in picture is active.
    ///   - isFullScreen: Whether the view is shown full screen.
    ///   - labels: Replacements for the built-in strings.
    ///   - showsControls: Whether the zoom, picture-in-picture and
    ///     full-screen controls are shown.
    ///   - onPictureInPicture: Starts or ends picture in picture. Adds the
    ///     button.
    ///   - onFullScreen: Enters or leaves full screen. Adds the button.
    ///   - onStopSharing: Stops sharing. Adds the button when `isLocal`.
    ///   - video: The picture, from the product's call SDK.
    public init(
        _ presenterName: String,
        zoom: Binding<Double>,
        isLocal: Bool = false,
        isWaiting: Bool = false,
        isPictureInPictureActive: Bool = false,
        isFullScreen: Bool = false,
        labels: MonarchScreenShareViewLabels = MonarchScreenShareViewLabels(),
        showsControls: Bool = true,
        onPictureInPicture: (@MainActor () -> Void)? = nil,
        onFullScreen: (@MainActor () -> Void)? = nil,
        onStopSharing: (@MainActor () -> Void)? = nil,
        @ViewBuilder video: () -> Video
    ) {
        self.presenterName = presenterName
        self.externalZoom = zoom
        self.isLocal = isLocal
        self.isWaiting = isWaiting
        self.isPictureInPictureActive = isPictureInPictureActive
        self.isFullScreen = isFullScreen
        self.labels = labels
        self.showsControls = showsControls
        self.onPictureInPicture = onPictureInPicture
        self.onFullScreen = onFullScreen
        self.onStopSharing = onStopSharing
        self.video = video()
    }

    /// The zoom in use, limited to the supported range.
    private var zoom: Binding<Double> {
        let source = externalZoom ?? $internalZoom
        return Binding(
            get: { MonarchScreenShareViewTokens.clamped(source.wrappedValue) },
            set: { source.wrappedValue = MonarchScreenShareViewTokens.clamped($0) }
        )
    }

    private var isPreviewHidden: Bool { isLocal && !showsLocalPreview }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchScreenShareViewTokens.sectionSpacing) {
            bar
            if isPreviewHidden {
                notice
            } else if isWaiting {
                placeholder {
                    Text(labels.waiting ?? MonarchScreenShareViewStrings.waiting)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .multilineTextAlignment(.center)
                }
            } else {
                MonarchScreenShareViewport(
                    zoom: zoom,
                    label: MonarchScreenShareViewTokens.regionText(name: presenterName, labels: labels),
                    video: video
                )
            }
        }
    }

    private var bar: some View {
        let presenter = HStack(spacing: MonarchScreenShareViewTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchScreenShareViewTokens.presenterSymbol, size: .sm)
                .foregroundStyle(theme.colors.successStrong)
            Text(MonarchScreenShareViewTokens.presenterText(name: presenterName, isLocal: isLocal, labels: labels))
                .monarchTextStyle(.label)
        }
        .foregroundStyle(theme.colors.textDefault)
        .accessibilityElement(children: .combine)
        let controls = MonarchScreenShareControls(
            zoom: zoom,
            showsZoom: showsControls && !isPreviewHidden && !isWaiting,
            isPictureInPictureActive: isPictureInPictureActive,
            isFullScreen: isFullScreen,
            labels: labels,
            onPictureInPicture: onPictureInPicture,
            onFullScreen: onFullScreen,
            onStopSharing: isLocal ? onStopSharing : nil
        )
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchScreenShareViewTokens.iconSpacing) {
                presenter
                Spacer(minLength: 0)
                controls
            }
            VStack(alignment: .leading, spacing: MonarchScreenShareViewTokens.controlSpacing) {
                presenter
                controls
            }
        }
    }

    private var notice: some View {
        placeholder {
            VStack(spacing: MonarchScreenShareViewTokens.noticeSpacing) {
                Text(labels.localPreviewHidden ?? MonarchScreenShareViewStrings.localPreviewHidden)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .multilineTextAlignment(.center)
                if !isWaiting {
                    Button(labels.showPreview ?? MonarchScreenShareViewStrings.showPreview) {
                        showsLocalPreview = true
                    }
                    .buttonStyle(.monarch(.secondary, size: .sm))
                }
            }
        }
    }

    /// The area that stands in for the picture. It is at least as tall as
    /// the picture would be and grows with its text.
    private func placeholder(@ViewBuilder content: () -> some View) -> some View {
        ZStack {
            // An invisible sizer with the shape of the picture.
            Rectangle()
                .aspectRatio(MonarchScreenShareViewTokens.aspectRatio, contentMode: .fit)
                .hidden()
            content().padding(MonarchScreenShareViewTokens.noticePadding)
        }
        .frame(maxWidth: .infinity)
        .background(MonarchShapes.container.fill(theme.colors[role: MonarchScreenShareViewTokens.placeholder]))
    }
}

#if DEBUG
    #Preview("MonarchScreenShareView") {
        MonarchPreviewMatrix {
            VStack {
                MonarchScreenShareView(
                    "Ana",
                    onPictureInPicture: {},
                    onFullScreen: {},
                    video: { Image(systemName: "macwindow").resizable().scaledToFit() }
                )
                MonarchScreenShareView("Ana", isWaiting: true) { EmptyView() }
                MonarchScreenShareView("You", isLocal: true, onStopSharing: {}, video: { EmptyView() })
            }
        }
    }
#endif
