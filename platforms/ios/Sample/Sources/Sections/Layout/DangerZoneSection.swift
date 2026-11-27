import MonarchUI
import SwiftUI

/// Recipe: irreversible actions as the last settings section, each a
/// destructive button with its consequence in the footer and a
/// confirmation; on a scrolling page, a danger-toned MonarchSectionCard.
struct DangerZoneSection: CataloguePage {
    static let title = "Danger zone (settings recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var isConfirmingDelete = false
    @State private var isConfirmingLeave = false
    @State private var lastAction = "No action yet."

    var body: some View {
        Text(lastAction).monarchTextStyle(.body)

        CatalogueCaption("In a settings form, opened in a sheet")
        Button("Workspace settings") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) { screen }

        CatalogueCaption("On a scrolling page: MonarchSectionCard with the danger tone")
        MonarchSectionCard("Danger zone", description: "Only workspace owners can use these actions.", tone: .danger) {
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                Text("Leave workspace")
                    .monarchTextStyle(.bodyStrong)
                    .accessibilityAddTraits(.isHeader)
                Text("You lose access to its projects until someone invites you again.")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                MonarchButton("Leave workspace", variant: .danger, size: .sm) { isConfirmingLeave = true }
            }
        }
        .alert(
            "Leave this workspace?",
            isPresented: $isConfirmingLeave,
            actions: {
                Button("Leave", role: .destructive) { lastAction = "Left the workspace" }
                Button("Cancel", role: .cancel) {}
            },
            message: {
                Text("You lose access to its projects.")
            }
        )
    }

    private var screen: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Weekly digest", isOn: .constant(true))
                } header: {
                    Text("Workspace")
                }
                Section {
                    Button("Transfer ownership", role: .destructive) {}
                        .disabled(true)
                } header: {
                    Text("Danger zone")
                } footer: {
                    Text("You are the only member, so there is no one to transfer this workspace to.")
                }
                Section {
                    Button("Delete workspace", role: .destructive) { isConfirmingDelete = true }
                } footer: {
                    Text("Deletes every project, file and member. This can't be undone.")
                }
            }
            .tint(theme.colors.accentSolid)
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { isOpen = false }
                }
            }
            .confirmationDialog("Delete this workspace?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button("Delete workspace", role: .destructive) {
                    lastAction = "Deleted the workspace"
                    isOpen = false
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Every project, file and member is deleted. This can't be undone.")
            }
        }
    }
}
