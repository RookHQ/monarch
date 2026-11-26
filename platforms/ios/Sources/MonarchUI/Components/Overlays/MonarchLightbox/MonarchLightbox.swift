import SwiftUI

/// A full-screen viewer for images and videos: swipe or step to the previous
/// and next item, zoom, captions and a position count.
///
/// Present it with `monarchLightbox(isPresented:items:selection:)`, which
/// covers the screen:
///
/// ```swift
/// Button("View photos") { isShown = true }
///     .monarchLightbox(isPresented: $isShown, items: photos, selection: $index)
/// ```
///
/// Use the view itself to put the viewer in a presentation of your own. Swipe
/// sideways to move between items, pinch or double-tap an image to zoom, and
/// drag to pan while zoomed. Zoom belongs to one item: moving to another item
/// starts fitted.
///
/// Accessibility: the viewer is a modal group named "Media viewer". Each
/// image is read with its `alt` text and its zoom level, and VoiceOver's
/// adjust and zoom gestures change the zoom. Moving to another item announces
/// the position and what the item shows. Every action is also a button with a
/// 44-point touch target, and the escape gesture closes the viewer. Zoom
/// animates unless Reduce Motion is on.
public struct MonarchLightbox: View {
    private let items: [MonarchLightboxItem]
    @Binding private var selection: Int
    private let isLooping: Bool
    private let maximumZoom: Double
    private let labels: MonarchLightboxLabels
    private let onClose: @MainActor () -> Void

    @State private var zoom = MonarchLightboxTokens.fitZoom
    @ScaledMetric(relativeTo: .body) private var captionMaximumHeight = MonarchLightboxTokens.captionMaximumHeight
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// A media viewer.
    ///
    /// - Parameters:
    ///   - items: The images and videos, in order.
    ///   - selection: The index of the item on screen, starting at 0. An
    ///     index outside the items shows the nearest item.
    ///   - isLooping: Whether stepping past the last item goes to the first,
    ///     and back.
    ///   - maximumZoom: The largest zoom factor of an image.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClose: Called by the close button and the escape gesture.
    ///     Dismiss the presentation here.
    public init(
        _ items: [MonarchLightboxItem],
        selection: Binding<Int>,
        isLooping: Bool = false,
        maximumZoom: Double = 4,
        labels: MonarchLightboxLabels = MonarchLightboxLabels(),
        onClose: @escaping @MainActor () -> Void
    ) {
        self.items = items
        _selection = selection
        self.isLooping = isLooping
        self.maximumZoom = maximumZoom
        self.labels = labels
        self.onClose = onClose
    }

    private var index: Int {
        MonarchLightboxTokens.clampedIndex(selection, count: items.count)
    }

    private var current: MonarchLightboxItem? {
        items.isEmpty ? nil : items[index]
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: MonarchLightboxTokens.sectionSpacing) {
            MonarchLightboxToolbar(
                position: MonarchLightboxTokens.positionText(index: index, count: items.count, labels: labels),
                isZoomable: MonarchLightboxTokens.isZoomable(current),
                zoom: $zoom,
                maximumZoom: maximumZoom,
                shareURL: current?.shareURL,
                labels: labels,
                onClose: onClose
            )
            MonarchLightboxPager(
                items: items,
                selection: Binding(get: { index }, set: { selection = $0 }),
                zoom: $zoom,
                maximumZoom: maximumZoom,
                labels: labels
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .layoutPriority(1)
            .overlay { sideSteps }
            // The stack keeps the caption at its full height beside the pager.
            ZStack { caption }.frame(maxWidth: .infinity).fixedSize(horizontal: false, vertical: true)
        }
        .padding(MonarchLightboxTokens.padding)
        .background(
            ZStack {
                theme.colors[role: MonarchLightboxTokens.canvas]
                // The dimmed backdrop of the web lightbox.
                theme.colors[role: \.overlayScrim]
            }
            .ignoresSafeArea()
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.title ?? MonarchLightboxStrings.title)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) { onClose() }
        .onChange(of: index) { _, newIndex in
            // Zoom belongs to one item: another item starts fitted.
            zoom = MonarchLightboxTokens.fitZoom
            let text = MonarchLightboxTokens.announcement(index: newIndex, items: items, labels: labels)
            AccessibilityNotification.Announcement(text).post()
        }
    }

    // MARK: Steps

    /// The previous and next buttons, round and at the two sides of the media, as on the web.
    @ViewBuilder private var sideSteps: some View {
        if items.count > 1 {
            HStack {
                stepButton(
                    MonarchLightboxTokens.previousSymbol,
                    label: labels.previous ?? MonarchLightboxStrings.previous,
                    offset: -1
                )
                .keyboardShortcut(.leftArrow, modifiers: [])
                Spacer(minLength: 0)
                stepButton(
                    MonarchLightboxTokens.nextSymbol,
                    label: labels.next ?? MonarchLightboxStrings.next,
                    offset: 1
                )
                .keyboardShortcut(.rightArrow, modifiers: [])
            }
        }
    }

    @ViewBuilder private var caption: some View {
        if let text = current?.caption, !text.isEmpty {
            // A caption taller than the limit scrolls, so it never takes the
            // stage away from the media.
            ViewThatFits(in: .vertical) {
                captionText(text)
                ScrollView { captionText(text) }
            }
            .frame(maxHeight: captionMaximumHeight)
            .padding(.vertical, MonarchLightboxTokens.captionVerticalPadding)
            .padding(.horizontal, MonarchLightboxTokens.captionHorizontalPadding)
            .background(theme.colors[role: MonarchLightboxTokens.raised], in: MonarchLightboxTokens.mediaShape)
        }
    }

    private func captionText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.label)
            .multilineTextAlignment(.center)
            .foregroundStyle(theme.colors.textDefault)
    }

    private func stepButton(_ symbol: String, label: String, offset: Int) -> some View {
        let target = MonarchLightboxTokens.index(
            from: index,
            offset: offset,
            count: items.count,
            isLooping: isLooping
        )
        return Button {
            guard let target else { return }
            withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                selection = target
            }
        } label: {
            MonarchIcon(systemName: symbol)
                .foregroundStyle(theme.colors.textDefault)
                .frame(width: MonarchLightboxTokens.stepSize, height: MonarchLightboxTokens.stepSize)
                .background(theme.colors[role: MonarchLightboxTokens.raised], in: Circle())
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .opacity(target == nil ? 0.5 : 1)
        .accessibilityLabel(label)
        .disabled(target == nil)
    }
}

#if DEBUG
    #Preview("MonarchLightbox") {
        MonarchPreviewMatrix {
            MonarchLightbox(
                [
                    MonarchLightboxItem(
                        image: Image(systemName: "photo"),
                        alt: "A placeholder picture",
                        caption: "The first of three pictures"
                    ),
                    MonarchLightboxItem(image: Image(systemName: "photo.fill"), alt: "A second placeholder picture"),
                    MonarchLightboxItem(image: nil, alt: "A picture that is missing"),
                ],
                selection: .constant(0),
                onClose: {}
            )
            .frame(height: MonarchLightboxTokens.previewHeight)
        }
    }
#endif
