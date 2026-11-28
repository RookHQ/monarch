import SwiftUI

/// The bar above the stage of a lightbox: the position count, the zoom
/// buttons, share and close.
struct MonarchLightboxToolbar: View {
    let position: String?
    let isZoomable: Bool
    @Binding var zoom: Double
    let maximumZoom: Double
    let shareURL: URL?
    let labels: MonarchLightboxLabels
    let onClose: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let spacing = MonarchLightboxTokens.sectionSpacing
        ViewThatFits(in: .horizontal) {
            HStack(spacing: spacing) {
                positionCount
                Spacer(minLength: 0)
                tools {
                    zoomTools
                    windowTools
                }
            }
            VStack(alignment: .trailing, spacing: spacing) {
                HStack(spacing: spacing) {
                    positionCount
                    Spacer(minLength: 0)
                    tools { windowTools }
                }
                if isZoomable {
                    tools { zoomTools }
                }
            }
        }
    }

    @ViewBuilder private var positionCount: some View {
        if let position {
            Text(position)
                .monarchTextStyle(.label)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textDefault)
                .padding(.vertical, MonarchLightboxTokens.positionVerticalPadding)
                .padding(.horizontal, MonarchLightboxTokens.positionHorizontalPadding)
                .background(theme.colors[role: MonarchLightboxTokens.raised], in: Capsule())
        }
    }

    private func tools(@ViewBuilder _ content: () -> some View) -> some View {
        HStack(spacing: MonarchLightboxTokens.toolSpacing) {
            content()
        }
        .padding(MonarchLightboxTokens.toolSpacing)
        .monarchElevation(.presented, fill: MonarchLightboxTokens.raised, in: MonarchShapes.container)
    }

    @ViewBuilder private var zoomTools: some View {
        if isZoomable {
            let zoomedOut = MonarchLightboxTokens.zoomedOut(from: zoom, maximum: maximumZoom)
            let zoomedIn = MonarchLightboxTokens.zoomedIn(from: zoom, maximum: maximumZoom)
            iconButton(MonarchLightboxTokens.zoomOutSymbol, label: labels.zoomOut ?? MonarchLightboxStrings.zoomOut) {
                set(zoomedOut)
            }
            .disabled(zoomedOut == nil)
            Button(MonarchLightboxTokens.percentText(zoom)) {
                set(MonarchLightboxTokens.fitZoom)
            }
            .buttonStyle(.monarch(.ghost))
            .monospacedDigit()
            // The level is never truncated: the bar wraps to two rows instead.
            .fixedSize(horizontal: true, vertical: false)
            .accessibilityLabel(labels.resetZoom ?? MonarchLightboxStrings.resetZoom)
            .accessibilityValue(MonarchLightboxTokens.percentText(zoom))
            .disabled(!MonarchLightboxTokens.isZoomed(zoom))
            iconButton(MonarchLightboxTokens.zoomInSymbol, label: labels.zoomIn ?? MonarchLightboxStrings.zoomIn) {
                set(zoomedIn)
            }
            .disabled(zoomedIn == nil)
        }
    }

    @ViewBuilder private var windowTools: some View {
        if let shareURL {
            ShareLink(item: shareURL) {
                MonarchIcon(systemName: MonarchLightboxTokens.shareSymbol)
            }
            .buttonStyle(.monarch(.ghost))
            .accessibilityLabel(labels.share ?? MonarchLightboxStrings.share)
        }
        iconButton(
            MonarchLightboxTokens.closeSymbol,
            label: labels.close ?? MonarchLightboxStrings.close,
            action: onClose
        )
        .keyboardShortcut(.cancelAction)
    }

    private func iconButton(_ symbol: String, label: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol)
        }
        .buttonStyle(.monarch(.ghost))
        .accessibilityLabel(label)
    }

    /// Sets a zoom chosen with a button and announces the new level, which
    /// VoiceOver would otherwise not read.
    private func set(_ next: Double?) {
        guard let next else { return }
        zoom = next
        AccessibilityNotification.Announcement(MonarchLightboxTokens.percentText(next)).post()
    }
}
