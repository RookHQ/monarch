import SwiftUI

extension View {
    /// Presents a ``MonarchLightbox`` over the whole screen.
    ///
    /// ```swift
    /// Button("View photos") { isShown = true }
    ///     .monarchLightbox(isPresented: $isShown, items: photos, selection: $index)
    /// ```
    ///
    /// The viewer is a full-screen cover, so the system confines VoiceOver
    /// and keyboard focus to it and returns focus when it closes. The close
    /// button and the escape gesture set `isPresented` to `false`.
    ///
    /// - Parameters:
    ///   - isPresented: Whether the viewer is shown.
    ///   - items: The images and videos, in order.
    ///   - selection: The index of the item on screen, starting at 0. Set it
    ///     to the tapped thumbnail before presenting.
    ///   - isLooping: Whether stepping past the last item goes to the first,
    ///     and back.
    ///   - maximumZoom: The largest zoom factor of an image.
    ///   - labels: Replacements for the built-in strings.
    /// - Returns: The view, with the viewer attached.
    public func monarchLightbox(
        isPresented: Binding<Bool>,
        items: [MonarchLightboxItem],
        selection: Binding<Int>,
        isLooping: Bool = false,
        maximumZoom: Double = 4,
        labels: MonarchLightboxLabels = MonarchLightboxLabels()
    ) -> some View {
        modifier(
            MonarchLightboxPresenter(
                isPresented: isPresented,
                items: items,
                selection: selection,
                isLooping: isLooping,
                maximumZoom: maximumZoom,
                labels: labels
            )
        )
    }
}

/// Presents the viewer: a full-screen cover on iOS.
private struct MonarchLightboxPresenter: ViewModifier {
    @Binding var isPresented: Bool
    let items: [MonarchLightboxItem]
    @Binding var selection: Int
    let isLooping: Bool
    let maximumZoom: Double
    let labels: MonarchLightboxLabels

    func body(content: Content) -> some View {
        #if os(iOS)
            content.fullScreenCover(isPresented: $isPresented) { viewer }
        #else
            // fullScreenCover does not exist on macOS, which is only a build
            // convenience: the host build uses a sheet.
            content.sheet(isPresented: $isPresented) { viewer }
        #endif
    }

    private var viewer: some View {
        MonarchLightbox(
            items,
            selection: $selection,
            isLooping: isLooping,
            maximumZoom: maximumZoom,
            labels: labels,
            onClose: { isPresented = false }
        )
    }
}
