import SwiftUI

/// The search field of the log viewer: a magnifying glass, the text and a
/// clear button. Kept here because `MonarchTextField` has no accessory
/// slots; it reads `MonarchTextFieldTokens` so it looks the same.
struct MonarchLogViewerSearchField: View {
    let label: String
    let prompt: String
    let clearLabel: String
    @Binding var text: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        HStack(spacing: MonarchLogViewerTokens.searchIconSpacing) {
            MonarchIcon(systemName: MonarchLogViewerTokens.searchSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
            TextField(label, text: $text, prompt: Text(prompt).foregroundStyle(theme.colors.textSubtle))
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .autocorrectionDisabled()
                .focused($isFocused)
                .accessibilityLabel(label)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    MonarchIcon(systemName: MonarchLogViewerTokens.clearSymbol, label: clearLabel, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchTextFieldTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchTextFieldTokens.border(hasError: false, isEnabled: isEnabled)],
                lineWidth: MonarchTextFieldTokens.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }
}
