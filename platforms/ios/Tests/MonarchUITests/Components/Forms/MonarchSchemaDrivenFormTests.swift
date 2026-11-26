import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private enum FormSamples {
    /// 2026-11-14 09:00 UTC.
    static let today = Date(timeIntervalSince1970: 1_794_646_800)
    static let locale = Locale(identifier: "en_GB")
    static let timeZone = TimeZone.gmt
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        calendar.locale = locale
        return calendar
    }
    static let workplaces = [
        MonarchFormFieldOption(value: "remote", label: "Remote"),
        MonarchFormFieldOption(value: "office", label: "Office"),
    ]
    static let remoteRule = MonarchCondition(id: "r1", field: "remote", operator: .isTrue)

    /// A name, a remote question, a section shown only when remote, with a required time zone and a
    /// conditional question inside it, and a question after a second section.
    static let form = [
        MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
        MonarchFormFieldDefinition(id: "remote", type: .yesNo, label: "Do you want to work remotely?"),
        MonarchFormFieldDefinition(
            id: "setup",
            type: .section,
            label: "Your setup",
            description: "Only for remote work.",
            conditions: [remoteRule]
        ),
        MonarchFormFieldDefinition(id: "timezone", type: .shortText, label: "Time zone", isRequired: true),
        MonarchFormFieldDefinition(id: "where", type: .select, label: "Where", options: workplaces),
        MonarchFormFieldDefinition(id: "more", type: .section, label: "More"),
        MonarchFormFieldDefinition(id: "email", type: .email, label: "Email"),
    ]

    static func field(_ id: String) -> MonarchFormFieldDefinition {
        form.first { $0.id == id } ?? MonarchFormFieldDefinition(id: id, type: .shortText, label: id)
    }

    static func display(_ field: MonarchFormFieldDefinition, _ value: MonarchFormFieldValue?) -> String? {
        MonarchSchemaDrivenFormTokens.display(
            field,
            value,
            locale: locale,
            calendar: calendar,
            timeZone: timeZone,
            yes: "Yes",
            no: "No"
        )
    }
}

private struct ServerRefused: LocalizedError {
    var errorDescription: String? { "The server is busy. Try again in a minute." }
}

private struct SilentFailure: Error {}

private let formHeight = MonarchTokens.Space.space24 * 6

enum MonarchSchemaDrivenFormFixtures: ComponentFixtures {
    static let id = "schema-driven-form"
    static let fixtures = [
        Fixture("empty, with sections hidden by a condition") {
            MonarchSchemaDrivenForm(FormSamples.form, values: .constant([:])) { _ in }
                .frame(height: formHeight)
        },
        Fixture("answered, conditional section shown, app error") {
            MonarchSchemaDrivenForm(
                FormSamples.form,
                values: .constant([
                    "name": .text("Alex Morgan"), "remote": .flag(true), "where": .text("office"),
                    "email": .text("alex@"),
                ]),
                errors: ["email": "This address is already registered."]
            ) { _ in }
            .frame(height: formHeight)
        },
        Fixture("submit on submit, disabled") {
            MonarchSchemaDrivenForm(
                FormSamples.form,
                values: .constant(["name": .text("Alex")]),
                validateOn: .submit
            ) { _ in }
            .disabled(true)
            .frame(height: formHeight)
        },
        Fixture("edit form with navigation bar actions") {
            NavigationStack {
                MonarchSchemaDrivenForm(
                    FormSamples.form,
                    values: .constant(["name": .text("Alex Morgan")]),
                    isDirty: true,
                    onSubmit: { _ in },
                    onDiscard: {}
                )
            }
            .frame(height: formHeight)
        },
        Fixture("no submit button, custom labels") {
            MonarchSchemaDrivenForm(
                [
                    MonarchFormFieldDefinition(
                        id: "note",
                        type: .longText,
                        label: "A very long question label that wraps over several lines at large text sizes"
                    )
                ],
                values: .constant([:]),
                labels: MonarchSchemaDrivenFormLabels(submit: "Send application")
            )
            .frame(height: formHeight)
        },
        Fixture("read only, answered and empty") {
            MonarchSchemaDrivenForm(
                FormSamples.form,
                values: .constant(["name": .text("Alex Morgan"), "remote": .flag(true), "where": .text("remote")]),
                isReadOnly: true
            )
            .frame(height: formHeight)
        },
        Fixture("no questions") {
            MonarchSchemaDrivenForm([], values: .constant([:])) { _ in }
                .frame(height: formHeight)
        },
    ]
}

@Suite("MonarchSchemaDrivenForm")
struct MonarchSchemaDrivenFormTests {
    // MARK: Visibility and sections

    @Test func hiddenSectionHidesItsQuestions() {
        let ids = MonarchSchemaDrivenFormDefaults.visibleFields(
            FormSamples.form,
            values: ["remote": .flag(false)],
            calendar: FormSamples.calendar
        ).map(\.id)
        #expect(ids == ["name", "remote", "more", "email"])
    }

    @Test func shownSectionShowsItsQuestions() {
        let ids = MonarchSchemaDrivenFormDefaults.visibleFields(
            FormSamples.form,
            values: ["remote": .flag(true)],
            calendar: FormSamples.calendar
        ).map(\.id)
        #expect(ids == ["name", "remote", "setup", "timezone", "where", "more", "email"])
    }

    @Test func questionConditionInsideSectionIsTested() {
        var form = FormSamples.form
        form[4].conditions = [MonarchCondition(id: "r2", field: "timezone", operator: .isNotEmpty)]
        let without = MonarchSchemaDrivenFormDefaults.visibleFields(
            form,
            values: ["remote": .flag(true)],
            calendar: FormSamples.calendar
        ).map(\.id)
        #expect(!without.contains("where"))
        let with = MonarchSchemaDrivenFormDefaults.visibleFields(
            form,
            values: ["remote": .flag(true), "timezone": .text("Europe/London")],
            calendar: FormSamples.calendar
        ).map(\.id)
        #expect(with.contains("where"))
    }

    @Test func groupsSplitAtHeadings() {
        let shown = MonarchSchemaDrivenFormDefaults.visibleFields(
            FormSamples.form,
            values: ["remote": .flag(true)],
            calendar: FormSamples.calendar
        )
        let groups = MonarchSchemaDrivenFormTokens.groups(shown)
        #expect(groups.map(\.id) == [MonarchSchemaDrivenFormTokens.leadingGroupID, "setup", "more"])
        #expect(groups[0].heading == nil)
        #expect(groups[0].questions.map(\.id) == ["name", "remote"])
        #expect(groups[1].heading?.label == "Your setup")
        #expect(groups[1].questions.map(\.id) == ["timezone", "where"])
        #expect(groups[2].questions.map(\.id) == ["email"])
    }

    @Test func groupsKeepEmptyHeadingAndSkipEmptyLeadingGroup() {
        let groups = MonarchSchemaDrivenFormTokens.groups([
            MonarchFormFieldDefinition(id: "a", type: .section, label: "A"),
            MonarchFormFieldDefinition(id: "b", type: .section, label: "B"),
            MonarchFormFieldDefinition(id: "q", type: .number, label: "Q"),
        ])
        #expect(groups.map(\.id) == ["a", "b"])
        #expect(groups[0].questions.isEmpty)
        #expect(MonarchSchemaDrivenFormTokens.groups([]).isEmpty)
    }

    // MARK: Validation and submission

    @Test func problemsCoverShownQuestionsOnly() {
        let hidden = MonarchSchemaDrivenFormDefaults.problems(
            FormSamples.form,
            values: ["remote": .flag(false), "email": .text("alex@")],
            calendar: FormSamples.calendar
        )
        #expect(hidden == ["name": .required, "email": .email])
        let shown = MonarchSchemaDrivenFormDefaults.problems(
            FormSamples.form,
            values: ["name": .text("Alex"), "remote": .flag(true)],
            calendar: FormSamples.calendar
        )
        #expect(shown == ["timezone": .required])
    }

    @Test func submittedValuesDropHiddenAndUnanswered() {
        let answers = MonarchSchemaDrivenFormDefaults.submittedValues(
            FormSamples.form,
            values: ["name": .text("Alex"), "remote": .flag(false), "timezone": .text("UTC"), "stale": .text("x")],
            calendar: FormSamples.calendar
        )
        #expect(answers == ["name": .text("Alex"), "remote": .flag(false)])
    }

    @Test func firstInvalidFollowsSchemaOrder() {
        let shown = MonarchSchemaDrivenFormDefaults.visibleFields(
            FormSamples.form,
            values: ["remote": .flag(true)],
            calendar: FormSamples.calendar
        )
        #expect(
            MonarchSchemaDrivenFormTokens.firstInvalid(shown, problems: ["email": .email, "timezone": .required])
                == "timezone"
        )
        #expect(MonarchSchemaDrivenFormTokens.firstInvalid(shown, problems: [:]) == nil)
        #expect(MonarchSchemaDrivenFormTokens.firstInvalid(shown, problems: [:], errors: ["name": "Taken"]) == "name")
    }

    @Test func problemShowsByValidateOn() {
        let tokens = MonarchSchemaDrivenFormTokens.self
        #expect(tokens.showsProblem(validateOn: .blur, isSubmitted: false, isVisited: true))
        #expect(!tokens.showsProblem(validateOn: .blur, isSubmitted: false, isVisited: false))
        #expect(!tokens.showsProblem(validateOn: .submit, isSubmitted: false, isVisited: true))
        #expect(tokens.showsProblem(validateOn: .submit, isSubmitted: true, isVisited: false))
        #expect(tokens.showsProblem(validateOn: .blur, isSubmitted: true, isVisited: false))
    }

    @Test func choicesAreVisitedWhenAnsweredAndTypedWhenLeft() {
        let tokens = MonarchSchemaDrivenFormTokens.self
        let typing = tokens.visited([], afterEditing: FormSamples.field("name"), previous: nil)
        #expect(typing.isEmpty)
        let stillTyping = tokens.visited(typing, afterEditing: FormSamples.field("name"), previous: "name")
        #expect(stillTyping.isEmpty)
        let movedOn = tokens.visited(stillTyping, afterEditing: FormSamples.field("remote"), previous: "name")
        #expect(movedOn == ["name", "remote"])
        #expect(tokens.visited(["x"], focusFrom: "name", to: "email") == ["x", "name"])
        #expect(tokens.visited([], focusFrom: nil, to: "email").isEmpty)
        #expect(tokens.visited([], focusFrom: "name", to: "name").isEmpty)
    }

    @Test func typedQuestions() {
        let typed = MonarchFormFieldType.allCases.filter(MonarchSchemaDrivenFormTokens.isTyped)
        #expect(typed == [.shortText, .longText, .email, .phone, .url, .number])
    }

    @Test func saveNeedsChangesEnabledAndIdle() {
        let tokens = MonarchSchemaDrivenFormTokens.self
        #expect(tokens.canSave(isDirty: true, isEnabled: true, isPending: false))
        #expect(!tokens.canSave(isDirty: false, isEnabled: true, isPending: false))
        #expect(!tokens.canSave(isDirty: true, isEnabled: false, isPending: false))
        #expect(!tokens.canSave(isDirty: true, isEnabled: true, isPending: true))
    }

    @Test func failureTextPrefersTheErrorsDescription() {
        let tokens = MonarchSchemaDrivenFormTokens.self
        let refused = tokens.failureText(ServerRefused(), fallback: "Failed")
        #expect(refused == "The server is busy. Try again in a minute.")
        #expect(tokens.failureText(SilentFailure(), fallback: "Failed") == "Failed")
    }

    // MARK: Read-only display

    @Test func displayFormatsEveryAnswer() {
        let choice = FormSamples.field("where")
        #expect(FormSamples.display(choice, .text("office")) == "Office")
        #expect(FormSamples.display(choice, .text("unknown")) == "unknown")
        #expect(FormSamples.display(FormSamples.field("remote"), .flag(true)) == "Yes")
        #expect(FormSamples.display(FormSamples.field("remote"), .flag(false)) == "No")
        let number = MonarchFormFieldDefinition(id: "n", type: .number, label: "Seats")
        #expect(FormSamples.display(number, .number(1234.5)) == "1,234.5")
        let date = MonarchFormFieldDefinition(id: "d", type: .date, label: "Start")
        #expect(FormSamples.display(date, .date(FormSamples.today)) == "14 Nov 2026")
        let many = MonarchFormFieldDefinition(
            id: "m",
            type: .checkboxes,
            label: "Where",
            options: FormSamples.workplaces
        )
        #expect(FormSamples.display(many, .values(["remote", "office"])) == "Remote and Office")
        let files = MonarchFormFieldDefinition(id: "f", type: .file, label: "CV")
        #expect(FormSamples.display(files, .files([MonarchFormFieldFile(name: "cv.pdf")])) == "cv.pdf")
    }

    @Test func displayOfNoAnswerIsNil() {
        #expect(FormSamples.display(FormSamples.field("name"), nil) == nil)
        #expect(FormSamples.display(FormSamples.field("name"), .text("   ")) == nil)
        #expect(FormSamples.display(FormSamples.field("where"), .values([])) == nil)
    }

    @Test func displayFollowsTheLocale() {
        let number = MonarchFormFieldDefinition(id: "n", type: .number, label: "Seats")
        let german = MonarchSchemaDrivenFormTokens.display(
            number,
            .number(1234.5),
            locale: Locale(identifier: "de_DE"),
            calendar: FormSamples.calendar,
            timeZone: FormSamples.timeZone,
            yes: "Ja",
            no: "Nein"
        )
        #expect(german == "1.234,5")
    }

    // MARK: Strings

    @Test func builtInStringsResolve() {
        for key in MonarchSchemaDrivenFormStrings.keys {
            let value = MonarchSchemaDrivenFormStrings.string(key)
            #expect(!value.isEmpty)
            #expect(!value.hasPrefix("monarch."), "\(key) is missing from the table")
        }
        let text = MonarchSchemaDrivenFormText(labels: MonarchSchemaDrivenFormLabels())
        #expect(text.submit == "Save")
        #expect(text.empty == "Not set")
        #expect(text.yes == "Yes")
    }

    @Test func labelsOverrideBuiltInStrings() {
        let text = MonarchSchemaDrivenFormText(
            labels: MonarchSchemaDrivenFormLabels(
                submit: "Send",
                submitFailed: "Not sent",
                empty: "Blank",
                saving: "Sending",
                discard: "Reset",
                discardTitle: "Reset?",
                discardMessage: "Gone.",
                discardConfirm: "Reset all",
                keepEditing: "Stay",
                fields: MonarchFieldRendererLabels(yes: "Oui", no: "Non")
            )
        )
        #expect(
            [
                text.submit, text.submitFailed, text.empty, text.saving, text.discard, text.discardTitle,
                text.discardMessage, text.discardConfirm, text.keepEditing, text.yes, text.no,
            ] == [
                "Send", "Not sent", "Blank", "Sending", "Reset", "Reset?", "Gone.", "Reset all", "Stay", "Oui", "Non",
            ]
        )
    }

    @Test func colourRolesAreTheThemes() {
        let theme = MonarchTheme.light
        let tokens = MonarchSchemaDrivenFormTokens.self
        #expect(theme.colors[role: tokens.canvas] == theme.colors.bgCanvas)
        #expect(theme.colors[role: tokens.row] == theme.colors.bgSurface)
        #expect(theme.colors[role: tokens.text] == theme.colors.textDefault)
        #expect(theme.colors[role: tokens.muted] == theme.colors.textMuted)
    }
}
