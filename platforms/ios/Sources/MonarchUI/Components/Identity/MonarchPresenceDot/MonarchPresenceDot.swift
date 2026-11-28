import SwiftUI

/// A small dot for a person's presence: online, away, busy, do not disturb or
/// offline.
///
/// ```swift
/// MonarchPresenceDot(.away, customStatus: "In a meeting")
/// ```
///
/// The app decides the presence; the dot subscribes to nothing. To place it on
/// an avatar, use `overlay(alignment: .bottomTrailing)` with `isOverlay`.
///
/// Accessibility: the dot is read as its text, such as "Away · In a meeting",
/// so the colour is never the only signal for VoiceOver. Do not disturb has a
/// bar and offline is a ring; with Differentiate Without Colour, away is
/// half-filled and busy is a square, so every presence has its own form. Pass
/// `isDecorative` when the same information is in text beside the dot. The
/// dot scales with Dynamic Type.
public struct MonarchPresenceDot: View {
    private let status: MonarchPresenceStatus
    private let customStatus: String?
    private let size: MonarchPresenceDotSize
    private let isDecorative: Bool
    private let statusLabel: String?
    private let isOverlay: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor
    @ScaledMetric private var diameter: CGFloat

    /// A presence dot.
    ///
    /// - Parameters:
    ///   - status: The presence to show.
    ///   - customStatus: The person's own status message, added to the spoken text.
    ///   - size: The diameter.
    ///   - isDecorative: Whether to hide the dot from VoiceOver, when nearby text says the same.
    ///   - statusLabel: Replaces the built-in name of the presence.
    ///   - isOverlay: Whether to draw a ring in the surface colour around the dot, for a dot on an avatar.
    public init(
        _ status: MonarchPresenceStatus,
        customStatus: String? = nil,
        size: MonarchPresenceDotSize = .md,
        isDecorative: Bool = false,
        statusLabel: String? = nil,
        isOverlay: Bool = false
    ) {
        self.status = status
        self.customStatus = customStatus
        self.size = size
        self.isDecorative = isDecorative
        self.statusLabel = statusLabel
        self.isOverlay = isOverlay
        _diameter = ScaledMetric(wrappedValue: MonarchPresenceDotTokens.diameter(size), relativeTo: .body)
    }

    /// The text a presence is read as: its name, joined with the person's
    /// status message when there is one, such as "Away · In a meeting".
    ///
    /// - Parameters:
    ///   - status: The presence.
    ///   - customStatus: The person's own status message.
    /// - Returns: The text, from the component's strings table.
    public nonisolated static func label(for status: MonarchPresenceStatus, customStatus: String? = nil) -> String {
        MonarchPresenceDotTokens.label(status, customStatus: customStatus, statusLabel: nil)
    }

    /// The content of the view.
    public var body: some View {
        let label = MonarchPresenceDotTokens.label(status, customStatus: customStatus, statusLabel: statusLabel)
        dot
            .frame(width: diameter, height: diameter)
            .padding(isOverlay ? MonarchPresenceDotTokens.ringWidth : 0)
            .background {
                if isOverlay {
                    Circle().fill(theme.colors[role: MonarchPresenceDotTokens.surface])
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityHidden(isDecorative)
    }

    @ViewBuilder private var dot: some View {
        let fill = theme.colors[role: MonarchPresenceDotTokens.fill(status)]
        let surface = theme.colors[role: MonarchPresenceDotTokens.surface]
        switch MonarchPresenceDotTokens.form(status, differentiateWithoutColor: differentiateWithoutColor) {
        case .filled:
            Circle().fill(fill)
        case .half:
            Circle()
                .fill(surface)
                .overlay(alignment: .leading) {
                    Rectangle().fill(fill).frame(width: diameter * MonarchPresenceDotTokens.halfRatio)
                }
                .clipShape(Circle())
                .overlay(Circle().strokeBorder(fill, lineWidth: MonarchShapes.borderWidth))
        case .square:
            RoundedRectangle(cornerRadius: MonarchPresenceDotTokens.squareRadius).fill(fill)
        case .barred:
            Circle()
                .fill(fill)
                .overlay {
                    Capsule()
                        .fill(theme.colors[role: MonarchPresenceDotTokens.bar])
                        .frame(
                            width: diameter * MonarchPresenceDotTokens.barWidthRatio,
                            height: diameter * MonarchPresenceDotTokens.barHeightRatio
                        )
                }
        case .ring:
            Circle()
                .fill(surface)
                .overlay(Circle().strokeBorder(fill, lineWidth: MonarchPresenceDotTokens.ringWidth))
        }
    }
}

#if DEBUG
    #Preview("MonarchPresenceDot") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchPresenceDotSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
                            MonarchPresenceDot(status, size: size)
                        }
                        MonarchPresenceDot(.online, size: size, isOverlay: true)
                    }
                }
            }
        }
    }
#endif
