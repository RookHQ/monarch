import MonarchUI
import SwiftUI

struct MonarchChartLegendSection: CataloguePage {
    static let title = "ChartLegend"

    @State private var hidden: Set<String> = []

    private let series = [
        MonarchChartLegendItem("web", label: "Web", value: "12,480"),
        MonarchChartLegendItem("ios", label: "iOS", value: "8,302"),
        MonarchChartLegendItem("android", label: "Android", value: "6,915"),
        MonarchChartLegendItem("last", label: "Last period", color: .neutral, isDashed: true),
    ]

    var body: some View {
        CatalogueCaption("Plain, line markers")
        MonarchChartLegend(series)
        CatalogueCaption("Square markers, vertical")
        MonarchChartLegend(series, marker: .square, axis: .vertical)
        CatalogueCaption("Toggles")
        MonarchChartLegend(series, hiddenSeries: $hidden, marker: .dot)
        Text("Hidden: \(hidden.sorted().joined(separator: ", "))").monarchTextStyle(.caption)
    }
}
