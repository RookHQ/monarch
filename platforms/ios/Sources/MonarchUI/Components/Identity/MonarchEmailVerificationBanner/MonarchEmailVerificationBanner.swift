import SwiftUI

/// A notice that the person's email address is not verified yet, with the
/// address, a Resend button that waits out a cooldown between sends, and an
/// optional Change email button.
///
/// ```swift
/// MonarchEmailVerificationBanner(
///     email: account.email,
///     initialCooldown: 60,
///     onChangeEmail: { showChangeEmail = true },
///     onResend: { try await api.resendVerification() }
/// )
/// ```
///
/// After a successful send the button reads "Resend in 60s" and counts down;
/// a failed send shows the reason and Resend can be used again at once. The
/// banner looks like ``MonarchCalloutBanner`` and is placed the same way:
/// the `page` placement goes in `safeAreaInset(edge: .top)`.
///
/// Accessibility: the icon, title and message are one element, read as the
/// tone name, the title and the message with the address. Resend, Change
/// email and Dismiss are separate buttons with 44-point touch targets.
/// During the cooldown the resend button is disabled, so VoiceOver still
/// reaches it and reads it as dimmed with the seconds left. "Verification
/// email sent" and a failure are announced, and each has an icon and text.
public struct MonarchEmailVerificationBanner: View {
    private let email: String
    private let tone: MonarchTone
    private let placement: MonarchCalloutBannerPlacement
    private let cooldownSeconds: Int
    private let labels: MonarchEmailVerificationBannerLabels
    private let onChangeEmail: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor () -> Void)?
    private let onResend: @MainActor () async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var state: MonarchResendCooldownState

    /// An email verification banner.
    ///
    /// - Parameters:
    ///   - email: The address waiting for verification.
    ///   - tone: `warning` when features are locked until verified, `info` when nothing is blocked yet. Other tones draw as warning.
    ///   - placement: A rounded callout in the content, or a strip across the top of the screen.
    ///   - cooldownSeconds: Seconds before another send is allowed after a successful one.
    ///   - initialCooldown: Seconds already remaining when the banner appears, for example right after sign-up.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChangeEmail: Shows a Change email button that runs this.
    ///   - onDismiss: Shows a dismiss button that runs this. Remove the banner in it.
    ///   - onResend: Sends the email again. Throw to show the failure; no cooldown starts.
    public init(
        email: String,
        tone: MonarchTone = .warning,
        placement: MonarchCalloutBannerPlacement = .section,
        cooldownSeconds: Int = 60,
        initialCooldown: Int = 0,
        labels: MonarchEmailVerificationBannerLabels = MonarchEmailVerificationBannerLabels(),
        onChangeEmail: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil,
        onResend: @escaping @MainActor () async throws -> Void
    ) {
        self.email = email
        self.tone = MonarchEmailVerificationBannerTokens.effectiveTone(tone)
        self.placement = placement
        self.cooldownSeconds = cooldownSeconds
        self.labels = labels
        self.onChangeEmail = onChangeEmail
        self.onDismiss = onDismiss
        self.onResend = onResend
        _state = State(initialValue: MonarchResendCooldownState(remaining: initialCooldown))
    }

    /// A banner that starts in a given resend state, for tests and previews.
    init(email: String, tone: MonarchTone = .warning, state: MonarchResendCooldownState) {
        self.init(email: email, tone: tone, onResend: {})
        _state = State(initialValue: state)
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchCalloutBannerTokens.appearance(tone: tone, variant: .callout)
        HStack(alignment: .top, spacing: 0) {
            VStack(alignment: .leading, spacing: MonarchEmailVerificationBannerTokens.blockSpacing) {
                text(appearance)
                status
                buttons
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                dismissButton(onDismiss)
            }
        }
        .padding(.horizontal, MonarchCalloutBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchCalloutBannerTokens.verticalPadding(.callout))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background { surface(appearance) }
        .accessibilityElement(children: .contain)
        .task(id: state.remaining) {
            guard state.remaining > 0 else { return }
            try? await Task.sleep(for: .seconds(1))
            if !Task.isCancelled { state.tick() }
        }
        .onChange(of: state.didSend) { _, didSend in
            if didSend {
                AccessibilityNotification.Announcement(labels.sent ?? MonarchEmailVerificationBannerStrings.sent).post()
            }
        }
        .onChange(of: state.errorMessage) { _, message in
            if let message {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func text(_ appearance: MonarchCalloutBannerTokens.Appearance) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchCalloutBannerTokens.iconSpacing) {
            icon.foregroundStyle(theme.colors[role: appearance.icon])
            VStack(alignment: .leading, spacing: MonarchCalloutBannerTokens.textSpacing) {
                Text(labels.title ?? MonarchEmailVerificationBannerStrings.title).monarchTextStyle(.bodyStrong)
                Text(MonarchEmailVerificationBannerTokens.message(email: email, labels: labels))
                    .monarchTextStyle(.body)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .foregroundStyle(theme.colors.textDefault)
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchCalloutBannerTokens.symbol(tone: tone, variant: .callout, override: nil)
        if let spoken = MonarchCalloutBannerTokens.spokenTone(tone: tone, variant: .callout, override: nil) {
            MonarchIcon(systemName: symbol, label: spoken)
        } else {
            MonarchIcon(systemName: symbol)
        }
    }

    @ViewBuilder private var status: some View {
        if let message = state.errorMessage {
            statusLine(
                MonarchEmailVerificationBannerTokens.errorSymbol,
                text: message,
                color: theme.colors.dangerStrong
            )
        } else if state.didSend {
            statusLine(
                MonarchEmailVerificationBannerTokens.sentSymbol,
                text: labels.sent ?? MonarchEmailVerificationBannerStrings.sent,
                color: theme.colors.successStrong
            )
        }
    }

    private func statusLine(_ symbol: String, text: String, color: Color) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchEmailVerificationBannerTokens.statusIconSpacing) {
            MonarchIcon(systemName: symbol, size: .sm)
            Text(text)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(color)
        .accessibilityElement(children: .combine)
    }

    private var buttons: some View {
        MonarchFlowLayout(spacing: MonarchEmailVerificationBannerTokens.buttonSpacing) {
            MonarchButton(
                MonarchEmailVerificationBannerTokens.resendTitle(remaining: state.remaining, labels: labels),
                variant: .secondary,
                size: .sm,
                isLoading: state.isSending
            ) {
                resend()
            }
            .disabled(state.remaining > 0)
            if let onChangeEmail {
                MonarchButton(
                    labels.changeEmail ?? MonarchEmailVerificationBannerStrings.changeEmail,
                    variant: .ghost,
                    size: .sm,
                    action: onChangeEmail
                )
            }
        }
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
        .accessibilityLabel(labels.dismiss ?? MonarchEmailVerificationBannerStrings.dismiss)
        .padding(.vertical, -MonarchCalloutBannerTokens.dismissVerticalOutset(.callout))
        .padding(.trailing, -MonarchCalloutBannerTokens.dismissOutset)
    }

    private func resend() {
        guard state.begin() else { return }
        let cooldown = cooldownSeconds
        let labels = labels
        Task { @MainActor in
            do {
                try await onResend()
                state.succeed(cooldown: cooldown)
            } catch {
                state.fail(message: MonarchEmailVerificationBannerTokens.errorMessage(error, labels: labels))
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchEmailVerificationBanner") {
        MonarchPreviewMatrix {
            VStack {
                MonarchEmailVerificationBanner(email: "ana@example.com", onChangeEmail: {}, onDismiss: {}) {}
                MonarchEmailVerificationBanner(email: "ana@example.com", tone: .info, initialCooldown: 42) {}
                MonarchEmailVerificationBanner(email: "ana@example.com", placement: .page) {}
            }
        }
    }
#endif
