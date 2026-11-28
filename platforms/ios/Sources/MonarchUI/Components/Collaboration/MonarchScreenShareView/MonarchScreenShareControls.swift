import SwiftUI

/// The controls of a screen share view: zoom out, the zoom level, zoom in,
/// fit, picture in picture, full screen and stop sharing.
struct MonarchScreenShareControls: View {
    @Binding var zoom: Double
    let showsZoom: Bool
    let isPictureInPictureActive: Bool
    let isFullScreen: Bool
    let labels: MonarchScreenShareViewLabels
    let onPictureInPicture: (@MainActor () -> Void)?
    let onFullScreen: (@MainActor () -> Void)?
    let onStopSharing: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let spacing = MonarchScreenShareViewTokens.controlSpacing
        ViewThatFits(in: .horizontal) {
            HStack(spacing: spacing) {
                zoomControls
                windowControls
            }
            VStack(alignment: .leading, spacing: spacing) {
                HStack(spacing: spacing) { zoomControls }
                HStack(spacing: spacing) { windowControls }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.controls ?? MonarchScreenShareViewStrings.controls)
    }

    @ViewBuilder private var zoomControls: some View {
        if showsZoom {
            iconButton(
                MonarchScreenShareViewTokens.zoomOutSymbol,
                label: labels.zoomOut ?? MonarchScreenShareViewStrings.zoomOut
            ) {
                set(MonarchScreenShareViewTokens.stepOut(from: zoom))
            }
            .disabled(MonarchScreenShareViewTokens.stepOut(from: zoom) == nil)
            Text(MonarchScreenShareViewTokens.percentText(zoom))
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textMuted)
            iconButton(
                MonarchScreenShareViewTokens.zoomInSymbol,
                label: labels.zoomIn ?? MonarchScreenShareViewStrings.zoomIn
            ) {
                set(MonarchScreenShareViewTokens.stepIn(from: zoom))
            }
            .disabled(MonarchScreenShareViewTokens.stepIn(from: zoom) == nil)
            Button(labels.fit ?? MonarchScreenShareViewStrings.fit) {
                set(MonarchScreenShareViewTokens.fitZoom)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .disabled(!MonarchScreenShareViewTokens.isZoomed(zoom))
        }
    }

    @ViewBuilder private var windowControls: some View {
        if showsZoom, let onPictureInPicture {
            iconButton(
                MonarchScreenShareViewTokens.pictureInPictureSymbol(isActive: isPictureInPictureActive),
                label: MonarchScreenShareViewTokens.pictureInPictureLabel(
                    isActive: isPictureInPictureActive,
                    labels: labels
                ),
                action: onPictureInPicture
            )
            .accessibilityAddTraits(isPictureInPictureActive ? .isSelected : [])
        }
        if showsZoom, let onFullScreen {
            iconButton(
                MonarchScreenShareViewTokens.fullScreenSymbol(isFullScreen: isFullScreen),
                label: MonarchScreenShareViewTokens.fullScreenLabel(isFullScreen: isFullScreen, labels: labels),
                action: onFullScreen
            )
        }
        if let onStopSharing {
            Button(labels.stopSharing ?? MonarchScreenShareViewStrings.stopSharing, action: onStopSharing)
                .buttonStyle(.monarch(.danger, size: .sm))
        }
    }

    private func iconButton(_ symbol: String, label: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(label)
    }

    /// Sets a zoom chosen with a button and announces the new level, which
    /// VoiceOver would otherwise not read.
    private func set(_ next: Double?) {
        guard let next else { return }
        zoom = next
        AccessibilityNotification.Announcement(MonarchScreenShareViewTokens.percentText(next)).post()
    }
}
