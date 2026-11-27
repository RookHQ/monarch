import MonarchUI
import SwiftUI

struct MonarchBulkActionBarSection: CataloguePage {
    static let title = "Bulk action bar"

    @Environment(\.monarchTheme) private var theme
    @State private var members = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dev Patel", "Eve Martin"]
    @State private var selection = Set<String>()
    @State private var running: String?
    @State private var isConfirming = false

    var body: some View {
        CatalogueCaption("Select members")
        VStack(alignment: .leading, spacing: theme.spacing.space1) {
            ForEach(members, id: \.self) { member in
                Toggle(member, isOn: binding(for: member))
                    .toggleStyle(.monarchCheckbox)
            }
        }
        MonarchBulkActionBar(
            count: selection.count,
            totalCount: members.count,
            actions: [
                MonarchBulkAction(id: "team", title: "Move to team", systemImage: "person.2") { run("team") },
                MonarchBulkAction(id: "role", title: "Change role") { run("role") },
                MonarchBulkAction(id: "export", title: "Export", systemImage: "square.and.arrow.up") { run("export") },
                MonarchBulkAction(id: "remove", title: "Remove", systemImage: "trash", isDestructive: true) {
                    isConfirming = true
                },
            ],
            busyActionID: running,
            onClear: { selection.removeAll() }
        )
        .confirmationDialog(
            "Remove \(selection.count) members?",
            isPresented: $isConfirming,
            titleVisibility: .visible,
            actions: {
                Button("Remove", role: .destructive) {
                    members.removeAll { selection.contains($0) }
                    selection.removeAll()
                }
                Button("Cancel", role: .cancel) {}
            },
            message: { Text("They lose access to the workspace.") }
        )
    }

    private func binding(for member: String) -> Binding<Bool> {
        Binding(
            get: { selection.contains(member) },
            set: { isOn in
                if isOn { selection.insert(member) } else { selection.remove(member) }
            }
        )
    }

    private func run(_ id: String) {
        running = id
        Task {
            try? await Task.sleep(for: .seconds(1))
            running = nil
        }
    }
}
