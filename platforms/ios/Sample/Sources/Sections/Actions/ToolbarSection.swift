import MonarchUI
import SwiftUI

/// Native adoption: the controls of a list live in the system bars. This
/// page puts its items in the catalogue's own navigation bar.
struct ToolbarSection: CataloguePage {
    static let title = "Toolbar (toolbar and searchable)"

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var layout = "list"
    @State private var showsOpen = true
    @State private var showsMine = false
    @State private var last = "Nothing done yet"

    private let tickets = ["Login fails on Safari", "Export is slow", "Typo on pricing page"]

    private var matches: [String] {
        tickets.filter { ticket in
            query.isEmpty || ticket.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        CatalogueCaption("The search field, View and the overflow menu are in the navigation bar")
        Text(last)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Filters under the bar")
        HStack(spacing: theme.spacing.space2) {
            Toggle("Open", isOn: $showsOpen)
            Toggle("Mine", isOn: $showsMine)
        }
        .toggleStyle(.monarchButton(.outline, size: .sm))

        CatalogueCaption(layout == "list" ? "List" : "Board")
        ForEach(matches, id: \.self) { ticket in
            Text(ticket).monarchTextStyle(.body)
        }
        .searchable(text: $query, prompt: "Search tickets")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Picker("View", selection: $layout) {
                        Text("List").tag("list")
                        Text("Board").tag("board")
                    }
                } label: {
                    Label("View", systemImage: "rectangle.grid.1x2")
                }
            }
            ToolbarItemGroup(placement: .secondaryAction) {
                Button("Export", systemImage: "square.and.arrow.down") { last = "Export" }
                Button("Compact rows", systemImage: "rectangle.compress.vertical") { last = "Compact rows" }
            }
        }
    }
}
