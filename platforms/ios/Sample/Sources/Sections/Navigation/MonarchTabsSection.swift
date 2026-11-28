import MonarchUI
import SwiftUI

struct MonarchTabsSection: CataloguePage {
    static let title = "Tabs"

    private enum Filter: String, CaseIterable {
        case open = "Open"
        case waiting = "Waiting"
        case closed = "Closed"
        case archived = "Archived"
    }

    @Environment(\.monarchTheme) private var theme
    @State private var filter = Filter.open
    @State private var view = "Board"
    @State private var month = "January"

    private var items: [MonarchTabsItem<Filter>] {
        [
            MonarchTabsItem("Open", value: .open, count: "12", countTone: .info),
            MonarchTabsItem("Waiting", value: .waiting, count: "3"),
            MonarchTabsItem("Closed", value: .closed),
            MonarchTabsItem("Archived", value: .archived, isDisabled: true),
        ]
    }

    var body: some View {
        CatalogueCaption("Line")
        MonarchTabs("Ticket filters", selection: $filter, items: items)
        Text("Showing: \(filter.rawValue)")
            .monarchTextStyle(.body)

        CatalogueCaption("Pill")
        MonarchTabs(
            "View",
            selection: $view,
            items: ["Board", "List", "Timeline"].map { MonarchTabsItem($0, value: $0) },
            variant: .pill
        )

        CatalogueCaption("Pill, with more tabs than fit: the strip scrolls")
        MonarchTabs(
            "Month",
            selection: $month,
            items: Calendar.current.monthSymbols.map { MonarchTabsItem($0, value: $0) },
            variant: .pill
        )

        CatalogueCaption("Disabled")
        MonarchTabs("Ticket filters", selection: $filter, items: items)
            .disabled(true)
    }
}
