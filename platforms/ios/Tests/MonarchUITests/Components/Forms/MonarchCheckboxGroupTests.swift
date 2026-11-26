import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCheckboxGroupFixtures: ComponentFixtures {
    static let id = "checkbox-group"

    static let options = [
        MonarchCheckboxGroupOption(id: "comments", label: "Comments"),
        MonarchCheckboxGroupOption(id: "mentions", label: "Mentions", description: "When someone tags you."),
        MonarchCheckboxGroupOption(
            id: "billing",
            label: "Billing",
            description: "Managed by your admin.",
            isDisabled: true
        ),
    ]

    static let fixtures = [
        Fixture("vertical with help text") {
            MonarchCheckboxGroup(
                "Notify me about",
                selection: .constant(["comments"]),
                options: options,
                helper: "Sent by email."
            )
        },
        Fixture("select all, mixed") {
            MonarchCheckboxGroup(
                "Notify me about",
                selection: .constant(["comments"]),
                options: options,
                showsSelectAll: true
            )
        },
        Fixture("select all, all checked") {
            MonarchCheckboxGroup(
                "Notify me about",
                selection: .constant(["comments", "mentions"]),
                options: options,
                showsSelectAll: true,
                labels: MonarchCheckboxGroupLabels(selectAll: "Everything")
            )
        },
        Fixture("horizontal, required, error") {
            MonarchCheckboxGroup(
                "Days",
                selection: .constant([]),
                options: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map {
                    MonarchCheckboxGroupOption(id: $0, label: $0)
                },
                error: "Choose at least one day.",
                layout: .horizontal,
                isRequired: true
            )
        },
        Fixture("long labels") {
            MonarchCheckboxGroup(
                "A group label long enough to wrap onto a second line at most widths",
                selection: .constant(["a"]),
                options: [
                    MonarchCheckboxGroupOption(
                        id: "a",
                        label: "An option whose label is long enough to wrap onto more than one line",
                        description: "And a description that is also quite long, to check that it wraps too."
                    )
                ]
            )
        },
        Fixture("every choice disabled") {
            MonarchCheckboxGroup(
                "Locked",
                selection: .constant(["comments"]),
                options: options,
                showsSelectAll: true
            )
            .disabled(true)
        },
        Fixture("empty") {
            MonarchCheckboxGroup("Nothing to choose", selection: .constant([]), options: [], showsSelectAll: true)
        },
    ]
}

@MainActor
private final class MonarchCheckboxGroupTestSelection {
    var ids: Set<String>
    init(_ ids: Set<String>) { self.ids = ids }
}

@Suite("MonarchCheckboxGroup")
@MainActor
struct MonarchCheckboxGroupTests {
    private typealias Tokens = MonarchCheckboxGroupTokens
    private let options = MonarchCheckboxGroupFixtures.options

    @Test("Select all acts only on choices that are not disabled")
    func enabledOptions() {
        #expect(Tokens.enabledOptions(options).map(\.id) == ["comments", "mentions"])
        #expect(Tokens.enabledOptions([]).isEmpty)
    }

    @Test("Checking and clearing one choice")
    func setting() {
        #expect(Tokens.setting("a", isOn: true, in: ["b"]) == ["a", "b"])
        #expect(Tokens.setting("b", isOn: false, in: ["a", "b"]) == ["a"])
        #expect(Tokens.setting("c", isOn: false, in: ["a"]) == ["a"])
    }

    @Test("The horizontal layout stacks at the accessibility sizes")
    func stacks() {
        #expect(Tokens.stacks(layout: .vertical, isAccessibilitySize: false))
        #expect(!Tokens.stacks(layout: .horizontal, isAccessibilitySize: false))
        #expect(Tokens.stacks(layout: .horizontal, isAccessibilitySize: true))
    }

    @Test("Descriptions dim when disabled")
    func colors() {
        #expect(Tokens.descriptionColor(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.descriptionColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("Select all writes every enabled choice and leaves disabled ones alone")
    func selectAllSources() {
        // Toggle(sources:isOn:) sets every source to the new value; these are
        // the sources the group gives it.
        let selection = MonarchCheckboxGroupTestSelection(["comments", "billing"])
        let sources = Tokens.enabledOptions(options).map { option in
            Binding<Bool>(
                get: { selection.ids.contains(option.id) },
                set: { selection.ids = Tokens.setting(option.id, isOn: $0, in: selection.ids) }
            )
        }
        #expect(sources.map(\.wrappedValue) == [true, false])
        for source in sources { source.wrappedValue = true }
        #expect(selection.ids == ["comments", "mentions", "billing"])
        for source in sources { source.wrappedValue = false }
        #expect(selection.ids == ["billing"])
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchCheckboxGroupStrings.selectAll == "Select all")
    }
}
