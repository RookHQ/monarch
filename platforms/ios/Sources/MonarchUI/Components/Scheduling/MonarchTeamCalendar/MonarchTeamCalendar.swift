import MonarchTokens
import SwiftUI

/// Who is away when: people down the leading side, the days of a month or a
/// custom range across, absences as bars that span their days (half days as
/// half bars, pending ones faded), weekends and holidays shaded and today
/// ringed.
///
/// ```swift
/// MonarchTeamCalendar(
///     members: [MonarchTeamCalendarMember("Alex Kim", id: "alex", description: "Design")],
///     entries: [MonarchTeamCalendarEntry("Annual leave", id: "1", memberID: "alex", start: monday, end: thursday)],
///     month: november,
///     onEntryTap: { entry, _ in open(entry) },
///     onCellTap: { member, day in addTimeAway(member, day) }
/// )
/// ```
///
/// SwiftUI has no grid of people by days, so Monarch builds one from system
/// scroll views: the rows scroll vertically with the day header pinned at
/// the top, and the days of every row scroll sideways together in one
/// horizontal `ScrollView` while the people column stays put. The days start
/// scrolled to the day before today when today is shown. Like a `List`, the
/// calendar fills the space it is given; give it a height and do not put it
/// inside another vertical scroll view. Data in, taps out: put a
/// ``MonarchCalendarToolbar`` above it to move between months.
///
/// Accessibility: the people column's header is a heading named for the grid
/// ("Team calendar, November 2026"). Each person is one heading element, so
/// the headings rotor moves from person to person. Each day header is read
/// as its full date, with "Today" and the holiday's name. Each bar is one
/// button read as "Annual leave, Alex Kim, Monday, November 2 to Thursday,
/// November 5", with the half and "pending" when they apply. With
/// `onCellTap` each empty day is a button read as "Sam Lee, Wednesday,
/// November 4". Days are at least 44 points wide and rows 44 points tall,
/// and both grow with Dynamic Type. Half days, pending entries, today and
/// holidays are drawn with shape (half bar, outline, ring, shading) and said
/// in words, not shown by colour alone.
@_spi(Experimental) public struct MonarchTeamCalendar: View {
    private let members: [MonarchTeamCalendarMember]
    private let entries: [MonarchTeamCalendarEntry]
    private let month: Date?
    private let from: Date?
    private let to: Date?
    private let holidays: [MonarchTeamCalendarHoliday]
    private let weekendDays: Set<Int>?
    private let today: Date
    private let calendar: Calendar
    private let isLoading: Bool
    private let errorMessage: String?
    private let showsLegend: Bool
    private let labels: MonarchTeamCalendarLabels
    private let onEntryTap: (@MainActor (MonarchTeamCalendarEntry, MonarchTeamCalendarMember) -> Void)?
    private let onCellTap: (@MainActor (MonarchTeamCalendarMember, Date) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var cellSize = MonarchTeamCalendarTokens.cellSize
    @ScaledMetric(relativeTo: .body) private var personWidth = MonarchTeamCalendarTokens.personWidth
    @ScaledMetric(relativeTo: .body) private var markSize = MonarchTeamCalendarTokens.markSize
    @State private var width: CGFloat = 0
    @State private var cornerHeight: CGFloat = 0
    @State private var dayHeaderHeight: CGFloat = 0
    @State private var rowHeights: [String: CGFloat] = [:]

    private typealias Tokens = MonarchTeamCalendarTokens

    /// A team calendar.
    ///
    /// - Parameters:
    ///   - members: The rows, in display order.
    ///   - entries: The absences. Days outside the range are left out, and an entry for an
    ///     unknown member is ignored.
    ///   - month: Any day of the month shown. `nil` is the month of `today`. Ignored when
    ///     `from` and `to` are both set.
    ///   - from: The first day of a custom range, used with `to`.
    ///   - to: The last day of a custom range, inclusive. At most 366 days are shown.
    ///   - holidays: Non-working days for everyone, shaded and named.
    ///   - weekendDays: The shaded days of the week as `Calendar` weekday numbers, 1 for Sunday
    ///     to 7 for Saturday. `nil` uses the calendar's own weekend.
    ///   - today: Today's date, ringed and where the days start scrolled to. Pass a fixed date
    ///     in tests and previews.
    ///   - calendar: The calendar and time zone days are read in.
    ///   - isLoading: Shows a placeholder instead of the grid.
    ///   - errorMessage: Shows this message in a danger banner instead of the grid.
    ///   - showsLegend: Shows the line under the grid that explains shading, half bars and
    ///     faded bars.
    ///   - labels: Replacements for the built-in strings.
    ///   - onEntryTap: Called with the entry and its member when a bar is tapped. Without it a
    ///     tap on a bar shows its details.
    ///   - onCellTap: Called with the member and the start of the day when an empty day is
    ///     tapped, for example to add time away. Without it empty days take no taps.
    public init(
        members: [MonarchTeamCalendarMember],
        entries: [MonarchTeamCalendarEntry],
        month: Date? = nil,
        from: Date? = nil,
        to: Date? = nil,
        holidays: [MonarchTeamCalendarHoliday] = [],
        weekendDays: Set<Int>? = nil,
        today: Date = Date(),
        calendar: Calendar = .autoupdatingCurrent,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        showsLegend: Bool = true,
        labels: MonarchTeamCalendarLabels = MonarchTeamCalendarLabels(),
        onEntryTap: (@MainActor (MonarchTeamCalendarEntry, MonarchTeamCalendarMember) -> Void)? = nil,
        onCellTap: (@MainActor (MonarchTeamCalendarMember, Date) -> Void)? = nil
    ) {
        self.members = members
        self.entries = entries
        self.month = month
        self.from = from
        self.to = to
        self.holidays = holidays
        self.weekendDays = weekendDays
        self.today = today
        self.calendar = calendar
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.showsLegend = showsLegend
        self.labels = labels
        self.onEntryTap = onEntryTap
        self.onCellTap = onCellTap
    }

    /// The content of the view.
    public var body: some View {
        switch Tokens.phase(isLoading: isLoading, errorMessage: errorMessage, memberCount: members.count) {
        case .loading:
            MonarchLoadingState(Tokens.loadingLabel(labels), variant: .rows)
        case .failed(let message):
            MonarchBanner(message: message, tone: .danger)
                .onAppear { AccessibilityNotification.Announcement(message).post() }
        case .empty:
            MonarchEmptyState(
                Tokens.emptyTitle(labels),
                message: Tokens.emptyMessage(labels),
                systemImage: Tokens.emptySymbol
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .grid:
            grid(model)
        }
    }

    private var formats: MonarchTeamCalendarFormats {
        MonarchTeamCalendarFormats(locale: locale, calendar: calendar)
    }

    private var model: MonarchTeamCalendarModel {
        MonarchTeamCalendarModel.make(
            members: members,
            entries: entries,
            month: month,
            from: from,
            to: to,
            holidays: holidays,
            weekendDays: weekendDays,
            today: today,
            formats: formats
        )
    }

    /// The people column's width in the calendar.
    private var personColumn: CGFloat { Tokens.personColumnWidth(scaled: personWidth, available: width) }

    /// The width of a day: the touch target, narrowed so that the days fit beside the people column (as the
    /// web's do) down to the smallest readable width, below which the days scroll sideways.
    private func dayWidth(_ model: MonarchTeamCalendarModel) -> CGFloat {
        Tokens.dayWidth(
            scaled: cellSize,
            dayCount: model.days.count,
            available: width - personColumn - 4 * MonarchShapes.borderWidth
        )
    }

    private var headerHeight: CGFloat { max(cellSize, cornerHeight, dayHeaderHeight) }

    private func rowHeight(_ member: MonarchTeamCalendarMember) -> CGFloat {
        max(cellSize, rowHeights[member.id] ?? 0)
    }

    // MARK: - Grid

    private func grid(_ model: MonarchTeamCalendarModel) -> some View {
        VStack(alignment: .leading, spacing: Tokens.legendSpacing) {
            ScrollView(.vertical) {
                HStack(alignment: .top, spacing: 0) {
                    people(model)
                        .frame(width: personColumn)
                        .overlay(alignment: .trailing) {
                            Rectangle()
                                .fill(theme.colors[role: Tokens.borderColor])
                                .frame(width: MonarchShapes.borderWidth)
                        }
                    ScrollViewReader { proxy in
                        ScrollView(.horizontal) {
                            days(model)
                        }
                        .onAppear { scroll(proxy, to: openingTarget(model)) }
                        .onChange(of: model.days.first) { scroll(proxy, to: openingTarget(model)) }
                    }
                }
            }
            .coordinateSpace(.named(Tokens.coordinateSpace))
            .background(theme.colors[role: Tokens.surfaceColor])
            .clipShape(MonarchShapes.container)
            .overlay {
                MonarchShapes.container.strokeBorder(
                    theme.colors[role: Tokens.borderColor],
                    lineWidth: MonarchShapes.borderWidth
                )
            }
            if let legend = Tokens.legend(labels, showsLegend: showsLegend) {
                Text(legend)
                    .monarchTextStyle(Tokens.captionStyle)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryText])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { width = $0 })
    }

    /// Where the days open: at the first day when today is among the days that fit on screen, else at the day
    /// before today.
    private func openingTarget(_ model: MonarchTeamCalendarModel) -> Int? {
        guard let target = model.scrollTarget else { return nil }
        let visibleDays = Int((max(width - personColumn, 0) / dayWidth(model)).rounded(.down))
        return target + 1 < visibleDays ? nil : target
    }

    private func scroll(_ proxy: ScrollViewProxy, to target: Int?) {
        guard let target else { return }
        proxy.scrollTo(Self.dayID(target), anchor: .leading)
    }

    private static func dayID(_ index: Int) -> String {
        "monarch.teamCalendar.day.\(index)"
    }

    /// Keeps a header at the top of the vertical scroll while the rows move under it.
    private func pinned<Content: View>(_ content: Content) -> some View {
        content
            .zIndex(1)
            .visualEffect { effect, proxy in
                effect.offset(y: max(0, -proxy.frame(in: .named(MonarchTeamCalendarTokens.coordinateSpace)).minY))
            }
    }

    // MARK: - People column

    private func people(_ model: MonarchTeamCalendarModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            pinned(
                VStack(spacing: 0) {
                    corner(model).frame(height: headerHeight)
                    divider(Tokens.borderColor)
                }
                .background(theme.colors[role: Tokens.surfaceColor])
            )
            ForEach(members) { member in
                person(member)
                divider(Tokens.dividerColor)
            }
        }
    }

    private func corner(_ model: MonarchTeamCalendarModel) -> some View {
        Text(Tokens.personHeader(labels))
            .monarchTextStyle(Tokens.captionStyle, weight: Tokens.headerWeight)
            .foregroundStyle(theme.colors[role: Tokens.secondaryText])
            .padding(.horizontal, Tokens.personHorizontalPadding)
            .padding(.vertical, Tokens.headerVerticalPadding)
            .onGeometryChange(for: CGFloat.self, of: { $0.size.height }, action: { cornerHeight = $0 })
            .frame(maxWidth: .infinity, alignment: .leading)
            // The header names the whole grid for VoiceOver, as the caption of the web table does.
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.gridName(model.rangeName, labels: labels))
            .accessibilityAddTraits(.isHeader)
    }

    private func person(_ member: MonarchTeamCalendarMember) -> some View {
        HStack(spacing: Tokens.personSpacing) {
            avatar(member)
            VStack(alignment: .leading, spacing: 0) {
                Text(member.name)
                    .monarchTextStyle(Tokens.labelStyle)
                    .foregroundStyle(theme.colors[role: Tokens.primaryText])
                if let description = member.description {
                    Text(description)
                        .monarchTextStyle(Tokens.captionStyle)
                        .foregroundStyle(theme.colors[role: Tokens.secondaryText])
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, Tokens.personHorizontalPadding)
        .padding(.vertical, Tokens.personVerticalPadding)
        .frame(maxWidth: .infinity, minHeight: cellSize, alignment: .leading)
        .onGeometryChange(for: CGFloat.self, of: { $0.size.height }, action: { rowHeights[member.id] = $0 })
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.personLabel(name: member.name, description: member.description))
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private func avatar(_ member: MonarchTeamCalendarMember) -> some View {
        if let image = member.image {
            MonarchAvatar(image: image, name: member.name, seed: member.id, size: Tokens.avatarSize, isDecorative: true)
        } else {
            MonarchAvatar(
                url: member.url,
                name: member.name,
                seed: member.id,
                size: Tokens.avatarSize,
                isDecorative: true
            )
        }
    }

    private func divider(_ role: MonarchColorRole) -> some View {
        Rectangle()
            .fill(theme.colors[role: role])
            .frame(height: MonarchShapes.borderWidth)
            .accessibilityHidden(true)
    }

    // MARK: - Days

    private func days(_ model: MonarchTeamCalendarModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            pinned(
                VStack(spacing: 0) {
                    dayHeaders(model).frame(height: headerHeight)
                    divider(Tokens.borderColor)
                }
            )
            VStack(alignment: .leading, spacing: 0) {
                ForEach(members) { member in
                    row(member, model: model).frame(height: rowHeight(member))
                    divider(Tokens.dividerColor)
                }
            }
            .background(alignment: .topLeading) { columns(model) }
        }
        .frame(width: dayWidth(model) * CGFloat(model.days.count), alignment: .leading)
    }

    private func dayHeaders(_ model: MonarchTeamCalendarModel) -> some View {
        HStack(spacing: 0) {
            ForEach(Array(model.days.enumerated()), id: \.offset) { index, day in
                dayHeader(index, day: day, model: model)
            }
        }
    }

    private func dayHeader(_ index: Int, day: Date, model: MonarchTeamCalendarModel) -> some View {
        let isToday = index == model.todayIndex
        let holiday: String? = model.holidays[day]
        let spoken = Tokens.dayHeaderLabel(date: formats.day(day), isToday: isToday, holiday: holiday, labels: labels)
        var measure: (@MainActor (CGFloat) -> Void)?
        if index == 0 {
            measure = { height in dayHeaderHeight = height }
        }
        return MonarchTeamCalendarDayHeader(
            number: formats.number(day),
            weekday: formats.narrowWeekday(day),
            isToday: isToday,
            isShaded: model.shaded[index],
            holiday: holiday.map { Tokens.holidayText($0, labels: labels) },
            spokenLabel: spoken,
            cellSize: dayWidth(model),
            markSize: min(markSize, dayWidth(model)),
            onContentHeight: measure
        )
        .id(Self.dayID(index))
    }

    /// The shading of weekend and holiday columns, behind every row.
    private func columns(_ model: MonarchTeamCalendarModel) -> some View {
        HStack(spacing: 0) {
            ForEach(Array(model.shaded.enumerated()), id: \.offset) { _, isShaded in
                Rectangle()
                    .fill(Tokens.columnColor(isShaded: isShaded).map { theme.colors[role: $0] } ?? .clear)
                    .frame(width: dayWidth(model))
            }
        }
        .accessibilityHidden(true)
    }

    private func row(_ member: MonarchTeamCalendarMember, model: MonarchTeamCalendarModel) -> some View {
        let cells = model.cells[member.id] ?? []
        return ZStack(alignment: .leading) {
            if let onCellTap {
                HStack(spacing: 0) {
                    ForEach(Array(model.days.enumerated()), id: \.offset) { index, day in
                        if cells.indices.contains(index), cells[index] == nil {
                            Button {
                                onCellTap(member, day)
                            } label: {
                                Color.clear.contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .frame(width: dayWidth(model))
                            .accessibilityLabel(Tokens.cellLabel(member: member.name, date: formats.day(day)))
                        } else {
                            Color.clear.frame(width: dayWidth(model))
                        }
                    }
                }
            }
            ForEach((model.segments[member.id] ?? []).flatMap(Tokens.perDay)) { segment in
                bar(segment, member: member, model: model)
                    .frame(width: dayWidth(model) * CGFloat(segment.count))
                    .padding(.leading, dayWidth(model) * CGFloat(segment.first))
            }
        }
        .frame(width: dayWidth(model) * CGFloat(model.days.count), alignment: .leading)
    }

    private func bar(
        _ segment: MonarchTeamCalendarSegment,
        member: MonarchTeamCalendarMember,
        model: MonarchTeamCalendarModel
    ) -> some View {
        let first = formats.day(model.days[segment.first])
        let last = formats.day(model.days[segment.first + segment.count - 1])
        let dates = Tokens.dates(first: first, last: last, count: segment.count, labels: labels)
        var action: (@MainActor () -> Void)?
        if let onEntryTap {
            action = { onEntryTap(segment.entry, member) }
        }
        return MonarchTeamCalendarBar(
            segment: segment,
            cellSize: dayWidth(model),
            spokenLabel: Tokens.barLabel(segment, member: member.name, dates: dates, labels: labels),
            detail: Tokens.spoken(
                member.name,
                dates,
                Tokens.portionText(segment.portion, labels: labels),
                segment.entry.isPending ? (labels.pending ?? MonarchTeamCalendarStrings.pending) : nil
            ),
            action: action
        )
    }
}

#if DEBUG
    #Preview("MonarchTeamCalendar") {
        let calendar = Calendar(identifier: .gregorian)
        let november = calendar.date(from: DateComponents(year: 2026, month: 11, day: 1)) ?? Date()
        let day: (Int) -> Date = { calendar.date(byAdding: .day, value: $0 - 1, to: november) ?? november }
        MonarchPreviewMatrix {
            MonarchTeamCalendar(
                members: [
                    MonarchTeamCalendarMember("Alex Kim", id: "alex", description: "Design"),
                    MonarchTeamCalendarMember("Sam Lee", id: "sam", description: "Engineering"),
                    MonarchTeamCalendarMember("Priya Nair", id: "priya"),
                ],
                entries: [
                    MonarchTeamCalendarEntry("Annual leave", id: "1", memberID: "alex", start: day(2), end: day(5)),
                    MonarchTeamCalendarEntry(
                        "Doctor",
                        id: "2",
                        memberID: "sam",
                        start: day(3),
                        portion: .firstHalf,
                        isPending: true
                    ),
                    MonarchTeamCalendarEntry("Conference", id: "3", memberID: "priya", start: day(9), end: day(11)),
                ],
                month: november,
                holidays: [MonarchTeamCalendarHoliday("Thanksgiving", date: day(26))],
                today: day(3),
                calendar: calendar,
                onCellTap: { _, _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
            MonarchTeamCalendar(members: [], entries: [], calendar: calendar)
            MonarchTeamCalendar(members: [], entries: [], calendar: calendar, isLoading: true)
            MonarchTeamCalendar(members: [], entries: [], calendar: calendar, errorMessage: "Could not load time away.")
        }
    }
#endif
