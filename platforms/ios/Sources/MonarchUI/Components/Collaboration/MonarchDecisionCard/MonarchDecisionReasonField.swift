import SwiftUI

/// The reason field of a decision card: a label, a multi-line `TextField`
/// and one line of supporting text.
///
/// It is built from the shared field chrome (`MonarchFieldLabelRow`,
/// `monarchFieldBox`, `MonarchFieldSupportingText`, `monarchAnnouncesError`),
/// so it looks and reads like ``MonarchTextField``, which is single-line.
struct MonarchDecisionReasonField: View {
    let label: String
    @Binding var text: String
    let prompt: String
    let helper: String?
    let error: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchTextFieldTokens.stackSpacing) {
            MonarchFieldLabelRow(label: label)
            field
            MonarchFieldSupportingText(helper: helper, error: error)
        }
        .monarchAnnouncesError(error)
    }

    private var field: some View {
        TextField(
            label,
            text: $text,
            prompt: Text(prompt).foregroundStyle(theme.colors.textSubtle),
            axis: .vertical
        )
        .lineLimit(MonarchDecisionCardTokens.reasonLines)
        .textFieldStyle(.plain)
        .monarchFieldText()
        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
        .focused($isFocused)
        .accessibilityLabel(label)
        .accessibilityHint(
            MonarchFieldChrome.accessibilityHint(
                helper: helper,
                error: MonarchFieldChrome.activeError(error),
                isRequired: false
            )
        )
        .monarchFieldBox(hasError: MonarchFieldChrome.activeError(error) != nil, isFocused: isFocused)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
    }
}
