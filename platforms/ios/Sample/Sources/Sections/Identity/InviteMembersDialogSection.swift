@_spi(Experimental) import MonarchUI
import SwiftUI

/// Recipe: invite people by email with a role and app access, in a sheet.
struct InviteMembersDialogSection: CataloguePage {
    static let title = "Invite members dialog (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var emails: [String] = []
    @State private var role: String? = "member"
    @State private var apps: Set<String> = ["home"]
    @State private var note = ""
    @State private var isSending = false
    @State private var failed: [String] = []
    @State private var lastInvited = 0

    var body: some View {
        MonarchButton("Invite people", systemImage: "person.badge.plus", variant: .primary) {
            emails = []
            failed = []
            note = ""
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                ScrollView { form.padding(theme.spacing.space4) }
                    .background(theme.colors.bgCanvas)
                    .navigationTitle("Invite people")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { isOpen = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button(emails.count > 1 ? "Send \(emails.count) invites" : "Send invite") { send() }
                                .disabled(emails.isEmpty || role == nil || isSending)
                        }
                    }
            }
            .interactiveDismissDisabled(isSending)
        }
        if lastInvited > 0 {
            Text("Invited \(lastInvited) people").monarchTextStyle(.caption)
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("They’ll get an email and join once they accept the invitation.")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            if !failed.isEmpty {
                MonarchBanner(
                    "Some invitations were not sent",
                    message: failed.map { "\($0): already a member" }.joined(separator: "\n"),
                    tone: .warning
                )
            }
            MonarchTagInput(
                "Email addresses",
                tags: $emails,
                prompt: "name@example.com",
                helper: "Separate several addresses with commas or spaces.",
                separators: [",", " "],
                maxCount: 20,
                validate: { value, _ in
                    value.contains("@") && value.contains(".") ? nil : "“\(value)” is not an email address."
                }
            )
            MonarchRolePicker(
                "Role",
                selection: $role,
                roles: [
                    MonarchRoleOption("member", title: "Member", description: "Can use the apps they have access to."),
                    MonarchRoleOption("admin", title: "Admin", description: "Can manage people and billing."),
                ],
                layout: .compact
            )
            MonarchAppAccessPicker(
                [
                    MonarchAppAccessOption(id: "home", name: "Home", description: "Everyone has it.", isDisabled: true),
                    MonarchAppAccessOption(id: "mail", name: "Mail"),
                    MonarchAppAccessOption(id: "tickets", name: "Tickets", badge: "Beta"),
                ],
                selection: $apps,
                label: "App access"
            )
            MonarchTextField("Personal note (optional)", text: $note, prompt: "Add a note to the invitation email")
            if isSending {
                ProgressView("Sending").progressViewStyle(.monarchLinear)
            }
        }
    }

    private func send() {
        isSending = true
        Task {
            try? await Task.sleep(for: .milliseconds(600))
            // The app sends the invitations here and returns the addresses that failed.
            let rejected = emails.filter { $0.hasPrefix("ana@") }
            isSending = false
            lastInvited = emails.count - rejected.count
            if rejected.isEmpty {
                isOpen = false
                AccessibilityNotification.Announcement("Invited \(lastInvited) people").post()
            } else {
                failed = rejected
                emails = rejected
            }
        }
    }
}
