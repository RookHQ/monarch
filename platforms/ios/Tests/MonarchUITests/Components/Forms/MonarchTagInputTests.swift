import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTagInputFixtures: ComponentFixtures {
    static let id = "tag-input"
    static let fixtures = [
        Fixture("tags with prompt and helper") {
            MonarchTagInput(
                "Skills",
                tags: .constant(["Research", "TypeScript", "Accessibility"]),
                prompt: "Add a skill",
                helper: "Separate skills with commas."
            )
        },
        Fixture("empty") {
            MonarchTagInput("Recipients", tags: .constant([]), prompt: "name@example.com")
        },
        Fixture("error") {
            MonarchTagInput("Labels", tags: .constant(["Billing"]), error: "Add at least two labels.")
        },
        Fixture("read only") {
            MonarchTagInput("Labels", tags: .constant(["Billing", "Priority"]), isReadOnly: true)
        },
        Fixture("disabled") {
            MonarchTagInput("Labels", tags: .constant(["Billing"])).disabled(true)
        },
        Fixture("long tags") {
            MonarchTagInput(
                "Keywords",
                tags: .constant([
                    "a keyword long enough to need more than one line on a narrow phone at a large text size",
                    "short",
                ])
            )
        },
    ]
}

@Suite("MonarchTagInput")
struct MonarchTagInputTests {
    private typealias Model = MonarchTagInputModel
    private let rules = MonarchTagInputModel.Rules()

    @Test("Typing a separator commits what comes before it")
    func typing() {
        let change = Model.process(old: "Research", new: "Research,", rules: rules)
        #expect(change.complete == ["Research"])
        #expect(change.remainder.isEmpty)
        let none = Model.process(old: "Rese", new: "Resea", rules: rules)
        #expect(none.complete.isEmpty)
        #expect(none.remainder == "Resea")
    }

    @Test("A paste is split into every part, on separators, new lines and tabs")
    func paste() {
        let change = Model.process(old: "", new: "a, b\nc\td", rules: rules)
        #expect(change.complete == ["a", " b", "c", "d"])
        #expect(change.remainder.isEmpty)
        var semicolons = rules
        semicolons.separators = [";"]
        #expect(Model.split("x;y,z", rules: semicolons) == ["x", "y,z"])
    }

    @Test("Tags are trimmed; empty parts are skipped")
    func normalize() {
        let outcome = Model.add([" Research ", "", "  "], to: [], rules: rules)
        #expect(outcome.tags == ["Research"])
        #expect(outcome.added == ["Research"])
        #expect(outcome.message == nil)
        var lower = rules
        lower.normalize = { $0.trimmingCharacters(in: .whitespaces).lowercased() }
        #expect(Model.add(["  TypeScript"], to: [], rules: lower).tags == ["typescript"])
    }

    @Test("Duplicates are rejected without case, unless allowed")
    func duplicates() {
        let outcome = Model.add(["research"], to: ["Research"], rules: rules)
        #expect(outcome.tags == ["Research"])
        #expect(outcome.rejected == ["research"])
        #expect(outcome.message == "“research” is already added.")
        var allowing = rules
        allowing.allowsDuplicates = true
        #expect(Model.add(["research"], to: ["Research"], rules: allowing).tags == ["Research", "research"])
    }

    @Test("The maximum and the app's check reject with their messages")
    func limits() {
        var limited = rules
        limited.maxCount = 2
        let full = Model.add(["a", "b", "c"], to: [], rules: limited)
        #expect(full.tags == ["a", "b"])
        #expect(full.rejected == ["c"])
        #expect(full.message == "You can add up to 2.")
        var checked = rules
        checked.validate = { tag, _ in tag.contains("@") ? nil : "Enter an email address." }
        let mixed = Model.add(["ana@example.com", "bob"], to: [], rules: checked)
        #expect(mixed.tags == ["ana@example.com"])
        #expect(mixed.rejected == ["bob"])
        #expect(mixed.message == "Enter an email address.")
    }

    @Test("Rejected parts go back into the field, before the text being typed")
    func leftover() {
        #expect(Model.leftover(rejected: [], remainder: "abc") == "abc")
        #expect(Model.leftover(rejected: ["x", "y"], remainder: "") == "x, y")
        #expect(Model.leftover(rejected: ["x"], remainder: "ab") == "x, ab")
    }

    @Test("Suggestions match the typed text and leave out added tags")
    func suggestions() {
        let all = ["Research", "Copywriting", "Data research", "TypeScript"]
        #expect(Model.matches(all, query: "res", tags: [], limit: 8) == ["Research", "Data research"])
        #expect(Model.matches(all, query: "res", tags: ["research"], limit: 8) == ["Data research"])
        #expect(Model.matches(all, query: "  ", tags: [], limit: 8).isEmpty)
        #expect(Model.matches(all, query: "r", tags: [], limit: 1).count == 1)
    }

    @Test("Announcements and spoken names")
    func accessibility() {
        let labels = MonarchTagInputLabels()
        #expect(Model.addedAnnouncement([], labels: labels) == nil)
        #expect(Model.addedAnnouncement(["a", "b"], labels: labels) == "Added a. Added b.")
        #expect(MonarchTagInputTokens.suggestionLabel("Research", labels: labels) == "Add Research")
        #expect(MonarchTagInputTokens.accessibilityHint(helper: "Use commas.", message: nil) == "Use commas.")
        #expect(
            MonarchTagInputTokens.accessibilityHint(helper: "Use commas.", message: "Too many") == "Error: Too many"
        )
        let custom = MonarchTagInputLabels(added: { "+\($0)" }, addSuggestion: { "Ajouter \($0)" })
        #expect(Model.addedAnnouncement(["a"], labels: custom) == "+a")
        #expect(MonarchTagInputTokens.suggestionLabel("x", labels: custom) == "Ajouter x")
    }

    @Test("A rejection wins over the app's error")
    func message() {
        #expect(MonarchTagInputTokens.message(rejection: nil, error: nil) == nil)
        #expect(MonarchTagInputTokens.message(rejection: nil, error: "Bad") == "Bad")
        #expect(MonarchTagInputTokens.message(rejection: "Dup", error: "Bad") == "Dup")
        #expect(MonarchTagInputTokens.message(rejection: "", error: "") == nil)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchTagInputStrings.added("x") == "Added x.")
        #expect(MonarchTagInputStrings.removed("x") == "Removed x.")
        #expect(MonarchTagInputStrings.maximum(5) == "You can add up to 5.")
        #expect(MonarchTagInputTokens.labelColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }
}
