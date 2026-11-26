import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let selectFixtureOptions = [
    MonarchSelectOption("Open", value: "open"),
    MonarchSelectOption("In progress", value: "progress", systemImage: "clock"),
    MonarchSelectOption("Closed", value: "closed", isDisabled: true),
]

enum MonarchSelectFieldFixtures: ComponentFixtures {
    static let id = "select"
    static let fixtures = [
        Fixture("chosen, helper") {
            MonarchSelectField(
                "Status",
                selection: .constant("open"),
                options: selectFixtureOptions,
                helper: "Shown to the customer."
            )
        },
        Fixture("compact, in a toolbar row") {
            HStack {
                MonarchSelectField(
                    "Period",
                    selection: .constant("open"),
                    options: selectFixtureOptions,
                    style: .compact
                )
                MonarchSelectField(
                    "Status",
                    selection: .constant("none"),
                    options: selectFixtureOptions,
                    prompt: "Status",
                    style: .compact
                )
            }
        },
        Fixture("empty with prompt, required, error") {
            MonarchSelectField(
                "Priority",
                selection: .constant("none"),
                options: selectFixtureOptions,
                prompt: "Choose a priority",
                error: "Choose a priority.",
                isRequired: true
            )
        },
        Fixture("disabled, hidden label, sizes") {
            VStack {
                MonarchSelectField("Status", selection: .constant("open"), options: selectFixtureOptions).disabled(true)
                MonarchSelectField(
                    "Status",
                    selection: .constant("open"),
                    options: selectFixtureOptions,
                    labelVisibility: .hidden
                )
                .monarchFieldSize(.small)
                MonarchSelectField("Status", selection: .constant("open"), options: selectFixtureOptions)
                    .monarchFieldSize(.large)
            }
        },
        Fixture("long value") {
            MonarchSelectField(
                "Assignee",
                selection: .constant("long"),
                options: [
                    MonarchSelectOption(
                        "A very long option title that does not fit on one line of a phone",
                        value: "long"
                    )
                ]
            )
        },
        Fixture("options as views") {
            MonarchSelectField("Status", selection: .constant("a"), valueText: "Alpha") {
                Text("Alpha").tag("a")
                Text("Beta").tag("b")
            }
        },
    ]
}

@Suite("MonarchSelectField")
struct MonarchSelectFieldTests {
    @Test("The shown text is the chosen title, else the prompt, else empty")
    func shownText() {
        let chosen = MonarchSelectFieldTokens.shownText(
            selection: "progress",
            options: selectFixtureOptions,
            prompt: "Choose"
        )
        #expect(chosen.text == "In progress")
        #expect(chosen.hasValue)
        let none = MonarchSelectFieldTokens.shownText(selection: "x", options: selectFixtureOptions, prompt: "Choose")
        #expect(none.text == "Choose")
        #expect(!none.hasValue)
        let bare = MonarchSelectFieldTokens.shownText(selection: "x", options: selectFixtureOptions, prompt: nil)
        #expect(bare.text.isEmpty)
    }

    @Test("A prompt is subtle, a value is the default text, a disabled field is disabled")
    func textColor() {
        #expect(MonarchSelectFieldTokens.textColor(hasValue: true, isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchSelectFieldTokens.textColor(hasValue: false, isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(
            MonarchSelectFieldTokens.textColor(hasValue: true, isEnabled: false) == \MonarchColorTokens.textDisabled
        )
        #expect(MonarchSelectFieldTokens.chevronColor(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchSelectFieldTokens.chevronColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The chevron points down")
    func chevron() {
        #expect(MonarchSelectFieldTokens.chevronSymbol == "chevron.down")
    }

    @Test("An option is identified by its value and keeps what it is given")
    func option() {
        let option = MonarchSelectOption("Open", value: 7, systemImage: "tray", isDisabled: true)
        #expect(option.id == 7)
        #expect(option.title == "Open")
        #expect(option.systemImage == "tray")
        #expect(option.isDisabled)
    }

    @Test("The field heights are 40, 44 and 52 points, and 44 is the default")
    func heights() {
        #expect(MonarchControlMetrics.fieldMinHeight(.small) == 40)
        #expect(MonarchControlMetrics.fieldMinHeight(.medium) == 44)
        #expect(MonarchControlMetrics.fieldMinHeight(.large) == 52)
        #expect(MonarchControlMetrics.fieldMinHeight(.medium) >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The compact trigger is as high as a button, inside a 44 point touch area")
    func compact() {
        #expect(MonarchSelectFieldTokens.compactHeight == 36)
        #expect(MonarchSelectFieldTokens.compactHeight < MonarchAccessibility.minimumHitTarget)
        #expect(MonarchSelectStyle.allCases == [.field, .compact])
    }

    @Test("The shown value takes one line, and up to three at the accessibility sizes")
    func lineLimit() {
        #expect(MonarchSelectFieldTokens.lineLimit(isAccessibilitySize: false) == 1)
        #expect(MonarchSelectFieldTokens.lineLimit(isAccessibilitySize: true) == 3)
    }
}
