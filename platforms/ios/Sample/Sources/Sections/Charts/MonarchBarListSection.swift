import MonarchUI
import SwiftUI

struct MonarchBarListSection: CataloguePage {
    static let title = "BarList"

    @State private var selected = "none"

    private let pages = [
        MonarchBarListItem("/pricing", value: 4_210, description: "Campaign: spring", systemImage: "tag"),
        MonarchBarListItem("/docs", value: 2_980, isHighlighted: true),
        MonarchBarListItem("/blog", value: 1_120),
        MonarchBarListItem("/changelog", value: nil),
        MonarchBarListItem("/about", value: 310),
    ]

    var body: some View {
        CatalogueCaption("Ranked, first three rows")
        MonarchBarList("Top pages", items: pages, limit: 3, isRanked: true)

        CatalogueCaption("Tappable rows. Selected: \(selected)")
        MonarchBarList("Top pages", items: pages, onSelect: { selected = $0.label })

        CatalogueCaption("Ratios of a whole, written as percentages")
        MonarchBarList(
            "Conversion by channel",
            items: [
                MonarchBarListItem("Email", value: 0.12),
                MonarchBarListItem("Search", value: 0.31),
                MonarchBarListItem("Social", value: 0.08),
            ],
            maximum: 1,
            formatValue: { $0.formatted(.percent) }
        )

        CatalogueCaption("Empty and loading")
        MonarchBarList("Top pages", items: [])
        MonarchBarList("Top pages", items: [], isLoading: true)
    }
}
