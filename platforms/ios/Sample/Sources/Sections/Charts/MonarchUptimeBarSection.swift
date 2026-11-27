import MonarchUI
import SwiftUI

struct MonarchUptimeBarSection: CataloguePage {
    static let title = "UptimeBar"

    @State private var shared: Int?
    @State private var selected = "none"

    private let api: [MonarchUptimeBarSegment] = (0..<30).map { day in
        let label = "Mar \(day + 1)"
        switch day {
        case 4: return MonarchUptimeBarSegment(id: "api\(day)", label: label, status: .maintenance, uptime: 100)
        case 11:
            return MonarchUptimeBarSegment(
                id: "api\(day)",
                label: label,
                uptime: 99.42,
                details: [
                    MonarchUptimeBarDetail("Checks", value: "1,440"), MonarchUptimeBarDetail("p95", value: "410 ms"),
                ],
                note: "Slow responses from the search index"
            )
        case 19: return MonarchUptimeBarSegment(id: "api\(day)", label: label, status: .partial, uptime: 98.1)
        case 23: return MonarchUptimeBarSegment(id: "api\(day)", label: label, uptime: 96.4, note: "Database failover")
        default: return MonarchUptimeBarSegment(id: "api\(day)", label: label, uptime: 100)
        }
    }

    private let web: [MonarchUptimeBarSegment] = (0..<30).map { day in
        MonarchUptimeBarSegment(id: "web\(day)", label: "Mar \(day + 1)", uptime: day < 3 ? nil : 100)
    }

    var body: some View {
        CatalogueCaption("Status page. Tap a day. Selected: \(selected)")
        MonarchUptimeBar(
            "Search API",
            segments: api,
            description: "Last 30 days",
            startLabel: "30 days ago",
            endLabel: "Today",
            showsLegend: true,
            onSegmentSelect: { selected = $0.label }
        )

        CatalogueCaption("Two small bars sharing one inspected day")
        MonarchUptimeBar("API", segments: api, size: .sm, activeIndex: $shared)
        MonarchUptimeBar("Website", segments: web, size: .sm, activeIndex: $shared)
    }
}
