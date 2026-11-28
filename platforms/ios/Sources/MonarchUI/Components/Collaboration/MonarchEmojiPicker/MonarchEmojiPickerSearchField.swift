import SwiftUI

/// The search field of the emoji picker: a magnifying glass, the text and a
/// clear button. `MonarchTextField` has no accessory slots, so the field is
/// drawn here from `MonarchTextFieldTokens`. `searchable` is not used
/// because it needs a navigation bar, which a picker in a popover or a
/// sheet section does not have.
struct MonarchEmojiPickerSearchField: View {
    let prompt: String
    let clearLabel: String
    @Binding var text: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        HStack(spacing: MonarchTextFieldTokens.messageIconSpacing) {
            MonarchIcon(systemName: MonarchEmojiPickerTokens.searchSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
            TextField(prompt, text: $text)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .autocorrectionDisabled()
                .submitLabel(.search)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    MonarchIcon(systemName: MonarchEmojiPickerTokens.clearSymbol, label: clearLabel, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .frame(minHeight: MonarchTextFieldTokens.minHeight)
        .background(
            RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
                .fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)])
        )
        .overlay(
            RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
                .strokeBorder(
                    theme.colors[role: MonarchEmojiPickerTokens.fieldBorder(isEnabled: isEnabled)],
                    lineWidth: MonarchTextFieldTokens.borderWidth
                )
        )
    }
}
