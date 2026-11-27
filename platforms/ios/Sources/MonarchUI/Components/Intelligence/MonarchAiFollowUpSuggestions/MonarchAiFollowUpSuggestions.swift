import SwiftUI

/// Follow-up questions shown after an assistant reply, as a horizontally
/// scrolling row of chips.
///
/// ```swift
/// MonarchAiFollowUpSuggestions(reply.followUps) { question in
///     conversation.send(question)
/// }
/// .disabled(conversation.isStreaming)
/// ```
///
/// Each chip is a standard `Button` that calls `onSelect` with its text.
/// Suggestions are trimmed, and empty or repeated ones are left out; with
/// none left nothing is shown. Disable every chip with `disabled(_:)`, for
/// example while a reply is being generated. The row scrolls when the chips
/// do not fit, and follows the layout direction.
///
/// Accessibility: the chips are a group named "Suggested follow-ups". Each is
/// a button named by its full text, even when the visible text is truncated,
/// with a touch target at least 44 points high. VoiceOver and Switch Control
/// move through the chips in order and scroll the row to the focused one.
public struct MonarchAiFollowUpSuggestions: View {
    private let suggestions: [String]
    private let label: String?
    private let onSelect: @MainActor (String) -> Void

    @ScaledMetric(relativeTo: .body) private var maxChipWidth = MonarchAiFollowUpSuggestionsTokens.maxChipWidth

    /// Follow-up suggestions.
    ///
    /// - Parameters:
    ///   - suggestions: Questions the person might ask next, as complete prompts, already localised.
    ///   - label: Replaces the built-in spoken name "Suggested follow-ups".
    ///   - onSelect: Runs with the text of the chosen suggestion.
    public init(
        _ suggestions: [String],
        label: String? = nil,
        onSelect: @escaping @MainActor (String) -> Void
    ) {
        self.suggestions = suggestions
        self.label = label
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let shown = MonarchAiFollowUpSuggestionsTokens.visible(suggestions)
        if !shown.isEmpty {
            MonarchFlowLayout(spacing: MonarchAiFollowUpSuggestionsTokens.spacing) {
                ForEach(shown, id: \.self) { suggestion in
                    Button {
                        onSelect(suggestion)
                    } label: {
                        Text(suggestion)
                            .lineLimit(MonarchAiFollowUpSuggestionsTokens.lineLimit)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: maxChipWidth)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .buttonStyle(MonarchAiFollowUpChipStyle())
                    .accessibilityLabel(suggestion)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label ?? MonarchAiFollowUpSuggestionsStrings.label)
        }
    }
}

/// The surface of one chip: a bordered capsule that takes the AI tint while pressed.
private struct MonarchAiFollowUpChipStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchAiFollowUpChipStyleBody(configuration: configuration)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchAiFollowUpChipStyleBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiFollowUpSuggestionsTokens.appearance(
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let inset = MonarchAiFollowUpSuggestionsTokens.hitInset
        configuration.label
            .monarchTextStyle(MonarchAiFollowUpSuggestionsTokens.textStyle)
            .foregroundStyle(theme.colors[role: appearance.foreground])
            .padding(.horizontal, MonarchAiFollowUpSuggestionsTokens.horizontalPadding)
            .padding(.vertical, MonarchAiFollowUpSuggestionsTokens.verticalPadding)
            .frame(minHeight: MonarchAiFollowUpSuggestionsTokens.minHeight)
            .background(Capsule().fill(theme.colors[role: appearance.background]))
            .overlay(
                Capsule().strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchAiFollowUpSuggestionsTokens.borderWidth
                )
            )
            // The visible chip is shorter than 44 points; the touch target is not.
            .padding(.vertical, inset)
            .contentShape(Rectangle())
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchAiFollowUpSuggestions") {
        let suggestions = [
            "What changed since last quarter?",
            "Show it as a table",
            "Which customers are at risk of leaving this month, and why?",
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiFollowUpSuggestions(suggestions) { _ in }
                MonarchAiFollowUpSuggestions(suggestions) { _ in }.disabled(true)
            }
        }
    }
#endif
