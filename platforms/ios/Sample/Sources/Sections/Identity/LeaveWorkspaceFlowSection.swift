import MonarchUI
import SwiftUI

/// Recipe: leave a workspace after a confirmation, or learn why not.
struct LeaveWorkspaceFlowSection: CataloguePage {
    static let title = "Leave workspace flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isLastOwner = false
    @State private var failsOnce = true
    @State private var isConfirming = false
    @State private var isBlocked = false
    @State private var isLeaving = false
    @State private var failure: String?
    @State private var result = "Still a member"

    var body: some View {
        CatalogueCaption("The first attempt fails, the next one succeeds")
        Toggle("I am the only owner", isOn: $isLastOwner)
        MonarchButton("Leave workspace", systemImage: "rectangle.portrait.and.arrow.right", variant: .danger) {
            if isLastOwner {
                isBlocked = true
            } else {
                failure = nil
                isConfirming = true
            }
        }
        .alert("You can’t leave yet", isPresented: $isBlocked) {
            Button("Transfer ownership") { result = "Transfer ownership opened" }
            Button("Close", role: .cancel) {}
        } message: {
            Text("You are the only owner. Transfer ownership to another member before you leave.")
        }
        .sheet(isPresented: $isConfirming) {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text("Leave Acme?")
                        .monarchTextStyle(.heading)
                        .accessibilityAddTraits(.isHeader)
                    Text("You are removed from the workspace straight away.")
                        .monarchTextStyle(.body)
                    MonarchConsequenceList([
                        MonarchConsequence(.removed, "You lose access to Projects, Docs and Chat"),
                        MonarchConsequence(.kept, "What you created stays in the workspace for the other members"),
                        MonarchConsequence(.warning, "You need a new invite to come back"),
                    ])
                    if let failure {
                        MonarchInlineNotice(failure, tone: .danger)
                    }
                    MonarchButton(
                        "Leave workspace",
                        variant: .danger,
                        isLoading: isLeaving,
                        fullWidth: true,
                        action: leave
                    )
                    MonarchButton("Cancel", variant: .ghost, fullWidth: true) { isConfirming = false }
                        .disabled(isLeaving)
                }
                .padding(theme.spacing.space6)
            }
            .interactiveDismissDisabled(isLeaving)
            .presentationDetents([.medium, .large])
        }
        Text(result).monarchTextStyle(.caption)
    }

    private func leave() {
        isLeaving = true
        failure = nil
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isLeaving = false
            if failsOnce {
                failsOnce = false
                failure = "Couldn’t leave the workspace. Try again."
                AccessibilityNotification.Announcement("Couldn’t leave the workspace. Try again.").post()
            } else {
                isConfirming = false
                result = "Left the workspace"
            }
        }
    }
}
