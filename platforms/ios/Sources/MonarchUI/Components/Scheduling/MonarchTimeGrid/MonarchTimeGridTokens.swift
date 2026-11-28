import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens, colour roles and accessibility text of ``MonarchTimeGrid``.
enum MonarchTimeGridTokens {
    /// The height of one hour at the default text size. It grows with Dynamic Type.
    /// The narrowest a day column is drawn; a grid of more days than fit scrolls sideways with the hours pinned.
    static let minDayWidth: CGFloat =
        MonarchTokens.Space.space24 + MonarchTokens.Space.space6 + MonarchTokens.Space.space2
    static let hourHeight: CGFloat = MonarchTokens.Space.space12
    /// The narrowest hour gutter at the default text size.
    static let gutterMinWidth: CGFloat = MonarchTokens.Space.space12
    /// The space between an hour label and the grid.
    static let gutterPadding: CGFloat = MonarchTokens.Space.space1
    /// The space between events that overlap, and at the trailing edge of a
    /// column so an empty strip stays tappable.
    static let eventGap: CGFloat = MonarchTokens.Space.space0_5
    /// The trailing space of a column that events leave free.
    static let columnTrailingInset: CGFloat = MonarchTokens.Space.space1
    /// The shortest an event is drawn.
    static let eventMinHeight: CGFloat = MonarchTokens.Space.space4
    /// The thickness of the now line.
    static let nowLineWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The diameter of the dot at the start of the now line.
    static let nowDotSize: CGFloat = MonarchTokens.Space.space2
    /// The thickness of grid lines.
    static let lineWidth: CGFloat = MonarchTokens.Space.px
    /// The distance between hatch lines of an unavailable block.
    static let hatchSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside an unavailable block and around a day heading.
    static let blockPadding: CGFloat = MonarchTokens.Space.space1
    /// The vertical space between the parts of the heading and the all-day row.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The diameter of the circle behind today's date.
    static let todaySize: CGFloat = MonarchTokens.Space.space8
    /// The width of the ghost's dashed border.
    static let ghostBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The dash of the ghost's border.
    static let ghostDash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space1]
    /// How long a press lasts before an event can be dragged, as in Calendar.
    static let pressDuration: Double = Double(MonarchTokens.Duration.slower) / 1000
    /// The share of the event colour in the ghost's fill.
    static let ghostTintRatio: Double = 0.2
    /// The opacity of an event while its ghost is being dragged.
    static let movingRatio: Double = 0.4
    /// Events shorter than this many minutes are drawn on one line.
    static let compactBelowMinutes = 45
    /// Events at least this many minutes long also show their detail line.
    static let detailFromMinutes = 70
    /// The type role of hour labels, weekdays and the all-day label.
    static let captionStyle: MonarchTextStyle = .caption
    /// The type role of the date number in a heading.
    static let dateStyle: MonarchTextStyle = .label
    /// The weight of the date number.
    static let dateWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)

    /// The background of the grid.
    static var background: MonarchColorRole { \.bgSurface }
    /// Hour lines and column separators.
    static var line: MonarchColorRole { \.borderSubtle }
    /// The line under the heading.
    static var headingLine: MonarchColorRole { \.borderDefault }
    /// Hour labels, weekdays and the all-day label.
    static var secondaryText: MonarchColorRole { \.textMuted }
    /// The date number of a day that is not today.
    static var dateText: MonarchColorRole { \.textDefault }
    /// The circle behind today's date, as in Calendar.
    static var todayFill: MonarchColorRole { \.accentSolid }
    /// Today's date number on its circle.
    static var todayText: MonarchColorRole { \.accentOnSolid }
    /// Time outside working hours.
    static var shade: MonarchColorRole { \.bgMuted }
    /// The fill of an unavailable block.
    static var unavailableFill: MonarchColorRole { \.bgMuted }
    /// The hatching of an unavailable block.
    static var unavailableHatch: MonarchColorRole { \.borderStrong }
    /// The text of an unavailable block.
    static var unavailableText: MonarchColorRole { \.textMuted }
    /// The now line and its dot.
    static var now: MonarchColorRole { \.dangerSolid }
    /// The ghost's colour when the event has none.
    static var ghostDefault: MonarchColorRole { \.accentSolid }
    /// The ghost's base fill.
    static var ghostFill: MonarchColorRole { \.bgSurface }
    /// The ghost's text.
    static var ghostText: MonarchColorRole { \.textDefault }

    // MARK: Text

    /// Formats for the grid's labels, in the grid's calendar, time zone and locale.
    struct Formats {
        let calendar: Calendar
        let locale: Locale

        /// "Wed".
        func weekday(_ day: Date) -> String { day.formatted(base.weekday(.abbreviated)) }
        /// "4".
        func dayNumber(_ day: Date) -> String { day.formatted(base.day()) }
        /// "Wednesday 4 November".
        func longDay(_ day: Date) -> String { day.formatted(base.weekday(.wide).day().month(.wide)) }
        /// "09:30" or "9:30 AM", in the grid's time zone.
        func time(_ date: Date) -> String {
            date.formatted(
                Date.FormatStyle(
                    date: .omitted,
                    time: .omitted,
                    locale: locale,
                    calendar: calendar,
                    timeZone: calendar.timeZone
                ).monarchTime()
            )
        }
        /// Wall-clock minutes as a label, "09:00" or "9:00 AM", the same on
        /// every day, including daylight-saving days.
        func minutes(_ minutes: Int) -> String {
            var utc = Calendar(identifier: .gregorian)
            utc.timeZone = TimeZone(secondsFromGMT: 0) ?? calendar.timeZone
            return Date(timeIntervalSinceReferenceDate: TimeInterval(minutes * 60)).formatted(
                Date.FormatStyle(
                    date: .omitted,
                    time: .omitted,
                    locale: locale,
                    calendar: utc,
                    timeZone: utc.timeZone
                ).monarchTime()
            )
        }

        private var base: Date.FormatStyle {
            Date.FormatStyle(
                date: .omitted,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            )
        }
    }

    /// The visible and spoken time of an event in its column, as the agenda
    /// list words it: "09:30 – 10:15", "– 11:00" on a later day, or "All day".
    static func time(
        of event: MonarchAgendaEvent,
        on day: Date,
        calendar: Calendar,
        labels: MonarchTimeGridLabels,
        format: (Date) -> String
    ) -> MonarchAgendaTime {
        MonarchAgendaListTokens.time(
            of: event,
            on: day,
            calendar: calendar,
            labels: MonarchAgendaListLabels(allDay: allDay(labels), range: labels.range),
            format: format
        )
    }

    /// The label of the all-day row.
    static func allDay(_ labels: MonarchTimeGridLabels) -> String {
        labels.allDay ?? MonarchTimeGridStrings.allDay
    }

    /// What VoiceOver reads for a day heading: "Wednesday 4 November", with
    /// ", Today" on today.
    static func headingLabel(longDay: String, isToday: Bool, labels: MonarchTimeGridLabels) -> String {
        isToday ? "\(longDay), \(labels.today ?? MonarchTimeGridStrings.today)" : longDay
    }

    /// The hint of a day heading that opens the day.
    static func openDayHint(longDay: String, labels: MonarchTimeGridLabels) -> String {
        labels.openDay?(longDay) ?? MonarchTimeGridStrings.openDay(longDay)
    }

    /// The spoken name of an empty hour: "Create event, Wednesday 4 November, 09:00".
    static func slotLabel(longDay: String, time: String, labels: MonarchTimeGridLabels) -> String {
        let when = "\(longDay), \(time)"
        return labels.createEvent?(when) ?? MonarchTimeGridStrings.createEvent(when)
    }

    /// The spoken name of an unavailable block: its label and its times.
    static func unavailableLabel(_ label: String?, time: String) -> String {
        [label, time].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The names of the VoiceOver move actions, in order: earlier, later,
    /// previous day, next day.
    static func moveActionNames(_ labels: MonarchTimeGridLabels) -> [String] {
        [
            labels.moveEarlier ?? MonarchTimeGridStrings.moveEarlier,
            labels.moveLater ?? MonarchTimeGridStrings.moveLater,
            labels.moveToPreviousDay ?? MonarchTimeGridStrings.moveToPreviousDay,
            labels.moveToNextDay ?? MonarchTimeGridStrings.moveToNextDay,
        ]
    }

    /// Whether an event of this many minutes is drawn on one line.
    static func isCompact(minutes: Int) -> Bool { minutes < compactBelowMinutes }

    /// Whether an event of this many minutes shows its detail line.
    static func showsDetail(minutes: Int) -> Bool { minutes >= detailFromMinutes }
}
