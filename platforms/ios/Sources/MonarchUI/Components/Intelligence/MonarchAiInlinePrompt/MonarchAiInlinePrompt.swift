import MonarchTokens
import SwiftUI

/// A prompt about selected text, shown in a sheet: an excerpt of the
/// selection, a ``MonarchAiPromptInput`` and quick prompts.
///
/// ```swift
/// MonarchAiInlinePromptTextView("Draft", text: $draft) { selection in
///     selected = selection
/// }
/// .sheet(item: $selected) { selection in
///     MonarchAiInlinePrompt(context: selection, suggestions: ["Shorten", "Make it more formal"]) { prompt in
///         rewrite(selection, with: prompt)
///     }
/// }
/// ```
///
/// The web shows a floating panel at the selection. On iOS the pattern is
/// an "Ask AI" item in the system text menu (``MonarchAiInlinePromptTextView``)
/// or a context menu (``MonarchAiInlinePromptArea``) that presents this view
/// with `.sheet`. It sets its own detents (medium and large) and shows the
/// drag indicator, and focuses the field when it appears. Send and a quick
/// prompt call `onSubmit` with the trimmed text and, with `closesOnSubmit`,
/// dismiss the sheet; Cancel, a swipe down and the VoiceOver escape gesture
/// dismiss it without sending.
///
/// Accessibility: the title is a header. The excerpt is one element read as
/// "Selection" and the excerpt. The quick prompts are a group named
/// "Suggestions"; each is a button of at least 44 points. While pending,
/// the field, Send and the quick prompts are disabled, and "Sending…" is
/// shown and announced.
public struct MonarchAiInlinePrompt: View {
    private let context: String?
    private let suggestions: [String]
    private let closesOnSubmit: Bool
    private let isPending: Bool
    private let labels: MonarchAiInlinePromptLabels
    private let onSubmit: @MainActor (String) -> Void

    @State private var text: String
    @FocusState private var isFocused: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @Environment(\.monarchAiPopoverCard) private var isCard

    private typealias Tokens = MonarchAiInlinePromptTokens
    private typealias Strings = MonarchAiInlinePromptStrings

    /// An inline prompt, the content of a sheet.
    ///
    /// - Parameters:
    ///   - context: What the prompt acts on, usually the selected text. Shown as an excerpt of at most 140 characters.
    ///   - suggestions: Quick prompts such as "Shorten". Tapping one sends it at once.
    ///   - initialValue: The text the field starts with each time the sheet is shown.
    ///   - closesOnSubmit: Dismisses the sheet after sending. Off, the sheet stays and the field is cleared.
    ///   - isPending: A request is being sent: the field, Send and the quick prompts are disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSubmit: Receives the trimmed prompt or the tapped quick prompt. Never called with blank text or while pending.
    public init(
        context: String? = nil,
        suggestions: [String] = [],
        initialValue: String = "",
        closesOnSubmit: Bool = true,
        isPending: Bool = false,
        labels: MonarchAiInlinePromptLabels = MonarchAiInlinePromptLabels(),
        onSubmit: @escaping @MainActor (String) -> Void
    ) {
        self.context = context
        self.suggestions = suggestions
        self.closesOnSubmit = closesOnSubmit
        self.isPending = isPending
        self.labels = labels
        self.onSubmit = onSubmit
        _text = State(initialValue: initialValue)
    }

    /// The content of the view.
    public var body: some View {
        if isCard { card } else { sheetContent }
    }

    /// The prompt as the web popover: the selection with an AI-coloured rule, the field, the quick prompts and the
    /// keyboard hint, in a bordered card.
    private var card: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            if let excerpt = Tokens.excerpt(context) {
                HStack(alignment: .top, spacing: Tokens.suggestionSpacing) {
                    Rectangle().fill(theme.colors.aiStrong.opacity(0.4)).frame(width: 2)
                    (Text((labels.context ?? Strings.context) + ": ").fontWeight(.semibold)
                        + Text(excerpt))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .lineLimit(2)
                }
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Tokens.excerptAccessibilityLabel(excerpt, labels: labels))
            }
            MonarchAiPromptInput(
                labels.inputLabel ?? Strings.inputLabel,
                text: $text,
                placeholder: labels.placeholder ?? Strings.placeholder,
                labels: MonarchAiPromptInputLabels(send: labels.submit ?? Strings.submit),
                focus: $isFocused,
                onSubmit: { submit($0) }
            )
            .environment(\.monarchPromptShowsGlyph, true)
            .disabled(isPending)
            if !suggestions.isEmpty { quickPrompts }
            Text(Strings.keyboardHint)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
        .padding(Tokens.sheetPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg))
        .overlay(
            RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg).stroke(theme.colors.borderDefault, lineWidth: 1)
        )
    }

    private var sheetContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                header
                if let excerpt = Tokens.excerpt(context) {
                    excerptBox(excerpt)
                }
                MonarchAiPromptInput(
                    labels.inputLabel ?? Strings.inputLabel,
                    text: $text,
                    placeholder: labels.placeholder ?? Strings.placeholder,
                    labels: MonarchAiPromptInputLabels(send: labels.submit ?? Strings.submit),
                    focus: $isFocused,
                    onSubmit: { submit($0) }
                )
                .environment(\.monarchPromptShowsGlyph, true)
                .disabled(isPending)
                if !suggestions.isEmpty {
                    quickPrompts
                }
                if isPending {
                    HStack(spacing: Tokens.suggestionSpacing) {
                        ProgressView()
                        Text(labels.pending ?? Strings.pending)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    .accessibilityElement(children: .combine)
                }
            }
            .padding(Tokens.sheetPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgSurface)
        .presentationBackground(theme.colors.bgSurface)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onAppear { isFocused = true }
        .onChange(of: isPending) { _, pending in
            if pending { AccessibilityNotification.Announcement(labels.pending ?? Strings.pending).post() }
        }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.suggestionSpacing) {
            Text(labels.label ?? Strings.label)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)
            MonarchButton(labels.cancel ?? Strings.cancel, variant: .ghost, size: .sm) {
                dismiss()
            }
        }
    }

    private func excerptBox(_ excerpt: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.excerptSpacing) {
            Text(labels.context ?? Strings.context)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.excerptLabelRole])
            Text(excerpt)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.excerptTextRole])
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(Tokens.excerptPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchSurface(Tokens.excerptSurface, in: MonarchShapes.control)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.excerptAccessibilityLabel(excerpt, labels: labels))
    }

    private var quickPrompts: some View {
        MonarchFlowLayout(spacing: Tokens.suggestionSpacing) {
            ForEach(suggestions, id: \.self) { suggestion in
                MonarchButton(suggestion, variant: .ghost, size: .sm) {
                    submit(suggestion)
                }
            }
        }
        .disabled(isPending)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.suggestions ?? Strings.suggestions)
    }

    private func submit(_ raw: String) {
        guard let prompt = Tokens.submission(raw, isPending: isPending) else { return }
        onSubmit(prompt)
        if closesOnSubmit { dismiss() }
    }
}

#if DEBUG
    #Preview("MonarchAiInlinePrompt") {
        MonarchPreviewMatrix {
            MonarchAiInlinePrompt(
                context: "Early numbers suggest more people finish setup on their first visit.",
                suggestions: ["Shorten", "Make it more formal", "Fix spelling"]
            ) { _ in }
            MonarchAiInlinePrompt(
                context: "Thanks for the update.",
                initialValue: "Translate to Spanish",
                isPending: true
            ) { _ in }
        }
    }
#endif
