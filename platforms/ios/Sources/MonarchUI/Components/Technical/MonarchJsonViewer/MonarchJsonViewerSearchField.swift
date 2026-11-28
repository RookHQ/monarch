import SwiftUI

/// The search field of ``MonarchJsonViewer``. A single-line `TextField` with
/// a leading search symbol and a clear button, drawn with the text field
/// tokens. Kept here because `MonarchTextField` has no accessory slots and
/// `.searchable` needs a navigation container the viewer does not own.
struct MonarchJsonViewerSearchField: View {
    let label: String
    let prompt: String
    @Binding var text: String
    let clearLabel: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        HStack(spacing: MonarchJsonViewerTokens.searchIconSpacing) {
            MonarchIcon(systemName: MonarchJsonViewerTokens.searchSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
            TextField(label, text: $text, prompt: Text(prompt).foregroundStyle(theme.colors.textSubtle))
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .autocorrectionDisabled()
                #if os(iOS)
                    .textInputAutocapitalization(.never)
                #endif
                .focused($isFocused)
                .accessibilityLabel(label)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    MonarchIcon(systemName: MonarchJsonViewerTokens.clearSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(clearLabel)
            }
        }
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .frame(minHeight: MonarchTextFieldTokens.minHeight)
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchTextFieldTokens.border(hasError: false, isEnabled: isEnabled)],
                lineWidth: MonarchTextFieldTokens.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
    }
}
