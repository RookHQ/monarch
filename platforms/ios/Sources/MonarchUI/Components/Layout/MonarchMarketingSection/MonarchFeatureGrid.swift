import SwiftUI

/// Features as cards in columns: a title, a sentence and an optional symbol each.
///
/// ```swift
/// MonarchFeatureGrid(items: [
///     MonarchFeatureGridItem("Slash commands", description: "Tables, lists and callouts, one keystroke away."),
///     MonarchFeatureGridItem("Version history", description: "Named snapshots you can restore safely."),
/// ])
/// ```
///
/// Accessibility: each card is one element, read as its title and description; the title has the header trait.
/// At the accessibility text sizes the cards stack in one column.
public struct MonarchFeatureGrid: View {
    private let items: [MonarchFeatureGridItem]
    private let columns: Int

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A feature grid.
    ///
    /// - Parameters:
    ///   - items: The features, in reading order.
    ///   - columns: The number of columns. Two by default; one at the accessibility text sizes.
    public init(items: [MonarchFeatureGridItem], columns: Int = 2) {
        self.items = items
        self.columns = columns
    }

    /// The content of the view.
    public var body: some View {
        let count = MonarchMarketingSectionTokens.columns(requested: columns, dynamicTypeSize: dynamicTypeSize)
        Grid(
            alignment: .topLeading,
            horizontalSpacing: MonarchMarketingSectionTokens.gridSpacing,
            verticalSpacing: MonarchMarketingSectionTokens.gridSpacing
        ) {
            ForEach(Array(stride(from: 0, to: items.count, by: count)), id: \.self) { start in
                GridRow {
                    ForEach(start..<min(start + count, items.count), id: \.self) { index in
                        card(items[index])
                    }
                    ForEach(0..<(start + count - min(start + count, items.count)), id: \.self) { _ in
                        Color.clear.gridCellUnsizedAxes([.horizontal, .vertical])
                    }
                }
            }
        }
    }

    private func card(_ item: MonarchFeatureGridItem) -> some View {
        VStack(alignment: .leading, spacing: MonarchMarketingSectionTokens.cardTextSpacing) {
            if let systemImage = item.systemImage {
                MonarchIcon(systemName: systemImage)
                    .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.eyebrowColor])
            }
            Text(item.title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.titleColor])
                .accessibilityAddTraits(.isHeader)
            if let description = item.description {
                Text(description)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(MonarchMarketingSectionTokens.cardPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.colors.bgSurface, in: MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchMarketingSectionTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .combine)
    }
}
