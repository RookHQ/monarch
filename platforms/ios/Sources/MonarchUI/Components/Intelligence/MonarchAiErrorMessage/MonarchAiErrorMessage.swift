import SwiftUI

/// The failure card for an assistant turn: what went wrong in plain words,
/// the actions that can fix it, an optional retry countdown and a technical
/// details disclosure.
///
/// ```swift
/// MonarchAiErrorMessage(
///     MonarchAiErrorKind(code: error.code),
///     retryAfter: 12,
///     onRetry: { conversation.retry() },
///     onEditPrompt: { editLastPrompt() }
/// )
/// ```
///
/// Only the actions with a handler are shown, in an order that depends on
/// the kind; the first is the primary button. While `retryAfter` counts
/// down, the retry button is disabled and reads "Try again in 12s". The card
/// looks like ``MonarchBanner``, in the warning or the danger tone.
///
/// Accessibility: the icon, title and message are one element, read as the
/// tone name ("Warning" or "Error"), the title and the message. Each action
/// is a separate button with a 44-point touch target. The title and message
/// are announced once when the card appears, and "You can try again now."
/// when a countdown ends. The raw error code is never shown or spoken.
public struct MonarchAiErrorMessage<Actions: View>: View {
    private let kind: MonarchAiErrorKind
    private let title: String?
    private let message: String?
    private let details: String?
    private let retryAfter: TimeInterval?
    private let isRetrying: Bool
    private let announces: Bool
    private let labels: MonarchAiErrorMessageLabels
    private let handlers: [MonarchAiErrorMessageTokens.Action: @MainActor () -> Void]
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @State private var secondsLeft: Int
    @State private var isDetailsExpanded = false

    /// A failure card with extra actions.
    ///
    /// - Parameters:
    ///   - kind: Why the turn failed. Use `MonarchAiErrorKind(code:)` for a raw error code.
    ///   - title: Replaces the built-in title of the kind.
    ///   - message: Replaces the built-in description of the kind.
    ///   - details: Technical information, such as a request id, shown as code behind a disclosure.
    ///   - retryAfter: Seconds until a retry is allowed. The retry action is disabled and counts down until then.
    ///   - isRetrying: Whether the retry is starting. Shows a progress indicator on the retry action.
    ///   - announces: Whether the title and message are announced when the card appears. Pass `false`
    ///     when the same error was already announced.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows "Try again".
    ///   - onEditPrompt: Shows "Edit message": put the failed prompt back in the input.
    ///   - onSwitchModel: Shows "Switch model".
    ///   - onNewConversation: Shows "New conversation", the primary action for a conversation that is too long.
    ///   - onSummarize: Shows "Summarize and continue", for a conversation that is too long only.
    ///   - actions: Extra actions after the built-in ones.
    public init(
        _ kind: MonarchAiErrorKind = .unknown,
        title: String? = nil,
        message: String? = nil,
        details: String? = nil,
        retryAfter: TimeInterval? = nil,
        isRetrying: Bool = false,
        announces: Bool = true,
        labels: MonarchAiErrorMessageLabels = MonarchAiErrorMessageLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onEditPrompt: (@MainActor () -> Void)? = nil,
        onSwitchModel: (@MainActor () -> Void)? = nil,
        onNewConversation: (@MainActor () -> Void)? = nil,
        onSummarize: (@MainActor () -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.kind = kind
        self.title = title
        self.message = message
        self.details = details
        self.retryAfter = retryAfter
        self.isRetrying = isRetrying
        self.announces = announces
        self.labels = labels
        var handlers: [MonarchAiErrorMessageTokens.Action: @MainActor () -> Void] = [:]
        handlers[.retry] = onRetry
        handlers[.editPrompt] = onEditPrompt
        handlers[.switchModel] = onSwitchModel
        handlers[.newConversation] = onNewConversation
        // Summarizing only helps a conversation that is too long.
        handlers[.summarize] = kind == .contextOverflow ? onSummarize : nil
        self.handlers = handlers
        self.actions = actions()
        _secondsLeft = State(initialValue: MonarchAiErrorMessageTokens.initialSeconds(retryAfter))
    }

    private var shownTitle: String { title ?? MonarchAiErrorMessageStrings.title(kind) }
    private var shownMessage: String { message ?? MonarchAiErrorMessageStrings.message(kind) }
    private var isWaiting: Bool { secondsLeft > 0 }

    private var retryInText: String {
        labels.retryIn?(secondsLeft) ?? MonarchAiErrorMessageStrings.retryIn(secondsLeft)
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchBannerTokens.appearance(tone: MonarchAiErrorMessageTokens.tone(kind))
        let shape = RoundedRectangle(cornerRadius: MonarchBannerTokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: MonarchAiErrorMessageTokens.sectionSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchBannerTokens.iconSpacing) {
                icon.foregroundStyle(theme.colors[role: appearance.icon])
                VStack(alignment: .leading, spacing: MonarchBannerTokens.textSpacing) {
                    Text(shownTitle)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                    Text(shownMessage)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .accessibilityElement(children: .combine)
            actionRow
            if handlers[.retry] == nil && isWaiting {
                Text(retryInText)
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let details, !details.isEmpty {
                detailsDisclosure(details)
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
        .onAppear(perform: announce)
        .task(id: retryAfter) { await countDown() }
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchAiErrorMessageTokens.symbol(kind)
        if let spokenTone = labels.tone ?? MonarchToneStrings.name(MonarchAiErrorMessageTokens.tone(kind)) {
            MonarchIcon(systemName: symbol, label: spokenTone)
        } else {
            MonarchIcon(systemName: symbol)
        }
    }

    @ViewBuilder private var actionRow: some View {
        let shown = MonarchAiErrorMessageTokens.actions(for: kind, handled: Set(handlers.keys))
        if !shown.isEmpty || Actions.self != EmptyView.self {
            // Side by side where they fit, otherwise one under the other, as at the largest text sizes.
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAiErrorMessageTokens.actionSpacing) {
                    buttons(shown)
                    actions
                }
                VStack(alignment: .leading, spacing: MonarchAiErrorMessageTokens.actionSpacing) {
                    buttons(shown)
                    actions
                }
            }
        }
    }

    private func buttons(_ shown: [MonarchAiErrorMessageTokens.Action]) -> some View {
        ForEach(Array(shown.enumerated()), id: \.element) { index, action in
            let isRetry = action == .retry
            let handler = handlers[action]
            MonarchButton(
                actionTitle(action),
                systemImage: MonarchAiErrorMessageTokens.symbol(action),
                variant: MonarchAiErrorMessageTokens.variant(at: index),
                size: .sm,
                isLoading: isRetry && isRetrying
            ) {
                handler?()
            }
            .monospacedDigit()
            .disabled(isRetry && isWaiting)
        }
    }

    private func actionTitle(_ action: MonarchAiErrorMessageTokens.Action) -> String {
        switch action {
        case .retry:
            if isWaiting { return retryInText }
            return labels.retry ?? MonarchAiErrorMessageStrings.action(action)
        case .editPrompt: return labels.editPrompt ?? MonarchAiErrorMessageStrings.action(action)
        case .switchModel: return labels.switchModel ?? MonarchAiErrorMessageStrings.action(action)
        case .newConversation: return labels.newConversation ?? MonarchAiErrorMessageStrings.action(action)
        case .summarize: return labels.summarize ?? MonarchAiErrorMessageStrings.action(action)
        }
    }

    private func detailsDisclosure(_ details: String) -> some View {
        let shape = RoundedRectangle(
            cornerRadius: MonarchAiErrorMessageTokens.detailsCornerRadius,
            style: .continuous
        )
        return DisclosureGroup(isExpanded: $isDetailsExpanded) {
            Text(details)
                .monarchTextStyle(.code)
                .foregroundStyle(theme.colors.textDefault)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, MonarchAiErrorMessageTokens.detailsHorizontalPadding)
                .padding(.vertical, MonarchAiErrorMessageTokens.detailsVerticalPadding)
                .background(shape.fill(theme.colors.bgSurface))
                .overlay(shape.strokeBorder(theme.colors.borderDefault, lineWidth: MonarchBannerTokens.borderWidth))
        } label: {
            Text(labels.details ?? MonarchAiErrorMessageStrings.details)
                .monarchTextStyle(.caption)
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        }
        .tint(theme.colors.textMuted)
        .foregroundStyle(theme.colors.textMuted)
    }

    private func announce() {
        guard announces else { return }
        var text = AttributedString(
            MonarchAiErrorMessageTokens.announcement(title: shownTitle, message: shownMessage)
        )
        // A failure interrupts what is being read, like an alert.
        text.accessibilitySpeechAnnouncementPriority = .high
        AccessibilityNotification.Announcement(text).post()
    }

    private func countDown() async {
        let total = MonarchAiErrorMessageTokens.initialSeconds(retryAfter)
        secondsLeft = total
        guard total > 0, let retryAfter else { return }
        let deadline = Date().addingTimeInterval(retryAfter)
        while secondsLeft > 0 {
            try? await Task.sleep(for: MonarchAiErrorMessageTokens.tick)
            if Task.isCancelled { return }
            secondsLeft = MonarchAiErrorMessageTokens.secondsLeft(until: deadline, now: Date())
        }
        AccessibilityNotification.Announcement(labels.retryReady ?? MonarchAiErrorMessageStrings.retryReady).post()
    }
}

extension MonarchAiErrorMessage where Actions == EmptyView {
    /// A failure card.
    ///
    /// - Parameters:
    ///   - kind: Why the turn failed. Use `MonarchAiErrorKind(code:)` for a raw error code.
    ///   - title: Replaces the built-in title of the kind.
    ///   - message: Replaces the built-in description of the kind.
    ///   - details: Technical information, such as a request id, shown as code behind a disclosure.
    ///   - retryAfter: Seconds until a retry is allowed. The retry action is disabled and counts down until then.
    ///   - isRetrying: Whether the retry is starting. Shows a progress indicator on the retry action.
    ///   - announces: Whether the title and message are announced when the card appears. Pass `false`
    ///     when the same error was already announced.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows "Try again".
    ///   - onEditPrompt: Shows "Edit message": put the failed prompt back in the input.
    ///   - onSwitchModel: Shows "Switch model".
    ///   - onNewConversation: Shows "New conversation", the primary action for a conversation that is too long.
    ///   - onSummarize: Shows "Summarize and continue", for a conversation that is too long only.
    public init(
        _ kind: MonarchAiErrorKind = .unknown,
        title: String? = nil,
        message: String? = nil,
        details: String? = nil,
        retryAfter: TimeInterval? = nil,
        isRetrying: Bool = false,
        announces: Bool = true,
        labels: MonarchAiErrorMessageLabels = MonarchAiErrorMessageLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onEditPrompt: (@MainActor () -> Void)? = nil,
        onSwitchModel: (@MainActor () -> Void)? = nil,
        onNewConversation: (@MainActor () -> Void)? = nil,
        onSummarize: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            kind,
            title: title,
            message: message,
            details: details,
            retryAfter: retryAfter,
            isRetrying: isRetrying,
            announces: announces,
            labels: labels,
            onRetry: onRetry,
            onEditPrompt: onEditPrompt,
            onSwitchModel: onSwitchModel,
            onNewConversation: onNewConversation,
            onSummarize: onSummarize,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiErrorMessage") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchAiErrorKind.allCases, id: \.self) { kind in
                    MonarchAiErrorMessage(kind, onRetry: {}, onEditPrompt: {}, onNewConversation: {}, onSummarize: {})
                }
                MonarchAiErrorMessage(.rateLimit, details: "request_id: req_8f3a", retryAfter: 12, onRetry: {})
            }
        }
    }
#endif
