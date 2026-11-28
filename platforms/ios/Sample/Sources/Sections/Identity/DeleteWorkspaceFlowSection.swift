import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the server rejecting a password.
private struct WrongPassword: LocalizedError {
    var errorDescription: String? { "That password isn’t right." }
}

/// Recipe: delete a workspace: what is deleted, keep a copy or transfer
/// instead, type the name, confirm it is you, done.
struct DeleteWorkspaceFlowSection: CataloguePage {
    static let title = "Delete workspace flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var typed = ""
    @State private var isMatched = false
    @State private var isVerifying = false
    @State private var isDeleting = false
    @State private var isDone = false
    @State private var isExporting = false
    @State private var failure: String?
    @State private var failsOnce = true
    @State private var result = "Workspace still exists"

    private let name = "Acme"

    var body: some View {
        CatalogueCaption("The password “monarch” is accepted; the first delete fails")
        MonarchButton("Delete workspace", systemImage: "trash", variant: .danger) {
            typed = ""
            failure = nil
            isDone = false
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                if isDone {
                    done
                } else {
                    impact
                }
            }
            .interactiveDismissDisabled(isDeleting)
        }
        Text(result).monarchTextStyle(.caption)
    }

    private var impact: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                Text("Everything in \(name) is deleted for every member.")
                    .monarchTextStyle(.body)
                MonarchConsequenceList(
                    [
                        MonarchConsequence(.removed, "apps", count: 4),
                        MonarchConsequence(.removed, "members lose access", count: 12),
                        MonarchConsequence(.removed, "records", count: 18_240),
                        MonarchConsequence(.changed, "active subscription is cancelled", count: 1),
                        MonarchConsequence(.warning, "custom domain is disconnected", count: 1),
                    ],
                    heading: "What will be deleted"
                )
                MonarchSectionHeader("Keep a copy", description: "Export everything before it is deleted.")
                MonarchButton("Export data", systemImage: "square.and.arrow.down", isLoading: isExporting) {
                    isExporting = true
                    Task {
                        try? await Task.sleep(for: .milliseconds(800))
                        isExporting = false
                        AccessibilityNotification.Announcement("Export started.").post()
                    }
                }
                MonarchSectionHeader("Transfer it instead?", description: "Another owner can keep the workspace going.")
                MonarchButton("Transfer ownership instead", variant: .ghost) {
                    isOpen = false
                    result = "Transfer ownership opened"
                }
                NavigationLink("Continue", value: "confirm")
                    .buttonStyle(.monarch(.primary, fullWidth: true))
            }
            .padding(theme.spacing.space6)
        }
        .navigationTitle("Delete workspace")
        .navigationDestination(for: String.self) { _ in confirm }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { isOpen = false }
            }
        }
    }

    private var confirm: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                Text("This cannot be undone.")
                    .monarchTextStyle(.body)
                MonarchTypeToConfirmInput(
                    phrase: name,
                    text: $typed,
                    label: "Type \(name) to confirm",
                    onMatchChange: { isMatched = $0 }
                )
                if let failure {
                    MonarchInlineNotice(failure, tone: .danger)
                }
                MonarchButton(
                    "Delete workspace",
                    variant: .danger,
                    isLoading: isDeleting,
                    fullWidth: true
                ) {
                    isVerifying = true
                }
                .disabled(!isMatched)
                if !isMatched {
                    Text("Type the workspace name to continue.")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .padding(theme.spacing.space6)
        }
        .navigationTitle("Confirm deletion")
        .sheet(isPresented: $isVerifying) {
            MonarchStepUpAuthDialog(
                description: "Deleting a workspace needs you to confirm your identity.",
                onVerified: { delete() },
                onVerify: { credential in
                    try await Task.sleep(for: .milliseconds(600))
                    guard case .password("monarch") = credential else { throw WrongPassword() }
                }
            )
        }
    }

    private var done: some View {
        VStack(spacing: theme.spacing.space4) {
            MonarchEmptyState(
                "Workspace deleted",
                message: "\(name) and everything in it has been deleted.",
                systemImage: "checkmark.circle",
                kind: .success,
                actionTitle: "Close",
                action: {
                    isOpen = false
                    result = "Workspace deleted"
                }
            )
        }
        .padding(theme.spacing.space6)
    }

    private func delete() {
        isDeleting = true
        failure = nil
        Task {
            try? await Task.sleep(for: .milliseconds(800))
            isDeleting = false
            if failsOnce {
                failsOnce = false
                failure = "The workspace couldn’t be deleted. Try again."
                AccessibilityNotification.Announcement("The workspace couldn’t be deleted. Try again.").post()
            } else {
                isDone = true
            }
        }
    }
}
