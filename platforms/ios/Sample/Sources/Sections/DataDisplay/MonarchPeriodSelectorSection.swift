import MonarchUI
import SwiftUI

struct MonarchPeriodSelectorSection: CataloguePage {
    static let title = "Period selector"

    @State private var period = MonarchPeriod(.last30Days)
    @State private var report = MonarchPeriod(.last90Days)
    @Environment(\.calendar) private var calendar

    var body: some View {
        CatalogueCaption("Segmented, with the resolved dates")
        MonarchPeriodSelector(selection: $period, showsRange: true)
        CatalogueCaption("Query: \(queryText(period))")
        CatalogueCaption("Menu, report presets, at most 366 days")
        MonarchPeriodSelector(
            "Report period",
            selection: $report,
            presets: [.last7Days, .last30Days, .last90Days, .thisMonth, .lastMonth, .lastQuarter, .custom],
            variant: .menu,
            showsRange: true,
            maximumDays: 366
        )
    }

    private func queryText(_ period: MonarchPeriod) -> String {
        guard let range = period.resolved(today: Date(), calendar: calendar) else { return "choose dates" }
        let style = Date.ISO8601FormatStyle().year().month().day()
        return "\(range.lowerBound.formatted(style)) – \(range.upperBound.formatted(style))"
    }
}
