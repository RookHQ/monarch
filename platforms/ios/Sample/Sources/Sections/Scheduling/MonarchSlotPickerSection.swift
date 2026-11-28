import MonarchUI
import SwiftUI

struct MonarchSlotPickerSection: CataloguePage {
    static let title = "Slot picker"

    @Environment(\.calendar) private var calendar
    @Environment(\.monarchTheme) private var theme
    @State private var selection: Date?
    @State private var day: Date?
    @State private var zone = TimeZone.current.identifier
    @State private var isLoading = false

    var body: some View {
        CatalogueCaption("Pick a day, then a time")
        MonarchSlotPicker(
            selection: $selection,
            day: $day,
            slots: slots,
            timeZone: zone,
            minimumDate: calendar.startOfDay(for: .now),
            businessTimeZone: "Europe/London",
            isLoading: isLoading,
            isDayDisabled: { calendar.isDateInWeekend($0) },
            onTimeZoneChange: { zone = $0 },
            onChooseOutside: {}
        )
        .task(id: day) {
            guard day != nil else { return }
            isLoading = true
            try? await Task.sleep(for: .milliseconds(600))
            isLoading = false
        }
        Text(selection.map { "Chosen: \($0.formatted(date: .abbreviated, time: .shortened))" } ?? "No time chosen")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private var slots: [MonarchSlotPickerSlot] {
        guard let day else { return [] }
        return [9, 9.5, 10, 11, 13.5, 15, 16.5].enumerated().map { index, hours in
            MonarchSlotPickerSlot(
                start: day.addingTimeInterval(hours * 3_600),
                seatsLeft: index == 4 ? 2 : nil,
                isDisabled: index == 1
            )
        }
    }
}
