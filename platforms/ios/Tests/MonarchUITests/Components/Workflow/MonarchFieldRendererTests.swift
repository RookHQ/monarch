import Foundation
import SwiftUI
import Testing
import UniformTypeIdentifiers

@testable import MonarchUI

private enum FieldSamples {
    /// 2026-11-14 09:00 UTC.
    static let today = Date(timeIntervalSince1970: 1_794_646_800)
    static let options = [
        MonarchFormFieldOption(value: "remote", label: "Remote"),
        MonarchFormFieldOption(value: "office", label: "Office"),
        MonarchFormFieldOption(value: "hybrid", label: "Hybrid, with two days a week in the office nearest to you"),
    ]
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        return calendar
    }

    static func field(
        _ type: MonarchFormFieldType,
        label: String = "Question",
        isRequired: Bool = false,
        isReadOnly: Bool = false
    ) -> MonarchFormFieldDefinition {
        MonarchFormFieldDefinition(
            id: type.rawValue,
            type: type,
            label: label,
            description: "Help for this question.",
            isRequired: isRequired,
            options: options,
            isReadOnly: isReadOnly
        )
    }

    @MainActor
    static func render(
        _ field: MonarchFormFieldDefinition,
        _ value: MonarchFormFieldValue?,
        problem: MonarchFormFieldProblem? = nil,
        confidence: Double? = nil
    ) -> some View {
        MonarchFieldRenderer(
            field,
            value: .constant(value),
            problem: problem,
            confidence: confidence,
            today: { today }
        )
        .environment(\.calendar, calendar)
        .environment(\.locale, Locale(identifier: "en_US"))
    }
}

enum MonarchFieldRendererFixtures: ComponentFixtures {
    static let id = "field-renderer"
    static let fixtures = [
        Fixture("text types, empty and answered") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.shortText, label: "Full name", isRequired: true), nil)
                FieldSamples.render(FieldSamples.field(.email, label: "Email"), .text("alex@example.com"))
                FieldSamples.render(FieldSamples.field(.phone, label: "Phone"), .text("+44 20 7946 0000"))
                FieldSamples.render(FieldSamples.field(.url, label: "Website"), nil)
                FieldSamples.render(FieldSamples.field(.number, label: "Years of experience"), .number(7))
            }
        },
        Fixture("long text with long content") {
            FieldSamples.render(
                FieldSamples.field(.longText, label: "Tell us about a project you are proud of and why it mattered"),
                .text(String(repeating: "A long answer that wraps across several lines. ", count: 8))
            )
        },
        Fixture("choices: select, radio, yes or no, checkboxes, multi-select") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.select, label: "Where?"), nil)
                FieldSamples.render(FieldSamples.field(.radio, label: "Where?"), .text("hybrid"))
                FieldSamples.render(FieldSamples.field(.yesNo, label: "Can you relocate?"), .flag(false))
                FieldSamples.render(FieldSamples.field(.checkboxes, label: "Where?"), .values(["remote", "office"]))
                FieldSamples.render(FieldSamples.field(.multiSelect, label: "Where?"), .values(["office"]))
            }
        },
        Fixture("dates and times, empty and set") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.date, label: "Start date"), nil)
                FieldSamples.render(FieldSamples.field(.date, label: "Start date"), .date(FieldSamples.today))
                FieldSamples.render(FieldSamples.field(.time, label: "Best time to call"), nil)
                FieldSamples.render(
                    FieldSamples.field(.time, label: "Best time to call", isRequired: true),
                    .text("14:30")
                )
            }
        },
        Fixture("consent, rating and section") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.section, label: "About you"), nil)
                FieldSamples.render(
                    FieldSamples.field(.consent, label: "I agree to be contacted.", isRequired: true),
                    .flag(true)
                )
                FieldSamples.render(FieldSamples.field(.rating, label: "How was the process?"), .number(4))
            }
        },
        Fixture("files, empty and full") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.file, label: "CV"), nil)
                FieldSamples.render(
                    FieldSamples.field(.file, label: "CV"),
                    .files([
                        MonarchFormFieldFile(name: "alex-morgan-curriculum-vitae-2026-final-version.pdf", size: 120_000)
                    ])
                )
            }
        },
        Fixture("errors on every kind of question") {
            VStack(alignment: .leading) {
                FieldSamples.render(
                    FieldSamples.field(.email, label: "Email", isRequired: true),
                    .text("alex@"),
                    problem: .email
                )
                FieldSamples.render(FieldSamples.field(.radio, isRequired: true), nil, problem: .required)
                FieldSamples.render(FieldSamples.field(.checkboxes), .values(["remote"]), problem: .minSelections(2))
                FieldSamples.render(FieldSamples.field(.consent, isRequired: true), nil, problem: .mustAgree)
                FieldSamples.render(FieldSamples.field(.rating, isRequired: true), nil, problem: .required)
                FieldSamples.render(FieldSamples.field(.file, isRequired: true), nil, problem: .required)
            }
        },
        Fixture("filled in automatically, sure and unsure") {
            VStack(alignment: .leading) {
                FieldSamples.render(FieldSamples.field(.shortText, label: "Company"), .text("Rook"), confidence: 0.92)
                FieldSamples.render(
                    FieldSamples.field(.phone, label: "Phone"),
                    .text("+44 20 7946 0000"),
                    confidence: 0.4
                )
            }
        },
        Fixture("read-only and disabled") {
            VStack(alignment: .leading) {
                FieldSamples.render(
                    FieldSamples.field(.email, label: "Verified email", isReadOnly: true),
                    .text("alex@example.com")
                )
                FieldSamples.render(FieldSamples.field(.select, isReadOnly: true), .text("office"))
                FieldSamples.render(FieldSamples.field(.yesNo), .flag(true)).disabled(true)
                FieldSamples.render(FieldSamples.field(.file), .files([MonarchFormFieldFile(name: "cv.pdf")])).disabled(
                    true
                )
            }
        },
    ]
}

@Suite("MonarchFieldRenderer")
@MainActor
struct MonarchFieldRendererTests {
    private let text = MonarchFieldRendererText(
        labels: MonarchFieldRendererLabels(),
        locale: Locale(identifier: "en_US")
    )

    @Test("The type names are the web's")
    func typeNames() {
        #expect(MonarchFormFieldType.allCases.count == 17)
        #expect(MonarchFormFieldType(rawValue: "short-text") == .shortText)
        #expect(MonarchFormFieldType(rawValue: "multi-select") == .multiSelect)
        #expect(MonarchFormFieldType(rawValue: "yes-no") == .yesNo)
        #expect(MonarchFormFieldType.checkboxes.rawValue == "checkboxes")
    }

    @Test("Empty answers: blank text, not a number, no options, no files")
    func emptiness() {
        #expect(MonarchFormFieldValue.text("   ").isEmpty)
        #expect(!MonarchFormFieldValue.text(" a ").isEmpty)
        #expect(MonarchFormFieldValue.number(.nan).isEmpty)
        #expect(!MonarchFormFieldValue.number(0).isEmpty)
        #expect(!MonarchFormFieldValue.flag(false).isEmpty)
        #expect(MonarchFormFieldValue.values([]).isEmpty)
        #expect(MonarchFormFieldValue.files([]).isEmpty)
        #expect(!MonarchFormFieldValue.date(FieldSamples.today).isEmpty)
    }

    @Test("Required questions report a missing answer; optional ones do not")
    func required() {
        let name = FieldSamples.field(.shortText, isRequired: true)
        #expect(name.problem(for: nil) == .required)
        #expect(name.problem(for: .text("  ")) == .required)
        #expect(name.problem(for: .text("Alex")) == nil)
        #expect(FieldSamples.field(.shortText).problem(for: nil) == nil)
        #expect(FieldSamples.field(.section, isRequired: true).problem(for: nil) == nil)
        #expect(FieldSamples.field(.number).problem(for: .number(.nan)) == .notANumber)
    }

    @Test("Yes or no needs a flag; a consent must be ticked")
    func flags() {
        let relocate = FieldSamples.field(.yesNo, isRequired: true)
        #expect(relocate.problem(for: .text("yes")) == .required)
        #expect(relocate.problem(for: .flag(false)) == nil)
        let consent = FieldSamples.field(.consent, isRequired: true)
        #expect(consent.problem(for: nil) == .mustAgree)
        #expect(consent.problem(for: .flag(false)) == .mustAgree)
        #expect(consent.problem(for: .flag(true)) == nil)
        #expect(FieldSamples.field(.consent).problem(for: .flag(false)) == nil)
    }

    @Test("Email and web address formats")
    func formats() {
        let email = FieldSamples.field(.email)
        #expect(email.problem(for: .text("alex@example.com")) == nil)
        #expect(email.problem(for: .text(" alex@example.com ")) == nil)
        #expect(email.problem(for: .text("alex@example")) == .email)
        #expect(email.problem(for: .text("alex @example.com")) == .email)
        #expect(email.problem(for: .text("a@b@c.com")) == .email)
        let url = FieldSamples.field(.url)
        #expect(url.problem(for: .text("https://example.com/path")) == nil)
        #expect(url.problem(for: .text("HTTP://example.com")) == nil)
        #expect(url.problem(for: .text("https://")) == .url)
        #expect(url.problem(for: .text("ftp://example.com")) == .url)
        #expect(url.problem(for: .text("example.com")) == .url)
    }

    @Test("Length, number and selection limits, in the web's order")
    func limits() {
        var name = FieldSamples.field(.shortText)
        name.minLength = 3
        name.maxLength = 5
        #expect(name.problem(for: .text("  ab  ")) == .minLength(3))
        #expect(name.problem(for: .text("abcdef")) == .maxLength(5))
        #expect(name.problem(for: .text(" abcde ")) == nil)
        var years = FieldSamples.field(.number)
        years.min = 1
        years.max = 40
        #expect(years.problem(for: .number(0)) == .min(1))
        #expect(years.problem(for: .number(41)) == .max(40))
        #expect(years.problem(for: .number(40)) == nil)
        var rating = FieldSamples.field(.rating)
        rating.max = 3
        #expect(rating.problem(for: .number(5)) == nil)
        var picks = FieldSamples.field(.checkboxes)
        picks.minSelections = 2
        picks.maxSelections = 2
        #expect(picks.problem(for: .values(["remote"])) == .minSelections(2))
        #expect(picks.problem(for: .values(["remote", "office", "hybrid"])) == .maxSelections(2))
        var email = FieldSamples.field(.email)
        email.minLength = 50
        #expect(email.problem(for: .text("x")) == .email)
    }

    @Test("Conditions decide visibility; lists match by item")
    func visibility() {
        let calendar = FieldSamples.calendar
        var visa = FieldSamples.field(.shortText)
        #expect(visa.isVisible(in: [:], calendar: calendar))
        visa.conditions = [
            MonarchCondition(id: "c1", field: "relocate", operator: .isTrue),
            MonarchCondition(id: "c2", field: "where", operator: .contains, value: .text("Office")),
        ]
        let both: [String: MonarchFormFieldValue] = ["relocate": .flag(true), "where": .values(["office"])]
        let one: [String: MonarchFormFieldValue] = ["relocate": .flag(true), "where": .values(["remote"])]
        #expect(visa.isVisible(in: both, calendar: calendar))
        #expect(!visa.isVisible(in: one, calendar: calendar))
        visa.conditionMatch = .any
        #expect(visa.isVisible(in: one, calendar: calendar))
        #expect(!visa.isVisible(in: [:], calendar: calendar))
        let empty = MonarchCondition(id: "e", field: "cv", operator: .isEmpty)
        #expect(MonarchFieldRendererTokens.matches(empty, answer: .files([]), calendar: calendar))
        #expect(
            !MonarchFieldRendererTokens.matches(
                empty,
                answer: .files([MonarchFormFieldFile(name: "a")]),
                calendar: calendar
            )
        )
        let notOffice = MonarchCondition(id: "n", field: "where", operator: .notEquals, value: .text("office"))
        #expect(!MonarchFieldRendererTokens.matches(notOffice, answer: .values(["office"]), calendar: calendar))
        #expect(MonarchFieldRendererTokens.matches(notOffice, answer: .values(["remote"]), calendar: calendar))
        let over = MonarchCondition(id: "o", field: "years", operator: .atLeast, value: .number(5))
        #expect(MonarchFieldRendererTokens.matches(over, answer: .number(7), calendar: calendar))
        #expect(!MonarchFieldRendererTokens.matches(over, answer: nil, calendar: calendar))
    }

    @Test("Typing stops at the most characters; read-only keeps text and numbers readable")
    func inputState() {
        #expect(MonarchFieldRendererTokens.typed("abcdef", maxLength: 4) == "abcd")
        #expect(MonarchFieldRendererTokens.typed("abc", maxLength: 4) == "abc")
        #expect(MonarchFieldRendererTokens.typed("abc", maxLength: nil) == "abc")
        #expect(!MonarchFieldRendererTokens.isDisabled(FieldSamples.field(.email, isReadOnly: true), isEnabled: true))
        #expect(!MonarchFieldRendererTokens.isDisabled(FieldSamples.field(.number, isReadOnly: true), isEnabled: true))
        #expect(MonarchFieldRendererTokens.isDisabled(FieldSamples.field(.select, isReadOnly: true), isEnabled: true))
        #expect(MonarchFieldRendererTokens.isDisabled(FieldSamples.field(.time, isReadOnly: true), isEnabled: true))
        #expect(MonarchFieldRendererTokens.isDisabled(FieldSamples.field(.email), isEnabled: false))
        #expect(MonarchFieldRendererTokens.prompt(FieldSamples.field(.url)) == "https://")
        #expect(MonarchFieldRendererTokens.prompt(FieldSamples.field(.shortText)) == nil)
    }

    @Test("Options: the chosen one must exist; several keep the order of the options")
    func options() {
        let options = FieldSamples.options
        #expect(MonarchFieldRendererTokens.chosenOption(.text("office"), options: options) == "office")
        #expect(MonarchFieldRendererTokens.chosenOption(.text("moon"), options: options) == nil)
        #expect(MonarchFieldRendererTokens.chosenOption(.flag(true), options: options) == nil)
        #expect(
            MonarchFieldRendererTokens.ordered(["hybrid", "remote", "gone"], options: options) == ["remote", "hybrid"]
        )
    }

    @Test("A rating uses its scale, or 1 to 5")
    func ratingRange() {
        var rating = FieldSamples.field(.rating)
        #expect(MonarchFieldRendererTokens.ratingRange(rating) == 1...5)
        rating.min = 0
        rating.max = 10
        #expect(MonarchFieldRendererTokens.ratingRange(rating) == 0...10)
        rating.min = 8
        rating.max = 2
        #expect(MonarchFieldRendererTokens.ratingRange(rating) == 1...5)
    }

    @Test("Times are HH:MM on a 24-hour clock, read in the given calendar")
    func times() throws {
        #expect(MonarchFieldRendererTokens.minutes(of: "09:05") == 545)
        #expect(MonarchFieldRendererTokens.minutes(of: "9:05") == 545)
        #expect(MonarchFieldRendererTokens.minutes(of: "23:59") == 1439)
        #expect(MonarchFieldRendererTokens.minutes(of: "24:00") == nil)
        #expect(MonarchFieldRendererTokens.minutes(of: "12:5") == nil)
        #expect(MonarchFieldRendererTokens.minutes(of: "noon") == nil)
        #expect(MonarchFieldRendererTokens.minutes(of: nil) == nil)
        #expect(MonarchFieldRendererTokens.timeAnswer(hour: 7, minute: 3) == "07:03")
        let calendar = FieldSamples.calendar
        let date = try #require(
            MonarchFieldRendererTokens.timeDate("14:30", on: FieldSamples.today, calendar: calendar)
        )
        #expect(calendar.component(.hour, from: date) == 14)
        #expect(calendar.isDate(date, inSameDayAs: FieldSamples.today))
        #expect(MonarchFieldRendererTokens.timeAnswer(from: date, calendar: calendar) == "14:30")
        #expect(MonarchFieldRendererTokens.timeDate("later", on: FieldSamples.today, calendar: calendar) == nil)
    }

    @Test("Choose a date starts today, kept inside the limits")
    func initialDate() {
        let today = FieldSamples.today
        let week: TimeInterval = 7 * 86_400
        #expect(MonarchFieldRendererTokens.initialDate(today: today, minDate: nil, maxDate: nil) == today)
        let later = today.addingTimeInterval(week)
        #expect(MonarchFieldRendererTokens.initialDate(today: today, minDate: later, maxDate: nil) == later)
        let earlier = today.addingTimeInterval(-week)
        #expect(MonarchFieldRendererTokens.initialDate(today: today, minDate: nil, maxDate: earlier) == earlier)
    }

    @Test("The note: none, filled, or check under the threshold; its colour follows")
    func aiNote() {
        #expect(MonarchFieldRendererTokens.aiNote(confidence: nil, threshold: 0.6) == nil)
        #expect(MonarchFieldRendererTokens.aiNote(confidence: 0.6, threshold: 0.6) == .filled)
        #expect(MonarchFieldRendererTokens.aiNote(confidence: 0.59, threshold: 0.6) == .check)
        #expect(MonarchFieldRendererTokens.aiNote(confidence: 0.9, threshold: 0.95) == .check)
        #expect(MonarchFieldRendererTokens.aiNoteColor(isUnsure: false) == \.aiStrong)
        #expect(MonarchFieldRendererTokens.aiNoteColor(isUnsure: true) == \.warningStrong)
    }

    @Test("Colours for enabled and disabled")
    func colors() {
        #expect(MonarchFieldRendererTokens.textColor(isEnabled: true) == \.textDefault)
        #expect(MonarchFieldRendererTokens.textColor(isEnabled: false) == \.textDisabled)
        #expect(MonarchFieldRendererTokens.noteColor(isEnabled: true) == \.textMuted)
        #expect(MonarchFieldRendererTokens.noteColor(isEnabled: false) == \.textDisabled)
        #expect(MonarchFieldRendererTokens.selectedColor(isEnabled: true) == \.accentStrong)
        #expect(MonarchFieldRendererTokens.selectedColor(isEnabled: false) == \.textDisabled)
    }

    @Test("An error message wins over the problem's message")
    func errors() {
        #expect(
            MonarchFieldRendererTokens.resolvedError(error: nil, problem: .required, text: text)
                == "This field is required."
        )
        #expect(MonarchFieldRendererTokens.resolvedError(error: "Taken.", problem: .required, text: text) == "Taken.")
        #expect(MonarchFieldRendererTokens.resolvedError(error: "  ", problem: nil, text: text) == nil)
        #expect(MonarchFieldRendererTokens.resolvedError(error: nil, problem: nil, text: text) == nil)
    }

    @Test("Problem messages, with numbers formatted for the locale")
    func messages() {
        #expect(text.message(.mustAgree) == "Tick this to continue.")
        #expect(text.message(.email) == "Enter an email address, like name@example.com.")
        #expect(text.message(.url) == "Enter a web address, like https://example.com.")
        #expect(text.message(.notANumber) == "Enter a number.")
        #expect(text.message(.min(1)) == "Enter 1 or more.")
        #expect(text.message(.max(1500.5)) == "Enter 1,500.5 or less.")
        #expect(text.message(.minLength(3)) == "Use at least 3 characters.")
        #expect(text.message(.maxLength(80)) == "Use 80 characters or fewer.")
        #expect(text.message(.minSelections(2)) == "Choose at least 2.")
        #expect(text.message(.maxSelections(3)) == "Choose 3 or fewer.")
        let german = MonarchFieldRendererText(labels: MonarchFieldRendererLabels(), locale: Locale(identifier: "de_DE"))
        #expect(german.message(.max(1500.5)) == "Enter 1.500,5 or less.")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(text.yes == "Yes")
        #expect(text.no == "No")
        #expect(text.choose == "Choose…")
        #expect(text.chooseDate == "Choose a date")
        #expect(text.chooseTime == "Choose a time")
        #expect(text.clear == "Clear")
        #expect(text.aiFilled == "Filled in automatically. Check it is right.")
        #expect(text.aiCheck == "Filled in automatically, but unsure. Please check.")
        #expect(text.remove == "Remove")
        #expect(text.removeFile("cv.pdf") == "Remove cv.pdf")
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchFieldRendererText(
            labels: MonarchFieldRendererLabels(
                required: "Needed.",
                min: { "At least \($0)." },
                minLength: { "\($0)+ letters." },
                yes: "Oui",
                clear: "Reset",
                aiCheck: "Please check.",
                removeFile: { "Delete \($0)" }
            ),
            locale: Locale(identifier: "en_US")
        )
        #expect(custom.message(.required) == "Needed.")
        #expect(custom.message(.min(2)) == "At least 2.")
        #expect(custom.message(.minLength(4)) == "4+ letters.")
        #expect(custom.message(.max(2)) == "Enter 2 or less.")
        #expect(custom.yes == "Oui")
        #expect(custom.no == "No")
        #expect(custom.clear == "Reset")
        #expect(custom.aiCheck == "Please check.")
        #expect(custom.removeFile("cv.pdf") == "Delete cv.pdf")
    }

    @Test("A definition keeps its defaults")
    func definitionDefaults() {
        let field = MonarchFormFieldDefinition(id: "cv", type: .file, label: "CV")
        #expect(field.maxFiles == 1)
        #expect(field.rows == 4)
        #expect(field.allowedContentTypes == [.item])
        #expect(field.conditionMatch == .all)
        #expect(!field.isRequired && !field.isReadOnly)
    }

    #if os(iOS)
        @Test("Choice rows are at least 44 points high")
        func touchTargets() {
            let size = Host.layout(
                MonarchFieldRendererChoiceList(options: [FieldSamples.options[0]], selection: .constant(nil))
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
