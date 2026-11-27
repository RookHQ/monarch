import Foundation
import SwiftUI

private typealias Tokens = MonarchWeeklyHoursEditorTokens

/// Edits weekly availability: each day switched on or off, one or more time intervals per day,
/// copying a day to the whole week, and overlap checks.
///
/// ```swift
/// @State private var hours = (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1020) }
///
/// MonarchWeeklyHoursEditor(value: $hours) {
///     Text("Times in Europe/London")
/// }
/// ```
///
/// The value is a plain list of ``MonarchWeeklyHoursInterval`` in minutes from midnight, with
/// ISO weekdays. Each day is a standard `Toggle` named by the weekday; while it is on, each
/// interval is a pair of `Picker` menus (start and end, every `step` minutes, so there is
/// nothing to type) with a remove button, followed by "Add interval" and "Copy to all".
/// Switching a day on adds the default interval, or restores the intervals it had when it was
/// switched off. Problems found by ``MonarchWeeklyHoursInterval/validate(_:)`` show under the
/// day as a ``MonarchInlineNotice`` and are announced; the app decides whether to block saving.
/// Disable the editor with `.disabled(true)`.
///
/// Weekday names follow the environment's locale, and the first day shown follows the
/// environment's calendar unless `firstWeekday` is given. The editor does not scroll: put it
/// in a `ScrollView` or a `Form` section.
///
/// Accessibility: the heading has the header trait. Each day is a container named by the
/// weekday; its switch reads "Unavailable" while off. Each interval is a container named
/// "Monday interval 1" holding the Start and End menus and "Remove Monday interval 1". Add and
/// copy carry the day ("Add interval to Monday"). A new problem (its message, as it appears
/// in the day being edited) and a copy are announced.
/// Touch targets are at least 44 points.
public struct MonarchWeeklyHoursEditor<TimeZoneLabel: View>: View {
    @Binding private var value: [MonarchWeeklyHoursInterval]
    private let firstWeekday: Int?
    private let uses24HourClock: Bool?
    private let step: Int
    private let defaultStart: Int
    private let defaultEnd: Int
    private let maxIntervals: Int
    private let isReadOnly: Bool
    private let text: MonarchWeeklyHoursEditorText
    private let timeZoneLabel: TimeZoneLabel

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    /// The intervals of each day switched off, restored if it is switched back on.
    @State private var remembered: [Int: [MonarchWeeklyHoursInterval]] = [:]
    @State private var copies = 0

    /// A weekly hours editor with content under the heading for the zone the hours are in.
    ///
    /// - Parameters:
    ///   - value: The intervals. Days are ISO weekdays (1 Monday … 7 Sunday); every change
    ///     writes the whole list, ordered by weekday.
    ///   - firstWeekday: The first day shown, as a `Calendar` weekday (1 Sunday … 7 Saturday).
    ///     By default the environment calendar's `firstWeekday`.
    ///   - uses24HourClock: Shows times on a 24-hour clock; `nil` follows the locale.
    ///   - step: Minutes between time options, 1 to 60; zero or less falls back to 15.
    ///   - defaultStart: The start, in minutes, of a day switched on for the first time.
    ///   - defaultEnd: The end, in minutes, of a day switched on for the first time.
    ///   - maxIntervals: The most intervals per day; the add button is hidden at this count.
    ///   - isReadOnly: Shows each day's hours as text, without controls.
    ///   - labels: Replacements for the built-in strings.
    ///   - timeZoneLabel: Content under the heading, such as "Times in Europe/London" or the
    ///     app's ``MonarchTimezoneSelect``.
    public init(
        value: Binding<[MonarchWeeklyHoursInterval]>,
        firstWeekday: Int? = nil,
        uses24HourClock: Bool? = nil,
        step: Int = 15,
        defaultStart: Int = 540,
        defaultEnd: Int = 1020,
        maxIntervals: Int = 6,
        isReadOnly: Bool = false,
        labels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels(),
        @ViewBuilder timeZoneLabel: () -> TimeZoneLabel
    ) {
        _value = value
        self.firstWeekday = firstWeekday
        self.uses24HourClock = uses24HourClock
        self.step = Tokens.step(step)
        self.defaultStart = defaultStart
        self.defaultEnd = defaultEnd
        self.maxIntervals = max(maxIntervals, 1)
        self.isReadOnly = isReadOnly
        self.text = MonarchWeeklyHoursEditorText(labels: labels)
        self.timeZoneLabel = timeZoneLabel()
    }

    /// The content of the view.
    public var body: some View {
        let formats = MonarchWeeklyHoursEditorFormats(
            locale: locale,
            uses24HourClock: uses24HourClock,
            midnight: text.midnight
        )
        let errors = MonarchWeeklyHoursInterval.validate(value)
        let days = Tokens.order(firstWeekday: firstWeekday ?? calendar.firstWeekday)
        VStack(alignment: .leading, spacing: Tokens.dayGap) {
            Text(text.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            timeZoneLabel
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.muted])
            VStack(spacing: 0) {
                ForEach(Array(days.enumerated()), id: \.element) { index, day in
                    if index > 0 {
                        Rectangle()
                            .fill(theme.colors[role: Tokens.divider])
                            .frame(height: MonarchShapes.borderWidth)
                            .accessibilityHidden(true)
                    }
                    MonarchWeeklyHoursEditorDay(
                        name: formats.day(day),
                        ranges: Tokens.dayOf(value, day),
                        error: errors[day],
                        step: step,
                        maxIntervals: maxIntervals,
                        isReadOnly: isReadOnly,
                        formats: formats,
                        text: text,
                        onToggle: { toggle(day, isOn: $0) },
                        onRangesChange: { change(day, to: $0) },
                        onAdd: { add(to: day) },
                        onCopy: { copy(day, name: formats.day(day)) }
                    )
                    .padding(Tokens.dayPadding)
                }
            }
            .overlay(
                RoundedRectangle(cornerRadius: Tokens.cardRadius, style: .continuous)
                    .strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth)
            )
            .background(
                RoundedRectangle(cornerRadius: Tokens.cardRadius, style: .continuous)
                    .fill(theme.colors.bgSurface)
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .sensoryFeedback(.success, trigger: copies)
        .onChange(of: errors) { old, new in
            for day in Tokens.newErrors(from: old, to: new) {
                guard let error = new[day] else { continue }
                AccessibilityNotification.Announcement(text.error(error)).post()
            }
        }
    }

    private func toggle(_ day: Int, isOn: Bool) {
        let current = Tokens.dayOf(value, day)
        if isOn {
            let ranges = Tokens.switchedOn(
                day: day,
                remembered: remembered[day],
                defaultStart: defaultStart,
                defaultEnd: defaultEnd
            )
            value = Tokens.withDay(value, day: day, ranges: ranges)
        } else {
            remembered[day] = current
            value = Tokens.withDay(value, day: day, ranges: [])
        }
    }

    private func change(_ day: Int, to ranges: [MonarchWeeklyHoursInterval]) {
        if ranges.isEmpty { remembered[day] = Tokens.dayOf(value, day) }
        value = Tokens.withDay(value, day: day, ranges: ranges)
    }

    private func add(to day: Int) {
        let current = Tokens.dayOf(value, day)
        let next = current + [Tokens.nextInterval(after: current, day: day, step: step)]
        value = Tokens.withDay(value, day: day, ranges: next)
    }

    private func copy(_ day: Int, name: String) {
        value = Tokens.copiedToAll(value, from: day)
        copies += 1
        AccessibilityNotification.Announcement(text.copied(name)).post()
    }
}

extension MonarchWeeklyHoursEditor where TimeZoneLabel == EmptyView {
    /// A weekly hours editor. Each parameter is described on
    /// ``init(value:firstWeekday:uses24HourClock:step:defaultStart:defaultEnd:maxIntervals:isReadOnly:labels:timeZoneLabel:)``.
    public init(
        value: Binding<[MonarchWeeklyHoursInterval]>,
        firstWeekday: Int? = nil,
        uses24HourClock: Bool? = nil,
        step: Int = 15,
        defaultStart: Int = 540,
        defaultEnd: Int = 1020,
        maxIntervals: Int = 6,
        isReadOnly: Bool = false,
        labels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels()
    ) {
        self.init(
            value: value,
            firstWeekday: firstWeekday,
            uses24HourClock: uses24HourClock,
            step: step,
            defaultStart: defaultStart,
            defaultEnd: defaultEnd,
            maxIntervals: maxIntervals,
            isReadOnly: isReadOnly,
            labels: labels,
            timeZoneLabel: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchWeeklyHoursEditor") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchWeeklyHoursEditor(
                    value: .constant([
                        MonarchWeeklyHoursInterval(day: 1, start: 540, end: 720),
                        MonarchWeeklyHoursInterval(day: 1, start: 780, end: 1020),
                        MonarchWeeklyHoursInterval(day: 2, start: 540, end: 1020),
                        MonarchWeeklyHoursInterval(day: 3, start: 600, end: 540),
                        MonarchWeeklyHoursInterval(day: 4, start: 540, end: 720),
                        MonarchWeeklyHoursInterval(day: 4, start: 660, end: 900),
                    ]),
                    firstWeekday: 2
                ) {
                    Text(verbatim: "Times in Europe/London")
                }
                MonarchWeeklyHoursEditor(
                    value: .constant([MonarchWeeklyHoursInterval(day: 5, start: 1320, end: 1440)]),
                    firstWeekday: 2,
                    uses24HourClock: true,
                    isReadOnly: true
                )
                MonarchWeeklyHoursEditor(value: .constant([]), firstWeekday: 2)
                    .disabled(true)
            }
        }
    }
#endif
