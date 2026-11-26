import SwiftUI

/// A bordered text field without a visible label, for a rule's value. The rule's field and
/// operator menus say what it is, so the label is spoken only. It reads
/// `MonarchTextFieldTokens` so it matches ``MonarchTextField``, which always shows a label.
struct MonarchQueryBuilderValueField: View {
    let label: String
    @Binding var text: String
    let prompt: String?
    let isNumber: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        TextField(label, text: $text, prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) })
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            #if os(iOS)
                .keyboardType(isNumber ? .decimalPad : .default)
            #endif
            .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
            .focused($isFocused)
            .accessibilityLabel(label)
            .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
            .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
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

/// A number value: keeps what is typed while it is not yet a number, and writes the parsed
/// number (or `nil`) to the rule.
struct MonarchQueryBuilderNumberField: View {
    let label: String
    @Binding var value: Double?
    let prompt: String?

    @Environment(\.locale) private var locale
    @State private var draft: String?

    var body: some View {
        MonarchQueryBuilderValueField(
            label: label,
            text: Binding(
                get: { draft ?? MonarchQueryBuilderTokens.numberText(value, locale: locale) },
                set: { typed in
                    draft = typed
                    value = MonarchQueryBuilderTokens.parseNumber(typed, locale: locale)
                }
            ),
            prompt: prompt,
            isNumber: true
        )
    }
}
