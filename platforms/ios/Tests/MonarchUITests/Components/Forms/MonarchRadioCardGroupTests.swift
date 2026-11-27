import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRadioCardGroupFixtures: ComponentFixtures {
    static let id = "radio-card-group"

    static let plans = [
        MonarchRadioCardOption("starter", title: "Starter", description: "For one person.", meta: "$0"),
        MonarchRadioCardOption(
            "team",
            title: "Team",
            description: "Up to 10 people, shared projects and roles.",
            systemImage: "person.2",
            meta: "$20"
        ),
        MonarchRadioCardOption(
            "business",
            title: "Business",
            systemImage: "building.2",
            meta: "$60",
            details: ["Unlimited projects", "Single sign-on"]
        ),
    ]

    static let fixtures = [
        Fixture("no selection") {
            MonarchRadioCardGroup("Plan", selection: .constant(String?.none), options: plans)
        },
        Fixture("selected") {
            MonarchRadioCardGroup("Plan", selection: .constant("team"), options: plans)
        },
        Fixture("two and three columns on a regular width") {
            VStack {
                MonarchRadioCardGroup("Plan", selection: .constant("team"), options: plans, columns: 2)
                MonarchRadioCardGroup("Plan", selection: .constant("starter"), options: plans, columns: 3)
            }
            .environment(\.horizontalSizeClass, .regular)
        },
        Fixture("one disabled card and titles only") {
            MonarchRadioCardGroup(
                "Region",
                selection: .constant(2),
                options: [
                    MonarchRadioCardOption(1, title: "Europe"),
                    MonarchRadioCardOption(2, title: "North America"),
                    MonarchRadioCardOption(
                        3,
                        title: "Asia Pacific",
                        description: "Not available yet.",
                        isDisabled: true
                    ),
                ]
            )
        },
        Fixture("group disabled, long text") {
            MonarchRadioCardGroup(
                "Plan",
                selection: .constant("team"),
                options: [
                    MonarchRadioCardOption(
                        "team",
                        title: "Team plan with a long name that wraps onto a second line",
                        description:
                            "Everything in Starter, with shared projects, roles, an audit log and priority help.",
                        systemImage: "person.2",
                        meta: "$20 per person each month"
                    )
                ]
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchRadioCardGroup")
struct MonarchRadioCardGroupTests {
    typealias Tokens = MonarchRadioCardGroupTokens

    @Test("Cards stack in one column on a compact width and at the accessibility text sizes")
    func columns() {
        #expect(Tokens.columnCount(requested: 3, isCompactWidth: true, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(requested: 3, isCompactWidth: false, isAccessibilitySize: true) == 1)
        #expect(Tokens.columnCount(requested: 2, isCompactWidth: false, isAccessibilitySize: false) == 2)
        #expect(Tokens.columnCount(requested: 3, isCompactWidth: false, isAccessibilitySize: false) == 3)
        #expect(Tokens.columnCount(requested: 9, isCompactWidth: false, isAccessibilitySize: false) == 3)
        #expect(Tokens.columnCount(requested: 0, isCompactWidth: false, isAccessibilitySize: false) == 1)
    }

    @Test("The selected card uses the accent and a heavier border")
    func selected() {
        let selected = Tokens.appearance(isSelected: true, isPressed: false, isEnabled: true)
        #expect(selected.background == \MonarchColorTokens.accentSubtle)
        #expect(selected.border == \MonarchColorTokens.accentStrong)
        #expect(selected.icon == \MonarchColorTokens.accentStrong)
        #expect(selected.mark == \MonarchColorTokens.accentStrong)
        #expect(selected.title == \MonarchColorTokens.textDefault)
        #expect(Tokens.borderWidth(isSelected: true) > Tokens.borderWidth(isSelected: false))
    }

    @Test("A card at rest is a bordered surface")
    func resting() {
        let resting = Tokens.appearance(isSelected: false, isPressed: false, isEnabled: true)
        #expect(resting.background == \MonarchColorTokens.bgSurface)
        #expect(resting.border == \MonarchColorTokens.borderDefault)
        #expect(resting.secondary == \MonarchColorTokens.textMuted)
        #expect(resting.mark == \MonarchColorTokens.borderInput)
    }

    @Test("Pressing changes the fill and keeps the text colours", arguments: [true, false])
    func pressed(isSelected: Bool) {
        let resting = Tokens.appearance(isSelected: isSelected, isPressed: false, isEnabled: true)
        let pressed = Tokens.appearance(isSelected: isSelected, isPressed: true, isEnabled: true)
        #expect(pressed.background != resting.background)
        #expect(pressed.title == resting.title)
        #expect(pressed.secondary == resting.secondary)
    }

    @Test("A disabled card uses the disabled roles and ignores presses", arguments: [true, false])
    func disabled(isSelected: Bool) {
        let disabled = Tokens.appearance(isSelected: isSelected, isPressed: false, isEnabled: false)
        #expect(disabled.title == \MonarchColorTokens.textDisabled)
        #expect(disabled.secondary == \MonarchColorTokens.textDisabled)
        #expect(disabled.background == \MonarchColorTokens.bgSubtle)
        #expect(disabled.border == \MonarchColorTokens.borderSubtle)
        #expect(disabled == Tokens.appearance(isSelected: isSelected, isPressed: true, isEnabled: false))
    }

    @Test("VoiceOver reads the meta text and the description as the value")
    func accessibilityValue() {
        #expect(Tokens.accessibilityValue(meta: nil, description: nil).isEmpty)
        #expect(Tokens.accessibilityValue(meta: "$20", description: nil) == "$20")
        #expect(Tokens.accessibilityValue(meta: nil, description: "Up to 10 people.") == "Up to 10 people.")
        #expect(Tokens.accessibilityValue(meta: "$20", description: "Up to 10 people.") == "$20, Up to 10 people.")
        #expect(Tokens.accessibilityValue(meta: "", description: "Up to 10 people.") == "Up to 10 people.")
        #expect(
            Tokens.accessibilityValue(meta: "$20", description: "Up to 10 people.", details: ["Invite guests", ""])
                == "$20, Up to 10 people., Invite guests"
        )
        #expect(Tokens.accessibilityValue(meta: nil, description: nil, details: ["Edit"]) == "Edit")
    }

    @Test("An option is identified by its value")
    func option() {
        let option = MonarchRadioCardOption(7, title: "Seven")
        #expect(option.id == 7)
        #expect(option.description == nil)
        #expect(!option.isDisabled)
    }

    #if os(iOS)
        @Test("A card with only a title keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let group = MonarchRadioCardGroup(
                "Region",
                selection: .constant(Int?.none),
                options: [MonarchRadioCardOption(1, title: "Europe")]
            )
            #expect(Host.layout(group).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
