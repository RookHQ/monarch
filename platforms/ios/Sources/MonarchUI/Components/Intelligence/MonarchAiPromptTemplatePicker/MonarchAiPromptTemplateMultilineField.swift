import SwiftUI

/// A multi-line text field drawn like ``MonarchTextField``, which is
/// single-line. It reads `MonarchTextFieldTokens`; it is kept here until a
/// shared multi-line field exists.
struct MonarchAiPromptTemplateMultilineField: View {
    let label: String
    let spokenLabel: String
    @Binding var text: String
    let prompt: String?
    let helper: String?
    let error: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        let hasError = error != nil
        VStack(alignment: .leading, spacing: MonarchTextFieldTokens.stackSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityHidden(true)
            TextField(
                label,
                text: $text,
                prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) },
                axis: .vertical
            )
            .lineLimit(
                MonarchAiPromptTemplatePickerTokens
                    .multilineMinLines...MonarchAiPromptTemplatePickerTokens
                    .multilineMaxLines
            )
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
            .focused($isFocused)
            .accessibilityLabel(spokenLabel)
            .accessibilityHint(MonarchTextFieldTokens.accessibilityHint(helper: helper, error: error))
            .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
            .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchTextFieldTokens.minHeight, alignment: .topLeading)
            .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)],
                    lineWidth: MonarchTextFieldTokens.borderWidth
                )
            )
            .monarchFocusRing(isFocused, in: shape)
            if let error {
                HStack(alignment: .firstTextBaseline, spacing: MonarchTextFieldTokens.messageIconSpacing) {
                    MonarchIcon(systemName: MonarchTextFieldTokens.errorSymbol, size: .sm)
                    Text(error).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityHidden(true)
            } else if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityHidden(true)
            }
        }
    }
}
