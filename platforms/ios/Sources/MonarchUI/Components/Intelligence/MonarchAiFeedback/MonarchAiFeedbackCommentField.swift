import SwiftUI

/// The comment of the feedback sheet: a label and a multi-line `TextField`.
///
/// ``MonarchTextField`` is single-line, so this field takes its colours,
/// sizes and spoken hint from `MonarchTextFieldTokens` to look and read the
/// same. The error of the sheet is passed in so it is read as the hint.
struct MonarchAiFeedbackCommentField: View {
    let label: String
    @Binding var text: String
    let prompt: String
    let error: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchTextFieldTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            field
        }
    }

    private var field: some View {
        let shape = MonarchShapes.control
        let border = MonarchTextFieldTokens.border(hasError: error != nil, isEnabled: isEnabled)
        let background = MonarchTextFieldTokens.background(isEnabled: isEnabled)
        return TextField(
            label,
            text: $text,
            prompt: Text(prompt).foregroundStyle(theme.colors.textSubtle),
            axis: .vertical
        )
        .lineLimit(MonarchAiFeedbackTokens.commentLines)
        .textFieldStyle(.plain)
        .monarchTextStyle(.body)
        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
        .focused($isFocused)
        .accessibilityLabel(label)
        .accessibilityHint(MonarchTextFieldTokens.accessibilityHint(helper: nil, error: error))
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchTextFieldTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchTextFieldTokens.borderWidth))
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }
}
