import MonarchUI
import SwiftUI

struct MonarchGaugeSection: CataloguePage {
    static let title = "Gauge"

    @State private var load = 72.0

    private let bands = [
        MonarchGaugeBand(0...70, tone: .success, label: "Healthy"),
        MonarchGaugeBand(70...90, tone: .warning, label: "At risk"),
        MonarchGaugeBand(90...100, tone: .danger, label: "Critical"),
    ]

    var body: some View {
        CatalogueCaption("Arc with bands and a target")
        MonarchGauge("CPU", value: load, description: "Last hour", bands: bands, target: 80, size: .lg)
        Slider(value: $load, in: 0...100, step: 1) {
            Text("CPU load")
        }

        CatalogueCaption("Needle")
        MonarchGauge("CPU", value: load, bands: bands, indicator: .needle)

        CatalogueCaption("No bands, a percentage, small")
        MonarchGauge("Quota used", value: load / 100, in: 0...1, size: .sm, formatValue: { $0.formatted(.percent) })

        CatalogueCaption("No value")
        MonarchGauge("Offline sensor", value: nil, bands: bands)
    }
}
