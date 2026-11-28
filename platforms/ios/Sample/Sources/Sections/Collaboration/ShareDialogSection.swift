import MonarchUI
import SwiftUI

/// Recipe: sharing a record with people, a role and general access, in a sheet.
struct ShareDialogSection: CataloguePage {
    static let title = "Share dialog (sheet recipe)"

    private struct Person: Identifiable {
        let id: String
        let name: String
        let email: String
        var role: String
        var isInvited = false
        var isSelf = false
    }

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var invitees: [String] = []
    @State private var inviteRole = "viewer"
    @State private var access = "invited"
    @State private var people = [
        Person(id: "me", name: "Ana Silva", email: "ana@example.com", role: "owner", isSelf: true),
        Person(id: "sam", name: "Sam Lee", email: "sam@example.com", role: "editor"),
    ]

    private let link = "https://example.com/docs/q3-plan"
    private let roles = [
        MonarchRoleOption("owner", title: "Owner", isDisabled: true, disabledReason: "Transfer ownership in Settings."),
        MonarchRoleOption("editor", title: "Editor", description: "Can edit and comment."),
        MonarchRoleOption("viewer", title: "Viewer", description: "Can view and comment."),
    ]
    private let directory = [
        MonarchEntityPickerEntity(id: "lee", name: "Lee Park", description: "lee@example.com"),
        MonarchEntityPickerEntity(id: "tom", name: "Tom Becker", description: "tom@example.com"),
        MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", description: "sam@example.com", isDisabled: true),
    ]
    private let accessOptions = [
        ("invited", "Only people invited", "Only the people listed above can open it."),
        ("workspace", "Anyone in the workspace", "Anyone in the workspace can open it with the link."),
    ]

    var body: some View {
        Text("\(people.count) people have access")
            .monarchTextStyle(.bodyStrong)
        MonarchButton("Share", systemImage: "person.badge.plus", variant: .primary) {
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                Form {
                    Section {
                        MonarchEntityPicker(
                            "Invite people",
                            selection: $invitees,
                            entities: directory,
                            prompt: "Name or email"
                        )
                        Picker("Role for new people", selection: $inviteRole) {
                            ForEach(roles.filter { !$0.isDisabled }, id: \.value) { Text($0.title).tag($0.value) }
                        }
                        MonarchButton("Invite", variant: .primary, fullWidth: true) { invite() }
                            .disabled(invitees.isEmpty)
                    }
                    Section("People with access") {
                        ForEach(people) { person in
                            MonarchMemberRow(
                                person.name,
                                url: nil,
                                email: person.email,
                                status: person.isInvited ? .invited : .active,
                                role: person.role,
                                roles: roles,
                                isSelf: person.isSelf,
                                onRoleChange: { role in setRole(role, for: person.id) },
                                onRemove: { people.removeAll { $0.id == person.id } }
                            )
                        }
                    }
                    Section {
                        Picker("Who can open it", selection: $access) {
                            ForEach(accessOptions, id: \.0) { Text($0.1).tag($0.0) }
                        }
                        .onChange(of: access) { _, value in
                            let label = accessOptions.first { $0.0 == value }?.1 ?? value
                            AccessibilityNotification.Announcement("General access: \(label).").post()
                        }
                    } header: {
                        Text("General access")
                    } footer: {
                        Text(accessOptions.first { $0.0 == access }?.2 ?? "")
                    }
                    Section("Link") {
                        MonarchCopyField("Link", value: link, isLabelHidden: true)
                        ShareLink("Send link", item: link)
                    }
                }
                .scrollContentBackground(.hidden)
                .background(theme.colors.bgSurface)
                .navigationTitle("Share “Q3 plan”")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { isOpen = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
            .presentationBackground(theme.colors.bgSurface)
        }
    }

    private func invite() {
        let added = directory.filter { invitees.contains($0.id) }
        people += added.map {
            Person(id: $0.id, name: $0.name, email: $0.description ?? "", role: inviteRole, isInvited: true)
        }
        invitees = []
        let count = added.count
        AccessibilityNotification.Announcement(count == 1 ? "1 person invited." : "\(count) people invited.").post()
    }

    private func setRole(_ role: String, for id: String) {
        guard let index = people.firstIndex(where: { $0.id == id }) else { return }
        people[index].role = role
    }
}
