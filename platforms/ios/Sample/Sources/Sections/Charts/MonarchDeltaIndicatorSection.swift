import MonarchUI
import SwiftUI

struct MonarchDeltaIndicatorSection: CataloguePage {
    static let title = "DeltaIndicator"

    @Environment(\.monarchTheme) private var theme
    @State private var change = 0.12

    var body: some View {
        CatalogueCaption("Plain and pill")
        MonarchDeltaIndicator(change, comparison: "vs last month")
        MonarchDeltaIndicator(change, comparison: "vs last month", variant: .subtle, size: .md)
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("−5 points", size: .sm) { change -= 0.05 }
            MonarchButton("+5 points", size: .sm) { change += 0.05 }
        }

        CatalogueCaption("Less is better: a fall in cost")
        MonarchDeltaIndicator(-0.2, comparison: "vs last month", goodDirection: .down, variant: .subtle)

        CatalogueCaption("No direction is better")
        MonarchDeltaIndicator(0.3, goodDirection: .neither, variant: .subtle)

        CatalogueCaption("A number with a unit, from two periods, no change, no data")
        MonarchDeltaIndicator(-42, goodDirection: .down, formatValue: { "\(Int($0)) ms" })
        MonarchDeltaIndicator(current: 120, previous: 100, comparison: "vs last week")
        MonarchDeltaIndicator(0, comparison: "vs last month")
        MonarchDeltaIndicator(nil)
    }
}
