import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// Fixed calendars, locales and dates, so layout and text do not depend on the machine.
private enum TimeGridClock {
    static func calendar(_ zone: String) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: zone) ?? TimeZone(secondsFromGMT: 0) ?? .gmt
        calendar.firstWeekday = 2
        return calendar
    }

    static let utc = calendar("UTC")
    static let newYork = calendar("America/New_York")
    static let kolkata = calendar("Asia/Kolkata")
    static let britishLocale = Locale(identifier: "en_GB")
    static let americanLocale = Locale(identifier: "en_US")

    static func date(
        _ day: Int,
        _ hour: Int = 0,
        _ minute: Int = 0,
        month: Int = 11,
        calendar: Calendar = utc
    ) -> Date {
        let components = DateComponents(year: 2026, month: month, day: day, hour: hour, minute: minute)
        return calendar.date(from: components) ?? Date(timeIntervalSince1970: 0)
    }

    /// Monday 2 to Friday 6 November 2026.
    static let week = (2...6).map { date($0) }
    /// Wednesday 4 November 2026, 10:40.
    static let now = date(4, 10, 40)
}

private let timeGridEvents = [
    MonarchAgendaEvent(
        "Conference",
        id: "conf",
        start: TimeGridClock.date(3),
        end: TimeGridClock.date(4),
        isAllDay: true
    ),
    MonarchAgendaEvent("Stand-up", id: "standup", start: TimeGridClock.date(2, 9), end: TimeGridClock.date(2, 9, 15)),
    MonarchAgendaEvent(
        "Design review",
        id: "review",
        start: TimeGridClock.date(4, 9, 30),
        end: TimeGridClock.date(4, 11),
        detail: "Room 4"
    ),
    MonarchAgendaEvent(
        "Interview",
        id: "interview",
        start: TimeGridClock.date(4, 10),
        end: TimeGridClock.date(4, 11),
        status: .tentative
    ),
    MonarchAgendaEvent(
        "Retrospective",
        id: "retro",
        start: TimeGridClock.date(6, 15),
        end: TimeGridClock.date(6, 16),
        status: .cancelled
    ),
    MonarchAgendaEvent("Release night", id: "release", start: TimeGridClock.date(5, 22), end: TimeGridClock.date(6, 2)),
]

private let workingWeek = (1...5).flatMap {
    [
        MonarchTimeGridWorkingHours(weekday: $0, start: 9 * 60, end: 12 * 60),
        MonarchTimeGridWorkingHours(weekday: $0, start: 13 * 60, end: 18 * 60),
    ]
}

enum MonarchTimeGridFixtures: ComponentFixtures {
    static let id = "time-grid"
    static let fixtures = [
        Fixture("week with overlaps, all-day row, now line and working hours") {
            MonarchTimeGrid(
                days: TimeGridClock.week,
                events: timeGridEvents,
                now: TimeGridClock.now,
                workingHours: workingWeek,
                unavailable: [
                    MonarchTimeGridUnavailable(
                        id: "off",
                        start: TimeGridClock.date(5, 13),
                        end: TimeGridClock.date(5, 17),
                        label: "Time off"
                    )
                ],
                scrollToMinutes: 8 * 60,
                calendar: TimeGridClock.utc,
                onSelectDay: { _ in },
                onSelectSlot: { _ in },
                onSelectEvent: { _ in },
                onMoveEvent: { _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        },
        Fixture("one day, read only, no now line") {
            MonarchTimeGrid(
                days: [TimeGridClock.date(4)],
                events: timeGridEvents,
                now: nil,
                calendar: TimeGridClock.utc
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
        Fixture("empty day with a forced all-day row and short range") {
            MonarchTimeGrid(
                days: [TimeGridClock.date(8)],
                now: TimeGridClock.date(8, 9),
                startHour: 8,
                endHour: 12,
                hourHeight: MonarchTokens.Space.space16,
                snapMinutes: 30,
                showsAllDayRow: true,
                calendar: TimeGridClock.utc,
                onSelectSlot: { _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
        Fixture("daylight-saving day in New York with custom labels") {
            MonarchTimeGrid(
                days: [TimeGridClock.date(8, month: 3, calendar: TimeGridClock.newYork)],
                events: [
                    MonarchAgendaEvent(
                        "Overnight shift with a very long title that has to wrap or be cut short",
                        id: "night",
                        start: TimeGridClock.date(8, 1, 30, month: 3, calendar: TimeGridClock.newYork),
                        end: TimeGridClock.date(8, 4, month: 3, calendar: TimeGridClock.newYork),
                        detail: "Warehouse"
                    )
                ],
                now: nil,
                calendar: TimeGridClock.newYork,
                labels: MonarchTimeGridLabels(allDay: "Whole day", createEvent: { "New at \($0)" }),
                canMoveEvent: { _ in false },
                onSelectSlot: { _ in },
                onMoveEvent: { _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
    ]
}

@Suite("MonarchTimeGrid")
struct MonarchTimeGridTests {
    private let utc = TimeGridClock.utc
    private let full = MonarchTimeGridSpan(top: 0, bottom: 24 * 60)

    // MARK: Range and hours

    @Test func rangeIsClampedAndAtLeastAnHour() {
        #expect(MonarchTimeGridModel.range(startHour: 0, endHour: 24) == full)
        #expect(MonarchTimeGridModel.range(startHour: 8, endHour: 18) == MonarchTimeGridSpan(top: 480, bottom: 1080))
        #expect(MonarchTimeGridModel.range(startHour: 10, endHour: 9) == MonarchTimeGridSpan(top: 600, bottom: 660))
        #expect(MonarchTimeGridModel.range(startHour: -3, endHour: 40) == full)
        #expect(MonarchTimeGridModel.hours(in: MonarchTimeGridSpan(top: 480, bottom: 720)) == [8, 9, 10, 11])
    }

    // MARK: Spans

    @Test func spanOfATimedEventOnItsDay() {
        let span = MonarchTimeGridModel.span(
            start: TimeGridClock.date(4, 9, 30),
            end: TimeGridClock.date(4, 11),
            on: TimeGridClock.date(4),
            calendar: utc,
            minimumLength: 15
        )
        #expect(span == MonarchTimeGridSpan(top: 570, bottom: 660))
    }

    @Test func multiDayEventIsClippedOnEachDay() {
        let start = TimeGridClock.date(5, 22)
        let end = TimeGridClock.date(6, 2)
        let first = MonarchTimeGridModel.span(
            start: start,
            end: end,
            on: TimeGridClock.date(5),
            calendar: utc,
            minimumLength: 15
        )
        let second = MonarchTimeGridModel.span(
            start: start,
            end: end,
            on: TimeGridClock.date(6),
            calendar: utc,
            minimumLength: 15
        )
        let third = MonarchTimeGridModel.span(
            start: start,
            end: end,
            on: TimeGridClock.date(7),
            calendar: utc,
            minimumLength: 15
        )
        #expect(first == MonarchTimeGridSpan(top: 1320, bottom: 1440))
        #expect(second == MonarchTimeGridSpan(top: 0, bottom: 120))
        #expect(third == nil)
    }

    @Test func eventEndingAtMidnightDoesNotTouchTheNextDay() {
        let span = MonarchTimeGridModel.span(
            start: TimeGridClock.date(4, 22),
            end: TimeGridClock.date(5),
            on: TimeGridClock.date(5),
            calendar: utc,
            minimumLength: 15
        )
        #expect(span == nil)
    }

    @Test func shortAndEndlessEventsGrowToTheMinimumLength() {
        let moment = MonarchTimeGridModel.span(
            start: TimeGridClock.date(4, 9),
            end: nil,
            on: TimeGridClock.date(4),
            calendar: utc,
            minimumLength: 15
        )
        let backwards = MonarchTimeGridModel.span(
            start: TimeGridClock.date(4, 9),
            end: TimeGridClock.date(4, 8),
            on: TimeGridClock.date(4),
            calendar: utc,
            minimumLength: 30
        )
        #expect(moment == MonarchTimeGridSpan(top: 540, bottom: 555))
        #expect(backwards == MonarchTimeGridSpan(top: 540, bottom: 570))
    }

    @Test func eventsArePlacedInTheCalendarsTimeZone() {
        // 09:00 UTC is 14:30 in Kolkata.
        let span = MonarchTimeGridModel.span(
            start: TimeGridClock.date(4, 9),
            end: TimeGridClock.date(4, 10),
            on: TimeGridClock.date(4, 12, calendar: TimeGridClock.kolkata),
            calendar: TimeGridClock.kolkata,
            minimumLength: 15
        )
        #expect(span == MonarchTimeGridSpan(top: 870, bottom: 930))
    }

    @Test func daylightSavingStartUsesWallClockMinutes() {
        let newYork = TimeGridClock.newYork
        let day = TimeGridClock.date(8, month: 3, calendar: newYork)
        // 01:30 to 04:00 local is 90 real minutes, because 02:00 to 03:00 does not exist.
        let start = TimeGridClock.date(8, 1, 30, month: 3, calendar: newYork)
        let end = TimeGridClock.date(8, 4, month: 3, calendar: newYork)
        #expect(end.timeIntervalSince(start) == 90 * 60)
        let span = MonarchTimeGridModel.span(start: start, end: end, on: day, calendar: newYork, minimumLength: 15)
        #expect(span == MonarchTimeGridSpan(top: 90, bottom: 240))
        // A skipped wall-clock time moves forward.
        let skipped = MonarchTimeGridModel.date(day: day, minutes: 150, calendar: newYork)
        #expect(MonarchTimeGridModel.wallClock(skipped, calendar: newYork).minutes == 210)
    }

    @Test func daylightSavingEndKeepsTwentyFourRows() {
        let newYork = TimeGridClock.newYork
        let day = TimeGridClock.date(1, month: 11, calendar: newYork)
        // 00:00 to 06:00 local is seven real hours on the day clocks go back.
        let start = TimeGridClock.date(1, 0, month: 11, calendar: newYork)
        let end = TimeGridClock.date(1, 6, month: 11, calendar: newYork)
        #expect(end.timeIntervalSince(start) == 7 * 3600)
        let span = MonarchTimeGridModel.span(start: start, end: end, on: day, calendar: newYork, minimumLength: 15)
        #expect(span == MonarchTimeGridSpan(top: 0, bottom: 360))
        #expect(MonarchTimeGridModel.hours(in: full).count == 24)
        let slot = MonarchTimeGridModel.date(day: day, minutes: 9 * 60, calendar: newYork)
        #expect(MonarchTimeGridModel.wallClock(slot, calendar: newYork).minutes == 540)
    }

    @Test func allDayEventsCoverTheirDaysInclusive() {
        let event = timeGridEvents[0]
        #expect(!MonarchTimeGridModel.covers(event, day: TimeGridClock.date(2), calendar: utc))
        #expect(MonarchTimeGridModel.covers(event, day: TimeGridClock.date(3), calendar: utc))
        #expect(MonarchTimeGridModel.covers(event, day: TimeGridClock.date(4), calendar: utc))
        #expect(!MonarchTimeGridModel.covers(event, day: TimeGridClock.date(5), calendar: utc))
    }

    @Test func isoWeekdayStartsOnMonday() {
        #expect(MonarchTimeGridModel.isoWeekday(TimeGridClock.date(2), calendar: utc) == 1)
        #expect(MonarchTimeGridModel.isoWeekday(TimeGridClock.date(8), calendar: utc) == 7)
    }

    // MARK: Overlaps

    @Test func overlappingEventsShareColumns() {
        let packed = MonarchTimeGridModel.pack([
            (item: "a", span: MonarchTimeGridSpan(top: 540, bottom: 660)),
            (item: "b", span: MonarchTimeGridSpan(top: 600, bottom: 660)),
            (item: "c", span: MonarchTimeGridSpan(top: 660, bottom: 720)),
            (item: "d", span: MonarchTimeGridSpan(top: 630, bottom: 645)),
        ])
        let byItem = Dictionary(uniqueKeysWithValues: packed.map { ($0.item, [$0.column, $0.columns]) })
        #expect(byItem["a"] == [0, 3])
        #expect(byItem["b"] == [1, 3])
        #expect(byItem["d"] == [2, 3])
        // Starts when the cluster ends: a new cluster of one.
        #expect(byItem["c"] == [0, 1])
    }

    @Test func freedColumnIsReused() {
        let packed = MonarchTimeGridModel.pack([
            (item: "long", span: MonarchTimeGridSpan(top: 0, bottom: 300)),
            (item: "early", span: MonarchTimeGridSpan(top: 0, bottom: 60)),
            (item: "later", span: MonarchTimeGridSpan(top: 120, bottom: 180)),
        ])
        let columns = Dictionary(uniqueKeysWithValues: packed.map { ($0.item, $0.column) })
        #expect(columns == ["long": 0, "early": 1, "later": 1])
        #expect(packed.allSatisfy { $0.columns == 2 })
    }

    // MARK: Shading and columns

    @Test func shadeIsTimeOutsideWorkingHours() {
        let shade = MonarchTimeGridModel.shade(weekday: 1, workingHours: workingWeek, range: full)
        #expect(
            shade == [
                MonarchTimeGridSpan(top: 0, bottom: 540),
                MonarchTimeGridSpan(top: 720, bottom: 780),
                MonarchTimeGridSpan(top: 1080, bottom: 1440),
            ]
        )
        #expect(MonarchTimeGridModel.shade(weekday: 7, workingHours: workingWeek, range: full) == [full])
        #expect(MonarchTimeGridModel.shade(weekday: 1, workingHours: nil, range: full).isEmpty)
    }

    @Test func columnsSortEventsIntoDays() {
        let range = MonarchTimeGridSpan(top: 8 * 60, bottom: 20 * 60)
        let columns = MonarchTimeGridModel.columns(
            days: TimeGridClock.week,
            events: timeGridEvents,
            unavailable: [
                MonarchTimeGridUnavailable(id: "off", start: TimeGridClock.date(5, 6), end: TimeGridClock.date(5, 9))
            ],
            workingHours: nil,
            range: range,
            snapMinutes: 15,
            calendar: utc
        )
        #expect(columns.count == 5)
        #expect(columns[0].timed.map(\.item.id) == ["standup"])
        #expect(columns[1].allDay.map(\.id) == ["conf"])
        #expect(columns[2].allDay.map(\.id) == ["conf"])
        #expect(Set(columns[2].timed.map(\.item.id)) == ["review", "interview"])
        #expect(columns[2].timed.allSatisfy { $0.columns == 2 })
        // Release night starts at 22:00, outside 08:00 to 20:00: hidden on both days.
        #expect(columns[3].timed.isEmpty)
        #expect(columns[4].timed.map(\.item.id) == ["retro"])
        // The unavailable block is clipped to the visible hours.
        #expect(columns[3].unavailable.map(\.span) == [MonarchTimeGridSpan(top: 480, bottom: 540)])
    }

    @Test func scrollTargetIsAnHourBeforeTheFirstEvent() {
        let columns = MonarchTimeGridModel.columns(
            days: TimeGridClock.week,
            events: timeGridEvents,
            unavailable: [],
            workingHours: nil,
            range: full,
            snapMinutes: 15,
            calendar: utc
        )
        // Release night shows from midnight on Friday.
        #expect(MonarchTimeGridModel.scrollTarget(requested: nil, columns: columns, range: full) == 0)
        let weekdays = Array(columns.prefix(3))
        #expect(MonarchTimeGridModel.scrollTarget(requested: nil, columns: weekdays, range: full) == 480)
        #expect(MonarchTimeGridModel.scrollTarget(requested: nil, columns: [], range: full) == 480)
        #expect(MonarchTimeGridModel.scrollTarget(requested: 1500, columns: [], range: full) == 1439)
    }

    // MARK: Taps and moves

    @Test func tapsSnapDownInsideTheRange() {
        let range = MonarchTimeGridSpan(top: 480, bottom: 720)
        // 0.8 points per minute: 100 points is 125 minutes after 08:00.
        #expect(MonarchTimeGridModel.slotMinutes(offset: 100, minuteHeight: 0.8, range: range, snapMinutes: 15) == 600)
        #expect(MonarchTimeGridModel.slotMinutes(offset: 100, minuteHeight: 0.8, range: range, snapMinutes: 30) == 600)
        #expect(MonarchTimeGridModel.slotMinutes(offset: -20, minuteHeight: 0.8, range: range, snapMinutes: 15) == 480)
        #expect(
            MonarchTimeGridModel.slotMinutes(offset: 9_999, minuteHeight: 0.8, range: range, snapMinutes: 15) == 705
        )
        #expect(MonarchTimeGridModel.slotMinutes(offset: 10, minuteHeight: 0, range: range, snapMinutes: 15) == 480)
    }

    @Test func dragSnapsAndKeepsTheEventOnScreen() {
        let range = MonarchTimeGridSpan(top: 480, bottom: 1080)
        let start = MonarchTimeGridDrag(eventID: "e", grab: 20, duration: 60, dayIndex: 0, startMinutes: 540)
        let moved = MonarchTimeGridModel.drag(start, to: 2, minutes: 628, range: range, snapMinutes: 15)
        #expect(moved.dayIndex == 2)
        #expect(moved.startMinutes == 615)
        let low = MonarchTimeGridModel.drag(start, to: 0, minutes: 2_000, range: range, snapMinutes: 15)
        #expect(low.startMinutes == 1020)
        let high = MonarchTimeGridModel.drag(start, to: 0, minutes: 0, range: range, snapMinutes: 15)
        #expect(high.startMinutes == 480)
        #expect(MonarchTimeGridModel.clampStart(500, duration: 2_000, range: range, snapMinutes: 15) == 480)
    }

    @Test func moveKeepsTheDurationAndSkipsNoOps() {
        let event = timeGridEvents[2]
        let move = MonarchTimeGridModel.move(
            event,
            to: TimeGridClock.date(5),
            startMinutes: 600,
            snapMinutes: 15,
            calendar: utc
        )
        #expect(move?.start == TimeGridClock.date(5, 10))
        #expect(move?.end == TimeGridClock.date(5, 11, 30))
        #expect(move?.event == event)
        #expect(
            MonarchTimeGridModel.move(
                event,
                to: TimeGridClock.date(4),
                startMinutes: 570,
                snapMinutes: 15,
                calendar: utc
            ) == nil
        )
    }

    @Test func voiceOverActionsMoveByOneStep() {
        let event = timeGridEvents[2]
        let days = TimeGridClock.week
        func nudge(_ minutes: Int, _ dayDelta: Int, _ event: MonarchAgendaEvent = event) -> MonarchTimeGridMove? {
            MonarchTimeGridModel.nudge(
                event,
                minutes: minutes,
                days: dayDelta,
                columns: days,
                range: full,
                snapMinutes: 15,
                calendar: utc
            )
        }
        #expect(nudge(-15, 0)?.start == TimeGridClock.date(4, 9, 15))
        #expect(nudge(15, 0)?.end == TimeGridClock.date(4, 11, 15))
        #expect(nudge(0, -1)?.start == TimeGridClock.date(3, 9, 30))
        #expect(nudge(0, 1)?.start == TimeGridClock.date(5, 9, 30))
        let monday = timeGridEvents[1]
        #expect(nudge(0, -1, monday) == nil)
        let midnight = MonarchAgendaEvent("Early", id: "x", start: TimeGridClock.date(4), end: TimeGridClock.date(4, 1))
        #expect(nudge(-15, 0, midnight) == nil)
    }

    @Test func dayUnderAFingerIgnoresLayoutDirection() {
        let frames: [Int: CGRect] = [
            0: CGRect(x: 200, y: 0, width: 100, height: 500),
            1: CGRect(x: 100, y: 0, width: 100, height: 500),
            2: CGRect(x: 0, y: 0, width: 100, height: 500),
        ]
        #expect(MonarchTimeGridModel.columnIndex(at: 250, frames: frames) == 0)
        #expect(MonarchTimeGridModel.columnIndex(at: 50, frames: frames) == 2)
        #expect(MonarchTimeGridModel.columnIndex(at: 999, frames: frames) == 0)
        #expect(MonarchTimeGridModel.columnIndex(at: -40, frames: frames) == 2)
        #expect(MonarchTimeGridModel.columnIndex(at: 10, frames: [:]) == nil)
    }

    @Test func durationIsAtLeastOneSnap() {
        #expect(MonarchTimeGridModel.duration(of: timeGridEvents[2], snapMinutes: 15) == 90)
        let moment = MonarchAgendaEvent("Moment", id: "m", start: TimeGridClock.date(4, 9))
        #expect(MonarchTimeGridModel.duration(of: moment, snapMinutes: 15) == 15)
    }

    // MARK: Layout

    @Test func layoutPlacesEventsSideBySide() {
        let bounds = CGRect(x: 0, y: 0, width: 104, height: 1152)
        let range = MonarchTimeGridSpan(top: 0, bottom: 1440)
        func frame(_ placement: MonarchTimeGridPlacement) -> CGRect {
            MonarchTimeGridColumnLayout.frame(
                for: placement,
                in: bounds,
                range: range,
                minuteHeight: 0.8,
                gap: 2,
                trailingInset: 4,
                eventMinHeight: 16
            )
        }
        let second = frame(
            MonarchTimeGridPlacement(
                span: MonarchTimeGridSpan(top: 600, bottom: 660),
                column: 1,
                columns: 2,
                isEvent: true
            )
        )
        #expect(second == CGRect(x: 50, y: 480, width: 48, height: 48))
        let alone = frame(MonarchTimeGridPlacement(span: MonarchTimeGridSpan(top: 600, bottom: 605), isEvent: true))
        #expect(alone == CGRect(x: 0, y: 480, width: 100, height: 16))
        let shade = frame(MonarchTimeGridPlacement(span: MonarchTimeGridSpan(top: 0, bottom: 540)))
        #expect(shade == CGRect(x: 0, y: 0, width: 104, height: 432))
        let now = frame(MonarchTimeGridPlacement(span: MonarchTimeGridSpan(top: 640, bottom: 640), lineHeight: 8))
        #expect(now == CGRect(x: 0, y: 508, width: 104, height: 8))
    }

    @Test func compactAndDetailThresholds() {
        #expect(MonarchTimeGridTokens.isCompact(minutes: 30))
        #expect(!MonarchTimeGridTokens.isCompact(minutes: 45))
        #expect(!MonarchTimeGridTokens.showsDetail(minutes: 60))
        #expect(MonarchTimeGridTokens.showsDetail(minutes: 90))
    }

    // MARK: Text

    @Test func labelsAreFormattedInTheGridsLocaleAndZone() {
        let british = MonarchTimeGridTokens.Formats(calendar: utc, locale: TimeGridClock.britishLocale)
        #expect(british.longDay(TimeGridClock.date(4)) == "Wednesday 4 November")
        #expect(british.weekday(TimeGridClock.date(4)) == "Wed")
        #expect(british.dayNumber(TimeGridClock.date(4)) == "4")
        #expect(british.time(TimeGridClock.date(4, 14, 5)).contains("14:05"))
        let american = MonarchTimeGridTokens.Formats(
            calendar: TimeGridClock.kolkata,
            locale: TimeGridClock.americanLocale
        )
        #expect(american.minutes(13 * 60).contains("1:00"))
        #expect(american.minutes(13 * 60).contains("PM"))
        // 09:00 UTC read in Kolkata.
        #expect(american.time(TimeGridClock.date(4, 9)).contains("2:30"))
    }

    @Test func eventTimesUseTheAgendaWording() {
        let labels = MonarchTimeGridLabels(range: { "\($0)–\($1)" })
        let format: (Date) -> String = {
            MonarchTimeGridTokens.Formats(calendar: utc, locale: TimeGridClock.britishLocale).time($0)
        }
        let time = MonarchTimeGridTokens.time(
            of: timeGridEvents[2],
            on: TimeGridClock.date(4),
            calendar: utc,
            labels: labels,
            format: format
        )
        let start = format(TimeGridClock.date(4, 9, 30))
        let end = format(TimeGridClock.date(4, 11))
        #expect(start.contains("9:30") && end.contains("11:00"))
        #expect(time.text == "\(start) – \(end)")
        #expect(time.spoken == "\(start)–\(end)")
        let release = MonarchTimeGridTokens.time(
            of: timeGridEvents[5],
            on: TimeGridClock.date(6),
            calendar: utc,
            labels: MonarchTimeGridLabels(),
            format: format
        )
        #expect(release.text.hasPrefix("–"))
    }

    @Test func accessibilityLabels() {
        let labels = MonarchTimeGridLabels()
        #expect(
            MonarchTimeGridTokens.headingLabel(longDay: "Wednesday 4 November", isToday: true, labels: labels)
                == "Wednesday 4 November, Today"
        )
        #expect(
            MonarchTimeGridTokens.headingLabel(longDay: "Thursday 5 November", isToday: false, labels: labels)
                == "Thursday 5 November"
        )
        #expect(
            MonarchTimeGridTokens.slotLabel(longDay: "Wednesday 4 November", time: "09:00", labels: labels)
                == "Create event, Wednesday 4 November, 09:00"
        )
        #expect(
            MonarchTimeGridTokens.openDayHint(longDay: "Wednesday 4 November", labels: labels)
                == "Opens Wednesday 4 November"
        )
        #expect(MonarchTimeGridTokens.unavailableLabel("Time off", time: "13:00 – 17:00") == "Time off, 13:00 – 17:00")
        #expect(MonarchTimeGridTokens.unavailableLabel(nil, time: "13:00 – 17:00") == "13:00 – 17:00")
        #expect(
            MonarchTimeGridTokens.moveActionNames(labels)
                == ["Move earlier", "Move later", "Move to previous day", "Move to next day"]
        )
    }

    @Test func customLabelsReplaceBuiltInStrings() {
        let labels = MonarchTimeGridLabels(
            allDay: "Whole day",
            createEvent: { "New: \($0)" },
            today: "Now",
            openDay: { "Show \($0)" },
            moveEarlier: "Up",
            moveLater: "Down",
            moveToPreviousDay: "Back",
            moveToNextDay: "On"
        )
        #expect(MonarchTimeGridTokens.allDay(labels) == "Whole day")
        #expect(MonarchTimeGridTokens.headingLabel(longDay: "Wed", isToday: true, labels: labels) == "Wed, Now")
        #expect(MonarchTimeGridTokens.slotLabel(longDay: "Wed", time: "9", labels: labels) == "New: Wed, 9")
        #expect(MonarchTimeGridTokens.openDayHint(longDay: "Wed", labels: labels) == "Show Wed")
        #expect(MonarchTimeGridTokens.moveActionNames(labels) == ["Up", "Down", "Back", "On"])
    }

    @Test func builtInStringsResolve() {
        let strings = [
            MonarchTimeGridStrings.allDay,
            MonarchTimeGridStrings.today,
            MonarchTimeGridStrings.createEvent("x"),
            MonarchTimeGridStrings.openDay("x"),
            MonarchTimeGridStrings.moveEarlier,
            MonarchTimeGridStrings.moveLater,
            MonarchTimeGridStrings.moveToPreviousDay,
            MonarchTimeGridStrings.moveToNextDay,
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchTimeGridStrings.allDay == "All day")
    }

    @Test func colourRolesAreDistinctWhereTheyMustBe() {
        for theme in [MonarchTheme.light, .dark, .highContrastLight, .highContrastDark] {
            #expect(
                theme.colors[role: MonarchTimeGridTokens.todayFill]
                    != theme.colors[role: MonarchTimeGridTokens.todayText]
            )
            #expect(
                theme.colors[role: MonarchTimeGridTokens.shade] != theme.colors[role: MonarchTimeGridTokens.background]
            )
            #expect(
                theme.colors[role: MonarchTimeGridTokens.now] != theme.colors[role: MonarchTimeGridTokens.background]
            )
            #expect(
                theme.colors[role: MonarchTimeGridTokens.unavailableHatch]
                    != theme.colors[role: MonarchTimeGridTokens.unavailableFill]
            )
        }
    }
}
