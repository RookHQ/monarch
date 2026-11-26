import MonarchUI
import SwiftUI

/// Native adoption: the system search field in the navigation bar.
struct SearchInputSection: CataloguePage {
    static let title = "Search input (searchable)"

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var settled = ""

    private let people = ["Ada Lovelace", "Alan Turing", "Grace Hopper", "Katherine Johnson", "Margaret Hamilton"]

    private var matches: [String] {
        query.isEmpty ? people : people.filter { $0.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        CatalogueCaption("The field is in the navigation bar; pull down if it is hidden")
        ForEach(matches, id: \.self) { name in
            Text(name).monarchTextStyle(.body)
        }
        if matches.isEmpty {
            ContentUnavailableView.search(text: query)
        }

        CatalogueCaption("Debounced: what a server would be asked")
        Text(settled.isEmpty ? "Nothing yet" : settled)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
            .searchable(text: $query, prompt: "Search people")
            .onSubmit(of: .search) { settled = query }
            .task(id: query) {
                try? await Task.sleep(for: .milliseconds(300))
                if !Task.isCancelled { settled = query }
            }
    }
}
