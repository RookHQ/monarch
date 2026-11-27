import SwiftUI

/// The cookie or tracking notice of an app that shows web content or measures use: a card at the bottom of the
/// screen with the regime's message, a link to the policy and equal choices.
///
/// ```swift
/// content
///     .safeAreaInset(edge: .bottom) {
///         if consent == nil {
///             MonarchCookieConsentBanner(
///                 policyURL: URL(string: "https://example.com/cookies"),
///                 onCustomize: { showsChoices = true }
///             ) { choice in
///                 consent = choice
///             }
///             .padding()
///         }
///     }
///     .sheet(isPresented: $showsChoices) { MonarchConsentPreferences(...) }
/// ```
///
/// Where consent is asked first (`optIn`) it offers Customize, Reject all and Accept all, with rejecting as easy
/// as accepting. Where people opt out (`optOut`) it offers "Do not sell or share my personal information" and OK;
/// a plain `notice` offers OK. Customize calls `onCustomize`, where the app presents ``MonarchConsentPreferences``
/// in a sheet. The banner stores nothing: the app records the choice. Tracking across other companies' apps and
/// websites also needs the system's App Tracking Transparency prompt, which this banner does not replace.
///
/// Accessibility: the card is a container named by its title, which is a heading. VoiceOver announces the banner
/// when it appears, without moving focus. The policy link is a link inside the message, and each button stays its
/// own element.
public struct MonarchCookieConsentBanner: View {
    private let regime: MonarchConsentRegime
    private let policyURL: URL?
    private let isGlobalPrivacyControl: Bool
    private let labels: MonarchCookieConsentBannerLabels
    private let onCustomize: (@MainActor () -> Void)?
    private let onDecide: @MainActor (MonarchCookieConsentChoice) -> Void

    @Environment(\.monarchTheme) private var theme

    /// A cookie consent banner.
    ///
    /// - Parameters:
    ///   - regime: Ask first (`optIn`, the default), opt out (`optOut`) or a plain `notice`.
    ///   - policyURL: The cookie policy, linked at the end of the message.
    ///   - isGlobalPrivacyControl: Whether a Global Privacy Control signal was honoured. In the opt-out regime the
    ///     opt-out button is then replaced by a line that says so.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCustomize: Opens the per-category choices. Without it the Customize button is not shown.
    ///   - onDecide: Records the person's choice.
    public init(
        regime: MonarchConsentRegime = .optIn,
        policyURL: URL? = nil,
        isGlobalPrivacyControl: Bool = false,
        labels: MonarchCookieConsentBannerLabels = MonarchCookieConsentBannerLabels(),
        onCustomize: (@MainActor () -> Void)? = nil,
        onDecide: @escaping @MainActor (MonarchCookieConsentChoice) -> Void
    ) {
        self.regime = regime
        self.policyURL = policyURL
        self.isGlobalPrivacyControl = isGlobalPrivacyControl
        self.labels = labels
        self.onCustomize = onCustomize
        self.onDecide = onDecide
    }

    /// The content of the view.
    public var body: some View {
        let title = labels.title ?? MonarchCookieConsentBannerStrings.title
        VStack(alignment: .leading, spacing: MonarchCookieConsentBannerTokens.spacing) {
            VStack(alignment: .leading, spacing: MonarchCookieConsentBannerTokens.textSpacing) {
                Text(title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                Text(message)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchCookieConsentBannerTokens.messageColor])
                    .tint(theme.colors[role: MonarchCookieConsentBannerTokens.linkColor])
                if regime == .optOut && isGlobalPrivacyControl {
                    Text(labels.globalPrivacyControl ?? MonarchCookieConsentBannerStrings.globalPrivacyControl)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors.textDefault)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            MonarchFlowLayout(spacing: MonarchCookieConsentBannerTokens.actionSpacing) {
                ForEach(
                    MonarchCookieConsentBannerTokens.choices(
                        regime: regime,
                        isGlobalPrivacyControl: isGlobalPrivacyControl,
                        allowsCustomize: onCustomize != nil
                    ),
                    id: \.self
                ) { action in
                    MonarchButton(
                        MonarchCookieConsentBannerTokens.title(action, labels: labels),
                        variant: MonarchCookieConsentBannerTokens.variant(action),
                        size: .sm
                    ) {
                        switch action {
                        case .customize: onCustomize?()
                        case .choice(let choice): onDecide(choice)
                        }
                    }
                }
            }
        }
        .padding(MonarchCookieConsentBannerTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchElevation(
            MonarchCookieConsentBannerTokens.elevation,
            fill: MonarchCookieConsentBannerTokens.fill,
            in: MonarchShapes.container
        )
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchCookieConsentBannerTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .onAppear {
            AccessibilityNotification.Announcement(
                labels.announcement ?? MonarchCookieConsentBannerStrings.announcement
            )
            .post()
        }
    }

    /// The message with the policy link at its end.
    private var message: AttributedString {
        var text = AttributedString(labels.message ?? MonarchCookieConsentBannerTokens.message(regime))
        if let policyURL {
            var link = AttributedString(labels.policy ?? MonarchCookieConsentBannerStrings.policy)
            link.link = policyURL
            link.underlineStyle = .single
            text += AttributedString(" ") + link
        }
        return text
    }
}

#if DEBUG
    #Preview("MonarchCookieConsentBanner") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCookieConsentBanner(policyURL: URL(string: "https://example.com/cookies"), onCustomize: {}) {
                    _ in
                }
                MonarchCookieConsentBanner(regime: .optOut) { _ in }
                MonarchCookieConsentBanner(regime: .notice) { _ in }
            }
        }
    }
#endif
