import MonarchUI
import SwiftUI

struct MonarchTableToolbarSection: CataloguePage {
    static let title = "Table toolbar"

    @Environment(\.monarchTheme) private var theme
    @State private var onlyOpen = false
    @State private var mode = "table"
    @State private var showsEmail = true
    @State private var selection = Set<String>()

    private let contacts = ["Ada Lovelace", "Alan Turing", "Grace Hopper", "Katherine Johnson"]

    var body: some View {
        let shown = onlyOpen ? Array(contacts.prefix(2)) : contacts
        CatalogueCaption("Search goes in the navigation bar with searchable")
        MonarchTableToolbar(
            resultCount: shown.count,
            selectedCount: selection.count,
            totalCount: shown.count,
            bulkActions: [MonarchBulkAction(id: "archive", title: "Archive") { selection.removeAll() }],
            onClearSelection: { selection.removeAll() },
            filters: {
                MonarchTag("Open only", isSelected: $onlyOpen)
            },
            controls: {
                Picker("View", selection: $mode) {
                    Image(systemName: "tablecells").accessibilityLabel("Table").tag("table")
                    Image(systemName: "rectangle.split.3x1").accessibilityLabel("Board").tag("board")
                }
                .pickerStyle(.segmented)
                .fixedSize()
                Menu {
                    Toggle("Email", isOn: $showsEmail)
                        .menuActionDismissBehavior(.disabled)
                } label: {
                    Label("Columns", systemImage: "tablecells")
                }
                .menuStyle(.button)
                .buttonStyle(.monarch(.secondary, size: .sm))
            }
        )
        VStack(alignment: .leading, spacing: theme.spacing.space1) {
            ForEach(shown, id: \.self) { contact in
                Toggle(showsEmail ? "\(contact) · email" : contact, isOn: binding(for: contact))
                    .toggleStyle(.monarchCheckbox)
            }
        }
    }

    private func binding(for contact: String) -> Binding<Bool> {
        Binding(
            get: { selection.contains(contact) },
            set: { isOn in
                if isOn { selection.insert(contact) } else { selection.remove(contact) }
            }
        )
    }
}
