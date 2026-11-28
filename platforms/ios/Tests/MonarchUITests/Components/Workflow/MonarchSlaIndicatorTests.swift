import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSlaIndicatorFixtures: ComponentFixtures {
    static let id = "sla-indicator"
    private static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

    static let fixtures = [
        Fixture("every state as a chip") {
            VStack(alignment: .leading) {
                ForEach(MonarchSlaIndicatorState.allCases, id: \.self) { state in
                    MonarchSlaIndicator(state)
                }
            }
        },
        Fixture("every state as text") {
            VStack(alignment: .leading) {
                ForEach(MonarchSlaIndicatorState.allCases, id: \.self) { state in
                    MonarchSlaIndicator(state, variant: .text, showsNotApplicable: true)
                }
            }
        },
        Fixture("countdowns at a fixed time") {
            VStack(alignment: .leading) {
                MonarchSlaIndicator(.onTrack, dueAt: now.addingTimeInterval(8100), target: "First response", now: now)
                MonarchSlaIndicator(.onTrack, dueAt: now.addingTimeInterval(1500), warnBefore: 3600, now: now)
                MonarchSlaIndicator(.atRisk, dueAt: now.addingTimeInterval(30), now: now)
                MonarchSlaIndicator(.onTrack, dueAt: now.addingTimeInterval(-2400), now: now)
                MonarchSlaIndicator(.breached, dueAt: now.addingTimeInterval(-277_200), variant: .text, now: now)
                MonarchSlaIndicator(.paused, dueAt: now.addingTimeInterval(600), hasEarlierBreach: true, now: now)
            }
        },
        Fixture("live countdown, announcing changes") {
            MonarchSlaIndicator(
                .onTrack,
                dueAt: Date(timeIntervalSinceNow: 5400),
                warnBefore: 3600,
                target: "Resolution",
                announcesChanges: true,
                onStateChange: { _ in }
            )
        },
        Fixture("not live, and with custom strings") {
            VStack(alignment: .leading) {
                MonarchSlaIndicator(.onTrack, dueAt: Date(timeIntervalSinceNow: 5400), isLive: false)
                MonarchSlaIndicator(
                    .atRisk,
                    dueAt: now.addingTimeInterval(4000),
                    now: now,
                    labels: MonarchSlaIndicatorLabels(
                        remaining: { "noch \($0)" },
                        duration: { "\(Int($0 / 60)) Min." }
                    )
                )
                MonarchSlaIndicator(.met, labels: MonarchSlaIndicatorLabels(met: "Eingehalten"))
            }
        },
        Fixture("a list of targets") {
            VStack(alignment: .leading) {
                LabeledContent("First response") {
                    MonarchSlaIndicator(.met, variant: .text)
                }
                LabeledContent("A target with a long name that wraps at large text sizes") {
                    MonarchSlaIndicator(.atRisk, dueAt: now.addingTimeInterval(900), variant: .text, now: now)
                }
                LabeledContent("Resolution") {
                    MonarchSlaIndicator(.notApplicable, variant: .text, showsNotApplicable: true)
                }
            }
            .monarchTextStyle(.caption)
        },
    ]
}

@Suite("MonarchSlaIndicator")
struct MonarchSlaIndicatorTests {
    typealias Tokens = MonarchSlaIndicatorTokens
    typealias State = MonarchSlaIndicatorState

    private let labels = MonarchSlaIndicatorLabels()
    private let english = Locale(identifier: "en_US")
    private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

    private func due(in seconds: TimeInterval) -> Date { now.addingTimeInterval(seconds) }

    // MARK: State

    @Test("A running target whose due time has passed is breached")
    func resolvesBreached() {
        #expect(Tokens.resolve(.onTrack, dueAt: due(in: -1), now: now, warnBefore: nil) == .breached)
        #expect(Tokens.resolve(.atRisk, dueAt: due(in: -3600), now: now, warnBefore: 600) == .breached)
        #expect(Tokens.resolve(.onTrack, dueAt: now, now: now, warnBefore: nil) == .breached)
    }

    @Test("An on-track target within the warning time is at risk")
    func resolvesAtRisk() {
        #expect(Tokens.resolve(.onTrack, dueAt: due(in: 600), now: now, warnBefore: 3600) == .atRisk)
        #expect(Tokens.resolve(.onTrack, dueAt: due(in: 3600), now: now, warnBefore: 3600) == .atRisk)
        #expect(Tokens.resolve(.onTrack, dueAt: due(in: 3601), now: now, warnBefore: 3600) == .onTrack)
        #expect(Tokens.resolve(.onTrack, dueAt: due(in: 600), now: now, warnBefore: nil) == .onTrack)
        #expect(Tokens.resolve(.atRisk, dueAt: due(in: 86400), now: now, warnBefore: 60) == .atRisk)
    }

    @Test(
        "Paused, met, breached and absent targets are never changed by the clock",
        arguments: [State.paused, .met, .breached, .notApplicable]
    )
    func settledStatesStay(state: State) {
        #expect(Tokens.resolve(state, dueAt: due(in: -3600), now: now, warnBefore: 600) == state)
        #expect(Tokens.resolve(state, dueAt: due(in: 60), now: now, warnBefore: 600) == state)
        #expect(Tokens.resolve(state, dueAt: nil, now: now, warnBefore: 600) == state)
    }

    @Test("Without a due time the reported state is shown")
    func noDueTime() {
        #expect(Tokens.resolve(.onTrack, dueAt: nil, now: now, warnBefore: 600) == .onTrack)
        #expect(Tokens.resolve(.atRisk, dueAt: nil, now: now, warnBefore: nil) == .atRisk)
    }

    @Test("Only a live, running target with a due time and no fixed time follows the clock")
    func ticking() {
        #expect(Tokens.isTicking(state: .onTrack, dueAt: due(in: 60), isLive: true, now: nil))
        #expect(Tokens.isTicking(state: .atRisk, dueAt: due(in: 60), isLive: true, now: nil))
        #expect(!Tokens.isTicking(state: .onTrack, dueAt: due(in: 60), isLive: false, now: nil))
        #expect(!Tokens.isTicking(state: .onTrack, dueAt: due(in: 60), isLive: true, now: now))
        #expect(!Tokens.isTicking(state: .onTrack, dueAt: nil, isLive: true, now: nil))
        for state in [State.breached, .paused, .met, .notApplicable] {
            #expect(!Tokens.isTicking(state: state, dueAt: due(in: 60), isLive: true, now: nil))
        }
    }

    // MARK: Clock

    @Test("The next tick is the next whole minute before or after the due time")
    func nextTick() {
        let dueAt = due(in: 600)
        #expect(Tokens.nextTick(after: now, dueAt: dueAt, warnBefore: nil) == due(in: 60))
        #expect(Tokens.nextTick(after: due(in: 10.5), dueAt: dueAt, warnBefore: nil) == due(in: 60))
        #expect(Tokens.nextTick(after: due(in: 540), dueAt: dueAt, warnBefore: nil) == dueAt)
        #expect(Tokens.nextTick(after: due(in: 599.9), dueAt: dueAt, warnBefore: nil) == dueAt)
        #expect(Tokens.nextTick(after: dueAt, dueAt: dueAt, warnBefore: nil) == due(in: 660))
        #expect(Tokens.nextTick(after: due(in: 615), dueAt: dueAt, warnBefore: nil) == due(in: 660))
    }

    @Test("The moment a target becomes at risk is a tick of its own")
    func nextTickAtWarningEdge() {
        let dueAt = due(in: 600)
        #expect(Tokens.nextTick(after: now, dueAt: dueAt, warnBefore: 570) == due(in: 30))
        #expect(Tokens.nextTick(after: due(in: 30), dueAt: dueAt, warnBefore: 570) == due(in: 60))
        #expect(Tokens.nextTick(after: now, dueAt: dueAt, warnBefore: 480) == due(in: 60))
        #expect(Tokens.nextTick(after: now, dueAt: dueAt, warnBefore: 7200) == due(in: 60))
        #expect(Tokens.nextTick(after: now, dueAt: dueAt, warnBefore: .infinity) == due(in: 60))
    }

    @Test("The schedule starts now, ticks in ascending order and reaches the due time exactly")
    func schedule() {
        let dueAt = due(in: 150)
        let schedule = MonarchSlaIndicatorSchedule(dueAt: dueAt, warnBefore: 100)
        let entries = Array(schedule.entries(from: now, mode: .normal).prefix(6))
        #expect(entries == [now, due(in: 30), due(in: 50), due(in: 90), dueAt, due(in: 210)])
        #expect(Tokens.resolve(.onTrack, dueAt: dueAt, now: entries[2], warnBefore: 100) == .atRisk)
        #expect(Tokens.resolve(.onTrack, dueAt: dueAt, now: entries[4], warnBefore: 100) == .breached)
    }

    @Test("A target that does not follow the clock has a schedule of one entry")
    func scheduleWithoutDueTime() {
        let schedule = MonarchSlaIndicatorSchedule(dueAt: nil, warnBefore: 100)
        #expect(Array(schedule.entries(from: now, mode: .normal).prefix(5)) == [now])
    }

    // MARK: Appearance

    @Test("Chip colours follow the state")
    func chipAppearance() {
        let onTrack = Tokens.appearance(state: .onTrack, variant: .chip, isHighContrast: false)
        #expect(onTrack.background == nil)
        #expect(onTrack.foreground == \MonarchColorTokens.textMuted)
        #expect(onTrack.border == nil)

        let atRisk = Tokens.appearance(state: .atRisk, variant: .chip, isHighContrast: false)
        #expect(atRisk.background == \MonarchColorTokens.warningSubtle)
        #expect(atRisk.foreground == \MonarchColorTokens.warningStrong)
        #expect(atRisk.border == \MonarchColorTokens.warningBorder)

        let breached = Tokens.appearance(state: .breached, variant: .chip, isHighContrast: false)
        #expect(breached.background == \MonarchColorTokens.dangerSolid)
        #expect(breached.foreground == \MonarchColorTokens.dangerOnSolid)

        let paused = Tokens.appearance(state: .paused, variant: .chip, isHighContrast: false)
        #expect(paused.background == \MonarchColorTokens.bgMuted)
        #expect(paused.foreground == \MonarchColorTokens.textMuted)

        let met = Tokens.appearance(state: .met, variant: .chip, isHighContrast: false)
        #expect(met.background == nil)
        #expect(met.foreground == \MonarchColorTokens.successStrong)

        let absent = Tokens.appearance(state: .notApplicable, variant: .chip, isHighContrast: false)
        #expect(absent.background == nil)
        #expect(absent.foreground == \MonarchColorTokens.textSubtle)
    }

    @Test("The text variant has no surface and keeps the colour of the state", arguments: State.allCases)
    func textAppearance(state: State) {
        for isHighContrast in [false, true] {
            let text = Tokens.appearance(state: state, variant: .text, isHighContrast: isHighContrast)
            #expect(text.background == nil)
            #expect(text.border == nil)
        }
        let text = Tokens.appearance(state: state, variant: .text, isHighContrast: false)
        let chip = Tokens.appearance(state: state, variant: .chip, isHighContrast: false)
        if state == .breached {
            #expect(text.foreground == \MonarchColorTokens.dangerStrong)
        } else {
            #expect(text.foreground == chip.foreground)
        }
    }

    @Test("In high contrast every chip has a border or a solid fill", arguments: State.allCases)
    func highContrastChip(state: State) {
        let chip = Tokens.appearance(state: state, variant: .chip, isHighContrast: true)
        #expect(chip.border != nil || chip.background == \MonarchColorTokens.dangerSolid)
    }

    @Test("Every state but the absent one has an icon, and the absent one is lighter")
    func symbolsAndWeight() {
        for state in State.allCases where state != .notApplicable {
            #expect(Tokens.symbol(for: state) != nil)
            #expect(Tokens.fontWeight(for: state) == .medium)
        }
        #expect(Tokens.symbol(for: .notApplicable) == nil)
        #expect(Tokens.fontWeight(for: .notApplicable) == .regular)
        #expect(Tokens.symbol(for: .breached) != Tokens.symbol(for: .met))
        #expect(Tokens.symbol(for: .paused) != Tokens.symbol(for: .onTrack))
    }

    // MARK: Text

    @Test("Durations are whole minutes, rounded down, in at most two units")
    func durations() {
        func text(_ seconds: TimeInterval) -> String { Tokens.durationText(seconds, locale: english) }
        #expect(text(0) == "<1m")
        #expect(text(59.9) == "<1m")
        #expect(text(60) == "1m")
        #expect(text(45 * 60 + 59) == "45m")
        #expect(text(3600) == "1h")
        #expect(text(2 * 3600 + 15 * 60 + 40) == "2h 15m")
        #expect(text(86400) == "1d")
        #expect(text(3 * 86400 + 4 * 3600 + 59 * 60) == "3d 4h")
        #expect(text(3 * 86400 + 5 * 60) == "3d")
        #expect(text(-2 * 3600) == "2h")
        #expect(text(.infinity) == "<1m")
    }

    @Test("Spoken durations write the units out")
    func spokenDurations() {
        #expect(Tokens.durationText(2 * 3600 + 15 * 60, isSpoken: true, locale: english) == "2 hours, 15 minutes")
        #expect(Tokens.durationText(60, isSpoken: true, locale: english) == "1 minute")
        #expect(Tokens.durationText(20, isSpoken: true, locale: english) == "less than a minute")
    }

    @Test("A duration formatter of the app replaces the built-in one, shown and spoken")
    func customDuration() {
        let custom = MonarchSlaIndicatorLabels(duration: { "\(Int($0 / 60)) min" })
        #expect(Tokens.durationText(8100, labels: custom, locale: english) == "135 min")
        #expect(Tokens.durationText(8100, isSpoken: true, labels: custom, locale: english) == "135 min")
        #expect(Tokens.durationText(-30, labels: custom, locale: english) == "0 min")
    }

    @Test("The visible text is the time left, the time overdue, or the state")
    func visibleText() {
        func text(_ state: State, dueIn: TimeInterval?, showsNotApplicable: Bool = false) -> String {
            Tokens.visibleText(
                state: state,
                dueAt: dueIn.map(due(in:)),
                now: now,
                showsNotApplicable: showsNotApplicable,
                labels: labels,
                locale: english
            )
        }
        #expect(text(.onTrack, dueIn: 8100) == "2h 15m left")
        #expect(text(.atRisk, dueIn: 30) == "<1m left")
        #expect(text(.breached, dueIn: -2400) == "Overdue 40m")
        #expect(text(.breached, dueIn: 600) == "Breached")
        #expect(text(.breached, dueIn: nil) == "Breached")
        #expect(text(.onTrack, dueIn: nil) == "On track")
        #expect(text(.atRisk, dueIn: nil) == "At risk")
        #expect(text(.paused, dueIn: 600) == "Paused")
        #expect(text(.met, dueIn: -600) == "Met")
        #expect(text(.notApplicable, dueIn: nil) == "—")
        #expect(text(.notApplicable, dueIn: 600) == "—")
        #expect(text(.notApplicable, dueIn: nil, showsNotApplicable: true) == "No SLA")
    }

    @Test("The label names the target and the time in words; the value carries the state and the due date")
    func accessibility() {
        let dueAt = due(in: 8100)
        let label = Tokens.accessibilityLabel(
            state: .onTrack,
            dueAt: dueAt,
            now: now,
            target: "First response",
            labels: labels,
            locale: english
        )
        #expect(label == "First response, 2 hours, 15 minutes left")

        let value = Tokens.accessibilityValue(
            state: .onTrack,
            dueAt: dueAt,
            now: now,
            dueText: "Nov 13, 2026 at 3:00 PM",
            hasEarlierBreach: true,
            labels: labels
        )
        #expect(value == "On track, due Nov 13, 2026 at 3:00 PM, an earlier target was breached")
    }

    @Test("Without a countdown the state is the label and is not repeated in the value")
    func accessibilityWithoutCountdown() {
        let label = Tokens.accessibilityLabel(
            state: .paused,
            dueAt: due(in: 600),
            now: now,
            target: nil,
            labels: labels,
            locale: english
        )
        #expect(label == "Paused")
        let value = Tokens.accessibilityValue(
            state: .paused,
            dueAt: due(in: 600),
            now: now,
            dueText: "tomorrow",
            hasEarlierBreach: false,
            labels: labels
        )
        #expect(value == "due tomorrow")

        let absent = Tokens.accessibilityLabel(
            state: .notApplicable,
            dueAt: nil,
            now: now,
            target: "Resolution",
            labels: labels,
            locale: english
        )
        #expect(absent == "Resolution, No SLA")
    }

    @Test("The due date is not read for met and absent targets, and a breach does not mention an earlier one")
    func accessibilityValueEdges() {
        func value(_ state: State, earlier: Bool) -> String {
            Tokens.accessibilityValue(
                state: state,
                dueAt: due(in: -600),
                now: now,
                dueText: "today",
                hasEarlierBreach: earlier,
                labels: labels
            )
        }
        #expect(value(.met, earlier: false).isEmpty)
        #expect(value(.notApplicable, earlier: false).isEmpty)
        #expect(value(.met, earlier: true) == "an earlier target was breached")
        #expect(value(.breached, earlier: true) == "Breached, due today")
        #expect(!Tokens.readsDueDate(state: .onTrack, dueAt: nil))
        #expect(Tokens.readsDueDate(state: .paused, dueAt: now))
    }

    @Test("The due date is written in the given locale and time zone")
    func dueText() throws {
        let utc = try #require(TimeZone(identifier: "UTC"))
        let date = Date(timeIntervalSince1970: 1_794_582_000)
        let text = Tokens.dueText(date, locale: english, timeZone: utc)
        #expect(text.contains("2026"))
        #expect(text.contains("Nov"))
        #expect(text != Tokens.dueText(date, locale: Locale(identifier: "de_DE"), timeZone: utc))
    }

    @Test("A change of state is announced with the target, or as SLA without one")
    func announcement() {
        #expect(
            Tokens.changeAnnouncement(to: .atRisk, target: "First response", labels: labels)
                == "First response: At risk"
        )
        #expect(Tokens.changeAnnouncement(to: .breached, target: nil, labels: labels) == "SLA Breached")
    }

    @Test("Every string can be replaced")
    func overrides() {
        let custom = MonarchSlaIndicatorLabels(
            onTrack: "Im Plan",
            atRisk: "Gefährdet",
            breached: "Verletzt",
            paused: "Pausiert",
            met: "Eingehalten",
            notApplicable: "Kein SLA",
            notApplicableMark: "n/a",
            remaining: { "noch \($0)" },
            overdue: { "\($0) überfällig" },
            due: { "fällig \($0)" },
            earlierBreach: "ein früheres Ziel wurde verletzt",
            changed: { state, target in "\(target ?? "SLA") ist jetzt \(state)" }
        )
        let names = State.allCases.map { Tokens.name(of: $0, labels: custom) }
        #expect(names == ["Im Plan", "Gefährdet", "Verletzt", "Pausiert", "Eingehalten", "Kein SLA"])

        func visible(_ state: State, dueIn: TimeInterval?) -> String {
            Tokens.visibleText(
                state: state,
                dueAt: dueIn.map(due(in:)),
                now: now,
                showsNotApplicable: false,
                labels: custom,
                locale: english
            )
        }
        #expect(visible(.onTrack, dueIn: 3600) == "noch 1h")
        #expect(visible(.breached, dueIn: -3600) == "1h überfällig")
        #expect(visible(.notApplicable, dueIn: nil) == "n/a")

        let value = Tokens.accessibilityValue(
            state: .paused,
            dueAt: due(in: 60),
            now: now,
            dueText: "morgen",
            hasEarlierBreach: true,
            labels: custom
        )
        #expect(value == "fällig morgen, ein früheres Ziel wurde verletzt")
        #expect(Tokens.changeAnnouncement(to: .met, target: nil, labels: custom) == "SLA ist jetzt Eingehalten")
    }

    @Test("The built-in strings resolve")
    func strings() {
        var all = State.allCases.map(MonarchSlaIndicatorStrings.name(of:))
        #expect(Set(all).count == State.allCases.count)
        all += [
            MonarchSlaIndicatorStrings.notApplicableMark,
            MonarchSlaIndicatorStrings.lessThanMinute,
            MonarchSlaIndicatorStrings.lessThanMinuteSpoken,
            MonarchSlaIndicatorStrings.earlierBreach,
            MonarchSlaIndicatorStrings.remaining("1h"),
            MonarchSlaIndicatorStrings.overdue("1h"),
            MonarchSlaIndicatorStrings.due("today"),
            MonarchSlaIndicatorStrings.changed("Met", target: nil),
            MonarchSlaIndicatorStrings.changed("Met", target: "Resolution"),
        ]
        for string in all {
            #expect(!string.contains("monarch."))
            #expect(!string.contains("%"))
        }
    }
}
