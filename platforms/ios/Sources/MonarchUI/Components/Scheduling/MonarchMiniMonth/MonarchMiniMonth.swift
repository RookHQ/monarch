import SwiftUI

/// A month calendar for choosing a day, with an earliest and latest day,
/// days the app marks unavailable, and dots under days that have events.
///
/// ```swift
/// MonarchMiniMonth(
///     selection: $day,
///     minimumDate: .now,
///     isDateDisabled: { calendar.isDateInWeekend($0) },
///     dayInfo: { events[$0].map { MonarchMiniMonthDayInfo(dotCount: $0.count, description: "\($0.count) events") } }
/// )
/// ```
///
/// The grid is drawn by Monarch as the web draws it: the month title with a chevron opens the list of months,
/// the arrows move a month, and the names, numerals and first day of the week follow the calendar and locale
/// of the environment. The selected day is the theme's accent, today is outlined, and days with events have dots.
///
/// Dates passed to the closures are the first moment of a day in the
/// environment's calendar. `selection` is set to such a date, or to `nil`
/// when the selected day is tapped again.
///
/// Accessibility: VoiceOver, Dynamic Type and right-to-left layout are the
/// system calendar view's. Dots are decorative: give each day a
/// `description`, and show the selected day's events as text under the
/// calendar.
public struct MonarchMiniMonth: View {
    @Binding private var selection: Date?
    private let visibleMonth: Binding<Date>?
    private let minimumDate: Date?
    private let maximumDate: Date?
    private let today: Date?
    private let isDateDisabled: (@MainActor (Date) -> Bool)?
    private let dayInfo: (@MainActor (Date) -> MonarchMiniMonthDayInfo?)?

    /// A month calendar.
    ///
    /// - Parameters:
    ///   - selection: The selected day, or `nil` for none.
    ///   - visibleMonth: Any date in the month on screen. The calendar
    ///     writes the first day of the month the person moves to. Without
    ///     it the calendar starts on the month of the selection, or today.
    ///   - minimumDate: The earliest day that can be chosen. Earlier months
    ///     cannot be reached.
    ///   - maximumDate: The latest day that can be chosen. Later months
    ///     cannot be reached.
    ///   - today: The day drawn as today. The clock by default; fixes it for tests and previews.
    ///   - isDateDisabled: Returns `true` for a day that cannot be chosen,
    ///     such as a weekend or a booked day. Called for every visible day,
    ///     so keep it cheap.
    ///   - dayInfo: The dots and their description for a day, or `nil` for
    ///     none. Called for every visible day, so keep it cheap.
    public init(
        selection: Binding<Date?>,
        visibleMonth: Binding<Date>? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        today: Date? = nil,
        isDateDisabled: (@MainActor (Date) -> Bool)? = nil,
        dayInfo: (@MainActor (Date) -> MonarchMiniMonthDayInfo?)? = nil
    ) {
        _selection = selection
        self.visibleMonth = visibleMonth
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.today = today
        self.isDateDisabled = isDateDisabled
        self.dayInfo = dayInfo
    }

    /// The content of the view.
    public var body: some View {
        #if os(iOS)
            MonarchMiniMonthGrid(
                selection: $selection,
                visibleMonth: visibleMonth,
                minimumDate: minimumDate,
                maximumDate: maximumDate,
                today: today,
                isDateDisabled: isDateDisabled,
                dayInfo: dayInfo
            )
        #else
            MonarchMiniMonthHostStandIn(selection: $selection, minimumDate: minimumDate, maximumDate: maximumDate)
        #endif
    }
}

#if !os(iOS)
    /// macOS is only a build convenience and has no `UICalendarView`: the
    /// host build shows the graphical date picker, with the bounds and
    /// without disabled days and dots, so the package builds and its tests
    /// run on a Mac.
    private struct MonarchMiniMonthHostStandIn: View {
        @Binding var selection: Date?
        let minimumDate: Date?
        let maximumDate: Date?

        @Environment(\.calendar) private var calendar
        @Environment(\.monarchNow) private var clockNow

        var body: some View {
            let range = MonarchMiniMonthTokens.availableRange(
                minimumDate: minimumDate,
                maximumDate: maximumDate,
                calendar: calendar
            )
            DatePicker(
                selection: Binding(
                    get: { min(max(selection ?? clockNow ?? .now, range.start), range.end) },
                    set: { selection = MonarchMiniMonthTokens.day($0, calendar: calendar) }
                ),
                in: range.start...range.end,
                displayedComponents: .date
            ) {
                EmptyView()
            }
            .datePickerStyle(.graphical)
            .labelsHidden()
        }
    }
#endif

#if DEBUG
    #Preview("MonarchMiniMonth") {
        MonarchPreviewMatrix {
            MonarchMiniMonth(
                selection: .constant(.now),
                minimumDate: Calendar.current.date(byAdding: .month, value: -1, to: .now),
                isDateDisabled: { Calendar.current.isDateInWeekend($0) },
                dayInfo: { date in
                    switch Calendar.current.component(.day, from: date) % 5 {
                    case 0: MonarchMiniMonthDayInfo(dotCount: 2, description: "2 events")
                    case 1: MonarchMiniMonthDayInfo(dots: [.success], description: "Available")
                    case 2: MonarchMiniMonthDayInfo(dots: [.danger, .warning, .info, .neutral], description: "Busy")
                    default: nil
                    }
                }
            )
        }
    }
#endif
