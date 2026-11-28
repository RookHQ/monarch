import SwiftUI

/// The dialog that sends a request back with a reason. It stays open while
/// the app works and shows the failure if the app throws.
struct MonarchApprovalQueueRejectSheet: View {
    let item: MonarchApprovalQueueItem
    let requirement: MonarchApprovalQueueRejectReason
    let labels: MonarchApprovalQueueLabels
    let onReject: @MainActor (MonarchApprovalQueueItem, String?) async throws -> Void
    let onDone: @MainActor () -> Void

    @State private var reason = ""
    @State private var isPending = false
    @State private var error: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss

    private typealias Tokens = MonarchApprovalQueueTokens

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.dialogSpacing) {
                Text(labels.rejectTitle(item.requester.name))
                    .monarchTextStyle(.heading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                Text(labels.rejectDescription)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                MonarchTextField(
                    labels.rejectReason,
                    text: $reason,
                    prompt: labels.rejectReasonPrompt,
                    error: error
                )
                .disabled(isPending)
                MonarchFlowLayout(spacing: Tokens.actionSpacing, justification: .trailing) {
                    MonarchButton(labels.cancel, variant: .ghost) {
                        dismiss()
                    }
                    .disabled(isPending)
                    MonarchButton(
                        labels.reject,
                        systemImage: Tokens.rejectSymbol,
                        variant: .danger,
                        isLoading: isPending
                    ) {
                        submit()
                    }
                    .disabled(!Tokens.canConfirmReject(reason: reason, requirement: requirement))
                }
            }
            .padding(Tokens.dialogPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgSurface)
        .interactiveDismissDisabled(isPending)
    }

    private func submit() {
        guard !isPending else { return }
        isPending = true
        error = nil
        Task {
            do {
                try await onReject(item, Tokens.submittedReason(reason, requirement: requirement))
                isPending = false
                onDone()
                dismiss()
            } catch {
                isPending = false
                self.error = labels.failed
            }
        }
    }
}
