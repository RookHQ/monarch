import SwiftUI

/// Thumbs up and down for an AI answer, then reasons and an optional comment
/// in a sheet, then a thank-you.
///
/// ```swift
/// MonarchAiFeedback(selection: $message.feedback) { submission in
///     try await api.sendFeedback(messageID: message.id, submission)
/// }
/// ```
///
/// Rating and sending are separate: the binding changes at once, so the
/// rating is kept even when the details are never sent. Pressing the pressed
/// thumb clears it. With `asksDetails` `.down` (the default) a thumbs down
/// opens a sheet with reasons (``MonarchCheckboxGroup``) and a comment, and a
/// thumbs up is sent at once. While `onSubmit` runs the send button shows
/// progress; if it throws, the sheet stays filled in with an error. Cancel
/// or a swipe down closes the sheet and keeps the rating.
///
/// Accessibility: the thumbs are standard toggles with the Monarch toggle
/// button style, named "Good response" and "Bad response", in a container
/// named "Rate this response"; a pressed thumb is filled, so the state is a
/// shape as well as a colour. The thank-you and errors are announced. Every
/// control has a 44-point touch target.
public struct MonarchAiFeedback: View {
    @Binding private var selection: MonarchAiMessageFeedback?
    private let reasons: [MonarchAiMessageFeedback: [MonarchCheckboxGroupOption]]?
    private let asksDetails: MonarchAiFeedbackDetails
    private let labels: MonarchAiFeedbackLabels
    private let onSubmit: (@MainActor (MonarchAiFeedbackSubmission) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var phase: MonarchAiFeedbackTokens.Phase = .idle
    @State private var chosen: Set<String> = []
    @State private var comment = ""
    @State private var hasFailed = false
    @State private var sendsFromForm = false

    /// A feedback control.
    ///
    /// - Parameters:
    ///   - selection: The rating, `nil` for none. Changes as soon as a thumb is pressed.
    ///   - reasons: The reasons offered for each rating; a rating without an
    ///     entry offers none. `nil` offers five built-in reasons after a thumbs
    ///     down and none after a thumbs up.
    ///   - asksDetails: Which ratings open the sheet with reasons and a comment.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSubmit: Sends the rating with its reasons and comment. Throw to show the error.
    public init(
        selection: Binding<MonarchAiMessageFeedback?>,
        reasons: [MonarchAiMessageFeedback: [MonarchCheckboxGroupOption]]? = nil,
        asksDetails: MonarchAiFeedbackDetails = .down,
        labels: MonarchAiFeedbackLabels = MonarchAiFeedbackLabels(),
        onSubmit: (@MainActor (MonarchAiFeedbackSubmission) async throws -> Void)? = nil
    ) {
        _selection = selection
        self.reasons = reasons
        self.asksDetails = asksDetails
        self.labels = labels
        self.onSubmit = onSubmit
    }

    private typealias Tokens = MonarchAiFeedbackTokens

    private func options(for value: MonarchAiMessageFeedback) -> [MonarchCheckboxGroupOption] {
        Tokens.options(for: value, reasons: reasons)
    }

    private var errorText: String { labels.error ?? MonarchAiFeedbackStrings.error }
    private var thanksText: String { labels.thanks ?? MonarchAiFeedbackStrings.thanks }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            HStack(spacing: Tokens.thumbSpacing) {
                thumb(.up)
                thumb(.down)
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.group ?? MonarchAiFeedbackStrings.group)
            if phase == .thanks {
                message(thanksText, systemImage: Tokens.thanksSymbol, color: theme.colors.textMuted)
            } else if hasFailed && !Tokens.showsSheet(phase, fromForm: sendsFromForm) {
                message(errorText, systemImage: Tokens.errorSymbol, color: theme.colors.dangerStrong)
            }
        }
        .sheet(isPresented: sheetBinding) {
            if let selection {
                MonarchAiFeedbackForm(
                    value: selection,
                    options: options(for: selection),
                    chosen: $chosen,
                    comment: $comment,
                    isPending: phase == .pending,
                    hasFailed: hasFailed,
                    labels: labels,
                    onSubmit: { submitForm(selection) },
                    onCancel: cancel
                )
                .presentationDetents([.medium, .large])
                .interactiveDismissDisabled(phase == .pending)
            }
        }
    }

    private var sheetBinding: Binding<Bool> {
        Binding(
            get: { Tokens.showsSheet(phase, fromForm: sendsFromForm) && selection != nil },
            set: { isPresented in
                if !isPresented && phase == .details { cancel() }
            }
        )
    }

    private func thumb(_ value: MonarchAiMessageFeedback) -> some View {
        let isOn = Binding(
            get: { selection == value },
            set: { _ in rate(value) }
        )
        return Toggle(isOn: isOn) {
            Label(
                Tokens.thumbLabel(value, labels: labels),
                systemImage: Tokens.symbol(value, isPressed: selection == value)
            )
            .labelStyle(.iconOnly)
        }
        .toggleStyle(.monarchButton(.ghost, size: Tokens.thumbSize))
        .disabled(phase == .pending)
    }

    private func message(_ text: String, systemImage: String, color: Color) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.messageIconSpacing) {
            MonarchIcon(systemName: systemImage, size: .sm)
            Text(text).monarchTextStyle(.caption)
        }
        .foregroundStyle(color)
        .accessibilityElement(children: .combine)
    }

    private func rate(_ value: MonarchAiMessageFeedback) {
        let next = Tokens.rating(after: value, current: selection)
        selection = next
        chosen = []
        comment = ""
        hasFailed = false
        switch Tokens.next(for: next, asks: asksDetails) {
        case .clear: phase = .idle
        case .askDetails: phase = .details
        case .send:
            if let next { send(MonarchAiFeedbackSubmission(value: next), fromForm: false) }
        }
    }

    private func submitForm(_ value: MonarchAiMessageFeedback) {
        guard phase != .pending else { return }
        send(
            Tokens.submission(value: value, chosen: chosen, options: options(for: value), comment: comment),
            fromForm: true
        )
    }

    private func cancel() {
        phase = .idle
        hasFailed = false
    }

    private func send(_ submission: MonarchAiFeedbackSubmission, fromForm: Bool) {
        hasFailed = false
        sendsFromForm = fromForm
        guard let onSubmit else {
            finish()
            return
        }
        phase = .pending
        Task { @MainActor in
            do {
                try await onSubmit(submission)
                finish()
            } catch {
                hasFailed = true
                phase = Tokens.phaseAfterFailure(fromForm: fromForm)
                AccessibilityNotification.Announcement(errorText).post()
            }
        }
    }

    private func finish() {
        phase = .thanks
        AccessibilityNotification.Announcement(thanksText).post()
    }
}

#if DEBUG
    #Preview("MonarchAiFeedback") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiFeedback(selection: .constant(nil))
                MonarchAiFeedback(selection: .constant(.up), asksDetails: .both)
            }
        }
    }
#endif
