import SwiftUI

/// An inline status message with a tone, an optional title and one optional
/// action.
///
/// A banner sits in the layout, next to the content it is about. For a
/// message that interrupts, use the system `alert` modifier instead.
///
/// ```swift
/// MonarchBanner(
///     "Payment failed",
///     message: "Check the card details and try again.",
///     tone: .danger,
///     actionTitle: "Retry"
/// ) {
///     retry()
/// }
/// ```
///
/// For more than one action, or a control that is not a button, pass the
/// actions as views:
///
/// ```swift
/// MonarchBanner("Verify your email", message: "We sent a link to ana@example.com.", tone: .warning) {
///     MonarchButton("Resend", size: .sm) { resend() }
///     MonarchButton("Change email", variant: .ghost, size: .sm) { edit() }
/// }
/// ```
///
/// `systemImage` replaces the tone's symbol, for a banner whose subject has
/// its own symbol (a lock, a cloud, a sparkle); the tone still sets the
/// colours and the spoken tone name.
///
/// Accessibility: the icon, title and message are one element, read as the
/// tone name ("Warning"), then the title, then the message, so the tone does
/// not depend on colour or on the icon. The action stays a separate button
/// with a 44-point touch target. The banner does not announce itself when it
/// appears; post an `AccessibilityNotification.Announcement` when you insert
/// one in response to an event.
public struct MonarchBanner: View {
    private let title: String?
    private let message: String?
    private let tone: MonarchTone
    private let toneLabel: String?
    private let systemImage: String?
    private let actionTitle: String?
    private let action: (@MainActor () -> Void)?
    private let actions: AnyView?

    @Environment(\.monarchTheme) private var theme

    /// A banner.
    ///
    /// - Parameters:
    ///   - title: A short title, already localised.
    ///   - message: The message, already localised. `nil` for a banner that is only a title, as the web's
    ///     `<Alert title="Saved" />`.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - systemImage: Replaces the tone's SF Symbol.
    ///   - actionTitle: The title of the action button. No button without it.
    ///   - action: Runs when the action button is activated.
    public init(
        _ title: String? = nil,
        message: String? = nil,
        tone: MonarchTone = .info,
        toneLabel: String? = nil,
        systemImage: String? = nil,
        actionTitle: String? = nil,
        action: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.tone = tone
        self.toneLabel = toneLabel
        self.systemImage = systemImage
        self.actionTitle = actionTitle
        self.action = action
        self.actions = nil
    }

    /// A banner with actions of its own, laid out in a row that wraps to a
    /// column when it does not fit.
    ///
    /// - Parameters:
    ///   - title: A short title, already localised.
    ///   - message: The message, already localised, or `nil` for a title only.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - systemImage: Replaces the tone's SF Symbol.
    ///   - actions: The actions, such as small ``MonarchButton``s.
    public init<Actions: View>(
        _ title: String? = nil,
        message: String? = nil,
        tone: MonarchTone = .info,
        toneLabel: String? = nil,
        systemImage: String? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.message = message
        self.tone = tone
        self.toneLabel = toneLabel
        self.systemImage = systemImage
        self.actionTitle = nil
        self.action = nil
        self.actions = AnyView(actions())
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchBannerTokens.appearance(tone: tone)
        let shape = RoundedRectangle(cornerRadius: MonarchBannerTokens.cornerRadius, style: .continuous)
        // As the web alert: the icon, the text, then the actions on the trailing edge, centred.
        HStack(alignment: .center, spacing: MonarchBannerTokens.iconSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchBannerTokens.iconSpacing) {
                icon
                    .foregroundStyle(theme.colors[role: appearance.icon])
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: MonarchBannerTokens.textSpacing) {
                    if let title {
                        Text(title)
                            .monarchTextStyle(.label, weight: MonarchBannerTokens.titleWeight)
                            .foregroundStyle(theme.colors.textDefault)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    if let message {
                        Text(message)
                            .monarchTextStyle(.label, weight: .regular)
                            .foregroundStyle(
                                theme.colors[role: MonarchBannerTokens.messageColor(hasTitle: title != nil)]
                            )
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchBannerTokens.spokenLabel(
                        tone: toneLabel ?? MonarchToneStrings.name(tone),
                        title: title,
                        message: message
                    )
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let actions {
                MonarchRowOrStack(spacing: MonarchBannerTokens.actionSpacing) { actions }
                    .fixedSize()
            } else if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.monarch(.secondary, size: .sm))
                    .fixedSize()
            }
        }
        .padding(.horizontal, MonarchBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchBannerTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: appearance.background]))
        .overlay(
            shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchBannerTokens.borderWidth)
        )
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchBannerTokens.symbol(tone: tone, override: systemImage)
        if let spokenTone = toneLabel ?? MonarchToneStrings.name(tone) {
            MonarchIcon(systemName: symbol, label: spokenTone, size: .sm)
        } else {
            MonarchIcon(systemName: symbol, size: .sm)
        }
    }
}

#if DEBUG
    #Preview("MonarchBanner") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchBanner("Title", message: "A short message.", tone: tone, actionTitle: "Action") {}
                }
                MonarchBanner(
                    "Verify your email",
                    message: "We sent a link.",
                    tone: .warning,
                    systemImage: "envelope"
                ) {
                    MonarchButton("Resend", size: .sm) {}
                    MonarchButton("Change email", variant: .ghost, size: .sm) {}
                }
            }
        }
    }
#endif
