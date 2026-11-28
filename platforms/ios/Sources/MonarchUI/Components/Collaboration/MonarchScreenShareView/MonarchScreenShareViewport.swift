import SwiftUI

/// The picture area of a screen share view: the video slot in a scroll view
/// that pans while zoomed, with pinch and double tap to zoom.
struct MonarchScreenShareViewport<Video: View>: View {
    @Binding var zoom: Double
    let label: String
    let video: Video

    @State private var pinchStart: Double?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let isZoomed = MonarchScreenShareViewTokens.isZoomed(zoom)
        GeometryReader { proxy in
            ScrollView([.horizontal, .vertical]) {
                video.frame(width: proxy.size.width * zoom, height: proxy.size.height * zoom)
            }
            .scrollDisabled(!isZoomed)
            .scrollIndicators(isZoomed ? .automatic : .hidden)
        }
        .aspectRatio(MonarchScreenShareViewTokens.aspectRatio, contentMode: .fit)
        .background(theme.colors[role: MonarchScreenShareViewTokens.stage])
        .clipShape(MonarchShapes.container)
        .gesture(
            MagnifyGesture()
                .onChanged { value in
                    let start = pinchStart ?? zoom
                    pinchStart = start
                    zoom = MonarchScreenShareViewTokens.clamped(start * value.magnification)
                }
                .onEnded { _ in pinchStart = nil }
        )
        .onTapGesture(count: MonarchScreenShareViewTokens.zoomTapCount) {
            withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                zoom = MonarchScreenShareViewTokens.toggled(from: zoom)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(MonarchScreenShareViewTokens.percentText(zoom))
        .accessibilityAddTraits(.isImage)
        .accessibilityZoomAction { action in
            switch action.direction {
            case .zoomIn: step(MonarchScreenShareViewTokens.stepIn(from: zoom))
            case .zoomOut: step(MonarchScreenShareViewTokens.stepOut(from: zoom))
            @unknown default: break
            }
        }
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: step(MonarchScreenShareViewTokens.stepIn(from: zoom))
            case .decrement: step(MonarchScreenShareViewTokens.stepOut(from: zoom))
            @unknown default: break
            }
        }
    }

    private func step(_ next: Double?) {
        if let next { zoom = next }
    }
}
