import SwiftUI

/// The button that opens and closes an AI assistant.
///
/// The launcher is a standard `Button` that toggles `isOpen`. Place the
/// floating launcher yourself, where the system keeps it clear of bars and
/// the keyboard:
///
/// ```swift
/// content
///     .safeAreaInset(edge: .bottom, alignment: .trailing) {
///         MonarchAiLauncher(isOpen: $isAssistantOpen, badge: .unread)
///             .padding(theme.spacing.space4)
///     }
/// ```
///
/// Use the `subtle` variant in a toolbar or a rail. The shortcut (Command-J
/// by default) works with a hardware keyboard only, and is listed by the
/// system when Command is held on iPad.
///
/// Accessibility: a button named "Assistant". Whether the assistant is open
/// and what the badge means ("New messages", "Working", "3 unread messages")
/// are read as the value. The touch target is at least 44 points. The
/// working ring moves; under Reduce Motion it is a still ring.
public struct MonarchAiLauncher: View {
    @Binding private var isOpen: Bool
    private let systemImage: String?
    private let variant: MonarchAiLauncherVariant
    private let badge: MonarchAiLauncherBadge?
    private let showsLabel: Bool?
    private let shortcut: KeyboardShortcut?
    private let label: String?
    private let badgeLabel: String?
    private let openLabel: String?

    /// An assistant launcher.
    ///
    /// - Parameters:
    ///   - isOpen: Whether the assistant the launcher controls is open.
    ///   - systemImage: An SF Symbol that replaces the AI glyph. Never a sparkle.
    ///   - variant: How strongly the launcher is drawn.
    ///   - badge: An unread dot, a working ring or a count.
    ///   - showsLabel: Whether the label is shown beside the glyph. By default only the `subtle` variant shows it.
    ///   - shortcut: The hardware keyboard shortcut that toggles `isOpen`, or `nil` for none.
    ///   - label: Replaces the built-in name "Assistant".
    ///   - badgeLabel: Replaces the built-in spoken text of the badge.
    ///   - openLabel: Replaces the built-in "Open" that is spoken while the assistant is open.
    public init(
        isOpen: Binding<Bool>,
        systemImage: String? = nil,
        variant: MonarchAiLauncherVariant = .solid,
        badge: MonarchAiLauncherBadge? = nil,
        showsLabel: Bool? = nil,
        shortcut: KeyboardShortcut? = KeyboardShortcut("j", modifiers: .command),
        label: String? = nil,
        badgeLabel: String? = nil,
        openLabel: String? = nil
    ) {
        _isOpen = isOpen
        self.systemImage = systemImage
        self.variant = variant
        self.badge = badge
        self.showsLabel = showsLabel
        self.shortcut = shortcut
        self.label = label
        self.badgeLabel = badgeLabel
        self.openLabel = openLabel
    }

    /// The content of the view.
    public var body: some View {
        let text = label ?? MonarchAiLauncherStrings.label
        let isLabelShown = MonarchAiLauncherTokens.showsLabel(showsLabel, variant: variant)
        Button {
            isOpen.toggle()
        } label: {
            HStack(spacing: MonarchAiLauncherTokens.spacing) {
                glyph
                if isLabelShown {
                    Text(text)
                }
            }
        }
        .buttonStyle(
            MonarchAiLauncherButtonStyle(variant: variant, isOpen: isOpen, badge: badge, isLabelShown: isLabelShown)
        )
        .keyboardShortcut(shortcut)
        .accessibilityLabel(text)
        .accessibilityValue(
            MonarchAiLauncherTokens.accessibilityValue(
                isOpen: isOpen,
                openLabel: openLabel ?? MonarchAiLauncherStrings.open,
                badgeLabel: badgeLabel ?? MonarchAiLauncherStrings.badge(badge)
            )
        )
    }

    @ViewBuilder private var glyph: some View {
        let size = MonarchAiLauncherTokens.iconSize(variant)
        switch MonarchAiLauncherTokens.glyph(isOpen: isOpen, variant: variant, systemImage: systemImage) {
        case .close: MonarchIcon(systemName: MonarchAiLauncherTokens.closeSymbol, size: size)
        case .symbol(let name): MonarchIcon(systemName: name, size: size)
        case .bot: MonarchAiBotGlyph(size: size)
        }
    }
}

/// The surface of the launcher, with its badge.
private struct MonarchAiLauncherButtonStyle: ButtonStyle {
    let variant: MonarchAiLauncherVariant
    let isOpen: Bool
    let badge: MonarchAiLauncherBadge?
    let isLabelShown: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchAiLauncherButtonStyleBody(
            configuration: configuration,
            variant: variant,
            isOpen: isOpen,
            badge: badge,
            isLabelShown: isLabelShown
        )
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchAiLauncherButtonStyleBody: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchAiLauncherVariant
    let isOpen: Bool
    let badge: MonarchAiLauncherBadge?
    let isLabelShown: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiLauncherTokens.appearance(
            variant: variant,
            isOpen: isOpen,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let minSize = MonarchAiLauncherTokens.minSize(variant)
        let shape = RoundedRectangle(cornerRadius: MonarchAiLauncherTokens.cornerRadius(variant), style: .continuous)
        configuration.label
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: appearance.foreground])
            .padding(
                .horizontal,
                isLabelShown ? MonarchAiLauncherTokens.labelHorizontalPadding : MonarchAiLauncherTokens.padding
            )
            .padding(.vertical, MonarchAiLauncherTokens.padding)
            .frame(minWidth: minSize, minHeight: minSize)
            .monarchElevation(
                MonarchAiLauncherTokens.elevation(variant),
                fill: appearance.background ?? \.bgSurface,
                in: shape
            )
            .overlay {
                if let border = appearance.border {
                    shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchAiLauncherTokens.borderWidth)
                }
            }
            .overlay {
                if badge == .working {
                    MonarchAiLauncherRing(shape: shape)
                }
            }
            .overlay(alignment: .topTrailing) {
                MonarchAiLauncherBadgeMark(badge: badge)
                    .alignmentGuide(.top) { $0[.top] + MonarchAiLauncherTokens.badgeOutset }
                    .alignmentGuide(.trailing) { $0[.trailing] - MonarchAiLauncherTokens.badgeOutset }
            }
            .contentShape(shape)
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

/// The unread dot or the count, ringed in the surface colour so it reads on any background.
private struct MonarchAiLauncherBadgeMark: View {
    let badge: MonarchAiLauncherBadge?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let ring = MonarchAiLauncherTokens.badgeBorderWidth
        switch badge {
        case .unread:
            Circle()
                .fill(theme.colors.dangerSolid)
                .frame(width: MonarchAiLauncherTokens.dotSize, height: MonarchAiLauncherTokens.dotSize)
                .overlay(Circle().strokeBorder(theme.colors.bgSurface, lineWidth: ring))
                .accessibilityHidden(true)
        case .count(let count):
            if let text = MonarchAiLauncherTokens.countText(count) {
                Text(text)
                    .monarchTextStyle(.caption, weight: MonarchAiLauncherTokens.countWeight)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.dangerOnSolid)
                    .padding(.horizontal, MonarchAiLauncherTokens.countHorizontalPadding)
                    .frame(
                        minWidth: MonarchAiLauncherTokens.countMinSize,
                        minHeight: MonarchAiLauncherTokens.countMinSize
                    )
                    .background(Capsule().fill(theme.colors.dangerSolid))
                    .overlay(Capsule().strokeBorder(theme.colors.bgSurface, lineWidth: ring))
                    .accessibilityHidden(true)
            }
        case .working, .none:
            EmptyView()
        }
    }
}

/// The working ring: a part of the outline that travels around the button.
/// Under Reduce Motion the whole outline is drawn and nothing moves.
private struct MonarchAiLauncherRing: View {
    let shape: RoundedRectangle

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let outline = shape.inset(by: -(MonarchAiLauncherTokens.ringOutset + MonarchAiLauncherTokens.ringWidth / 2))
        let style = StrokeStyle(lineWidth: MonarchAiLauncherTokens.ringWidth, lineCap: .round)
        Group {
            if reduceMotion {
                outline.stroke(theme.colors.aiThinking, style: style)
            } else {
                TimelineView(.animation) { context in
                    let phase = context.date.timeIntervalSinceReferenceDate / MonarchAiLauncherTokens.ringPeriod
                    ZStack {
                        ForEach(
                            Array(MonarchAiLauncherTokens.ringSegments(phase: phase).enumerated()),
                            id: \.offset
                        ) { _, segment in
                            outline
                                .trim(from: segment.lowerBound, to: segment.upperBound)
                                .stroke(theme.colors.aiThinking, style: style)
                        }
                    }
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchAiLauncher") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    MonarchAiLauncher(isOpen: .constant(false))
                    MonarchAiLauncher(isOpen: .constant(true))
                    MonarchAiLauncher(isOpen: .constant(false), badge: .unread)
                    MonarchAiLauncher(isOpen: .constant(false), badge: .working)
                    MonarchAiLauncher(isOpen: .constant(false), badge: .count(128))
                }
                HStack {
                    MonarchAiLauncher(isOpen: .constant(false), variant: .subtle)
                    MonarchAiLauncher(isOpen: .constant(true), variant: .subtle, badge: .count(3))
                }
                MonarchAiLauncher(isOpen: .constant(false), showsLabel: true).disabled(true)
            }
        }
    }
#endif
