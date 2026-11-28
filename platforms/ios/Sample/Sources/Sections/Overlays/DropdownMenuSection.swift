import MonarchUI
import SwiftUI

/// Native adoption: the standard `Menu`, with the Monarch button style on its trigger.
struct DropdownMenuSection: CataloguePage {
    static let title = "Dropdown menu (standard Menu)"

    @Environment(\.monarchTheme) private var theme
    @State private var showsArchived = false
    @State private var sort = "name"
    @State private var last = "Nothing chosen yet"

    var body: some View {
        Text(last)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Actions, a checkbox item, radio items, a submenu and a destructive item")
        Menu {
            Button("Rename", systemImage: "pencil") { last = "Rename" }
            Button("Duplicate", systemImage: "plus.square.on.square") { last = "Duplicate" }
            Divider()
            Toggle("Show archived", isOn: $showsArchived)
            Picker("Sort by", selection: $sort) {
                Text("Name").tag("name")
                Text("Date").tag("date")
            }
            Menu("Move to") {
                Button("Inbox") { last = "Move to Inbox" }
                Button("Archive") { last = "Move to Archive" }
            }
            Divider()
            Button("Delete", systemImage: "trash", role: .destructive) { last = "Delete" }
        } label: {
            Label("Actions", systemImage: "ellipsis.circle")
        }
        .buttonStyle(.monarch())

        CatalogueCaption("An icon-only trigger with a spoken name")
        Menu {
            Button("Share", systemImage: "square.and.arrow.up") { last = "Share" }
            Button("Print", systemImage: "printer") { last = "Print" }
        } label: {
            MonarchIcon(systemName: "ellipsis")
        }
        .buttonStyle(.monarch(.ghost))
        .accessibilityLabel("More")

        CatalogueCaption("The same items on press and hold")
        Text("Invoice 1042")
            .monarchTextStyle(.body)
            .padding(theme.spacing.space3)
            .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
            .contextMenu {
                Button("Copy number", systemImage: "doc.on.doc") { last = "Copy number" }
                Button("Delete", systemImage: "trash", role: .destructive) { last = "Delete" }
            }
    }
}
