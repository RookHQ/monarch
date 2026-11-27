import SwiftUI

/// A banner for a screen or a section: trials, maintenance, billing, or a
/// notice that the view is narrowed.
///
/// ```swift
/// MonarchCalloutBanner(
///     "Your trial ends in 3 days",
///     message: "Add a payment method to keep your workspace.",
///     tone: .warning,
///     placement: .page,
///     actionTitle: "Add payment method",
///     action: { showBilling() },
///     onDismiss: { hideBanner() }
/// )
/// ```
///
/// At the top of a screen, put a `page` banner in
/// `safeAreaInset(edge: .top)` so the content scrolls under it. The banner
/// does not hide itself: remove it from the view in `onDismiss`.
///
/// Accessibility: the icon, title and message are one element, read as the
/// tone name, the title, then the message. The action and the dismiss
/// button are separate buttons with 44-point touch targets. The banner does
/// not announce itself; post an `AccessibilityNotification.Announcement`
/// when you insert one in response to an event.
public struct MonarchCalloutBanner: View {
    private let title: String?
    private let message: String
    private let systemImage: String?
    private let tone: MonarchTone
    private let variant: MonarchCalloutBannerVariant
    private let placement: MonarchCalloutBannerPlacement
    private let toneLabel: String?
    private let actionTitle: String?
    private let actionVariant: MonarchButtonVariant
    private let dismissLabel: String?
    private let action: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// A callout banner.
    ///
    /// - Parameters:
    ///   - title: A short title, already localised.
    ///   - message: The message, already localised.
    ///   - systemImage: Replaces the SF Symbol of the tone.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - variant: A callout, or a compact neutral scope notice.
    ///   - placement: A rounded callout in the content, or a strip across the screen.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - actionTitle: The title of the action button. No button without it.
    ///   - actionVariant: How the action button is drawn: secondary by default, primary for the one action the banner asks for.
    ///   - dismissLabel: Replaces the built-in "Dismiss" spoken name.
    ///   - action: Runs when the action button is activated.
    ///   - onDismiss: Runs when the dismiss button is activated. No button without it.
    public init(
        _ title: String? = nil,
        message: String,
        systemImage: String? = nil,
        tone: MonarchTone = .info,
        variant: MonarchCalloutBannerVariant = .callout,
        placement: MonarchCalloutBannerPlacement = .section,
        toneLabel: String? = nil,
        actionTitle: String? = nil,
        actionVariant: MonarchButtonVariant = .secondary,
        dismissLabel: String? = nil,
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.systemImage = systemImage
        self.tone = tone
        self.variant = variant
        self.placement = placement
        self.toneLabel = toneLabel
        self.actionTitle = actionTitle
        self.actionVariant = actionVariant
        self.dismissLabel = dismissLabel
        self.action = action
        self.onDismiss = onDismiss
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchCalloutBannerTokens.appearance(tone: tone, variant: variant)
        HStack(alignment: .top, spacing: 0) {
            Group {
                switch variant {
                case .callout:
                    stacked(appearance)
                case .scope:
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: MonarchCalloutBannerTokens.actionSpacing) {
                            text(appearance)
                            Spacer(minLength: 0)
                            actionButton
                        }
                        stacked(appearance)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                dismissButton(onDismiss)
            }
        }
        .padding(.horizontal, MonarchCalloutBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchCalloutBannerTokens.verticalPadding(variant))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background { surface(appearance) }
        .accessibilityElement(children: .contain)
    }

    /// The text with the action under it, starting on the same left edge as the title.
    private func stacked(_ appearance: MonarchCalloutBannerTokens.Appearance) -> some View {
        text(appearance, includesAction: true)
    }

    private func text(_ appearance: MonarchCalloutBannerTokens.Appearance, includesAction: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchCalloutBannerTokens.iconSpacing) {
            icon
                .foregroundStyle(theme.colors[role: appearance.icon])
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchCalloutBannerTokens.actionSpacing) {
                VStack(alignment: .leading, spacing: MonarchCalloutBannerTokens.textSpacing) {
                    if let title {
                        Text(title)
                            .monarchTextStyle(.label, weight: MonarchCalloutBannerTokens.titleWeight)
                            .foregroundStyle(theme.colors.textDefault)
                    }
                    Text(message)
                        .monarchTextStyle(MonarchCalloutBannerTokens.messageStyle(variant), weight: .regular)
                        .foregroundStyle(
                            theme.colors[role: MonarchCalloutBannerTokens.messageColor(hasTitle: title != nil)]
                        )
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchSpokenLabel.join(
                        MonarchCalloutBannerTokens.spokenTone(tone: tone, variant: variant, override: toneLabel),
                        title,
                        message
                    )
                )
                if includesAction { actionButton }
            }
        }
    }

    @ViewBuilder private var actionButton: some View {
        if let actionTitle, let action {
            Button(actionTitle, action: action)
                .buttonStyle(.monarch(actionVariant, size: .sm))
        }
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchCalloutBannerTokens.symbol(tone: tone, variant: variant, override: systemImage)
        MonarchIcon(systemName: symbol, size: .sm)
    }

    @ViewBuilder
    private func surface(_ appearance: MonarchCalloutBannerTokens.Appearance) -> some View {
        let fill = theme.colors[role: appearance.background]
        let border = theme.colors[role: appearance.border]
        switch placement {
        case .section:
            MonarchShapes.container
                .fill(fill)
                .overlay(
                    MonarchShapes.container.strokeBorder(border, lineWidth: MonarchCalloutBannerTokens.borderWidth)
                )
        case .page:
            Rectangle()
                .fill(fill)
                .overlay(alignment: .bottom) {
                    Rectangle().fill(border).frame(height: MonarchCalloutBannerTokens.borderWidth)
                }
        }
    }

    private func dismissButton(_ onDismiss: @escaping @MainActor () -> Void) -> some View {
        Button(action: onDismiss) {
            MonarchIcon(systemName: MonarchCalloutBannerTokens.dismissSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(dismissLabel ?? MonarchCalloutBannerStrings.dismiss)
        .padding(.vertical, -MonarchCalloutBannerTokens.dismissVerticalOutset(variant))
        .padding(.trailing, -MonarchCalloutBannerTokens.dismissOutset)
    }
}

#if DEBUG
    #Preview("MonarchCalloutBanner") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCalloutBanner(
                        "Title",
                        message: "A short message.",
                        tone: tone,
                        actionTitle: "Action",
                        action: {},
                        onDismiss: {}
                    )
                }
                MonarchCalloutBanner(
                    message: "Showing tickets in Support",
                    variant: .scope,
                    actionTitle: "Clear",
                    action: {}
                )
                MonarchCalloutBanner(
                    "Scheduled maintenance",
                    message: "Saturday from 02:00 to 04:00.",
                    tone: .warning,
                    placement: .page
                )
            }
        }
    }
#endif
