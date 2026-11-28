import MonarchUI
import SwiftUI

struct MonarchDateRangePickerSection: CataloguePage {
    static let title = "Date range picker"

    @Environment(\.monarchTheme) private var theme
    @State private var period: ClosedRange<Date>? = {
        let today = Calendar.current.startOfDay(for: Date())
        return (Calendar.current.date(byAdding: .day, value: -6, to: today) ?? today)...today
    }()
    @State private var leave: ClosedRange<Date>?
    @State private var booking: ClosedRange<Date>?

    var body: some View {
        MonarchDateRangePicker(
            "Report period",
            selection: $period,
            helper: "Up to a year, ending today at the latest.",
            maximumDate: Date(),
            maximumDays: 366,
            presets: [
                .lastDays(7, label: "Last 7 days"),
                .lastDays(30, label: "Last 30 days"),
                .month(0, label: "This month"),
                .month(-1, label: "Last month"),
            ]
        )
        Text(
            period.map {
                "\($0.lowerBound.formatted(date: .abbreviated, time: .omitted)) to \($0.upperBound.formatted(date: .abbreviated, time: .omitted))"
            } ?? "No period"
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Leave: at least 2 days, from today")
        MonarchDateRangePicker(
            "Leave",
            selection: $leave,
            minimumDate: Date(),
            minimumDays: 2,
            presets: [.nextDays(7, label: "Next 7 days")],
            isRequired: true
        )

        CatalogueCaption("Weekends cannot start or end a booking")
        MonarchDateRangePicker(
            "Booking",
            selection: $booking,
            minimumDate: Date(),
            isDateDisabled: { Calendar.current.isDateInWeekend($0) }
        )

        CatalogueCaption("Disabled")
        MonarchDateRangePicker("Archived period", selection: .constant(period))
            .disabled(true)
    }
}
