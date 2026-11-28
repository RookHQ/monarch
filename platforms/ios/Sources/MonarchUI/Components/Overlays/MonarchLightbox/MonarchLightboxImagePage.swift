import SwiftUI

/// One image of a lightbox: fitted to the stage, with pinch and double tap
/// to zoom and a scroll view that pans while zoomed.
struct MonarchLightboxImagePage: View {
    let item: MonarchLightboxItem
    @Binding var zoom: Double
    let maximumZoom: Double
    let labels: MonarchLightboxLabels

    @State private var pinchStart: Double?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        if MonarchLightboxTokens.isZoomable(item) {
            zoomable
        } else {
            MonarchLightboxNotice(text: labels.unavailable ?? MonarchLightboxStrings.unavailable)
        }
    }

    private var zoomable: some View {
        let isZoomed = MonarchLightboxTokens.isZoomed(zoom)
        return GeometryReader { proxy in
            // The scroll view is disabled at "fit", so a horizontal swipe
            // reaches the pager; while zoomed it pans the image instead.
            ScrollView([.horizontal, .vertical]) {
                picture.frame(width: proxy.size.width * zoom, height: proxy.size.height * zoom)
            }
            .scrollDisabled(!isZoomed)
            .scrollIndicators(isZoomed ? .automatic : .hidden)
        }
        .gesture(
            MagnifyGesture()
                .onChanged { value in
                    let start = pinchStart ?? zoom
                    pinchStart = start
                    zoom = MonarchLightboxTokens.clamped(start * value.magnification, maximum: maximumZoom)
                }
                .onEnded { _ in pinchStart = nil }
        )
        .onTapGesture(count: MonarchLightboxTokens.zoomTapCount) {
            withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                zoom = MonarchLightboxTokens.toggled(from: zoom, maximum: maximumZoom)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.alt)
        .accessibilityValue(MonarchLightboxTokens.percentText(zoom))
        .accessibilityAddTraits(.isImage)
        .accessibilityZoomAction { action in
            switch action.direction {
            case .zoomIn: step(MonarchLightboxTokens.zoomedIn(from: zoom, maximum: maximumZoom))
            case .zoomOut: step(MonarchLightboxTokens.zoomedOut(from: zoom, maximum: maximumZoom))
            @unknown default: break
            }
        }
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: step(MonarchLightboxTokens.zoomedIn(from: zoom, maximum: maximumZoom))
            case .decrement: step(MonarchLightboxTokens.zoomedOut(from: zoom, maximum: maximumZoom))
            @unknown default: break
            }
        }
    }

    @ViewBuilder private var picture: some View {
        if let image = item.image {
            fitted(image)
        } else {
            AsyncImage(url: item.url) { phase in
                switch phase {
                case .success(let image):
                    fitted(image)
                case .empty:
                    ProgressView()
                        .accessibilityLabel(labels.loading ?? MonarchLightboxStrings.loading)
                case .failure:
                    MonarchLightboxNotice(text: labels.unavailable ?? MonarchLightboxStrings.unavailable)
                @unknown default:
                    MonarchLightboxNotice(text: labels.unavailable ?? MonarchLightboxStrings.unavailable)
                }
            }
        }
    }

    private func fitted(_ image: Image) -> some View {
        image
            .resizable()
            .scaledToFit()
            .clipShape(MonarchLightboxTokens.mediaShape)
    }

    private func step(_ next: Double?) {
        if let next { zoom = next }
    }
}
