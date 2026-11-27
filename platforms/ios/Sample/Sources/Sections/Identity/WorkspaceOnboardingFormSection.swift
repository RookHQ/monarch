import MonarchUI
import SwiftUI

/// Recipe: the form that creates a workspace, composed from Monarch fields.
struct WorkspaceOnboardingFormSection: CataloguePage {
    static let title = "Workspace onboarding form (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var name = ""
    @State private var username = ""
    @State private var type: String?
    @State private var usernameStatus = MonarchUsernameAvailabilityStatus.idle
    @State private var hasSubmitted = false
    @State private var isCreating = false
    @State private var result = "Not created yet. \"acmeinc\" and \"rookhq\" are taken."

    private var nameError: String? {
        let count = name.trimmingCharacters(in: .whitespacesAndNewlines).count
        guard hasSubmitted || count > 0 else { return nil }
        if count < 4 { return "Name must be at least 4 characters" }
        if count > 25 { return "Name must be at most 25 characters" }
        return nil
    }

    private var typeError: String? {
        hasSubmitted && type == nil ? "Choose a workspace type" : nil
    }

    var body: some View {
        Text(result).monarchTextStyle(.caption)
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
            VStack(alignment: .leading, spacing: theme.spacing.space1_5) {
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
                if let typeError {
                    Label(typeError, systemImage: "exclamationmark.circle")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.dangerStrong)
                }
            }
            MonarchButton("Continue", variant: .primary, isLoading: isCreating, fullWidth: true) {
                submit()
            }
        }
    }

    private func submit() {
        hasSubmitted = true
        guard nameError == nil, typeError == nil else { return }
        guard usernameStatus == .valid else {
            result =
                usernameStatus == .checking
                ? "Wait for the availability check to finish." : "Choose an available username."
            return
        }
        isCreating = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            isCreating = false
            result = "Created @\(username)."
        }
    }

    /// The local rules first, then a stand-in for the product's availability lookup.
    nonisolated private static func checkUsername(_ handle: String) async -> MonarchUsernameAvailabilityResult? {
        guard !handle.isEmpty else { return nil }
        guard handle.allSatisfy({ $0.isASCII && ($0.isLetter || $0.isNumber) }) else {
            return MonarchUsernameAvailabilityResult(isValid: false, message: "Use letters and numbers only")
        }
        guard handle.count >= 5 else {
            return MonarchUsernameAvailabilityResult(isValid: false, message: "Username must be at least 5 characters")
        }
        guard handle.count <= 30 else {
            return MonarchUsernameAvailabilityResult(isValid: false, message: "Username must be at most 30 characters")
        }
        try? await Task.sleep(for: .milliseconds(500))
        let taken = ["acmeinc", "rookhq"]
        return MonarchUsernameAvailabilityResult(isValid: !taken.contains(handle.lowercased()))
    }
}
