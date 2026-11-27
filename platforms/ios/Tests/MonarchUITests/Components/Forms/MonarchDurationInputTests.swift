import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDurationInputFixtures: ComponentFixtures {
    static let id = "duration-input"
    static let fixtures = [
        Fixture("value with helper") {
            MonarchDurationInput("Estimate", minutes: .constant(90), helper: "Type 1h 30m, 90m or 1.5h.")
        },
        Fixture("empty with the built-in prompt") {
            MonarchDurationInput("Time logged", minutes: .constant(nil))
        },
        Fixture("bounds and a small step") {
            MonarchDurationInput("Buffer", minutes: .constant(10), minimum: 5, maximum: 120, step: 5)
        },
        Fixture("days as the largest unit, work days") {
            MonarchDurationInput("Response target", minutes: .constant(1_680), largestUnit: .days, minutesPerDay: 480)
        },
        Fixture("at the maximum") {
            MonarchDurationInput("Meeting", minutes: .constant(120), maximum: 120)
        },
        Fixture("error from the app, long label, no stepper") {
            MonarchDurationInput(
                "Time the team expects to spend on the migration before review",
                minutes: .constant(2_400),
                helper: "Hidden while the error shows.",
                error: "Longer than the sprint.",
                showsStepper: false
            )
        },
        Fixture("custom format") {
            MonarchDurationInput("Length", minutes: .constant(75), format: { "\($0) min" })
        },
        Fixture("disabled") {
            MonarchDurationInput("Estimate", minutes: .constant(45), helper: "Locked.").disabled(true)
        },
    ]
}

@Suite("MonarchDurationInput")
struct MonarchDurationInputTests {
    private typealias Math = MonarchDurationMath
    private typealias Tokens = MonarchDurationInputTokens

    @Test("Reads the forms people type")
    func parsing() {
        #expect(Math.parse("90") == .minutes(90))
        #expect(Math.parse("90m") == .minutes(90))
        #expect(Math.parse("1h 30m") == .minutes(90))
        #expect(Math.parse("1h30") == .minutes(90))
        #expect(Math.parse("1h30m") == .minutes(90))
        #expect(Math.parse("1.5h") == .minutes(90))
        #expect(Math.parse("1,5h") == .minutes(90))
        #expect(Math.parse("1:30") == .minutes(90))
        #expect(Math.parse("2 hours 15 minutes") == .minutes(135))
        #expect(Math.parse("1d 4h") == .minutes(1_680))
        #expect(Math.parse("  45 MIN ") == .minutes(45))
        #expect(Math.parse("1.5") == .minutes(2), "a bare fraction is rounded to whole minutes")
        #expect(Math.parse("0") == .minutes(0))
    }

    @Test("Days follow minutesPerDay")
    func workDays() {
        #expect(Math.parse("1d", minutesPerDay: 480) == .minutes(480))
        #expect(Math.parse("2d 1h", minutesPerDay: 480) == .minutes(1_020))
    }

    @Test("Empty text is empty; text it cannot read is invalid")
    func unreadable() {
        #expect(Math.parse("") == .empty)
        #expect(Math.parse("   ") == .empty)
        #expect(Math.parse("soon") == .invalid)
        #expect(Math.parse("1x") == .invalid)
        #expect(Math.parse("1h 30x") == .invalid)
        #expect(Math.parse("30m 15") == .invalid, "a bare number counts as minutes only after hours")
        #expect(Math.parse("1:75") == .invalid)
        #expect(Math.parse("1:5") == .invalid)
        #expect(Math.parse(".5h") == .invalid)
        #expect(Math.parse("-30") == .invalid)
        #expect(Math.parse("h") == .invalid)
    }

    @Test("Writes minutes one way")
    func formatting() {
        #expect(Math.format(0) == "0m")
        #expect(Math.format(45) == "45m")
        #expect(Math.format(60) == "1h")
        #expect(Math.format(90) == "1h 30m")
        #expect(Math.format(1_560) == "26h")
        #expect(Math.format(1_560, largestUnit: .days) == "1d 2h")
        #expect(Math.format(1_680, largestUnit: .days, minutesPerDay: 480) == "3d 4h")
        #expect(Math.format(-5) == "0m")
        #expect(Math.format(1_441, largestUnit: .days) == "1d 1m")
    }

    @Test("Steps snap to the grid and stop at the bounds")
    func stepping() {
        #expect(Math.stepped(50, by: 15, direction: 1, minimum: 0, maximum: nil) == 60)
        #expect(Math.stepped(50, by: 15, direction: -1, minimum: 0, maximum: nil) == 45)
        #expect(Math.stepped(60, by: 15, direction: 1, minimum: 0, maximum: nil) == 75)
        #expect(Math.stepped(60, by: 15, direction: -1, minimum: 0, maximum: nil) == 45)
        #expect(Math.stepped(10, by: 15, direction: -1, minimum: 0, maximum: nil) == 0)
        #expect(Math.stepped(0, by: 15, direction: -1, minimum: 0, maximum: nil) == 0)
        #expect(Math.stepped(115, by: 15, direction: 1, minimum: 0, maximum: 120) == 120)
        #expect(Math.stepped(7, by: 5, direction: -1, minimum: 5, maximum: 120) == 5)
        #expect(
            Math.stepped(90, by: 60, direction: 1, minimum: 0, maximum: nil) == 120,
            "page up goes to the next hour"
        )
        #expect(Math.stepped(90, by: 60, direction: -1, minimum: 0, maximum: nil) == 60)
        #expect(Math.stepped(10, by: 0, direction: 1, minimum: 0, maximum: nil) == 11, "a zero step moves by one")
    }

    @Test("A step starts from the text when it reads, else the saved value, else the minimum")
    func stepBase() {
        #expect(Math.stepBase(parsed: .minutes(50), saved: 90, minimum: 0) == 50)
        #expect(Math.stepBase(parsed: .invalid, saved: 90, minimum: 0) == 90)
        #expect(Math.stepBase(parsed: .empty, saved: nil, minimum: 5) == 5)
        #expect(Tokens.canIncrement(nil, maximum: 10))
        #expect(!Tokens.canIncrement(10, maximum: 10))
        #expect(Tokens.canIncrement(500, maximum: nil))
        #expect(Tokens.canDecrement(nil, minimum: 0))
        #expect(!Tokens.canDecrement(0, minimum: 0))
        #expect(Tokens.canDecrement(6, minimum: 5))
    }

    @Test("Reading the text saves a duration inside the bounds and rejects the rest")
    func commit() {
        #expect(Tokens.commit(.empty, minimum: 5, maximum: 120) == .save(nil))
        #expect(Tokens.commit(.minutes(30), minimum: 5, maximum: 120) == .save(30))
        #expect(Tokens.commit(.minutes(5), minimum: 5, maximum: 120) == .save(5))
        #expect(Tokens.commit(.minutes(120), minimum: 5, maximum: 120) == .save(120))
        #expect(Tokens.commit(.minutes(121), minimum: 5, maximum: 120) == .reject(.outOfRange))
        #expect(Tokens.commit(.minutes(2), minimum: 5, maximum: nil) == .reject(.outOfRange))
        #expect(Tokens.commit(.invalid, minimum: 0, maximum: nil) == .reject(.invalid))
    }

    @Test("Messages name the bounds in the field's format")
    func messages() {
        let labels = MonarchDurationInputLabels()
        func message(_ problem: Tokens.Problem, _ minimum: Int, _ maximum: Int?) -> String {
            Tokens.message(problem, minimum: minimum, maximum: maximum, labels: labels) { Math.format($0) }
        }
        #expect(message(.invalid, 0, nil) == "Use a duration like 1h 30m, 90m or 1.5h.")
        #expect(message(.outOfRange, 15, 120) == "Enter between 15m and 2h.")
        #expect(message(.outOfRange, 15, nil) == "Enter at least 15m.")
        #expect(message(.outOfRange, 0, 120) == "Enter at most 2h.")
        let custom = MonarchDurationInputLabels(invalid: "Nope", range: { low, high in "\(low)-\(high ?? -1)" })
        #expect(Tokens.message(.invalid, minimum: 0, maximum: nil, labels: custom) { "\($0)" } == "Nope")
        #expect(Tokens.message(.outOfRange, minimum: 5, maximum: 9, labels: custom) { "\($0)" } == "5-9")
    }

    @Test("The app's error wins; VoiceOver reads the error before the helper")
    func errors() {
        let labels = MonarchDurationInputLabels()
        #expect(Tokens.shownError(error: "App", problem: "Field") == "App")
        #expect(Tokens.shownError(error: "", problem: "Field") == "Field")
        #expect(Tokens.shownError(error: nil, problem: nil) == nil)
        #expect(Tokens.accessibilityHint(helper: "Help", error: nil, labels: labels) == "Help")
        #expect(Tokens.accessibilityHint(helper: "Help", error: "Too long", labels: labels) == "Error: Too long")
        #expect(Tokens.accessibilityHint(helper: nil, error: nil, labels: labels).isEmpty)
        let custom = MonarchDurationInputLabels(errorHint: { "Fehler: \($0)" })
        #expect(Tokens.accessibilityHint(helper: nil, error: "Zu lang", labels: custom) == "Fehler: Zu lang")
    }

    @Test("The spoken value is the duration spelled out in the locale")
    func spokenValue() {
        let english = Locale(identifier: "en_US")
        let labels = MonarchDurationInputLabels()
        #expect(Tokens.valueText(90, locale: english, labels: labels) == "1 hour, 30 minutes")
        #expect(Tokens.valueText(120, locale: english, labels: labels) == "2 hours")
        #expect(Tokens.valueText(45, locale: english, labels: labels) == "45 minutes")
        #expect(Tokens.valueText(1, locale: english, labels: labels) == "1 minute")
        #expect(Tokens.valueText(0, locale: english, labels: labels) == "0 minutes")
        #expect(Tokens.valueText(1_560, locale: english, labels: labels) == "26 hours")
        #expect(Tokens.valueText(nil, locale: english, labels: labels).isEmpty)
        let german = Tokens.valueText(90, locale: Locale(identifier: "de_DE"), labels: labels)
        #expect(german.contains("Stunde"))
        let custom = MonarchDurationInputLabels(valueText: { "\($0) min" })
        #expect(Tokens.valueText(90, locale: english, labels: custom) == "90 min")
    }

    @Test("The field takes its colours and its height from the text field")
    func appearance() {
        #expect(Tokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.minHeight >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDurationInputStrings.prompt == "e.g. 1h 30m")
        #expect(MonarchDurationInputStrings.invalid == "Use a duration like 1h 30m, 90m or 1.5h.")
        #expect(MonarchDurationInputStrings.between("15m", "2h") == "Enter between 15m and 2h.")
        #expect(MonarchDurationInputStrings.atLeast("15m") == "Enter at least 15m.")
        #expect(MonarchDurationInputStrings.atMost("2h") == "Enter at most 2h.")
        #expect(MonarchDurationInputStrings.error("Too long") == "Error: Too long")
    }

    #if os(iOS)
        @Test("The field is at least as tall as the touch target")
        @MainActor
        func hitTarget() {
            let input = MonarchDurationInput("Estimate", minutes: .constant(90), showsStepper: false)
            #expect(Host.layout(input).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
