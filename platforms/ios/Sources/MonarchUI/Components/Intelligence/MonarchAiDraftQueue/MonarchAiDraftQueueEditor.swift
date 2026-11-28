import SwiftUI

/// The multi-line editor of a draft in ``MonarchAiDraftQueue``, drawn like
/// ``MonarchTextField``, which is single-line. It reads
/// `MonarchTextFieldTokens`; it is kept here until a shared multi-line
/// field exists.
struct MonarchAiDraftQueueEditor: View {
    let label: String
    @Binding var text: String
    let labels: MonarchAiDraftQueueLabels
    let onSave: @MainActor () -> Void
    let onCancel: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @FocusState private var isFocused: Bool

    private typealias Tokens = MonarchAiDraftQueueTokens

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        let canSave = !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            TextField(label, text: $text, axis: .vertical)
                .lineLimit(Tokens.editorMinLines...Tokens.editorMaxLines)
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .focused($isFocused)
                .accessibilityLabel(label)
                .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
                .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
                .frame(maxWidth: .infinity, minHeight: MonarchTextFieldTokens.minHeight, alignment: .topLeading)
                .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: true)]))
                .overlay(
                    shape.strokeBorder(
                        theme.colors[role: MonarchTextFieldTokens.border(hasError: !canSave, isEnabled: true)],
                        lineWidth: MonarchTextFieldTokens.borderWidth
                    )
                )
                .monarchFocusRing(isFocused, in: shape)
                .onAppear { isFocused = true }
            HStack(spacing: Tokens.spacing) {
                MonarchButton(labels.save, variant: .primary, size: .sm, action: onSave)
                    .disabled(!canSave)
                MonarchButton(labels.cancel, variant: .ghost, size: .sm, action: onCancel)
            }
        }
    }
}
