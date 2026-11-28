import MonarchUI
import SwiftUI

struct MonarchCalendarToolbarSection: CataloguePage {
    static let title = "Calendar toolbar"

    @State private var view = MonarchCalendarToolbarView.month
    @State private var offset = 0
    @State private var isLoading = false
    @State private var created = 0

    private var title: String {
        let calendar = Calendar.current
        let component: Calendar.Component =
            switch view {
            case .day: .day
            case .week: .weekOfYear
            case .month, .agenda: .month
            }
        let date = calendar.date(byAdding: component, value: offset, to: Date()) ?? Date()
        switch view {
        case .day: return date.formatted(.dateTime.weekday(.wide).day().month(.wide))
        case .week: return "Week of " + date.formatted(.dateTime.day().month(.abbreviated).year())
        case .month, .agenda: return date.formatted(.dateTime.month(.wide).year())
        }
    }

    var body: some View {
        MonarchCalendarToolbar(
            title,
            view: $view,
            isBusy: isLoading,
            isTodayDisabled: offset == 0,
            onToday: { offset = 0 },
            onPrevious: { offset -= 1 },
            onNext: { offset += 1 },
            actions: {
                MonarchButton("New", systemImage: "plus", variant: .primary, size: .sm) { created += 1 }
            }
        )
        Text("\(created) events created").monarchTextStyle(.caption)
        Toggle("Loading", isOn: $isLoading).monarchTextStyle(.body)

        CatalogueCaption("One view, so no switcher")
        MonarchCalendarToolbar(
            "Bookings",
            view: .constant(.agenda),
            views: [.agenda],
            isPreviousDisabled: true,
            onToday: {},
            onPrevious: {},
            onNext: {}
        )
    }
}
