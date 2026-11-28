import MonarchTokens
import SwiftUI

/// Who works which shift on which day: a shift, a day off or nothing for
/// each person and day of a range, with each person's hours and the number
/// of people working per day.
///
/// ```swift
/// MonarchShiftRoster(
///     members: [MonarchShiftRosterMember(id: "a", name: "Alex Kim", description: "Support")],
///     shifts: [MonarchShiftRosterShift(id: "m", name: "Morning", start: 6 * 60, end: 14 * 60, shortName: "M")],
///     from: monday,
///     value: $roster,
///     onChange: { change in save(change) }
/// )
/// ```
///
/// SwiftUI has no roster, so Monarch composes one from ``MonarchTabs``,
/// ``MonarchEventChip``, ``MonarchAvatar`` and system menus. A grid of people
/// by days does not fit a phone, so the `automatic` layout shows the grid
/// only when every day column fits the width and the text is no larger than
/// xxLarge (an iPad or a wide window), and otherwise one day at a time: a
/// ``MonarchTabs`` strip of the days, opened on today, then a row per person.
/// Editable, each cell is a system `Menu` with the shifts and Off (the
/// current one checked) and Clear. Disable the cells with `.disabled(true)`,
/// for example while saving.
///
/// The roster does not scroll vertically; place it in a scrolling screen.
/// Days are matched in `calendar`, so keys in `value` may be any time on the
/// day; a change replaces any key on that day with the start of the day.
///
/// Accessibility: each cell is one element named "<person>, <day>: <assignment>",
/// for example "Alex Kim, Wednesday, November 4, Today: Morning, 06:00 to
/// 14:00", with a 44-point touch target; an overnight shift adds "overnight".
/// Read only, it has no action. Each person is one element, "Alex Kim,
/// Support, Hours: 40 h", and a button with `onSelectMember`. The day's
/// date and grid day headers are headers, and after an assignment the new
/// number working is announced. Avatars are decorative, and every state is
/// written as well as coloured.
@_spi(Experimental) public struct MonarchShiftRoster<Empty: View>: View {
    private let members: [MonarchShiftRosterMember]
    private let shifts: [MonarchShiftRosterShift]
    private let from: Date
    @Binding private var value: [String: [Date: MonarchShiftRosterAssignment]]
    private let days: Int
    private let layout: MonarchShiftRosterLayout
    private let isReadOnly: Bool
    private let isLoading: Bool
    private let showsTotals: Bool
    private let today: Date?
    private let calendar: Calendar
    private let uses24HourClock: Bool?
    private let error: String?
    private let labels: MonarchShiftRosterLabels
    private let onChange: (@MainActor (MonarchShiftRosterChange) -> Void)?
    private let onSelectMember: (@MainActor (MonarchShiftRosterMember) -> Void)?
    private let empty: Empty

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var chosenDay: Date?
    @State private var chosenMember: String?

    /// A shift roster with the app's own empty state.
    ///
    /// - Parameters:
    ///   - members: The people, in display order.
    ///   - shifts: The shifts that can be assigned, in menu order.
    ///   - from: The first day shown: any time on that day in `calendar`.
    ///   - value: The assignments by member id, then by any time on the day. A missing day is
    ///     not assigned.
    ///   - days: How many days to show from `from`, 1 to 62.
    ///   - layout: The grid, one day at a time, one person at a time, or whichever fits.
    ///   - isReadOnly: Shows the assignments without menus.
    ///   - isLoading: Shows a placeholder, read as "Loading roster", instead of the roster.
    ///   - showsTotals: Shows each person's hours and the number of people working per day.
    ///   - today: The day marked Today, or `nil` for no marker. The day layout opens on it
    ///     when it is in the range.
    ///   - calendar: The calendar and time zone days are read in.
    ///   - uses24HourClock: Shows shift times on a 24-hour clock (`true`) or a 12-hour clock
    ///     (`false`). `nil` follows the locale.
    ///   - error: A message shown in a danger ``MonarchBanner`` instead of the roster.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Called after `value` changes, with the one change, so the app can save it.
    ///   - onSelectMember: Makes each person a button, for example to open their schedule.
    ///   - empty: Replaces the built-in empty state shown when `members` is empty.
    public init(
        members: [MonarchShiftRosterMember],
        shifts: [MonarchShiftRosterShift],
        from: Date,
        value: Binding<[String: [Date: MonarchShiftRosterAssignment]]>,
        days: Int = 7,
        layout: MonarchShiftRosterLayout = .automatic,
        isReadOnly: Bool = false,
        isLoading: Bool = false,
        showsTotals: Bool = true,
        today: Date? = Date(),
        calendar: Calendar = .autoupdatingCurrent,
        uses24HourClock: Bool? = nil,
        error: String? = nil,
        labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
        onChange: (@MainActor (MonarchShiftRosterChange) -> Void)? = nil,
        onSelectMember: (@MainActor (MonarchShiftRosterMember) -> Void)? = nil,
        @ViewBuilder empty: () -> Empty
    ) {
        self.members = members
        self.shifts = shifts
        self.from = from
        self._value = value
        self.days = days
        self.layout = layout
        self.isReadOnly = isReadOnly
        self.isLoading = isLoading
        self.showsTotals = showsTotals
        self.today = today
        self.calendar = calendar
        self.uses24HourClock = uses24HourClock
        self.error = error
        self.labels = labels
        self.onChange = onChange
        self.onSelectMember = onSelectMember
        self.empty = empty()
    }

    /// The content of the view.
    public var body: some View {
        let text = MonarchShiftRosterText(labels: labels)
        Group {
            if isLoading {
                placeholder(text)
            } else if let error {
                MonarchBanner(message: error, tone: .danger)
            } else if members.isEmpty {
                if Empty.self == EmptyView.self {
                    MonarchEmptyState(
                        text.emptyTitle,
                        message: text.emptyDescription,
                        systemImage: MonarchShiftRosterTokens.emptySymbol,
                        size: .sm
                    )
                } else {
                    empty
                }
            } else {
                content(model(text))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func model(_ text: MonarchShiftRosterText) -> MonarchShiftRosterModel {
        MonarchShiftRosterModel(
            members: members,
            shifts: shifts,
            range: MonarchShiftRosterTokens.range(from: from, days: days, calendar: calendar),
            value: $value,
            isReadOnly: isReadOnly,
            showsTotals: showsTotals,
            today: today,
            calendar: calendar,
            text: text,
            formats: MonarchShiftRosterFormats(locale: locale, calendar: calendar, uses24HourClock: uses24HourClock),
            onChange: onChange,
            onSelectMember: onSelectMember
        )
    }

    @ViewBuilder private func content(_ model: MonarchShiftRosterModel) -> some View {
        switch layout {
        case .automatic:
            if MonarchShiftRosterTokens.allowsGrid(dynamicTypeSize) {
                ViewThatFits(in: .horizontal) {
                    MonarchShiftRosterGrid(model: model)
                    byDay(model)
                }
            } else {
                byDay(model)
            }
        case .grid:
            ScrollView(.horizontal) {
                MonarchShiftRosterGrid(model: model)
            }
        case .byDay:
            byDay(model)
        case .byPerson:
            byPerson(model)
        }
    }

    // MARK: One day at a time

    @ViewBuilder private func byDay(_ model: MonarchShiftRosterModel) -> some View {
        if let day = MonarchShiftRosterTokens.shownDay(
            chosen: chosenDay,
            range: model.range,
            today: today,
            calendar: calendar
        ) {
            VStack(alignment: .leading, spacing: MonarchShiftRosterTokens.sectionSpacing) {
                MonarchTabs(
                    model.text.day,
                    selection: Binding(get: { day }, set: { chosenDay = $0 }),
                    items: model.range.map { MonarchTabsItem(model.tabTitle($0), value: $0) }
                )
                MonarchShiftRosterDaySummary(model: model, day: day)
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(model.members.enumerated()), id: \.element.id) { index, member in
                        if index > 0 { MonarchDivider() }
                        MonarchShiftRosterRow {
                            MonarchShiftRosterPerson(model: model, member: member)
                        } cell: {
                            MonarchShiftRosterCell(model: model, member: member, day: day, isCompact: false)
                        }
                    }
                }
            }
        }
    }

    // MARK: One person at a time

    @ViewBuilder private func byPerson(_ model: MonarchShiftRosterModel) -> some View {
        if let shown = MonarchShiftRosterTokens.shownMember(chosen: chosenMember, members: model.members),
            let member = model.members.first(where: { $0.id == shown })
        {
            VStack(alignment: .leading, spacing: MonarchShiftRosterTokens.sectionSpacing) {
                MonarchTabs(
                    model.text.person,
                    selection: Binding(get: { shown }, set: { chosenMember = $0 }),
                    items: model.members.map { MonarchTabsItem($0.name, value: $0.id) }
                )
                MonarchShiftRosterPerson(model: model, member: member)
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(model.range.enumerated()), id: \.element) { index, day in
                        if index > 0 { MonarchDivider() }
                        MonarchShiftRosterRow {
                            MonarchShiftRosterDayLabel(model: model, day: day)
                        } cell: {
                            MonarchShiftRosterCell(model: model, member: member, day: day, isCompact: false)
                        }
                    }
                }
            }
        }
    }

    // MARK: Loading

    private func placeholder(_ text: MonarchShiftRosterText) -> some View {
        VStack(alignment: .leading, spacing: MonarchShiftRosterTokens.sectionSpacing) {
            ForEach(0..<MonarchShiftRosterTokens.placeholderRows, id: \.self) { _ in
                HStack(spacing: MonarchShiftRosterTokens.rowSpacing) {
                    Text(text.roster)
                        .monarchTextStyle(MonarchShiftRosterTokens.nameStyle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(text.unassigned)
                        .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                        .frame(maxWidth: .infinity, minHeight: MonarchShiftRosterTokens.slotMinHeight)
                }
            }
        }
        .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(text.loading)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

extension MonarchShiftRoster where Empty == EmptyView {
    /// A shift roster.
    ///
    /// - Parameters:
    ///   - members: The people, in display order.
    ///   - shifts: The shifts that can be assigned, in menu order.
    ///   - from: The first day shown: any time on that day in `calendar`.
    ///   - value: The assignments by member id, then by any time on the day. A missing day is
    ///     not assigned.
    ///   - days: How many days to show from `from`, 1 to 62.
    ///   - layout: The grid, one day at a time, one person at a time, or whichever fits.
    ///   - isReadOnly: Shows the assignments without menus.
    ///   - isLoading: Shows a placeholder, read as "Loading roster", instead of the roster.
    ///   - showsTotals: Shows each person's hours and the number of people working per day.
    ///   - today: The day marked Today, or `nil` for no marker.
    ///   - calendar: The calendar and time zone days are read in.
    ///   - uses24HourClock: Shows shift times on a 24-hour clock (`true`) or a 12-hour clock
    ///     (`false`). `nil` follows the locale.
    ///   - error: A message shown in a danger ``MonarchBanner`` instead of the roster.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Called after `value` changes, with the one change, so the app can save it.
    ///   - onSelectMember: Makes each person a button, for example to open their schedule.
    public init(
        members: [MonarchShiftRosterMember],
        shifts: [MonarchShiftRosterShift],
        from: Date,
        value: Binding<[String: [Date: MonarchShiftRosterAssignment]]>,
        days: Int = 7,
        layout: MonarchShiftRosterLayout = .automatic,
        isReadOnly: Bool = false,
        isLoading: Bool = false,
        showsTotals: Bool = true,
        today: Date? = Date(),
        calendar: Calendar = .autoupdatingCurrent,
        uses24HourClock: Bool? = nil,
        error: String? = nil,
        labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
        onChange: (@MainActor (MonarchShiftRosterChange) -> Void)? = nil,
        onSelectMember: (@MainActor (MonarchShiftRosterMember) -> Void)? = nil
    ) {
        self.init(
            members: members,
            shifts: shifts,
            from: from,
            value: value,
            days: days,
            layout: layout,
            isReadOnly: isReadOnly,
            isLoading: isLoading,
            showsTotals: showsTotals,
            today: today,
            calendar: calendar,
            uses24HourClock: uses24HourClock,
            error: error,
            labels: labels,
            onChange: onChange,
            onSelectMember: onSelectMember
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchShiftRoster") {
        let monday = Calendar.current.startOfDay(for: Date())
        let shifts = [
            MonarchShiftRosterShift(id: "m", name: "Morning", start: 6 * 60, end: 14 * 60, shortName: "M"),
            MonarchShiftRosterShift(id: "n", name: "Night", start: 22 * 60, end: 6 * 60, shortName: "N"),
        ]
        let members = [
            MonarchShiftRosterMember(id: "a", name: "Alex Kim", description: "Support"),
            MonarchShiftRosterMember(id: "b", name: "Sam Lee"),
        ]
        let value: Binding<[String: [Date: MonarchShiftRosterAssignment]]> = .constant([
            "a": [monday: .shift("m")], "b": [monday: .off],
        ])
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchShiftRoster(members: members, shifts: shifts, from: monday, value: value, layout: .byDay)
                MonarchShiftRoster(
                    members: members,
                    shifts: shifts,
                    from: monday,
                    value: value,
                    days: 3,
                    layout: .byPerson,
                    isReadOnly: true
                )
                MonarchShiftRoster(members: members, shifts: shifts, from: monday, value: value, layout: .grid)
                MonarchShiftRoster(members: [], shifts: shifts, from: monday, value: value)
                MonarchShiftRoster(members: members, shifts: shifts, from: monday, value: value, isLoading: true)
                MonarchShiftRoster(
                    members: members,
                    shifts: shifts,
                    from: monday,
                    value: value,
                    error: "Could not load"
                )
            }
        }
    }
#endif
