import SwiftUI

/// The content of the feedback sheet: the reasons, the comment, the error
/// and the send button, with Cancel in the toolbar.
struct MonarchAiFeedbackForm: View {
    let value: MonarchAiMessageFeedback
    let options: [MonarchCheckboxGroupOption]
    @Binding var chosen: Set<String>
    @Binding var comment: String
    let isPending: Bool
    let hasFailed: Bool
    let labels: MonarchAiFeedbackLabels
    let onSubmit: @MainActor () -> Void
    let onCancel: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiFeedbackTokens

    private var errorText: String? {
        hasFailed ? labels.error ?? MonarchAiFeedbackStrings.error : nil
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Tokens.formSpacing) {
                    if !options.isEmpty {
                        MonarchCheckboxGroup(
                            Tokens.question(value, labels: labels),
                            selection: $chosen,
                            options: options
                        )
                    }
                    MonarchAiFeedbackCommentField(
                        label: labels.comment ?? MonarchAiFeedbackStrings.comment,
                        text: $comment,
                        prompt: labels.commentPlaceholder ?? MonarchAiFeedbackStrings.commentPlaceholder,
                        error: errorText
                    )
                    if let errorText {
                        HStack(alignment: .firstTextBaseline, spacing: Tokens.messageIconSpacing) {
                            MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
                            Text(errorText).monarchTextStyle(.caption)
                        }
                        .foregroundStyle(theme.colors.dangerStrong)
                        .accessibilityElement(children: .combine)
                    }
                    MonarchButton(
                        labels.submit ?? MonarchAiFeedbackStrings.submit,
                        variant: .primary,
                        isLoading: isPending,
                        fullWidth: true,
                        action: onSubmit
                    )
                }
                .padding(Tokens.formPadding)
                .disabled(isPending)
            }
            .navigationTitle(labels.group ?? MonarchAiFeedbackStrings.group)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.cancel ?? MonarchAiFeedbackStrings.cancel, action: onCancel)
                        .disabled(isPending)
                }
            }
        }
        .accessibilityElement(children: .contain)
    }
}
