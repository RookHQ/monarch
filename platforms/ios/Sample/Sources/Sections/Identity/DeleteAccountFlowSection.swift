@_spi(Experimental) import MonarchUI
import SwiftUI

/// Recipe: delete the account from inside the app, in a sheet with steps.
struct DeleteAccountFlowSection: CataloguePage {
    static let title = "Delete account flow (recipe)"

    private enum Step: Hashable {
        case export
        case reason
        case confirm
    }

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var path: [Step] = []
    @State private var ownedWorkspaces = ["Acme"]
    @State private var exportStatus = MonarchDataExportStatus.idle
    @State private var reason = ""
    @State private var details = ""
    @State private var typed = ""
    @State private var isVerifying = false
    @State private var isDeleted = false

    private let email = "ana@example.com"

    var body: some View {
        MonarchButton("Delete account", systemImage: "trash", variant: .danger) {
            path = []
            typed = ""
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack(path: $path) {
                page { review }
                    .navigationTitle("Delete account")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { isOpen = false } }
                    }
                    .navigationDestination(for: Step.self) { step in
                        switch step {
                        case .export: page { export }
                        case .reason: page { why }
                        case .confirm: page { confirm }
                        }
                    }
            }
            .sheet(isPresented: $isVerifying) {
                MonarchStepUpAuthDialog(
                    description: "Deleting your account needs you to confirm your identity.",
                    methods: [.password, .deviceOwner],
                    onCancel: { isVerifying = false },
                    onVerified: {
                        isVerifying = false
                        isOpen = false
                        isDeleted = true
                    },
                    onVerify: { _ in
                        // Send the credential and the deletion to the server here.
                        try await Task.sleep(for: .milliseconds(400))
                    }
                )
            }
        }
        if isDeleted {
            MonarchBanner(
                "Account deleted",
                message: "Your account has been removed. Thank you for the time you spent with us.",
                tone: .success
            )
        }
    }

    private func page(@ViewBuilder content: () -> some View) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) { content() }
                .padding(theme.spacing.space4)
        }
        .background(theme.colors.bgCanvas)
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Before you delete your account").monarchTextStyle(.heading).accessibilityAddTraits(.isHeader)
            if !ownedWorkspaces.isEmpty {
                MonarchBanner(
                    "Transfer or delete the workspaces you own first",
                    message: "You are the only owner of \(ownedWorkspaces.joined(separator: ", ")).",
                    tone: .warning,
                    actionTitle: "Transfer Acme",
                    action: { ownedWorkspaces = [] }
                )
            }
            MonarchConsequenceList(
                [
                    MonarchConsequence(.removed, "Your profile, messages and files are deleted"),
                    MonarchConsequence(.changed, "You leave every workspace you are a member of"),
                    MonarchConsequence(.kept, "Invoices stay with the workspace that paid them"),
                ],
                heading: "What happens"
            )
            MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(.export) }
                .disabled(!ownedWorkspaces.isEmpty)
        }
    }

    private var export: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchDataExportRequest(
                status: exportStatus,
                scope: [
                    MonarchCheckboxGroupOption(id: "profile", label: "Profile and settings"),
                    MonarchCheckboxGroupOption(id: "files", label: "Files"),
                ],
                onRequest: { _ in exportStatus = .requested }
            )
            MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(.reason) }
        }
    }

    private var why: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchFormField("Why are you leaving?", helper: "Optional. Your answer helps us improve.") {
                Picker("Why are you leaving?", selection: $reason) {
                    Text("Choose a reason").tag("")
                    Text("I don’t use it any more").tag("unused")
                    Text("It costs too much").tag("price")
                    Text("Something else").tag("other")
                }
                .pickerStyle(.menu)
            }
            MonarchFormField("Anything else you want to tell us?") {
                TextField("Anything else you want to tell us?", text: $details, axis: .vertical)
                    .lineLimit(3...6)
            }
            MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(.confirm) }
        }
    }

    private var confirm: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Your account is deleted permanently. This cannot be undone.").monarchTextStyle(.body)
            MonarchTypeToConfirmInput(phrase: email, text: $typed, label: "Type \(email) to confirm")
            MonarchButton("Delete account", variant: .danger, fullWidth: true) { isVerifying = true }
                .disabled(!MonarchTypeToConfirmInput.matches(typed, phrase: email))
        }
    }
}
