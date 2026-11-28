import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMultiSelectFixtures: ComponentFixtures {
    static let id = "multi-select"

    static let labels = [
        MonarchComboboxOption(id: "bug", label: "Bug", description: "Something is broken", group: "Type"),
        MonarchComboboxOption(id: "feature", label: "Feature", group: "Type"),
        MonarchComboboxOption(id: "docs", label: "Documentation", hint: "12"),
        MonarchComboboxOption(id: "ui", label: "Interface", systemImage: "rectangle.on.rectangle"),
        MonarchComboboxOption(id: "old", label: "Archived", isDisabled: true),
    ]

    static let fixtures = [
        Fixture("chosen, with help text") {
            MonarchMultiSelect("Labels", selection: .constant(["bug", "ui"]), options: labels, helper: "Up to three.")
        },
        Fixture("empty with prompt") {
            MonarchMultiSelect("Labels", selection: .constant([]), options: labels, prompt: "Choose labels")
        },
        Fixture("required with error") {
            MonarchMultiSelect(
                "Labels",
                selection: .constant([]),
                options: labels,
                error: "Choose at least one label.",
                isRequired: true
            )
        },
        Fixture("limit reached with count") {
            MonarchMultiSelect(
                "Labels",
                selection: .constant(["bug", "docs", "ui"]),
                options: labels,
                maxSelections: 3
            )
        },
        Fixture("many long values and a value not in the options") {
            MonarchMultiSelect(
                "Reviewers",
                selection: .constant(["a", "b", "c", "user-42"]),
                options: [
                    MonarchComboboxOption(id: "a", label: "A reviewer with a long name that wraps in the field"),
                    MonarchComboboxOption(id: "b", label: "Sam Lee"),
                    MonarchComboboxOption(id: "c", label: "Ada Byron"),
                ]
            )
        },
        Fixture("disabled") {
            MonarchMultiSelect("Labels", selection: .constant(["docs"]), options: labels).disabled(true)
        },
        Fixture("list: groups, chosen, limit reached") {
            MonarchMultiSelectList(
                title: "Labels",
                selection: .constant(["bug", "docs"]),
                options: labels,
                query: .constant(""),
                maxSelections: 2,
                isLoading: false,
                sortsByMatch: false,
                limit: nil,
                labels: MonarchMultiSelectLabels(),
                filter: nil,
                canCreate: true,
                onCreate: { _ in },
                onDone: {}
            )
        },
        Fixture("list: no match with create, limited") {
            MonarchMultiSelectList(
                title: "Labels",
                selection: .constant([]),
                options: labels,
                query: .constant("Zz"),
                maxSelections: nil,
                isLoading: false,
                sortsByMatch: true,
                limit: 1,
                labels: MonarchMultiSelectLabels(),
                filter: nil,
                canCreate: true,
                onCreate: { _ in },
                onDone: {}
            )
        },
        Fixture("list: loading") {
            MonarchMultiSelectList(
                title: "People",
                selection: .constant([]),
                options: [],
                query: .constant("ana"),
                maxSelections: nil,
                isLoading: true,
                sortsByMatch: false,
                limit: nil,
                labels: MonarchMultiSelectLabels(),
                filter: { _, _ in true },
                canCreate: false,
                onCreate: { _ in },
                onDone: {}
            )
        },
    ]
}

@Suite("MonarchMultiSelect")
struct MonarchMultiSelectTests {
    private typealias Tokens = MonarchMultiSelectTokens
    private let options = MonarchMultiSelectFixtures.labels

    @Test("Activating a row adds at the end or removes, and respects the limit")
    func toggling() {
        #expect(Tokens.toggled([], id: "bug", maxSelections: nil) == ["bug"])
        #expect(Tokens.toggled(["ui"], id: "bug", maxSelections: nil) == ["ui", "bug"])
        #expect(Tokens.toggled(["ui", "bug"], id: "ui", maxSelections: nil) == ["bug"])
        #expect(Tokens.toggled(["ui", "bug"], id: "docs", maxSelections: 2) == ["ui", "bug"])
        #expect(Tokens.toggled(["ui", "bug"], id: "bug", maxSelections: 2) == ["ui"])
        #expect(Tokens.toggled([], id: "bug", maxSelections: 0) == [])
    }

    @Test("The limit disables only rows that are not chosen, and disabled options always")
    func rowDisabled() {
        let bug = options[0]
        let docs = options[2]
        #expect(!Tokens.isRowDisabled(bug, selection: [], maxSelections: 1))
        #expect(Tokens.isRowDisabled(docs, selection: ["bug"], maxSelections: 1))
        #expect(!Tokens.isRowDisabled(bug, selection: ["bug"], maxSelections: 1))
        #expect(Tokens.isRowDisabled(options[4], selection: [], maxSelections: nil))
        #expect(!Tokens.isFull(count: 5, maxSelections: nil))
        #expect(Tokens.isFull(count: 2, maxSelections: 2))
    }

    @Test("Picks keep the pick order and fall back to remembered labels, then ids")
    func picks() {
        let picks = Tokens.picks(["ui", "gone", "x"], options: options, remembered: ["gone": "Gone label"])
        #expect(picks.map(\.label) == ["Interface", "Gone label", "x"])
        #expect(picks.map(\.id) == ["ui", "gone", "x"])
    }

    @Test("The button reads the prompt, the built-in Select… or Edit selection")
    func buttonTitle() {
        let labels = MonarchMultiSelectLabels()
        #expect(Tokens.buttonTitle(hasSelection: false, prompt: nil, labels: labels) == "Select…")
        #expect(Tokens.buttonTitle(hasSelection: false, prompt: "Choose labels", labels: labels) == "Choose labels")
        #expect(Tokens.buttonTitle(hasSelection: true, prompt: "Choose labels", labels: labels) == "Edit selection")
        #expect(
            Tokens.buttonTitle(hasSelection: true, prompt: nil, labels: MonarchMultiSelectLabels(change: "Change"))
                == "Change"
        )
    }

    @Test("The count shows only with a limit and can be replaced")
    func count() {
        #expect(Tokens.countText(count: 2, maxSelections: nil, labels: MonarchMultiSelectLabels()) == nil)
        #expect(Tokens.countText(count: 2, maxSelections: 3, labels: MonarchMultiSelectLabels()) == "2 of 3 selected")
        let custom = MonarchMultiSelectLabels(count: { "\($0)/\($1)" })
        #expect(Tokens.countText(count: 1, maxSelections: 4, labels: custom) == "1/4")
    }

    @Test("VoiceOver reads the chosen labels or None, and a hint with the count")
    func accessibility() {
        let labels = MonarchMultiSelectLabels()
        let picks = Tokens.picks(["bug", "docs"], options: options, remembered: [:])
        #expect(Tokens.accessibilityValue(picks, labels: labels) == "Bug, Documentation")
        #expect(Tokens.accessibilityValue([], labels: labels) == "None")
        #expect(
            Tokens.accessibilityValue(
                [],
                labels: MonarchMultiSelectLabels(list: MonarchComboboxLabels(noSelection: "Nothing"))
            )
                == "Nothing"
        )
        #expect(
            Tokens.accessibilityHint(
                helper: "Up to three.",
                error: "Choose one.",
                isRequired: true,
                count: "1 of 3 selected",
                labels: labels
            ) == "Error: Choose one., Up to three., Required, 1 of 3 selected"
        )
        #expect(
            Tokens.accessibilityHint(helper: nil, error: nil, isRequired: false, count: nil, labels: labels).isEmpty
        )
    }

    @Test("Colours of the button text")
    func colors() {
        #expect(Tokens.buttonColor(hasSelection: false, isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.buttonColor(hasSelection: true, isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.buttonColor(hasSelection: true, isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchMultiSelectStrings.select == "Select…")
        #expect(MonarchMultiSelectStrings.change == "Edit selection")
        #expect(MonarchMultiSelectStrings.done == "Done")
        #expect(MonarchMultiSelectStrings.count(0, 5) == "0 of 5 selected")
    }
}
