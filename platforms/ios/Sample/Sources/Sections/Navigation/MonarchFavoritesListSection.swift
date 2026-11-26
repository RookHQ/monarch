import MonarchUI
import SwiftUI

struct MonarchFavoritesListSection: CataloguePage {
    static let title = "Favorites list"

    @Environment(\.monarchTheme) private var theme
    @State private var favorites = [
        MonarchFavoriteItem("Roadmap", id: "doc:1", systemImage: "doc.text", isCurrent: true),
        MonarchFavoriteItem("Q3 launch", id: "project:2", systemImage: "folder"),
        MonarchFavoriteItem("My open tickets", id: "view:3", systemImage: "line.3.horizontal.decrease"),
        MonarchFavoriteItem("Design system", id: "project:4", systemImage: "folder"),
    ]
    @State private var opened = "Nothing yet"
    @State private var isPinned = true

    var body: some View {
        CatalogueCaption("Editable section in a list")
        List {
            MonarchFavoritesList(
                items: favorites,
                emptyMessage: "Star a page to pin it here.",
                onRemove: { item in favorites.removeAll { $0.id == item.id } },
                onReorder: { favorites = $0 },
                onSelect: { opened = $0.title }
            )
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget * 7)
        Text("Opened: \(opened)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        CatalogueCaption("Pin toggle on a page header")
        Toggle("Favorite", systemImage: isPinned ? "star.fill" : "star", isOn: $isPinned)
            .labelStyle(.iconOnly)
            .toggleStyle(.monarchButton())
    }
}
