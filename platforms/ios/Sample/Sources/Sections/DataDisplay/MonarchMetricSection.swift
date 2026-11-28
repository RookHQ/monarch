import MonarchUI
import SwiftUI

struct MonarchMetricSection: CataloguePage {
    static let title = "Metric"

    @Environment(\.monarchTheme) private var theme
    @State private var size: MonarchMetricSize = .md
    @State private var isCompact = false

    var body: some View {
        Picker("Size", selection: $size) {
            Text("sm").tag(MonarchMetricSize.sm)
            Text("md").tag(MonarchMetricSize.md)
            Text("lg").tag(MonarchMetricSize.lg)
        }
        .pickerStyle(.segmented)
        Toggle("Compact numbers", isOn: $isCompact)
            .monarchTextStyle(.body)
        CatalogueCaption("Stacked")
        HStack(alignment: .top, spacing: theme.spacing.space6) {
            MonarchMetric(
                "Open tickets",
                value: 12_840,
                change: MonarchMetricChange(0.12, comparison: "vs last week"),
                size: size,
                isCompact: isCompact
            )
            MonarchMetric(
                "Revenue",
                value: 1_284_000,
                change: MonarchMetricChange(current: 1_284_000, previous: 1_400_000),
                format: .currency(code: "EUR"),
                size: size,
                isCompact: isCompact
            )
        }
        CatalogueCaption("Unit, lower is better, empty")
        MonarchMetric(
            "Response time",
            value: 840,
            unit: "ms",
            change: MonarchMetricChange(-0.08, comparison: "vs last week", goodDirection: .down, variant: .subtle),
            size: size
        )
        MonarchMetric("Churn", value: nil, unit: "%", size: size)
        CatalogueCaption("Inline, for toolbars and footers")
        MonarchMetric("Conversion", value: 0.034, format: .percent, layout: .inline)
        CatalogueCaption("Trailing, for numeric columns")
        MonarchMetric("Total", value: 9_048.5, alignment: .trailing)
            .frame(maxWidth: .infinity, alignment: .trailing)
    }
}
