import MonarchUI
import SwiftUI

/// Recipe: a searchable sheet of the app's commands, opened by a button or Command-K.
struct CommandPaletteSection: CataloguePage {
    static let title = "Command palette (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var query = ""
    @State private var lastCommand = "None"

    private let commands = [
        MonarchSearchResultGroup(
            "Create",
            id: "create",
            results: [
                MonarchSearchResult("New invoice", id: "new-invoice", systemImage: "doc.badge.plus", meta: "⌘N"),
                MonarchSearchResult("New customer", id: "new-customer", systemImage: "person.badge.plus"),
                MonarchSearchResult("New project", id: "new-project", systemImage: "folder.badge.plus"),
            ]
        ),
        MonarchSearchResultGroup(
            "Go to",
            id: "go",
            results: [
                MonarchSearchResult("Reports", id: "reports", systemImage: "chart.bar"),
                MonarchSearchResult("Customers", id: "customers", systemImage: "person.2"),
                MonarchSearchResult("Settings", id: "settings", systemImage: "gearshape", meta: "⌘,"),
            ]
        ),
        MonarchSearchResultGroup(
            "Account",
            id: "account",
            results: [
                MonarchSearchResult("Sign out", id: "sign-out", systemImage: "rectangle.portrait.and.arrow.right")
            ]
        ),
    ]

    private var matches: [MonarchSearchResultGroup] {
        guard !query.isEmpty else { return commands }
        return commands.map { group in
            MonarchSearchResultGroup(
                group.title,
                id: group.id,
                results: group.results.filter { $0.title.localizedCaseInsensitiveContains(query) }
            )
        }
    }

    var body: some View {
        MonarchButton("Search or run a command", systemImage: "magnifyingglass") { isOpen = true }
            .keyboardShortcut("k", modifiers: .command)
            .sheet(isPresented: $isOpen) {
                NavigationStack {
                    MonarchSearchResults(groups: matches, query: query) { command, _ in
                        lastCommand = command.title
                        isOpen = false
                    }
                    .searchable(
                        text: $query,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: "Search commands"
                    )
                    .navigationTitle("Commands")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Close") { isOpen = false }
                        }
                    }
                }
                .presentationDragIndicator(.visible)
                .onDisappear { query = "" }
            }
        Text("Last command: \(lastCommand)")
            .monarchTextStyle(.body)
        Text("With a hardware keyboard, Command-K opens the palette.")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
