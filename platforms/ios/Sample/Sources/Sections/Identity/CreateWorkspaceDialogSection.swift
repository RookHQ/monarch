import MonarchUI
import SwiftUI

/// Recipe: a sheet around the workspace onboarding form.
struct CreateWorkspaceDialogSection: CataloguePage {
    static let title = "Create workspace dialog (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @State private var name = ""
    @State private var username = ""
    @State private var type: String?
    @State private var usernameStatus = MonarchUsernameAvailabilityStatus.idle
    @State private var hasSubmitted = false
    @State private var isCreating = false
    @State private var message: String?
    @State private var result = "No workspace created yet. \"acmeinc\" is taken."

    private var nameError: String? {
        let count = name.trimmingCharacters(in: .whitespacesAndNewlines).count
        guard hasSubmitted || count > 0 else { return nil }
        if count < 4 { return "Name must be at least 4 characters" }
        if count > 25 { return "Name must be at most 25 characters" }
        return nil
    }

    var body: some View {
        MonarchButton("New workspace", systemImage: "plus") { isPresented = true }
            .sheet(isPresented: $isPresented) { sheet }
        Text(result).monarchTextStyle(.caption)
    }

    private var sheet: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    MonarchTextField("Workspace name", text: $name, prompt: "Acme Inc.", error: nameError)
                    MonarchUsernameAvailabilityInput(
                        "Workspace username",
                        text: $username,
                        prompt: "acme",
                        helper: "Letters and numbers only, 5 to 30 characters.",
                        prefix: "@",
                        onStatusChange: { status, _ in usernameStatus = status },
                        validate: { handle in await Self.checkUsername(handle) }
                    )
                    MonarchRadioCardGroup(
                        "Type",
                        selection: $type,
                        options: [
                            MonarchRadioCardOption(
                                "creator",
                                title: "Creator",
                                description: "For individual creators and personal projects",
                                systemImage: "person"
                            ),
                            MonarchRadioCardOption(
                                "business",
                                title: "Business",
                                description: "For teams and companies",
                                systemImage: "building.2"
                            ),
                        ],
                        columns: 2
                    )
                    if let message {
                        MonarchInlineNotice(message, tone: .warning)
                    }
                    MonarchButton("Create workspace", variant: .primary, isLoading: isCreating, fullWidth: true) {
                        submit()
                    }
                }
                .padding(theme.spacing.space4)
            }
            .navigationTitle("Create new workspace")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isPresented = false }
                        .disabled(isCreating)
                }
            }
        }
        .interactiveDismissDisabled(isCreating)
    }

    private func submit() {
        hasSubmitted = true
        message = nil
        guard nameError == nil else { return }
        guard type != nil else {
            message = "Choose a workspace type."
            return
        }
        guard usernameStatus == .valid else {
            message =
                usernameStatus == .checking
                ? "Wait for the availability check to finish." : "Choose an available username."
            return
        }
        isCreating = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            isCreating = false
            result = "Created @\(username)."
            isPresented = false
        }
    }

    /// The local rules first, then a stand-in for the product's availability lookup.
    nonisolated private static func checkUsername(_ handle: String) async -> MonarchUsernameAvailabilityResult? {
        guard !handle.isEmpty else { return nil }
        guard handle.allSatisfy({ $0.isASCII && ($0.isLetter || $0.isNumber) }) else {
            return MonarchUsernameAvailabilityResult(isValid: false, message: "Use letters and numbers only")
        }
        guard (5...30).contains(handle.count) else {
            return MonarchUsernameAvailabilityResult(isValid: false, message: "Use 5 to 30 characters")
        }
        try? await Task.sleep(for: .milliseconds(500))
        return MonarchUsernameAvailabilityResult(isValid: handle.lowercased() != "acmeinc")
    }
}
