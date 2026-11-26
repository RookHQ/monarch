import MonarchUI
import SwiftUI

/// Recipe: a "jump to" sheet with recent places, forgiving letter matching and a
/// "search everywhere" row, opened by a button or Command-K.
struct QuickSwitcherSection: CataloguePage {
    static let title = "Quick switcher (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var query = ""
    @State private var current = "#general"

    private let places = [
        MonarchSearchResultGroup(
            "Channels",
            id: "channels",
            results: [
                MonarchSearchResult("#general", id: "c-general", systemImage: "number"),
                MonarchSearchResult("#design-review", id: "c-design", systemImage: "number"),
                MonarchSearchResult("#launch-plan", id: "c-launch", systemImage: "number"),
            ]
        ),
        MonarchSearchResultGroup(
            "People",
            id: "people",
            results: [
                MonarchSearchResult("Ana Silva", id: "p-ana", snippet: "ana@example.com", systemImage: "person"),
                MonarchSearchResult("Kofi Mensah", id: "p-kofi", snippet: "kofi@example.com", systemImage: "person"),
            ]
        ),
    ]

    private let recent = [
        MonarchSearchResult("#design-review", id: "c-design", systemImage: "number"),
        MonarchSearchResult("Ana Silva", id: "p-ana", snippet: "ana@example.com", systemImage: "person"),
    ]

    /// Forgiving match: the query's letters in order, so "gnl" finds "#general".
    private func matches(_ text: String) -> Bool {
        var rest = Substring(text.lowercased())
        for letter in query.lowercased() where !letter.isWhitespace {
            guard let found = rest.firstIndex(of: letter) else { return false }
            rest = rest[rest.index(after: found)...]
        }
        return true
    }

    private var groups: [MonarchSearchResultGroup] {
        let term = query.trimmingCharacters(in: .whitespaces)
        guard !term.isEmpty else { return [MonarchSearchResultGroup("Recent", id: "recent", results: recent)] }
        var groups = places.map { group in
            MonarchSearchResultGroup(
                group.title,
                id: group.id,
                results: group.results
                    .filter { matches($0.title) || ($0.snippet.map(matches) ?? false) }
                    // Whole-word matches first, letter matches after.
                    .sorted { lhs, rhs in
                        lhs.title.localizedCaseInsensitiveContains(term)
                            && !rhs.title.localizedCaseInsensitiveContains(term)
                    }
            )
        }
        if term.count >= 2 {
            groups.append(
                MonarchSearchResultGroup(
                    "Search",
                    id: "search",
                    results: [
                        MonarchSearchResult("Search for “\(term)”", id: "search-all", systemImage: "magnifyingglass")
                    ]
                )
            )
        }
        return groups
    }

    var body: some View {
        MonarchButton("Jump to…", systemImage: "magnifyingglass") { isOpen = true }
            .keyboardShortcut("k", modifiers: .command)
            .sheet(
                isPresented: $isOpen,
                onDismiss: { query = "" },
                content: {
                    NavigationStack {
                        MonarchSearchResults(groups: groups, query: query) { place, _ in
                            current = place.id == "search-all" ? "Search results" : place.title
                            isOpen = false
                        }
                        .searchable(
                            text: $query,
                            placement: .navigationBarDrawer(displayMode: .always),
                            prompt: "Jump to…"
                        )
                        .navigationTitle("Jump to")
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Close") { isOpen = false }
                            }
                        }
                    }
                    .presentationDragIndicator(.visible)
                }
            )
        Text("Open: \(current)")
            .monarchTextStyle(.body)
        Text("Try “gnl” to find #general. With a hardware keyboard, Command-K opens the switcher.")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
