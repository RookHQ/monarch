import MonarchUI
import SwiftUI

struct MonarchWorldMapSection: CataloguePage {
    static let title = "World map"

    @State private var variant = MonarchWorldMapVariant.choropleth
    @State private var selected = "Tap a country with a value."

    private let data = [
        MonarchWorldMapDatum("US", value: 1_200, details: [MonarchWorldMapDetail("Users", value: "340")]),
        MonarchWorldMapDatum("DE", value: 300, details: [MonarchWorldMapDetail("Users", value: "90")]),
        MonarchWorldMapDatum("IN", value: 150),
        MonarchWorldMapDatum("BR", value: 120),
        MonarchWorldMapDatum("AU", value: 80),
        MonarchWorldMapDatum("SG", value: 40),
        MonarchWorldMapDatum("XX", value: 5),
    ]

    var body: some View {
        Picker("Variant", selection: $variant) {
            Text("Choropleth").tag(MonarchWorldMapVariant.choropleth)
            Text("Bubble").tag(MonarchWorldMapVariant.bubble)
        }
        .pickerStyle(.segmented)

        MonarchWorldMap(
            "Workspaces by country",
            data: data,
            description: "Last 30 days",
            variant: variant,
            extras: [MonarchWorldMapExtra("Global", value: 75)],
            footer: "XX is not a real country code, so it is listed under the map.",
            onSelect: { datum in selected = "Selected \(datum.country): \(Int(datum.value))" }
        )
        Text(selected).monarchTextStyle(.caption)

        CatalogueCaption("Thresholds and another colour")
        MonarchWorldMap("Revenue", data: data, color: .data4, thresholds: [50, 200, 1_000])

        CatalogueCaption("States")
        MonarchWorldMap("Traffic", data: [], emptyMessage: "Data appears after the first visit.")
        MonarchWorldMap("Traffic", data: data, status: .loading)
        MonarchWorldMap("Traffic", data: data, status: .error, onRetry: {})
    }
}
