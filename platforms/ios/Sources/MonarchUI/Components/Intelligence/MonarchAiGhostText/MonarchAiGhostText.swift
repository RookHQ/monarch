import MonarchTokens
import SwiftUI

/// A multi-line text field with inline completion ("ghost text"): the
/// model's suggested continuation is drawn in a muted colour after the
/// typed text.
///
/// ```swift
/// MonarchAiGhostText("Message", text: $message, prompt: "Write a reply") { text, cursor in
///     try await myService.complete(text, at: cursor)
/// }
/// ```
///
/// A tap on the suggestion accepts it, and so does the "Accept suggestion"
/// button under the field. With a hardware keyboard, Tab or the forward
/// arrow accepts it, Control or Option with the forward arrow accepts one
/// word, and Escape dismisses it. Requests are debounced, stale ones are
/// cancelled, and nothing is suggested in the middle of the text, while
/// text is selected, or while an input method is composing. The system's
/// own inline predictions are turned off in this field, so the two never
/// draw in the same place; the QuickType bar stays. The suggestion is drawn
/// over the field, so nothing moves when it appears.
///
/// To read or drive the completion yourself, keep a ``MonarchAiCompletion``
/// and pass it as `completion`.
///
/// Accessibility: the field is one element whose label is `label`, whose
/// value is the typed text, and whose hint is how to accept the suggestion
/// while one shows, otherwise the helper text. A new suggestion is
/// announced. VoiceOver has "Accept suggestion" and "Dismiss suggestion"
/// actions on the field, and the accept button has a 44-point touch target.
/// The suggestion differs from the typed text by its position after the
/// caret as well as its colour.
public struct MonarchAiGhostText: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let helper: String?
    private let isCompletionEnabled: Bool?
    private let showsAcceptButton: Bool
    private let showsLabel: Bool
    private let keyHint: String?
    private let labels: MonarchAiGhostTextLabels
    private let onAccept: (@MainActor (MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Void)?
    private let onDismiss: (@MainActor () -> Void)?
    private let external: MonarchAiCompletion?

    @State private var owned: MonarchAiCompletion
    @State private var isFocused = false
    @State private var lastAnnounced = ""
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.layoutDirection) private var layoutDirection
    @ScaledMetric private var fontSize: CGFloat

    /// A field that completes with your closure.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required.
    ///   - text: The text to edit.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - debounce: How long typing must pause before a request is sent.
    ///   - minimumCharacters: The fewest characters before the cursor for a request.
    ///   - isCompletionEnabled: Whether suggestions are on.
    ///   - showsAcceptButton: Whether the "Accept suggestion" button is shown under the field while a
    ///     suggestion shows. Its room is always kept, so nothing moves.
    ///   - showsLabel: Whether the label is drawn above the field. Off, it is still the accessibility label.
    ///   - keyHint: The key that accepts the suggestion, such as "Tab", drawn as a key cap after it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAccept: Called after a suggestion, or one word of it, went into the text.
    ///   - onDismiss: Called when the person dismisses the suggestion.
    ///   - complete: Asks the model for a continuation of the text at the cursor, a UTF-16 offset.
    ///     Return the text to insert, or `nil` for no suggestion.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        debounce: Duration = .milliseconds(300),
        minimumCharacters: Int = 3,
        isCompletionEnabled: Bool = true,
        showsAcceptButton: Bool = true,
        showsLabel: Bool = true,
        keyHint: String? = nil,
        labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels(),
        onAccept: (@MainActor (MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil,
        complete: @escaping @Sendable (String, Int) async throws -> String?
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.isCompletionEnabled = isCompletionEnabled
        self.showsAcceptButton = showsAcceptButton
        self.showsLabel = showsLabel
        self.keyHint = keyHint
        self.labels = labels
        self.onAccept = onAccept
        self.onDismiss = onDismiss
        self.external = nil
        _owned = State(
            wrappedValue: MonarchAiCompletion(
                debounce: debounce,
                minimumCharacters: minimumCharacters,
                isEnabled: isCompletionEnabled,
                complete: complete
            )
        )
        _fontSize = ScaledMetric(
            wrappedValue: MonarchAiGhostTextTokens.textStyle.spec.size,
            relativeTo: MonarchAiGhostTextTokens.textStyle.spec.relativeTo
        )
    }

    /// A field driven by a completion you keep.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Always required.
    ///   - text: The text to edit.
    ///   - completion: The completion state. Keep it in `@State`.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field.
    ///   - showsAcceptButton: Whether the "Accept suggestion" button is shown under the field while a
    ///     suggestion shows. Its room is always kept, so nothing moves.
    ///   - showsLabel: Whether the label is drawn above the field. Off, it is still the accessibility label.
    ///   - keyHint: The key that accepts the suggestion, such as "Tab", drawn as a key cap after it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAccept: Called after a suggestion, or one word of it, went into the text.
    ///   - onDismiss: Called when the person dismisses the suggestion.
    public init(
        _ label: String,
        text: Binding<String>,
        completion: MonarchAiCompletion,
        prompt: String? = nil,
        helper: String? = nil,
        showsAcceptButton: Bool = true,
        showsLabel: Bool = true,
        keyHint: String? = nil,
        labels: MonarchAiGhostTextLabels = MonarchAiGhostTextLabels(),
        onAccept: (@MainActor (MonarchAiCompletionAcceptance, MonarchAiGhostTextAcceptKind) -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.isCompletionEnabled = nil
        self.showsAcceptButton = showsAcceptButton
        self.showsLabel = showsLabel
        self.keyHint = keyHint
        self.labels = labels
        self.onAccept = onAccept
        self.onDismiss = onDismiss
        self.external = completion
        // Not used: the completion that was passed in is.
        _owned = State(wrappedValue: MonarchAiCompletion(isEnabled: false) { _, _ in nil })
        _fontSize = ScaledMetric(
            wrappedValue: MonarchAiGhostTextTokens.textStyle.spec.size,
            relativeTo: MonarchAiGhostTextTokens.textStyle.spec.relativeTo
        )
    }

    private var completion: MonarchAiCompletion { external ?? owned }

    private var shownSuggestion: String {
        MonarchAiGhostTextTokens.shownSuggestion(
            status: completion.status,
            suggestion: completion.suggestion,
            isEnabled: isEnabled
        )
    }

    private var acceptTitle: String { labels.accept ?? MonarchAiGhostTextStrings.accept }

    /// The content of the view.
    public var body: some View {
        let suggestion = shownSuggestion
        VStack(alignment: .leading, spacing: MonarchAiGhostTextTokens.stackSpacing) {
            if showsLabel {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityHidden(true)
            }
            field(suggestion: suggestion)
            supportingRow(isShowing: !suggestion.isEmpty)
        }
        .onChange(of: suggestion) { _, newValue in
            announce(newValue)
        }
        .onChange(of: text) { _, newValue in
            // A value set from outside makes the suggestion stale.
            if completion.status != .idle && completion.text != newValue { completion.dismiss() }
        }
        .onChange(of: isCompletionEnabled, initial: true) { _, newValue in
            if let newValue { owned.setEnabled(newValue) }
        }
        .onChange(of: isEnabled) { _, newValue in
            if !newValue { completion.dismiss() }
        }
    }

    private func field(suggestion: String) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        let spec = MonarchAiGhostTextTokens.textStyle.spec
        let style = MonarchAiGhostTextFieldStyle(
            fontSize: fontSize,
            fontFamily: theme.typography.fontFamily,
            lineSpacing: max(0, (spec.lineHeight - MonarchTextStyleSpec.systemLineHeight) * fontSize),
            textColor: theme.colors[role: MonarchAiGhostTextTokens.textRole(isEnabled: isEnabled)],
            suggestionColor: theme.colors[role: MonarchAiGhostTextTokens.suggestionRole],
            isRightToLeft: layoutDirection == .rightToLeft,
            isEnabled: isEnabled,
            label: label,
            hint: MonarchAiGhostTextTokens.accessibilityHint(
                helper: helper,
                instruction: labels.instruction ?? MonarchAiGhostTextStrings.instruction,
                isShowing: !suggestion.isEmpty
            ),
            acceptActionName: acceptTitle,
            dismissActionName: labels.dismiss ?? MonarchAiGhostTextStrings.dismiss,
            keyHint: keyHint,
            caretAfterExternalChange: MonarchAiGhostTextTokens.caret(
                for: text,
                completionText: completion.text,
                completionCursor: completion.cursor
            )
        )
        let events = MonarchAiGhostTextFieldEvents(
            onInput: handle,
            onFocusChange: { focused in
                isFocused = focused
                if !focused { completion.dismiss() }
            },
            onAccept: accept,
            onDismiss: dismiss
        )
        return MonarchAiGhostTextField(text: $text, suggestion: suggestion, style: style, events: events)
            .overlay(alignment: .topLeading) {
                if text.isEmpty, let prompt {
                    Text(prompt)
                        .monarchTextStyle(MonarchAiGhostTextTokens.textStyle)
                        .foregroundStyle(theme.colors.textSubtle)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
            .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchTokens.Space.space24, alignment: .topLeading)
            .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: MonarchTextFieldTokens.border(hasError: false, isEnabled: isEnabled)],
                    lineWidth: MonarchTextFieldTokens.borderWidth
                )
            )
            .monarchFocusRing(isFocused, in: shape)
    }

    @ViewBuilder
    private func supportingRow(isShowing: Bool) -> some View {
        if showsAcceptButton || helper?.isEmpty == false {
            HStack(alignment: .center, spacing: MonarchAiGhostTextTokens.supportingSpacing) {
                if let helper, !helper.isEmpty {
                    Text(helper)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityHidden(true)
                }
                Spacer(minLength: 0)
                if showsAcceptButton {
                    // Always laid out, so the content under the field does not move when a suggestion appears.
                    MonarchButton(
                        acceptTitle,
                        systemImage: MonarchAiGhostTextTokens.acceptSymbol,
                        variant: .ghost,
                        size: .sm
                    ) {
                        accept(.all)
                    }
                    .opacity(isShowing ? 1 : 0)
                    .allowsHitTesting(isShowing)
                    .accessibilityHidden(!isShowing)
                }
            }
        }
    }

    private func handle(_ input: MonarchAiGhostTextInput) {
        completion.setComposing(input.isComposing)
        let evaluation = MonarchAiGhostTextTokens.evaluate(
            text: input.text,
            caret: input.caret,
            isComposing: input.isComposing,
            isEditable: isEnabled
        )
        switch evaluation {
        case .dismiss: completion.dismiss()
        case .update(let cursor): completion.update(text: input.text, cursor: cursor)
        }
    }

    private func accept(_ kind: MonarchAiGhostTextAcceptKind) {
        let acceptance = kind == .word ? completion.acceptWord() : completion.accept()
        guard let acceptance else { return }
        text = acceptance.text
        if kind == .all {
            AccessibilityNotification.Announcement(labels.accepted ?? MonarchAiGhostTextStrings.accepted).post()
        }
        onAccept?(acceptance, kind)
    }

    private func dismiss() {
        guard completion.status != .idle else { return }
        completion.dismiss()
        AccessibilityNotification.Announcement(labels.dismissed ?? MonarchAiGhostTextStrings.dismissed).post()
        onDismiss?()
    }

    private func announce(_ suggestion: String) {
        guard !suggestion.isEmpty else {
            lastAnnounced = ""
            return
        }
        guard MonarchAiGhostTextTokens.shouldAnnounce(suggestion: suggestion, lastAnnounced: lastAnnounced)
        else { return }
        lastAnnounced = suggestion
        AccessibilityNotification.Announcement(
            labels.announce?(suggestion) ?? MonarchAiGhostTextStrings.announce(suggestion)
        ).post()
    }
}

#if DEBUG
    #Preview("MonarchAiGhostText") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiGhostText("Message", text: .constant(""), prompt: "Write a reply") { _, _ in nil }
                MonarchAiGhostText("Message", text: .constant("Thanks for"), helper: "Suggestions come from the model.")
                {
                    _,
                    _ in " getting back to me so quickly."
                }
                MonarchAiGhostText("Disabled", text: .constant("Read only")) { _, _ in nil }
                    .disabled(true)
            }
        }
    }
#endif
