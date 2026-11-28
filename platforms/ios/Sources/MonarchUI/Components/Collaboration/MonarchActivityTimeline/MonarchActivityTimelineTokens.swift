import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchActivityTimeline``.
enum MonarchActivityTimelineTokens {
    /// The gap between day groups, and between the feed and Load more.
    static let groupSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between a day heading and its first event.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the marker column and the event.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of one event.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the time and the tags.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The side of the marker column before Dynamic Type: the small avatar.
    static let markerColumn: CGFloat = MonarchTokens.Space.space6
    /// The side of the dot marker.
    static let dotSide: CGFloat = MonarchTokens.Space.space2_5
    /// The width of the line that joins the markers.
    static let lineWidth: CGFloat = MonarchShapes.borderWidth
    /// The colour of the line that joins the markers.
    static var lineRole: MonarchColorRole { \.borderSubtle }
    /// The number of placeholder rows while loading.
    static let loadingRowCount = 3
    /// The SF Symbol of the empty state.
    static let emptySymbol = "clock.arrow.circlepath"
    /// The SF Symbol of the error message without a retry.
    static let errorSymbol = "exclamationmark.triangle.fill"
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "

    /// The space after each event.
    static func rowSpacing(_ density: MonarchActivityTimelineDensity) -> CGFloat {
        switch density {
        case .regular: MonarchTokens.Space.space4
        case .compact: MonarchTokens.Space.space2
        }
    }

    /// The fill of a dot: the solid tone, or the strong border for neutral.
    static func dotRole(_ tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.borderStrong : MonarchTonePalette.palette(for: tone).solid
    }

    /// The background and symbol colours of an icon marker.
    static func iconRoles(_ tone: MonarchTone) -> (fill: MonarchColorRole, symbol: MonarchColorRole) {
        let palette = MonarchTonePalette.palette(for: tone)
        return (palette.subtle, palette.strong)
    }

    /// Whether an actor gets the "Automated" tag.
    static func isAutomated(_ actor: MonarchActivityTimelineActor?) -> Bool {
        guard let actor else { return false }
        return actor.kind != .person
    }

    /// One day of events.
    struct DayGroup: Equatable, Identifiable {
        /// The start of the day in the calendar's time zone.
        let day: Date
        /// The events of that day, in the order given.
        let events: [MonarchActivityTimelineEvent]

        var id: Date { day }
    }

    /// The events grouped by day in the calendar's time zone. Days appear in
    /// the order of their first event, and events keep their order, so a
    /// feed given newest first stays newest first.
    static func groups(_ events: [MonarchActivityTimelineEvent], calendar: Calendar) -> [DayGroup] {
        var order: [Date] = []
        var byDay: [Date: [MonarchActivityTimelineEvent]] = [:]
        for event in events {
            let day = calendar.startOfDay(for: event.time)
            if byDay[day] == nil { order.append(day) }
            byDay[day, default: []].append(event)
        }
        return order.map { DayGroup(day: $0, events: byDay[$0] ?? []) }
    }

    /// The heading of a day: Today, Yesterday, or the weekday and date,
    /// with the year when it is not the current year.
    static func dayLabel(
        _ day: Date,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        labels: MonarchActivityTimelineLabels
    ) -> String {
        if calendar.isDate(day, inSameDayAs: now) {
            return labels.today ?? MonarchActivityTimelineStrings.today
        }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: now),
            calendar.isDate(day, inSameDayAs: yesterday)
        {
            return labels.yesterday ?? MonarchActivityTimelineStrings.yesterday
        }
        var style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone)
            .weekday(.wide).month(.wide).day()
        if calendar.component(.year, from: day) != calendar.component(.year, from: now) {
            style = style.year()
        }
        return day.formatted(style)
    }

    /// The time relative to `now`, such as "3 hours ago" or "yesterday".
    static func relativeTime(_ date: Date, now: Date, calendar: Calendar, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }

    /// The absolute time, which VoiceOver reads instead of the relative one.
    static func absoluteTime(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// The sentence: the actor in strong text, the verb muted, the object strong and its link part underlined.
    static func sentence(
        _ event: MonarchActivityTimelineEvent,
        theme: MonarchTheme? = nil,
        style: MonarchTextStyle = .bodySmall
    ) -> AttributedString {
        var result = AttributedString()
        func append(_ text: String, strong: Bool, color: Color?, link: Bool = false) {
            if !result.characters.isEmpty { result.append(AttributedString(" ")) }
            var part = AttributedString(text)
            if strong { part.inlinePresentationIntent = .stronglyEmphasized }
            if let color { part.foregroundColor = color }
            if link { part.underlineStyle = .single }
            if strong, let theme { part = part.monarchStrongFaces(theme: theme, style: style) }
            result.append(part)
        }
        let strongColor = theme?.colors.textDefault
        if let actor = event.actor { append(actor.name, strong: true, color: strongColor) }
        append(event.verb, strong: false, color: theme?.colors.textMuted)
        if let object = event.object, !object.isEmpty {
            if let link = event.link, !link.isEmpty, object.hasPrefix(link) {
                append(link, strong: true, color: theme?.colors.textLink, link: true)
                let rest = object.dropFirst(link.count).trimmingCharacters(in: .whitespaces)
                if !rest.isEmpty { append(rest, strong: true, color: strongColor) }
            } else {
                append(object, strong: true, color: strongColor)
            }
        }
        return result
    }

    /// What VoiceOver reads for an event: the sentence, the time, the
    /// automated tag, the tags and the body.
    static func accessibilityLabel(
        _ event: MonarchActivityTimelineEvent,
        time: String,
        automated: String
    ) -> String {
        var parts = [String(sentence(event).characters), time]
        if isAutomated(event.actor) { parts.append(automated) }
        parts += event.tags
        if let body = event.body, !body.isEmpty { parts.append(body) }
        return parts.joined(separator: separator)
    }

    /// The expanded set after an event's details are opened or closed.
    static func toggled(_ expanded: Set<String>, id: String, isExpanded: Bool) -> Set<String> {
        var next = expanded
        if isExpanded { next.insert(id) } else { next.remove(id) }
        return next
    }
}
