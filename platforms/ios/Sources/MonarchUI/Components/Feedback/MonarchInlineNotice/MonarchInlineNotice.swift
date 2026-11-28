import SwiftUI

/// A one-line message with a tone, attached to a field, a row, a card or a
/// panel.
///
/// ```swift
/// MonarchInlineNotice("This email is already invited.", tone: .warning, variant: .plain)
/// MonarchInlineNotice("Someone else edited this record.", title: "Conflict.", tone: .danger, actionTitle: "Reload") {
///     reload()
/// }
/// ```
///
/// It is a standard `Label` with a Monarch label style. For a block with a
/// title on its own line, use ``MonarchBanner``.
///
/// Accessibility: the icon, title and message are one element, read as the
/// tone name, the title, then the message, so the tone does not depend on
/// colour. The action and the dismiss button are separate buttons with
/// 44-point touch targets. The notice does not announce itself; post an
/// `AccessibilityNotification.Announcement` when you insert one in response
/// to an event.
public struct MonarchInlineNotice: View {
    private let message: String
    private let title: String?
    private let systemImage: String?
    private let tone: MonarchTone
    private let variant: MonarchInlineNoticeVariant
    private let toneLabel: String?
    private let actionTitle: String?
    private let dismissLabel: String?
    private let action: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// An inline notice.
    ///
    /// - Parameters:
    ///   - message: The message, already localised.
    ///   - title: A short bold lead-in on the same line, such as "Conflict."
    ///   - systemImage: Replaces the SF Symbol of the tone.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - variant: A tinted strip, or icon and coloured text only.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - actionTitle: The title of the action button. No button without it.
    ///   - dismissLabel: Replaces the built-in "Dismiss" spoken name.
    ///   - action: Runs when the action button is activated.
    ///   - onDismiss: Runs when the dismiss button is activated. No button without it.
    public init(
        _ message: String,
        title: String? = nil,
        systemImage: String? = nil,
        tone: MonarchTone = .info,
        variant: MonarchInlineNoticeVariant = .subtle,
        toneLabel: String? = nil,
        actionTitle: String? = nil,
        dismissLabel: String? = nil,
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.message = message
        self.title = title
        self.systemImage = systemImage
        self.tone = tone
        self.variant = variant
        self.toneLabel = toneLabel
        self.actionTitle = actionTitle
        self.dismissLabel = dismissLabel
        self.action = action
        self.onDismiss = onDismiss
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchInlineNoticeTokens.appearance(tone: tone, variant: variant)
        let hasSurface = appearance.background != nil
        HStack(alignment: .center, spacing: MonarchInlineNoticeTokens.actionSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchInlineNoticeTokens.actionSpacing) {
                    label(appearance)
                    Spacer(minLength: 0)
                    actionButton
                }
                VStack(alignment: .leading, spacing: MonarchInlineNoticeTokens.actionSpacing) {
                    label(appearance)
                    actionButton
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                Button(action: onDismiss) {
                    MonarchIcon(systemName: MonarchInlineNoticeTokens.dismissSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(dismissLabel ?? MonarchInlineNoticeStrings.dismiss)
            }
        }
        .padding(.horizontal, hasSurface ? MonarchInlineNoticeTokens.horizontalPadding : 0)
        .padding(.vertical, hasSurface ? MonarchInlineNoticeTokens.verticalPadding : 0)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            if let background = appearance.background {
                MonarchShapes.control.fill(theme.colors[role: background])
            }
        }
        .accessibilityElement(children: .contain)
    }

    private func label(_ appearance: MonarchInlineNoticeTokens.Appearance) -> some View {
        Label {
            text.foregroundStyle(theme.colors[role: appearance.text])
        } icon: {
            MonarchIcon(systemName: MonarchInlineNoticeTokens.symbol(tone: tone, override: systemImage), size: .sm)
                .foregroundStyle(theme.colors[role: appearance.icon])
        }
        .labelStyle(MonarchInlineNoticeLabelStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchInlineNoticeTokens.accessibilityLabel(
                toneName: toneLabel ?? MonarchToneStrings.name(tone),
                title: title,
                message: message
            )
        )
    }

    /// The title and the message as one run of text, so they wrap together.
    private var text: some View {
        MonarchInlineNoticeText(title: title, message: message)
    }

    @ViewBuilder private var actionButton: some View {
        if let actionTitle, let action {
            Button(actionTitle, action: action)
                .buttonStyle(.monarch(.secondary, size: .sm))
        }
    }
}

/// Lays the icon and the text of a notice out on the first baseline with
/// Monarch's spacing.
private struct MonarchInlineNoticeLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchInlineNoticeTokens.iconSpacing) {
            configuration.icon
            configuration.title
        }
    }
}

#if DEBUG
    #Preview("MonarchInlineNotice") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchInlineNoticeVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchInlineNotice("A short message.", title: "Title.", tone: tone, variant: variant)
                    }
                }
                MonarchInlineNotice(
                    "Someone else edited this record.",
                    tone: .warning,
                    actionTitle: "Reload",
                    action: {},
                    onDismiss: {}
                )
            }
        }
    }
#endif

/// The title in semibold and the message in regular weight as one run of text, so they wrap together.
private struct MonarchInlineNoticeText: View {
    let title: String?
    let message: String

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var size: CGFloat

    init(title: String?, message: String) {
        self.title = title
        self.message = message
        let spec = MonarchInlineNoticeTokens.textStyle.spec
        _size = ScaledMetric(wrappedValue: spec.size, relativeTo: spec.relativeTo)
    }

    var body: some View {
        let spec = MonarchInlineNoticeTokens.textStyle.spec
        let regular = MonarchTextStyleModifier.font(
            spec: spec,
            weight: MonarchTextStyleSpec.fontWeight(spec.weight),
            size: size,
            theme: theme
        )
        let strong = MonarchTextStyleModifier.font(spec: spec, weight: .semibold, size: size, theme: theme)
        Group {
            if let title, !title.isEmpty {
                Text(title).font(strong) + Text(" " + message).font(regular)
            } else {
                Text(message).font(regular)
            }
        }
        .tracking(spec.letterSpacing * size)
        .lineSpacing(max(0, (spec.lineHeight - MonarchTextStyleSpec.systemLineHeight) * size))
    }
}
