import MonarchUI
import SwiftUI

struct MonarchStackedProportionBarSection: CataloguePage {
    static let title = "StackedProportionBar"

    @State private var content = MonarchStackedProportionBarLegendContent.both
    @State private var size = MonarchStackedProportionBarSize.md

    private let pipeline = [
        MonarchStackedProportionBarSegment("new", label: "New", value: 1_240),
        MonarchStackedProportionBarSegment("campaign", label: "In a campaign", value: 610),
        MonarchStackedProportionBarSegment("replied", label: "Replied", value: 182),
        MonarchStackedProportionBarSegment("converted", label: "Converted", value: 41),
        MonarchStackedProportionBarSegment("unsubscribed", label: "Unsubscribed", value: 6),
    ]

    var body: some View {
        CatalogueCaption("Pipeline stages")
        Picker("Legend", selection: $content) {
            Text("Both").tag(MonarchStackedProportionBarLegendContent.both)
            Text("Value").tag(MonarchStackedProportionBarLegendContent.value)
            Text("Percent").tag(MonarchStackedProportionBarLegendContent.percent)
            Text("None").tag(MonarchStackedProportionBarLegendContent.none)
        }
        .pickerStyle(.segmented)
        Picker("Size", selection: $size) {
            Text("Small").tag(MonarchStackedProportionBarSize.sm)
            Text("Medium").tag(MonarchStackedProportionBarSize.md)
            Text("Large").tag(MonarchStackedProportionBarSize.lg)
        }
        .pickerStyle(.segmented)
        MonarchStackedProportionBar("Lead pipeline", segments: pipeline, legendContent: content, size: size)

        CatalogueCaption("Storage capacity with a remainder")
        MonarchStackedProportionBar(
            "Storage",
            segments: [
                MonarchStackedProportionBarSegment("media", label: "Media", value: 42),
                MonarchStackedProportionBarSegment("docs", label: "Documents", value: 18),
                MonarchStackedProportionBarSegment("backups", label: "Backups", value: 9),
            ],
            total: 100,
            remainderLabel: "Free",
            legendContent: .value,
            size: .lg,
            formatValue: { "\(Int($0)) GB" }
        )

        CatalogueCaption("Monitors by state")
        MonarchStackedProportionBar(
            "Monitors",
            segments: [
                MonarchStackedProportionBarSegment("up", label: "Operational", value: 41, tone: .success),
                MonarchStackedProportionBarSegment("slow", label: "Degraded", value: 3, tone: .warning),
                MonarchStackedProportionBarSegment("down", label: "Down", value: 1, tone: .danger),
                MonarchStackedProportionBarSegment("paused", label: "Paused", value: 2, tone: .neutral),
            ],
            size: .sm
        )

        CatalogueCaption("Legend hidden, and no data")
        MonarchStackedProportionBar("Lead pipeline", segments: pipeline, showsLegend: false)
        MonarchStackedProportionBar("Lead pipeline", segments: [])
    }
}
