import MonarchUI
import SwiftUI

/// Recipe: a settings screen as a list of sections. Each row opens its own
/// Form screen with Save in the toolbar; back asks first while there are
/// unsaved changes; the danger zone is last.
struct SettingsPageSection: CataloguePage {
    static let title = "Settings page (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var path: [Pane] = []
    @State private var saved = Notifications()
    @State private var draft = Notifications()
    @State private var savedName = "Acme"
    @State private var name = "Acme"
    @State private var isAskingNotifications = false
    @State private var isAskingProfile = false
    @State private var isConfirmingDelete = false
    @State private var failsToSave = false
    @State private var lastAction = ""

    private enum Pane: String, CaseIterable, Identifiable, Hashable {
        case profile, notifications, members, danger

        var id: String { rawValue }

        var title: String {
            switch self {
            case .profile: "Workspace profile"
            case .notifications: "Notifications"
            case .members: "Members"
            case .danger: "Danger zone"
            }
        }

        var systemImage: String {
            switch self {
            case .profile: "building.2"
            case .notifications: "bell"
            case .members: "person.2"
            case .danger: "exclamationmark.triangle"
            }
        }
    }

    private struct Notifications: Equatable {
        var digest = true
        var mentions = true
        var frequency = "daily"
    }

    private struct SaveFailed: LocalizedError {
        var errorDescription: String? { "The server could not be reached." }
    }

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Saving fails", isOn: $failsToSave)
            .monarchTextStyle(.body)

        CatalogueCaption("Settings")
        NavigationStack(path: $path) {
            List {
                Section {
                    ForEach([Pane.profile, .notifications, .members]) { pane in
                        NavigationLink(value: pane) {
                            row(pane)
                        }
                    }
                }
                Section {
                    NavigationLink(value: Pane.danger) {
                        Label(Pane.danger.title, systemImage: Pane.danger.systemImage)
                            .foregroundStyle(theme.colors.dangerStrong)
                    }
                } footer: {
                    Text(summary(.danger))
                }
            }
            .navigationTitle("Settings")
            .navigationDestination(for: Pane.self) { pane in
                destination(pane)
            }
        }
        .frame(minHeight: theme.spacing.space24 * 6)
        if !lastAction.isEmpty {
            Text("Last action: \(lastAction)").monarchTextStyle(.caption)
        }
    }

    private func summary(_ pane: Pane) -> String {
        switch pane {
        case .profile: savedName
        case .notifications: saved.digest ? "Weekly digest on" : "Weekly digest off"
        case .members: "12 people"
        case .danger: "Transfer or delete this workspace"
        }
    }

    private func row(_ pane: Pane) -> some View {
        Label(
            title: {
                VStack(alignment: .leading, spacing: theme.spacing.space1) {
                    Text(pane.title).monarchTextStyle(.body)
                    Text(summary(pane))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            },
            icon: { Image(systemName: pane.systemImage) }
        )
    }

    private func save() async throws {
        if failsToSave { throw SaveFailed() }
        saved = draft
        savedName = name
        lastAction = "Saved"
    }

    private func leave(_ reason: MonarchUnsavedChangesGuardLeaveReason) {
        if reason == .discard {
            draft = saved
            name = savedName
            lastAction = "Discarded"
        }
        if !path.isEmpty { path.removeLast() }
    }

    @ViewBuilder private func destination(_ pane: Pane) -> some View {
        switch pane {
        case .profile:
            Form {
                Section {
                    TextField("Name", text: $name)
                } footer: {
                    Text("Shown to everyone in the workspace.")
                }
            }
            .navigationTitle(pane.title)
            .toolbar { saveButton(isDirty: name != savedName) }
            .monarchUnsavedChangesGuard(
                isPresented: $isAskingProfile,
                hasChanges: name != savedName,
                onSave: { try await save() },
                onLeave: { leave($0) }
            )
        case .notifications:
            Form {
                Section {
                    Toggle("Weekly digest", isOn: $draft.digest)
                    Toggle("Mentions", isOn: $draft.mentions)
                    Picker("Email me", selection: $draft.frequency) {
                        Text("Daily").tag("daily")
                        Text("Weekly").tag("weekly")
                    }
                } footer: {
                    Text("These apply to everyone in the workspace.")
                }
            }
            .tint(theme.colors.accentSolid)
            .navigationTitle(pane.title)
            .toolbar { saveButton(isDirty: draft != saved) }
            .monarchUnsavedChangesGuard(
                isPresented: $isAskingNotifications,
                hasChanges: draft != saved,
                onSave: { try await save() },
                onLeave: { leave($0) }
            )
        case .members:
            List {
                LabeledContent("Ana Lima", value: "Owner")
                LabeledContent("Sam Ortiz", value: "Member")
            }
            .labeledContentStyle(.monarch)
            .navigationTitle(pane.title)
        case .danger:
            Form {
                Section {
                    Button("Transfer ownership", role: .destructive) {}
                        .disabled(true)
                } footer: {
                    Text("Add another owner first.")
                }
                Section {
                    Button("Delete workspace", role: .destructive) { isConfirmingDelete = true }
                } footer: {
                    Text("Deletes every project, file and member. This can't be undone.")
                }
            }
            .navigationTitle(pane.title)
            .confirmationDialog(
                "Delete this workspace?",
                isPresented: $isConfirmingDelete,
                titleVisibility: .visible,
                actions: {
                    Button("Delete workspace", role: .destructive) { lastAction = "Delete workspace" }
                    Button("Cancel", role: .cancel) {}
                },
                message: {
                    Text("Every project, file and member is deleted. This can't be undone.")
                }
            )
        }
    }

    private func saveButton(isDirty: Bool) -> some ToolbarContent {
        ToolbarItem(placement: .confirmationAction) {
            Button("Save") {
                Task {
                    do {
                        try await save()
                    } catch {
                        lastAction = "Save failed"
                    }
                }
            }
            .disabled(!isDirty)
        }
    }
}
