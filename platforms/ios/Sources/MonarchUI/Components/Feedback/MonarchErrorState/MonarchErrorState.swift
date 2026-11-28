import SwiftUI

/// A region failed to load: what failed, why in plain words, and a retry.
///
/// ```swift
/// MonarchErrorState(
///     "Invoices didn't load",
///     message: "Check your connection and try again.",
///     kind: .offline,
///     isRetrying: isLoading
/// ) {
///     reload()
/// }
/// ```
///
/// It shares the layout of ``MonarchEmptyState``. For a compact row inside
/// a card, use ``MonarchInlineErrorRetry``.
///
/// Accessibility: the title has the header trait and the icon is hidden
/// from VoiceOver; the title and message say what failed, so the meaning
/// does not depend on the icon or its colour. While `isRetrying` is true
/// the retry button does not run its action and reads "Retrying" as its
/// value. The request id is read as its label and value, and can be
/// selected and copied with the system text selection.
public struct MonarchErrorState: View {
    private let title: String
    private let message: String?
    private let systemImage: String?
    private let kind: MonarchErrorStateKind
    private let size: MonarchEmptyStateSize
    private let requestID: String?
    private let isRetrying: Bool
    private let retryLabel: String?
    private let retryingLabel: String?
    private let requestIDLabel: String?
    private let actionTitle: String?
    private let actionVariant: MonarchButtonVariant
    private let action: (@MainActor () -> Void)?
    private let secondaryActionTitle: String?
    private let secondaryAction: (@MainActor () -> Void)?
    private let onRetry: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// An error state.
    ///
    /// - Parameters:
    ///   - title: What failed, already localised.
    ///   - message: Why, in plain words, and what to do.
    ///   - systemImage: Replaces the SF Symbol of the kind.
    ///   - kind: What went wrong, which selects the icon and whether a retry is offered.
    ///   - size: How much room the state takes.
    ///   - requestID: The id of the failed request, shown so the person can quote it.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - retryLabel: Replaces the built-in "Try again" title.
    ///   - retryingLabel: Replaces the built-in "Retrying" spoken value.
    ///   - requestIDLabel: Replaces the built-in "Request ID" text.
    ///   - actionTitle: The title of a second button, such as "Contact support". No button without it.
    ///   - actionVariant: How the second button is drawn: ghost by default, primary for the one action the page asks for.
    ///   - secondaryActionTitle: The title of a third button, such as "Go back". No button without it.
    ///   - onRetry: Runs when the retry button is activated. No retry button
    ///     without it, or for the `forbidden` and `notFound` kinds.
    ///   - action: Runs when the second button is activated.
    ///   - secondaryAction: Runs when the third button is activated.
    public init(
        _ title: String,
        message: String? = nil,
        systemImage: String? = nil,
        kind: MonarchErrorStateKind = .error,
        size: MonarchEmptyStateSize = .md,
        requestID: String? = nil,
        isRetrying: Bool = false,
        retryLabel: String? = nil,
        retryingLabel: String? = nil,
        requestIDLabel: String? = nil,
        actionTitle: String? = nil,
        actionVariant: MonarchButtonVariant = .ghost,
        secondaryActionTitle: String? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        action: (@MainActor () -> Void)? = nil,
        secondaryAction: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.systemImage = systemImage
        self.kind = kind
        self.size = size
        self.requestID = requestID
        self.isRetrying = isRetrying
        self.retryLabel = retryLabel
        self.retryingLabel = retryingLabel
        self.requestIDLabel = requestIDLabel
        self.actionTitle = actionTitle
        self.actionVariant = actionVariant
        self.action = action
        self.secondaryActionTitle = secondaryActionTitle
        self.secondaryAction = secondaryAction
        self.onRetry = onRetry
    }

    private var hasActions: Bool {
        MonarchErrorStateTokens.showsRetry(kind: kind, hasHandler: onRetry != nil)
            || MonarchEmptyStateTokens.showsAction(title: actionTitle, hasHandler: action != nil)
            || MonarchEmptyStateTokens.showsAction(title: secondaryActionTitle, hasHandler: secondaryAction != nil)
            || MonarchErrorStateTokens.requestIDLabel(requestID, label: requestIDLabel) != nil
    }

    /// The content of the view.
    public var body: some View {
        let buttonSize = MonarchEmptyStateTokens.buttonSize(size)
        MonarchEmptyStateLayout(
            title: title,
            message: message,
            symbol: MonarchErrorStateTokens.symbol(kind: kind, override: systemImage),
            iconRole: MonarchErrorStateTokens.iconRole(kind: kind),
            tileRole: MonarchErrorStateTokens.tileRole(kind: kind),
            size: size,
            hasActions: hasActions
        ) {
            if let onRetry, MonarchErrorStateTokens.showsRetry(kind: kind, hasHandler: true) {
                MonarchButton(
                    retryLabel ?? MonarchErrorStateStrings.retry,
                    variant: .primary,
                    size: buttonSize,
                    isLoading: isRetrying,
                    loadingLabel: retryingLabel ?? MonarchErrorStateStrings.retrying,
                    action: onRetry
                )
            }
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.monarch(actionVariant, size: buttonSize))
            }
            if let secondaryActionTitle, let secondaryAction {
                Button(secondaryActionTitle, action: secondaryAction)
                    .buttonStyle(.monarch(.secondary, size: buttonSize))
            }
            requestIDRow
        }
    }

    @ViewBuilder private var requestIDRow: some View {
        if let requestID,
            let spoken = MonarchErrorStateTokens.requestIDLabel(requestID, label: requestIDLabel)
        {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchErrorStateTokens.requestIDSpacing) {
                    requestIDText(requestID)
                }
                VStack(spacing: MonarchErrorStateTokens.requestIDSpacing) {
                    requestIDText(requestID)
                }
            }
            .foregroundStyle(theme.colors[role: MonarchEmptyStateTokens.message])
            .accessibilityElement(children: .combine)
            .accessibilityLabel(spoken)
        }
    }

    @ViewBuilder
    private func requestIDText(_ requestID: String) -> some View {
        Text(requestIDLabel ?? MonarchErrorStateStrings.requestID)
            .monarchTextStyle(.caption)
        Text(requestID)
            .monarchTextStyle(.code)
            .textSelection(.enabled)
        // The web draws a copy icon after the id.
        MonarchCopyButton(value: requestID, variant: .ghost, size: .sm, isIconOnly: true)
    }
}

#if DEBUG
    #Preview("MonarchErrorState") {
        MonarchPreviewMatrix {
            VStack {
                MonarchErrorState(
                    "Invoices didn't load",
                    message: "Something went wrong on our side.",
                    kind: .server,
                    requestID: "req_8f2k1",
                    actionTitle: "Contact support",
                    onRetry: {},
                    action: {}
                )
                MonarchErrorState("You don't have access", kind: .forbidden, size: .sm, onRetry: {})
                MonarchErrorState("You're offline", kind: .offline, size: .sm, isRetrying: true, onRetry: {})
            }
        }
    }
#endif
