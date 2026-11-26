import MonarchUI
import SwiftUI

/// Native adoption: `LazyVGrid` and `Grid` with spacing from the space scale.
struct GridSection: CataloguePage {
    static let title = "Grid (LazyVGrid and Grid)"

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var minimumTile: CGFloat = 120

    var body: some View {
        CatalogueCaption("As many columns as fit: LazyVGrid with adaptive items")
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: minimumTile), spacing: theme.spacing.space4)],
            spacing: theme.spacing.space4
        ) {
            ForEach(1..<7, id: \.self) { index in
                tile("Tile \(index)")
            }
        }

        CatalogueCaption("A set number of columns: flexible items")
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: theme.spacing.space2), count: 3),
            spacing: theme.spacing.space2
        ) {
            ForEach(1..<7, id: \.self) { index in
                tile("\(index)")
            }
        }

        CatalogueCaption("Aligned rows and columns: Grid")
        Grid(
            alignment: .leading,
            horizontalSpacing: theme.spacing.space4,
            verticalSpacing: theme.spacing.space2
        ) {
            GridRow {
                Text("Plan").monarchTextStyle(.label)
                Text("Team").monarchTextStyle(.body)
            }
            GridRow {
                Text("Renews").monarchTextStyle(.label)
                Text("4 March").monarchTextStyle(.body)
            }
        }
    }

    private func tile(_ title: String) -> some View {
        Text(title)
            .monarchTextStyle(.label)
            .frame(maxWidth: .infinity, minHeight: theme.spacing.space12)
            .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
    }
}
