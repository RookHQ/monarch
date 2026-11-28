import MonarchUI
import SwiftUI

struct MonarchSectionHeaderSection: CataloguePage {
    static let title = "Section header"

    @Environment(\.monarchTheme) private var theme
    @State private var isBusy = false

    var body: some View {
        CatalogueCaption("Title only, small and medium")
        MonarchSectionHeader("Recent activity")
        MonarchSectionHeader("Recent activity", size: .md)

        CatalogueCaption("Description, meta and actions")
        MonarchSectionHeader("Invoices", description: "Sent in the last 30 days", meta: "12 invoices") {
            MonarchButton("Export", variant: .ghost, size: .sm) {}
            MonarchButton("New invoice", size: .sm) {}
        }

        CatalogueCaption("With a trailing link")
        MonarchSectionHeader("Members", meta: "128 people") {
            Button {
            } label: {
                HStack(spacing: theme.spacing.space1) {
                    Text("View all members")
                    MonarchIcon(systemName: "chevron.forward", size: .sm)
                }
            }
            .buttonStyle(.monarchLink(.subtle))
            .monarchTextStyle(.caption)
        }

        CatalogueCaption("Busy")
        MonarchSectionHeader("Team", size: .md, isBusy: isBusy) {
            MonarchButton(isBusy ? "Stop" : "Refresh", size: .sm) { isBusy.toggle() }
        }

        CatalogueCaption("As the header of a List section")
        List {
            Section {
                Text("Ada Lovelace")
                Text("Grace Hopper")
            } header: {
                MonarchSectionHeader("Members", meta: "2 people")
            }
        }
        .frame(height: theme.spacing.space24 * 2)
    }
}
