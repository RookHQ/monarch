import SwiftUI

/// A compact "this part didn't load" row with a retry button.
///
/// ```swift
/// MonarchInlineErrorRetry("Your earnings didn't load.", isRetrying: isLoading) {
///     reload()
/// }
/// ```
///
/// The message and the button share one line when they fit, and stack when
/// they do not. For a whole region that failed, use ``MonarchErrorState``.
///
/// Accessibility: the icon and message are one element, read as the tone
/// name, then the message. The button is separate, with a 44-point touch
/// target. When `isRetrying` becomes true "Retrying" is announced, and
/// while it is true the button does not run its action and reads "Retrying"
/// as its value.
public struct MonarchInlineErrorRetry: View {
    private let message: String
    private let systemImage: String?
    private let tone: MonarchTone
    private let variant: MonarchInlineErrorRetryVariant
    private let isRetrying: Bool
    private let toneLabel: String?
    private let retryLabel: String?
    private let retryingLabel: String?
    private let onRetry: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    /// An inline error row.
    ///
    /// - Parameters:
    ///   - message: The failure in plain words, already localised.
    ///   - systemImage: Replaces the SF Symbol of the tone.
    ///   - tone: `danger` when nothing could load; `warning` when stale data is still shown.
    ///   - variant: A plain row, or a tinted strip.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - retryLabel: Replaces the built-in "Try again" title.
    ///   - retryingLabel: Replaces the built-in "Retrying" announcement and value.
    ///   - onRetry: Runs when the retry button is activated and no retry is in progress.
    public init(
        _ message: String,
        systemImage: String? = nil,
        tone: MonarchTone = .danger,
        variant: MonarchInlineErrorRetryVariant = .inline,
        isRetrying: Bool = false,
        toneLabel: String? = nil,
        retryLabel: String? = nil,
        retryingLabel: String? = nil,
        onRetry: @escaping @MainActor () -> Void
    ) {
        self.message = message
        self.systemImage = systemImage
        self.tone = tone
        self.variant = variant
        self.isRetrying = isRetrying
        self.toneLabel = toneLabel
        self.retryLabel = retryLabel
        self.retryingLabel = retryingLabel
        self.onRetry = onRetry
    }

    private var spokenRetrying: String { retryingLabel ?? MonarchInlineErrorRetryStrings.retrying }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchInlineErrorRetryTokens.appearance(tone: tone, variant: variant)
        let hasSurface = appearance.background != nil
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchInlineErrorRetryTokens.actionSpacing) {
                text(appearance)
                Spacer(minLength: 0)
                button
            }
            VStack(alignment: .leading, spacing: MonarchInlineErrorRetryTokens.actionSpacing) {
                text(appearance)
                button
            }
        }
        .padding(.horizontal, hasSurface ? MonarchInlineErrorRetryTokens.horizontalPadding : 0)
        .padding(.vertical, hasSurface ? MonarchInlineErrorRetryTokens.verticalPadding : 0)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            if let background = appearance.background {
                MonarchShapes.control.fill(theme.colors[role: background])
            }
        }
        .overlay {
            if let border = appearance.border {
                MonarchShapes.control
                    .strokeBorder(theme.colors[role: border], lineWidth: MonarchInlineErrorRetryTokens.borderWidth)
                    .allowsHitTesting(false)
            }
        }
        .accessibilityElement(children: .contain)
        .onChange(of: isRetrying) { wasRetrying, isRetrying in
            if MonarchInlineErrorRetryTokens.announcesRetry(wasRetrying: wasRetrying, isRetrying: isRetrying) {
                AccessibilityNotification.Announcement(spokenRetrying).post()
            }
        }
    }

    private func text(_ appearance: MonarchInlineErrorRetryTokens.Appearance) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchInlineErrorRetryTokens.iconSpacing) {
            icon.foregroundStyle(theme.colors[role: appearance.icon])
            Text(message)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: appearance.text])
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchInlineErrorRetryTokens.symbol(tone: tone, override: systemImage)
        if let spoken = toneLabel ?? MonarchToneStrings.name(tone) {
            MonarchIcon(systemName: symbol, label: spoken, size: .sm)
        } else {
            MonarchIcon(systemName: symbol, size: .sm)
        }
    }

    private var button: some View {
        MonarchButton(
            retryLabel ?? MonarchInlineErrorRetryStrings.retry,
            systemImage: MonarchInlineErrorRetryTokens.retrySymbol,
            variant: .secondary,
            size: .sm,
            isLoading: isRetrying,
            loadingLabel: spokenRetrying,
            action: onRetry
        )
    }
}

#if DEBUG
    #Preview("MonarchInlineErrorRetry") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchInlineErrorRetry("Your earnings didn't load.") {}
                MonarchInlineErrorRetry("Showing data from 10 minutes ago.", tone: .warning, variant: .banner) {}
                MonarchInlineErrorRetry("Comments didn't load.", variant: .banner, isRetrying: true) {}
            }
        }
    }
#endif
