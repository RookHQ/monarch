import MonarchUI
import SwiftUI

/// Style coverage: the standard `Menu` with the Monarch button style.
struct MenuButtonSection: CataloguePage {
    static let title = "MenuButton (Menu with the button style)"

    @Environment(\.monarchTheme) private var theme
    @State private var last = "Nothing chosen yet"
    @State private var sort = "name"

    var body: some View {
        Text(last)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("A title and a chevron")
        Menu {
            Button("Edit", systemImage: "pencil") { last = "Edit" }
            Button("Duplicate", systemImage: "plus.square.on.square") { last = "Duplicate" }
            Divider()
            Button("Delete", systemImage: "trash", role: .destructive) { last = "Delete" }
        } label: {
            HStack(spacing: theme.spacing.space1_5) {
                Text("Actions")
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
        }
        .buttonStyle(.monarch())

        CatalogueCaption("A choice that stays shown")
        Menu {
            Picker("Sort by", selection: $sort) {
                Text("Name").tag("name")
                Text("Date").tag("date")
            }
        } label: {
            HStack(spacing: theme.spacing.space1_5) {
                Text(sort == "name" ? "Sort: Name" : "Sort: Date")
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
        }
        .buttonStyle(.monarch(.ghost, size: .sm))

        CatalogueCaption("Icon only, with a spoken name")
        Menu {
            Button("Rename") { last = "Rename" }
            Button("Archive") { last = "Archive" }
        } label: {
            MonarchIcon(systemName: "ellipsis")
        }
        .buttonStyle(.monarch(.ghost))
        .accessibilityLabel("More actions")

        CatalogueCaption("Primary and disabled")
        HStack(spacing: theme.spacing.space2) {
            Menu {
                Button("Invoice") { last = "New invoice" }
                Button("Quote") { last = "New quote" }
            } label: {
                HStack(spacing: theme.spacing.space1_5) {
                    Text("New")
                    MonarchIcon(systemName: "chevron.down", size: .sm)
                }
            }
            .buttonStyle(.monarch(.primary))
            Menu("Locked") {
                Button("Nothing") {}
            }
            .buttonStyle(.monarch())
            .disabled(true)
        }
    }
}
