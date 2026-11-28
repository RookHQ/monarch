import SwiftUI

/// A success moment for a real milestone, such as a domain verified or a
/// first project published: an icon in a tinted tile with confetti that
/// falls once, a title, a message and the next actions.
///
/// Show it inline at the end of a flow, or as the content of a sheet:
///
/// ```swift
/// .sheet(isPresented: $isVerified) {
///     MonarchCelebration("Domain verified", message: "Email from acme.com is now signed.") {
///         MonarchButton("Continue", variant: .primary, fullWidth: true) { isVerified = false }
///     }
///     .presentationDetents([.medium])
/// }
/// ```
///
/// The confetti is decorative, never flashes, and stays still when Reduce
/// Motion is on. Use it for milestones, not for routine saves (use a toast).
///
/// Accessibility: the title is a heading. When `announces` is on, the title
/// and message are announced when the celebration appears. The icon and the
/// confetti are hidden; say everything in the title and message.
public struct MonarchCelebration<Actions: View>: View {
    private let title: String
    private let message: String?
    private let systemImage: String
    private let tone: MonarchTone
    private let decoration: MonarchCelebrationDecoration
    private let announces: Bool
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var tileSize = MonarchCelebrationTokens.tileSize
    @State private var isPopped = false

    /// A celebration with actions.
    ///
    /// - Parameters:
    ///   - title: What was achieved, such as "Domain verified".
    ///   - message: What it means or what to do next.
    ///   - systemImage: The SF Symbol in the tile. Decorative.
    ///   - tone: The colour of the tile.
    ///   - decoration: Confetti, or none.
    ///   - announces: Whether the title and message are announced when it
    ///     appears. Turn it off when it is part of the screen from the start.
    ///   - actions: The next steps, such as a primary Continue button.
    public init(
        _ title: String,
        message: String? = nil,
        systemImage: String = "checkmark",
        tone: MonarchTone = .success,
        decoration: MonarchCelebrationDecoration = .confetti,
        announces: Bool = true,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.message = message
        self.systemImage = systemImage
        self.tone = tone
        self.decoration = decoration
        self.announces = announces
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: MonarchCelebrationTokens.spacing) {
            visual
            Text(title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if let message {
                Text(message)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if Actions.self != EmptyView.self {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: MonarchCelebrationTokens.spacing) { actions }
                    VStack(spacing: MonarchCelebrationTokens.spacing) { actions }
                }
                .padding(.top, MonarchCelebrationTokens.actionsTopPadding)
            }
        }
        .multilineTextAlignment(.center)
        .padding(.vertical, MonarchCelebrationTokens.verticalPadding)
        .padding(.horizontal, MonarchCelebrationTokens.horizontalPadding)
        .frame(maxWidth: MonarchCelebrationTokens.maxWidth)
        .frame(maxWidth: .infinity)
        .onAppear {
            isPopped = true
            if announces {
                AccessibilityNotification.Announcement(
                    MonarchCelebrationTokens.announcement(title: title, message: message)
                )
                .post()
            }
        }
    }

    private var visual: some View {
        let colors = MonarchCelebrationTokens.tile(tone: tone)
        return MonarchIcon(systemName: systemImage, size: .lg)
            .foregroundStyle(theme.colors[role: colors.foreground])
            .frame(width: tileSize, height: tileSize)
            .background(Circle().fill(theme.colors[role: colors.fill]))
            .scaleEffect(isPopped ? 1 : MonarchCelebrationTokens.popScale)
            .monarchAnimation(.slow, easing: .emphasized, value: isPopped)
            .padding(.top, MonarchCelebrationTokens.visualTopPadding)
            .frame(maxWidth: MonarchCelebrationTokens.visualWidth)
            .background {
                if decoration == .confetti {
                    MonarchCelebrationConfetti()
                }
            }
            .padding(.bottom, MonarchCelebrationTokens.spacing)
            .accessibilityHidden(true)
    }
}

extension MonarchCelebration where Actions == EmptyView {
    /// A celebration without actions.
    ///
    /// - Parameters:
    ///   - title: What was achieved, such as "Domain verified".
    ///   - message: What it means or what to do next.
    ///   - systemImage: The SF Symbol in the tile. Decorative.
    ///   - tone: The colour of the tile.
    ///   - decoration: Confetti, or none.
    ///   - announces: Whether the title and message are announced when it appears.
    public init(
        _ title: String,
        message: String? = nil,
        systemImage: String = "checkmark",
        tone: MonarchTone = .success,
        decoration: MonarchCelebrationDecoration = .confetti,
        announces: Bool = true
    ) {
        self.init(
            title,
            message: message,
            systemImage: systemImage,
            tone: tone,
            decoration: decoration,
            announces: announces,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchCelebration") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCelebration("Domain verified", message: "Email from acme.com is now signed.") {
                    MonarchButton("Continue", variant: .primary) {}
                }
                MonarchCelebration("First project published", tone: .info, decoration: .none, announces: false)
            }
        }
    }
#endif
