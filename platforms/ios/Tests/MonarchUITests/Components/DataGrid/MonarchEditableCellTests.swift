import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEditableCellFixtures: ComponentFixtures {
    static let id = "editable-cell"
    private static let statuses = [
        MonarchEditableCellOption("Open", value: "open"),
        MonarchEditableCellOption("In review", value: "review"),
        MonarchEditableCellOption("Done", value: "done", isDisabled: true),
    ]
    private static let day = Date(timeIntervalSince1970: 1_794_614_400)

    static let fixtures = [
        Fixture("text") {
            MonarchEditableCell("Title", value: .constant(.text("Quarterly review"))) { _ in }
        },
        Fixture("text, long") {
            MonarchEditableCell(
                "Title",
                value: .constant(.text("A very long title that wraps onto several lines at the largest text size"))
            ) { _ in }
        },
        Fixture("empty with a placeholder") {
            MonarchEditableCell("Notes", value: .constant(.empty), placeholder: "Add a note") { _ in }
        },
        Fixture("number, numeric alignment") {
            MonarchEditableCell("Quantity", value: .constant(.number(1_234.5)), kind: .number, minimum: 1) { _ in }
        },
        Fixture("select") {
            MonarchEditableCell("Status", value: .constant(.text("review")), kind: .select, options: statuses) { _ in }
        },
        Fixture("select, required and empty") {
            MonarchEditableCell("Status", value: .constant(.empty), kind: .select, options: statuses, isRequired: true)
            { _ in }
        },
        Fixture("date") {
            MonarchEditableCell("Due", value: .constant(.date(day)), kind: .date) { _ in }
        },
        Fixture("date, empty") {
            MonarchEditableCell("Due", value: .constant(.empty), kind: .date, placeholder: "No due date") { _ in }
        },
        Fixture("editing text") {
            MonarchEditableCell("Title", value: .constant(.text("Quarterly review")), startsEditing: true) { _ in }
        },
        Fixture("editing a number") {
            MonarchEditableCell("Quantity", value: .constant(.number(12)), kind: .number, startsEditing: true) { _ in }
        },
        Fixture("disabled") {
            MonarchEditableCell("Owner", value: .constant(.text("Sam Lee"))) { _ in }.disabled(true)
        },
        Fixture("disabled select") {
            MonarchEditableCell("Status", value: .constant(.text("open")), kind: .select, options: statuses) { _ in }
                .disabled(true)
        },
    ]
}

@Suite("MonarchEditableCell")
@MainActor
struct MonarchEditableCellTests {
    private struct Described: LocalizedError {
        var errorDescription: String? { "The title is already in use." }
    }

    private struct Plain: Error {}

    private let us = Locale(identifier: "en_US")
    private let german = Locale(identifier: "de_DE")
    private var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = us
        return calendar
    }

    private func rules(
        _ kind: MonarchEditableCellKind,
        isRequired: Bool = false,
        minimum: Double? = nil,
        maximum: Double? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil
    ) -> MonarchEditableCellRules {
        MonarchEditableCellRules(
            kind: kind,
            isRequired: isRequired,
            minimum: minimum,
            maximum: maximum,
            minimumDate: minimumDate,
            maximumDate: maximumDate,
            calendar: calendar,
            locale: us
        )
    }

    private func day(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day)) ?? .distantPast
    }

    private func check(
        _ next: MonarchEditableCellValue,
        saved: MonarchEditableCellValue,
        _ rules: MonarchEditableCellRules,
        labels: MonarchEditableCellLabels = MonarchEditableCellLabels(),
        validate: (@MainActor (MonarchEditableCellValue) -> String?)? = nil
    ) -> MonarchEditableCellCheck {
        MonarchEditableCellTokens.check(next, saved: saved, rules: rules, labels: labels, validate: validate)
    }

    // MARK: Parsing

    @Test("Drafts parse into the cell's kind; an empty draft is empty")
    func parses() {
        let parse = { (draft: String, kind: MonarchEditableCellKind, locale: Locale) in
            MonarchEditableCellTokens.parse(draft, kind: kind, locale: locale, invalidNumber: "Enter a number.")
        }
        #expect(parse("  Plan ", .text, us) == .success(.text("Plan")))
        #expect(parse("   ", .text, us) == .success(.empty))
        #expect(parse("12", .number, us) == .success(.number(12)))
        #expect(parse("-3.5", .number, us) == .success(.number(-3.5)))
        #expect(parse("1,234.5", .number, us) == .success(.number(1_234.5)))
        #expect(parse("1 234", .number, us) == .success(.number(1_234)))
        #expect(parse("12,5", .number, german) == .success(.number(12.5)))
        #expect(parse("12abc", .number, us) == .failure(MonarchEditableCellParseError(message: "Enter a number.")))
        #expect(parse("abc", .number, us) == .failure(MonarchEditableCellParseError(message: "Enter a number.")))
        #expect(parse("", .number, us) == .success(.empty))
    }

    @Test("The draft starts from the value, numbers without grouping in the locale")
    func drafts() {
        #expect(MonarchEditableCellTokens.draft(from: .text("Plan"), locale: us) == "Plan")
        #expect(MonarchEditableCellTokens.draft(from: .number(1_234.5), locale: us) == "1234.5")
        #expect(MonarchEditableCellTokens.draft(from: .number(1_234.5), locale: german) == "1234,5")
        #expect(MonarchEditableCellTokens.draft(from: .empty, locale: us).isEmpty)
        let draft = MonarchEditableCellTokens.draft(from: .number(1_234.5), locale: german)
        let parsed = MonarchEditableCellTokens.parse(draft, kind: .number, locale: german, invalidNumber: "x")
        #expect(parsed == .success(.number(1_234.5)), "a draft parses back to its value")
    }

    // MARK: Checking

    @Test("Required, unchanged, bounds and the app's rule are checked in order")
    func checks() {
        #expect(check(.empty, saved: .text("Plan"), rules(.text, isRequired: true)) == .invalid("Enter a value."))
        #expect(check(.empty, saved: .empty, rules(.text, isRequired: true)) == .invalid("Enter a value."))
        #expect(check(.text(""), saved: .empty, rules(.text)) == .unchanged)
        #expect(check(.number(12), saved: .number(12), rules(.number, minimum: 20)) == .unchanged)
        #expect(check(.number(0), saved: .number(12), rules(.number, minimum: 1)) == .invalid("Enter 1 or more."))
        #expect(
            check(.number(1_500), saved: .number(12), rules(.number, maximum: 1_000))
                == .invalid("Enter 1,000 or less.")
        )
        #expect(check(.number(50), saved: .number(12), rules(.number, minimum: 1, maximum: 99)) == .valid(.number(50)))
        let refuse: @MainActor (MonarchEditableCellValue) -> String? = {
            $0 == .text("X") ? "Use at least 2 characters." : nil
        }
        #expect(
            check(.text("X"), saved: .text("Sam"), rules(.text), validate: refuse)
                == .invalid("Use at least 2 characters.")
        )
        #expect(
            check(.text("Sam Lee"), saved: .text("Sam"), rules(.text), validate: refuse) == .valid(.text("Sam Lee"))
        )
        #expect(check(.text("Sam"), saved: .text("Sam"), rules(.text), validate: { _ in "never" }) == .unchanged)
    }

    @Test("Dates compare and are bounded by day")
    func dates() {
        let noon = day(2026, 11, 14).addingTimeInterval(12 * 3_600)
        #expect(check(.date(noon), saved: .date(day(2026, 11, 14)), rules(.date)) == .unchanged)
        let early = check(.date(day(2026, 11, 1)), saved: .empty, rules(.date, minimumDate: day(2026, 11, 10)))
        #expect(early == .invalid("Enter Nov 10, 2026 or more."))
        let late = check(.date(day(2026, 12, 1)), saved: .empty, rules(.date, maximumDate: day(2026, 11, 30)))
        #expect(late == .invalid("Enter Nov 30, 2026 or less."))
        let inside = check(.date(day(2026, 11, 20)), saved: .empty, rules(.date, minimumDate: day(2026, 11, 10)))
        #expect(inside == .valid(.date(day(2026, 11, 20))))
        let start = MonarchEditableCellTokens.initialDay(
            today: day(2026, 11, 14).addingTimeInterval(3_600),
            minimumDate: day(2026, 12, 1),
            maximumDate: nil,
            calendar: calendar
        )
        #expect(start == day(2026, 12, 1), "an empty date editor starts inside the range")
    }

    @Test("Labels replace the built-in messages")
    func labelOverrides() {
        let labels = MonarchEditableCellLabels(
            required: "Required.",
            belowMinimum: { "At least \($0)." },
            aboveMaximum: { "At most \($0)." },
            error: { "Problem: \($0)" }
        )
        #expect(
            check(.empty, saved: .text("a"), rules(.text, isRequired: true), labels: labels) == .invalid("Required.")
        )
        #expect(check(.number(0), saved: .empty, rules(.number, minimum: 1), labels: labels) == .invalid("At least 1."))
        #expect(check(.number(9), saved: .empty, rules(.number, maximum: 5), labels: labels) == .invalid("At most 5."))
        #expect(MonarchEditableCellTokens.errorHint("Bad", labels: labels) == "Problem: Bad")
        #expect(MonarchEditableCellTokens.errorHint(nil, labels: labels).isEmpty)
    }

    // MARK: Display and accessibility

    @Test("View mode shows the option label, the number or day in the locale, or the placeholder")
    func displays() {
        let options = [MonarchEditableCellOption("In review", value: "review")]
        let show = { (value: MonarchEditableCellValue, locale: Locale) in
            MonarchEditableCellTokens.display(value, options: options, calendar: calendar, locale: locale, format: nil)
        }
        #expect(show(.text("review"), us) == "In review")
        #expect(show(.text("unknown"), us) == "unknown")
        #expect(show(.number(1_234.5), us) == "1,234.5")
        #expect(show(.number(1_234.5), german) == "1.234,5")
        #expect(show(.date(day(2026, 3, 31)), us) == "Mar 31, 2026")
        #expect(show(.empty, us) == nil)
        #expect(show(.text(""), us) == nil)
        let custom = MonarchEditableCellTokens.display(
            .number(3),
            options: [],
            calendar: calendar,
            locale: us,
            format: { _ in "3 seats" }
        )
        #expect(custom == "3 seats")
        #expect(MonarchEditableCellTokens.shownText(display: nil, placeholder: "Add a note") == "Add a note")
        #expect(MonarchEditableCellTokens.shownText(display: "Plan", placeholder: "Add a note") == "Plan")
    }

    @Test("Spoken value, hint and failure messages")
    func spoken() {
        let none = MonarchEditableCellLabels()
        #expect(MonarchEditableCellTokens.accessibilityValue(shown: "Plan", isSaving: false, labels: none) == "Plan")
        #expect(MonarchEditableCellTokens.accessibilityValue(shown: "Plan", isSaving: true, labels: none) == "Saving")
        #expect(MonarchEditableCellTokens.editHint(none) == "Edit")
        #expect(MonarchEditableCellTokens.editHint(MonarchEditableCellLabels(edit: "Change")) == "Change")
        #expect(MonarchEditableCellTokens.errorHint("Enter a number.", labels: none) == "Error: Enter a number.")
        #expect(
            MonarchEditableCellTokens.failureMessage(for: Described(), labels: none) == "The title is already in use."
        )
        #expect(MonarchEditableCellTokens.failureMessage(for: Plain(), labels: none) == "Couldn’t save. Try again.")
        #expect(
            MonarchEditableCellTokens.failureMessage(for: Plain(), labels: MonarchEditableCellLabels(saveFailed: "No."))
                == "No."
        )
    }

    @Test("Colours by state, alignment and the None choice")
    func appearance() {
        #expect(MonarchEditableCellTokens.viewForeground(isEmpty: false, isEnabled: true) == \.textDefault)
        #expect(MonarchEditableCellTokens.viewForeground(isEmpty: true, isEnabled: true) == \.textSubtle)
        #expect(MonarchEditableCellTokens.viewForeground(isEmpty: false, isEnabled: false) == \.textDisabled)
        #expect(MonarchEditableCellTokens.border(hasError: true) == \.dangerStrong)
        #expect(MonarchEditableCellTokens.border(hasError: false) == \.borderInput)
        #expect(MonarchEditableCellTokens.isNumeric(nil, kind: .number))
        #expect(!MonarchEditableCellTokens.isNumeric(nil, kind: .text))
        #expect(MonarchEditableCellTokens.isNumeric(true, kind: .text))
        #expect(MonarchEditableCellTokens.showsNoValueChoice(isRequired: false, value: .text("open")))
        #expect(!MonarchEditableCellTokens.showsNoValueChoice(isRequired: true, value: .text("open")))
        #expect(MonarchEditableCellTokens.showsNoValueChoice(isRequired: true, value: .empty))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchEditableCellStrings.edit, MonarchEditableCellStrings.saving, MonarchEditableCellStrings.cancel,
            MonarchEditableCellStrings.save, MonarchEditableCellStrings.invalidNumber,
            MonarchEditableCellStrings.required,
            MonarchEditableCellStrings.noValue, MonarchEditableCellStrings.saveFailed,
            MonarchEditableCellStrings.belowMinimum("1"), MonarchEditableCellStrings.aboveMaximum("2"),
            MonarchEditableCellStrings.error("x"),
        ]
        for string in all { #expect(!string.hasPrefix("monarch.")) }
    }

    // MARK: Session

    private func commit(
        _ session: MonarchEditableCellSession,
        _ parsed: Result<MonarchEditableCellValue, MonarchEditableCellParseError>,
        saved: MonarchEditableCellValue,
        rules: MonarchEditableCellRules,
        onCommit: @MainActor (MonarchEditableCellValue) async throws -> Void = { _ in }
    ) async -> MonarchEditableCellSession.Outcome {
        await session.commit(
            parsed,
            check: {
                MonarchEditableCellTokens.check(
                    $0,
                    saved: saved,
                    rules: rules,
                    labels: MonarchEditableCellLabels(),
                    validate: nil
                )
            },
            message: { MonarchEditableCellTokens.failureMessage(for: $0, labels: MonarchEditableCellLabels()) },
            onCommit: onCommit
        )
    }

    @Test("A valid draft saves and closes the editor")
    func saves() async {
        let session = MonarchEditableCellSession()
        session.begin(draft: "12")
        session.draft = "40"
        var saved: [MonarchEditableCellValue] = []
        let outcome = await commit(session, .success(.number(40)), saved: .number(12), rules: rules(.number)) {
            saved.append($0)
        }
        #expect(outcome == .saved(.number(40)))
        #expect(saved == [.number(40)])
        #expect(!session.isEditing)
        #expect(!session.isSaving)
        #expect(session.pending == nil)
    }

    @Test("An unreadable or refused draft keeps the editor open and is not retried on focus loss")
    func refuses() async {
        let session = MonarchEditableCellSession()
        session.begin(draft: "12")
        session.draft = "abc"
        var calls = 0
        let outcome = await commit(
            session,
            .failure(MonarchEditableCellParseError(message: "Enter a number.")),
            saved: .number(12),
            rules: rules(.number)
        ) { _ in calls += 1 }
        #expect(outcome == .invalid)
        #expect(calls == 0)
        #expect(session.isEditing)
        #expect(session.errorMessage == "Enter a number.")
        #expect(!session.savesOnFocusLoss)
        session.draft = "0"
        #expect(session.savesOnFocusLoss)
        let bounded = await commit(session, .success(.number(0)), saved: .number(12), rules: rules(.number, minimum: 1))
        #expect(bounded == .invalid)
        #expect(session.errorMessage == "Enter 1 or more.")
    }

    @Test("A failed save keeps the draft and shows the error's message; a retry can succeed")
    func fails() async {
        let session = MonarchEditableCellSession()
        session.begin(draft: "Plan")
        session.draft = "New plan"
        let outcome = await commit(session, .success(.text("New plan")), saved: .text("Plan"), rules: rules(.text)) {
            _ in
            throw Described()
        }
        #expect(outcome == .failed)
        #expect(session.isEditing)
        #expect(session.draft == "New plan")
        #expect(session.errorMessage == "The title is already in use.")
        #expect(session.pending == nil, "a chosen value goes back to the saved one")
        let retry = await commit(session, .success(.text("New plan")), saved: .text("Plan"), rules: rules(.text))
        #expect(retry == .saved(.text("New plan")))
        #expect(session.errorMessage == nil)
    }

    @Test("An unchanged value closes without saving; cancel closes and drops the draft")
    func unchangedAndCancel() async {
        let session = MonarchEditableCellSession()
        session.begin(draft: "Plan")
        var calls = 0
        let outcome = await commit(session, .success(.text("Plan")), saved: .text("Plan"), rules: rules(.text)) { _ in
            calls += 1
        }
        #expect(outcome == .unchanged)
        #expect(calls == 0)
        #expect(!session.isEditing)
        session.begin(draft: "Plan")
        session.draft = "Changed"
        session.begin(draft: "Plan")
        #expect(session.draft == "Changed", "opening twice keeps the draft")
        #expect(session.cancel())
        #expect(!session.isEditing)
        #expect(session.draft.isEmpty)
        #expect(!session.cancel(), "cancelling a closed editor does nothing")
    }

    @Test("A select or date choice saves without opening the editor")
    func immediate() async {
        let session = MonarchEditableCellSession()
        let outcome = await commit(session, .success(.text("done")), saved: .text("open"), rules: rules(.select))
        #expect(outcome == .saved(.text("done")))
        let cleared = await commit(
            session,
            .success(.empty),
            saved: .text("open"),
            rules: rules(.select, isRequired: true)
        )
        #expect(cleared == .invalid)
        #expect(session.errorMessage == "Enter a value.")
    }
}
