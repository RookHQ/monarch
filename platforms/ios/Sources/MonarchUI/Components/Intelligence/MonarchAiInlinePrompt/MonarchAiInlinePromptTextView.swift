import SwiftUI

/// Text, editable or read only, whose text selection menu has an "Ask AI"
/// item that passes the selected text, to open a ``MonarchAiInlinePrompt``.
///
/// ```swift
/// MonarchAiInlinePromptTextView("Draft", text: $draft) { selection in
///     selected = selection
/// }
/// .sheet(item: $selected) { selection in
///     MonarchAiInlinePrompt(context: selection) { prompt in rewrite(selection, with: prompt) }
/// }
/// ```
///
/// SwiftUI's `TextEditor` and `Text` with `.textSelection(.enabled)` on
/// iOS 17 neither report the selection nor take items in the text menu, so
/// this is a `UITextView`: "Ask AI" is added after the system's own items
/// (Cut, Copy, Paste, Look Up and others) through
/// `UITextViewDelegate.textView(_:editMenuForTextIn:suggestedActions:)`,
/// only while text is selected, so the caret menu is unchanged. The text
/// uses the Monarch body style and grows with its content; put it in a
/// scroll view for long text.
///
/// Accessibility: the text view keeps the system's text behaviour and is
/// named by `title`. While its text is not blank, it has an "Ask AI"
/// VoiceOver action that passes the selection, or the whole text when
/// nothing is selected. The text follows Dynamic Type.
public struct MonarchAiInlinePromptTextView: View {
    private let title: String
    @Binding private var text: String
    private let isEditable: Bool
    private let isEnabled: Bool
    private let label: String?
    private let onAskAi: @MainActor (String) -> Void

    @ScaledMetric private var fontSize: CGFloat

    @Environment(\.monarchTheme) private var theme
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.isEnabled) private var isEnvironmentEnabled

    private typealias Tokens = MonarchAiInlinePromptTokens

    /// A text view with "Ask AI" in its text menu.
    ///
    /// - Parameters:
    ///   - title: The spoken name of the text, such as "Draft".
    ///   - text: The text.
    ///   - isEditable: Whether the text can be edited. Off, it can still be selected.
    ///   - isEnabled: Off, the menu item and the action are not added.
    ///   - label: The menu item's title. Built in: "Ask AI".
    ///   - onAskAi: Receives the selected text. Present the ``MonarchAiInlinePrompt`` sheet.
    public init(
        _ title: String,
        text: Binding<String>,
        isEditable: Bool = true,
        isEnabled: Bool = true,
        label: String? = nil,
        onAskAi: @escaping @MainActor (String) -> Void
    ) {
        self.title = title
        _text = text
        self.isEditable = isEditable
        self.isEnabled = isEnabled
        self.label = label
        self.onAskAi = onAskAi
        _fontSize = ScaledMetric(wrappedValue: Tokens.textStyle.spec.size, relativeTo: Tokens.textStyle.spec.relativeTo)
    }

    /// The content of the view.
    public var body: some View {
        let spec = Tokens.textStyle.spec
        MonarchAiInlinePromptTextField(
            text: $text,
            style: MonarchAiInlinePromptTextFieldStyle(
                title: title,
                menuTitle: label ?? MonarchAiInlinePromptStrings.label,
                fontSize: fontSize,
                fontFamily: theme.typography.fontFamily,
                lineSpacing: max(0, (spec.lineHeight - MonarchTextStyleSpec.systemLineHeight) * fontSize),
                textColor: theme.colors[role: Tokens.textRole(isEnabled: isEnvironmentEnabled)],
                isRightToLeft: layoutDirection == .rightToLeft,
                isEditable: isEditable && isEnvironmentEnabled,
                isMenuEnabled: isEnabled && isEnvironmentEnabled
            ),
            onAskAi: onAskAi
        )
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if DEBUG
    #Preview("MonarchAiInlinePromptTextView") {
        MonarchPreviewMatrix {
            MonarchAiInlinePromptTextView(
                "Draft",
                text: .constant("Thanks for the update, I will send it today."),
                onAskAi: { _ in }
            )
            MonarchAiInlinePromptTextView(
                "Summary",
                text: .constant("Early numbers suggest more people finish setup on their first visit."),
                isEditable: false,
                onAskAi: { _ in }
            )
        }
    }
#endif
