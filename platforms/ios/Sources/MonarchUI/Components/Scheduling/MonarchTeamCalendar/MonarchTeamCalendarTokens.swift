import Foundation
import MonarchTokens
import SwiftUI

/// What one person has on one day.
struct MonarchTeamCalendarCell: Equatable {
    let entry: MonarchTeamCalendarEntry
    let portion: MonarchTeamCalendarPortion

    /// Whether two days of a row join into one bar: the same entry, both whole days.
    func joins(_ other: MonarchTeamCalendarCell?) -> Bool {
        guard let other else { return false }
        return entry.id == other.entry.id && portion == .full && other.portion == .full
    }
}

/// A run of days of one entry on a row, drawn as one bar.
struct MonarchTeamCalendarSegment: Equatable, Identifiable {
    let entry: MonarchTeamCalendarEntry
    /// The index of the first day in the visible range.
    let first: Int
    /// The number of days.
    let count: Int
    let portion: MonarchTeamCalendarPortion
    /// Whether the entry's first day is in this segment, so the bar has its leading edge.
    let startsEntry: Bool
    /// Whether the entry's last day is in this segment, so the bar has its trailing edge.
    let endsEntry: Bool

    var id: Int { first }
}

/// How a bar is drawn.
struct MonarchTeamCalendarBarAppearance: Equatable {
    /// The theme colour under the tint.
    let base: MonarchColorRole
    /// How much of the entry colour is laid over the base, from 0 to 1.
    let tint: Double
    /// Whether the solid stripe in the entry colour is drawn at the leading edge.
    let hasStripe: Bool
    /// Whether the bar is outlined in the entry colour.
    let isOutlined: Bool
    /// The colour role of the label.
    let content: MonarchColorRole
}

/// The day number in the header: its colour and whether it is ringed.
struct MonarchTeamCalendarHeaderAppearance: Equatable {
    let text: MonarchColorRole
    let ring: MonarchColorRole?
}

/// Component tokens and pure logic for ``MonarchTeamCalendar``.
enum MonarchTeamCalendarTokens {
    /// The width of a day and the smallest height of a row before Dynamic Type: the touch target.
    static let cellSize: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The width of the people column before Dynamic Type.
    static let personWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The largest share of the calendar's width the people column takes at large text sizes.
    static let personMaximumShare: CGFloat = 0.34
    /// The narrowest a day is drawn when the days are fitted to the calendar's width; fewer room scrolls.
    static let minimumDayWidth: CGFloat = MonarchTokens.Space.space4
    /// Padding before and after the people column's content.
    static let personHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below a person.
    static let personVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the avatar and the name.
    static let personSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the day header.
    static let headerVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The smallest side of the day number's ring before Dynamic Type.
    static let markSize: CGFloat = MonarchTokens.Space.space8
    /// The smallest height of a bar.
    static let barMinHeight: CGFloat = MonarchTokens.Space.space6
    /// The side of a day's bar, as on the web.
    static let barSide: CGFloat = MonarchTokens.Space.space5
    /// The gap beside a half-day bar.
    static let barHalfInset: CGFloat = MonarchTokens.Space.space0_5
    /// How much of its colour a waiting bar keeps.
    static let pendingOpacity: Double = 0.5
    /// The border role of a waiting bar.
    static var pendingBorder: MonarchColorRole { \.borderStrong }
    /// The gap between a bar's edge and the edge of its first or last day.
    static let barInset: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after a bar's label.
    static let barPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The width of the stripe at the start of an entry.
    static let stripeWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The corner radius of a bar.
    static let barRadius: CGFloat = MonarchTokens.Radius.xs
    /// The gap between the grid and the legend.
    static let legendSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the detail shown when a bar or a holiday is tapped.
    static let detailPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of that detail.
    static let detailSpacing: CGFloat = MonarchTokens.Space.space1
    /// The share of the entry colour laid over the surface for the fill of a bar: all of it, a solid bar.
    static let barTintRatio: Double = 1
    /// The same share for a bar that waits for approval: faded.
    static let pendingTintRatio: Double = 0.4
    /// The name of the vertical scroll view's coordinate space, which pins the header.
    static let coordinateSpace = "monarch.teamCalendar.grid"
    /// The longest range shown, in days.
    static let maximumDays = 366
    /// The avatar beside a name.
    static let avatarSize: MonarchAvatarSize = .sm
    /// The SF Symbol of the empty state.
    static let emptySymbol = "person.2"
    /// The type role of names and day numbers.
    static let labelStyle: MonarchTextStyle = .label
    /// The type role of descriptions, weekday letters, bar labels and the legend.
    static let captionStyle: MonarchTextStyle = .caption
    /// The weight of today's number.
    static let todayWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.bold)
    /// The weight of the other day numbers and the people column's header.
    static let headerWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The colour of the grid's background.
    static var surfaceColor: MonarchColorRole { \.bgSurface }
    /// The colour of the outer border and of the line under the header.
    static var borderColor: MonarchColorRole { \.borderDefault }
    /// The colour of the lines between rows.
    static var dividerColor: MonarchColorRole { \.borderSubtle }
    /// The colour of names.
    static var primaryText: MonarchColorRole { \.textDefault }
    /// The colour of descriptions, weekday letters, the header and the legend.
    static var secondaryText: MonarchColorRole { \.textMuted }
    /// The colour of a bar when the entry has none.
    static var defaultBarColor: MonarchColorRole { \.textMuted }

    // MARK: - Range

    /// The first and last day shown: `from` to `to` when both are set (a last day before the
    /// first gives one day, and at most ``maximumDays`` days), otherwise the month of `month`,
    /// or of `today` when `month` is `nil`.
    static func range(
        month: Date?,
        from: Date?,
        to: Date?,
        today: Date,
        calendar: Calendar
    ) -> (first: Date, last: Date) {
        if let from, let to {
            let first = calendar.startOfDay(for: from)
            let wanted = max(first, calendar.startOfDay(for: to))
            let cap = calendar.date(byAdding: .day, value: maximumDays - 1, to: first) ?? first
            return (first, min(wanted, cap))
        }
        let reference = month ?? today
        let first = calendar.dateInterval(of: .month, for: reference)?.start ?? calendar.startOfDay(for: reference)
        let next = calendar.date(byAdding: .month, value: 1, to: first) ?? first
        let last = calendar.date(byAdding: .day, value: -1, to: next) ?? first
        return (first, last)
    }

    /// Every day from `first` to `last`, inclusive, at the start of the day.
    static func days(from first: Date, through last: Date, calendar: Calendar) -> [Date] {
        var days: [Date] = []
        var day = calendar.startOfDay(for: first)
        let end = calendar.startOfDay(for: last)
        while day <= end && days.count < maximumDays {
            days.append(day)
            guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
            day = next
        }
        return days
    }

    /// Whether a day is shaded: a weekend day or a holiday. `weekendDays` are
    /// `Calendar` weekday numbers (1 is Sunday); `nil` asks the calendar for its weekend.
    static func isShaded(_ day: Date, weekendDays: Set<Int>?, holidays: [Date: String], calendar: Calendar) -> Bool {
        if holidays[calendar.startOfDay(for: day)] != nil { return true }
        if let weekendDays { return weekendDays.contains(calendar.component(.weekday, from: day)) }
        return calendar.isDateInWeekend(day)
    }

    /// The holidays by the start of their day. A later holiday on the same day replaces an earlier one.
    static func holidayNames(_ holidays: [MonarchTeamCalendarHoliday], calendar: Calendar) -> [Date: String] {
        var names: [Date: String] = [:]
        for holiday in holidays { names[calendar.startOfDay(for: holiday.date)] = holiday.name }
        return names
    }

    /// The index of the day the days start scrolled to: the day before today, or `nil` when
    /// today is not shown or is the first day.
    static func scrollTarget(today: Date, days: [Date], calendar: Calendar) -> Int? {
        guard let index = days.firstIndex(where: { calendar.isDate($0, inSameDayAs: today) }), index > 0 else {
            return nil
        }
        return index - 1
    }

    // MARK: - Entries

    /// The last day of an entry: its end, or its start when the end is missing or earlier.
    static func lastDay(of entry: MonarchTeamCalendarEntry, calendar: Calendar) -> Date {
        let start = calendar.startOfDay(for: entry.start)
        guard let end = entry.end else { return start }
        return max(start, calendar.startOfDay(for: end))
    }

    /// What each day holds for one person, from that person's entries. A later entry replaces
    /// an earlier one on a day both cover.
    static func cells(
        _ entries: [MonarchTeamCalendarEntry],
        days: [Date],
        calendar: Calendar
    ) -> [MonarchTeamCalendarCell?] {
        var cells = [MonarchTeamCalendarCell?](repeating: nil, count: days.count)
        guard let firstDay = days.first, let lastShown = days.last else { return cells }
        var index: [Date: Int] = [:]
        for (offset, day) in days.enumerated() { index[day] = offset }
        for entry in entries {
            let portions = Dictionary(
                entry.portions.map { (calendar.startOfDay(for: $0.key), $0.value) },
                uniquingKeysWith: { _, last in last }
            )
            var day = max(calendar.startOfDay(for: entry.start), firstDay)
            let end = min(lastDay(of: entry, calendar: calendar), lastShown)
            while day <= end {
                if let offset = index[day] {
                    cells[offset] = MonarchTeamCalendarCell(entry: entry, portion: portions[day] ?? entry.portion)
                }
                guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
                day = next
            }
        }
        return cells
    }

    /// The bars of a row: runs of whole days of the same entry join, half days stand alone.
    static func segments(
        _ cells: [MonarchTeamCalendarCell?],
        days: [Date],
        calendar: Calendar
    ) -> [MonarchTeamCalendarSegment] {
        var segments: [MonarchTeamCalendarSegment] = []
        var index = 0
        while index < cells.count {
            guard let cell = cells[index] else {
                index += 1
                continue
            }
            var count = 1
            while index + count < cells.count, cell.joins(cells[index + count]) { count += 1 }
            let lastIndex = index + count - 1
            segments.append(
                MonarchTeamCalendarSegment(
                    entry: cell.entry,
                    first: index,
                    count: count,
                    portion: cell.portion,
                    startsEntry: days[index] == calendar.startOfDay(for: cell.entry.start),
                    endsEntry: days[lastIndex] == lastDay(of: cell.entry, calendar: calendar)
                )
            )
            index += count
        }
        return segments
    }

    /// The entries of one member, in the order the app gave them.
    static func entries(of memberID: String, in entries: [MonarchTeamCalendarEntry]) -> [MonarchTeamCalendarEntry] {
        entries.filter { $0.memberID == memberID }
    }

    // MARK: - Appearance

    /// How a bar is drawn, as on the web: the entry colour solid, with the label in the on-solid text colour;
    /// a pending entry is the same bar faded. Either way the colour is the entry's own.
    static func barAppearance(isPending: Bool) -> MonarchTeamCalendarBarAppearance {
        isPending
            ? MonarchTeamCalendarBarAppearance(
                base: \.bgSurface,
                tint: pendingTintRatio,
                hasStripe: false,
                isOutlined: false,
                content: \.textDefault
            )
            : MonarchTeamCalendarBarAppearance(
                base: \.bgSurface,
                tint: barTintRatio,
                hasStripe: false,
                isOutlined: false,
                content: \.accentOnSolid
            )
    }

    /// The fill of a day's column: the muted fill when shaded, nothing otherwise.
    static func columnColor(isShaded: Bool) -> MonarchColorRole? {
        isShaded ? \.bgMuted : nil
    }

    /// The day number in the header: today in the strong accent inside an accent ring.
    static func headerAppearance(isToday: Bool) -> MonarchTeamCalendarHeaderAppearance {
        isToday
            ? MonarchTeamCalendarHeaderAppearance(text: \.accentStrong, ring: \.accentBorder)
            : MonarchTeamCalendarHeaderAppearance(text: \.textMuted, ring: nil)
    }

    /// The radii of a bar: rounded where the entry starts or ends, and on both sides of a half day.
    static func corners(_ segment: MonarchTeamCalendarSegment) -> (leading: CGFloat, trailing: CGFloat) {
        let isHalf = segment.portion != .full
        return (
            isHalf || segment.startsEntry ? barRadius : 0,
            isHalf || segment.endsEntry ? barRadius : 0
        )
    }

    /// One bar for each day of a segment, as the web draws a square in every day.
    static func perDay(_ segment: MonarchTeamCalendarSegment) -> [MonarchTeamCalendarSegment] {
        (0..<segment.count).map { offset in
            MonarchTeamCalendarSegment(
                entry: segment.entry,
                first: segment.first + offset,
                count: 1,
                portion: segment.portion,
                startsEntry: segment.startsEntry && offset == 0,
                endsEntry: segment.endsEntry && offset == segment.count - 1
            )
        }
    }

    /// Whether a bar shows its label: whole-day bars of two days or more.
    static func showsLabel(_ segment: MonarchTeamCalendarSegment) -> Bool {
        segment.portion == .full && segment.count > 1
    }

    /// The width of the people column: the scaled width, at most a share of the calendar's width.
    static func personColumnWidth(scaled: CGFloat, available: CGFloat) -> CGFloat {
        guard available > 0 else { return scaled }
        return min(scaled, available * personMaximumShare)
    }

    /// The width of a day: the scaled touch target, or narrower so that all `dayCount` days fit in `available`,
    /// but never below the smallest readable width.
    static func dayWidth(scaled: CGFloat, dayCount: Int, available: CGFloat) -> CGFloat {
        guard available > 0, dayCount > 0 else { return scaled }
        return min(scaled, max(minimumDayWidth, available / CGFloat(dayCount)))
    }

    // MARK: - Text

    /// Spoken parts joined with commas, blanks left out.
    static func spoken(_ parts: String?...) -> String {
        parts.compactMap { $0 }.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }.joined(separator: ", ")
    }

    /// The name of the grid: "Team calendar, November 2026".
    static func gridName(_ range: String, labels: MonarchTeamCalendarLabels) -> String {
        labels.grid?(range) ?? MonarchTeamCalendarStrings.grid(range)
    }

    /// The header of the people column.
    static func personHeader(_ labels: MonarchTeamCalendarLabels) -> String {
        labels.person ?? MonarchTeamCalendarStrings.person
    }

    /// A holiday's phrase: "Holiday: Thanksgiving".
    static func holidayText(_ name: String, labels: MonarchTeamCalendarLabels) -> String {
        labels.holiday?(name) ?? MonarchTeamCalendarStrings.holiday(name)
    }

    /// What VoiceOver reads for a day header: "Today, Monday, November 2, Holiday: name".
    static func dayHeaderLabel(
        date: String,
        isToday: Bool,
        holiday: String?,
        labels: MonarchTeamCalendarLabels
    ) -> String {
        spoken(
            isToday ? (labels.today ?? MonarchTeamCalendarStrings.today) : nil,
            date,
            holiday.map { holidayText($0, labels: labels) }
        )
    }

    /// The days of a bar: one date, or "first to last".
    static func dates(first: String, last: String, count: Int, labels: MonarchTeamCalendarLabels) -> String {
        guard count > 1 else { return first }
        return labels.range?(first, last) ?? MonarchTeamCalendarStrings.range(first, last)
    }

    /// The spoken word of a portion, or `nil` for a whole day.
    static func portionText(_ portion: MonarchTeamCalendarPortion, labels: MonarchTeamCalendarLabels) -> String? {
        switch portion {
        case .full: nil
        case .firstHalf: labels.firstHalf ?? MonarchTeamCalendarStrings.firstHalf
        case .secondHalf: labels.secondHalf ?? MonarchTeamCalendarStrings.secondHalf
        }
    }

    /// What VoiceOver reads for a bar: "Annual leave, Alex Kim, Monday, November 2 to
    /// Thursday, November 5", with the half and "pending" when they apply.
    static func barLabel(
        _ segment: MonarchTeamCalendarSegment,
        member: String,
        dates: String,
        labels: MonarchTeamCalendarLabels
    ) -> String {
        spoken(
            segment.entry.label,
            member,
            dates,
            portionText(segment.portion, labels: labels),
            segment.entry.isPending ? (labels.pending ?? MonarchTeamCalendarStrings.pending) : nil
        )
    }

    /// What VoiceOver reads for an empty day that takes a tap: "Sam Lee, Wednesday, November 4".
    static func cellLabel(member: String, date: String) -> String {
        spoken(member, date)
    }

    /// What VoiceOver reads for a person: "Alex Kim, Design".
    static func personLabel(name: String, description: String?) -> String {
        spoken(name, description)
    }

    /// The title of the empty state.
    static func emptyTitle(_ labels: MonarchTeamCalendarLabels) -> String {
        labels.emptyTitle ?? MonarchTeamCalendarStrings.emptyTitle
    }

    /// The message of the empty state.
    static func emptyMessage(_ labels: MonarchTeamCalendarLabels) -> String {
        labels.emptyMessage ?? MonarchTeamCalendarStrings.emptyMessage
    }

    /// The legend, or `nil` when it is hidden.
    static func legend(_ labels: MonarchTeamCalendarLabels, showsLegend: Bool) -> String? {
        showsLegend ? (labels.legend ?? MonarchTeamCalendarStrings.legend) : nil
    }

    /// The spoken name of the placeholder.
    static func loadingLabel(_ labels: MonarchTeamCalendarLabels) -> String {
        labels.loading ?? MonarchTeamCalendarStrings.loading
    }

    /// What the calendar shows instead of the grid, if anything.
    enum Phase: Equatable {
        case loading
        case failed(String)
        case empty
        case grid
    }

    /// Loading first, then the error, then the empty state, then the grid.
    static func phase(isLoading: Bool, errorMessage: String?, memberCount: Int) -> Phase {
        if isLoading { return .loading }
        if let errorMessage { return .failed(errorMessage) }
        return memberCount == 0 ? .empty : .grid
    }
}

/// Names of days and months for ``MonarchTeamCalendar``, in a locale and a calendar.
struct MonarchTeamCalendarFormats {
    let locale: Locale
    let calendar: Calendar

    private func style() -> Date.FormatStyle {
        Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
    }

    /// "November 2026".
    func month(_ day: Date) -> String {
        day.formatted(style().month(.wide).year())
    }

    /// "Nov 2 – 8, 2026", the name of a custom range.
    func range(_ first: Date, _ last: Date) -> String {
        guard last > first else { return first.formatted(style().month(.abbreviated).day().year()) }
        var interval = Date.IntervalFormatStyle(date: .abbreviated, time: .omitted, locale: locale, calendar: calendar)
        interval.timeZone = calendar.timeZone
        return (first..<last).formatted(interval)
    }

    /// "Monday, November 2".
    func day(_ day: Date) -> String {
        day.formatted(style().weekday(.wide).month(.wide).day())
    }

    /// "2".
    func number(_ day: Date) -> String {
        day.formatted(style().day(.defaultDigits))
    }

    /// "M".
    func narrowWeekday(_ day: Date) -> String {
        day.formatted(style().weekday(.narrow))
    }

    /// The name of the range shown: the month, or the custom range.
    func rangeName(first: Date, last: Date, isCustom: Bool) -> String {
        isCustom ? range(first, last) : month(first)
    }
}
