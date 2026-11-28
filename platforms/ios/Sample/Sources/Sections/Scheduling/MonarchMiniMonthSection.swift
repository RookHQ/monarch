import MonarchUI
import SwiftUI

struct MonarchMiniMonthSection: CataloguePage {
    static let title = "Mini month"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @State private var day: Date? = Calendar.current.startOfDay(for: .now)
    @State private var month = Date.now
    @State private var skipsWeekends = true
    @State private var bookingDay: Date?

    var body: some View {
        CatalogueCaption("Events as dots; the selected day's events are also text")
        MonarchMiniMonth(
            selection: $day,
            visibleMonth: $month,
            isDateDisabled: nil,
            dayInfo: { date in info(for: date) }
        )
        Text(summary)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
        Text("Showing \(month.formatted(.dateTime.month(.wide).year()))")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        MonarchButton("Go to today") {
            month = .now
            day = calendar.startOfDay(for: .now)
        }

        CatalogueCaption("Booking: the next 30 days, weekends unavailable")
        Toggle("Weekends are unavailable", isOn: $skipsWeekends)
            .monarchTextStyle(.body)
        MonarchMiniMonth(
            selection: $bookingDay,
            minimumDate: .now,
            maximumDate: calendar.date(byAdding: .day, value: 30, to: .now),
            isDateDisabled: { date in skipsWeekends && calendar.isDateInWeekend(date) },
            dayInfo: { date in
                calendar.isDateInWeekend(date)
                    ? nil : MonarchMiniMonthDayInfo(dots: [.success], description: "Available")
            }
        )
        Text(bookingDay.map { "Booked for \($0.formatted(date: .complete, time: .omitted))" } ?? "No day chosen")
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
    }

    /// Made-up events: a few days of each month have one to three.
    private func eventCount(on date: Date) -> Int {
        let day = calendar.component(.day, from: date)
        return day % 4 == 0 ? day % 3 + 1 : 0
    }

    private func info(for date: Date) -> MonarchMiniMonthDayInfo? {
        let count = eventCount(on: date)
        return count == 0 ? nil : MonarchMiniMonthDayInfo(dotCount: count, description: eventText(count))
    }

    private func eventText(_ count: Int) -> String {
        count == 1 ? "1 event" : "\(count) events"
    }

    private var summary: String {
        guard let day else { return "No day selected" }
        let count = eventCount(on: day)
        let name = day.formatted(date: .complete, time: .omitted)
        return count == 0 ? "\(name): no events" : "\(name): \(eventText(count))"
    }
}
