import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiModeSwitcherFixtures: ComponentFixtures {
    static let id = "ai-mode-switcher"
    static let fixtures = [
        Fixture("default modes, sm") {
            MonarchAiModeSwitcher(selection: .constant("ask"))
        },
        Fixture("md, no description") {
            MonarchAiModeSwitcher(selection: .constant("edit"), size: .md, showsDescription: false)
        },
        Fixture("own modes: icon only, disabled, no description on the selected one") {
            MonarchAiModeSwitcher(
                selection: .constant("write"),
                modes: [
                    MonarchAiMode(
                        id: "research",
                        label: "Research",
                        description: "Reads sources and cites them.",
                        systemImage: "magnifyingglass",
                        isIconOnly: true
                    ),
                    MonarchAiMode(id: "write", label: "Write"),
                    MonarchAiMode(
                        id: "automate",
                        label: "Automate",
                        description: "Not set up in this workspace.",
                        isDisabled: true
                    ),
                ],
                label: "Assistant mode"
            )
        },
        Fixture("disabled") {
            MonarchAiModeSwitcher(selection: .constant("agent")).disabled(true)
        },
    ]
}

@Suite("MonarchAiModeSwitcher")
struct MonarchAiModeSwitcherTests {
    typealias Tokens = MonarchAiModeSwitcherTokens

    @Test("The default modes are Ask, Edit and Agent with their descriptions")
    func defaults() {
        let modes = MonarchAiMode.defaults
        #expect(modes.map(\.id) == ["ask", "edit", "agent"])
        #expect(modes.map(\.label) == ["Ask", "Edit", "Agent"])
        #expect(modes[0].description == "Answers questions. Changes nothing.")
        #expect(modes[1].description == "Proposes edits you review before they apply.")
        #expect(modes[2].description == "Works through steps and uses tools. Asks before anything risky.")
        #expect(modes.allSatisfy { !$0.isDisabled && !$0.isIconOnly })
    }

    @Test("The description is the selected mode's, or nil")
    func description() {
        let modes = MonarchAiMode.defaults
        #expect(Tokens.description(selection: "edit", modes: modes) == modes[1].description)
        #expect(Tokens.description(selection: "missing", modes: modes) == nil)
        let bare = [MonarchAiMode(id: "a", label: "A", description: "")]
        #expect(Tokens.description(selection: "a", modes: bare) == nil)
    }

    @Test("The description line exists only when asked for and some mode has one")
    func describes() {
        #expect(Tokens.describes(modes: MonarchAiMode.defaults, showsDescription: true))
        #expect(!Tokens.describes(modes: MonarchAiMode.defaults, showsDescription: false))
        let bare = [MonarchAiMode(id: "a", label: "A"), MonarchAiMode(id: "b", label: "B", description: "")]
        #expect(!Tokens.describes(modes: bare, showsDescription: true))
    }

    @Test("A disabled or unknown mode is never selected")
    func accepts() {
        let modes = [
            MonarchAiMode(id: "a", label: "A"),
            MonarchAiMode(id: "b", label: "B", isDisabled: true),
        ]
        #expect(Tokens.accepts("a", modes: modes))
        #expect(!Tokens.accepts("b", modes: modes))
        #expect(!Tokens.accepts("c", modes: modes))
    }

    @Test("A segment is icon only only when it has a symbol")
    func iconOnly() {
        #expect(Tokens.showsIconOnly(MonarchAiMode(id: "a", label: "A", systemImage: "pencil", isIconOnly: true)))
        #expect(!Tokens.showsIconOnly(MonarchAiMode(id: "a", label: "A", isIconOnly: true)))
        #expect(!Tokens.showsIconOnly(MonarchAiMode(id: "a", label: "A", systemImage: "pencil")))
    }

    @Test("Sizes map to the small and regular control sizes; the description is muted")
    func sizes() {
        #expect(Tokens.controlSize(.sm) == .small)
        #expect(Tokens.controlSize(.md) == .regular)
        #expect(Tokens.segmentedSize(.sm) == .sm)
        #expect(Tokens.segmentedSize(.md) == .md)
        #expect(Tokens.description == \MonarchColorTokens.textMuted)
        #expect(Tokens.disabledDescription == \MonarchColorTokens.textDisabled)
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiModeSwitcherStrings.label == "Mode")
        #expect(MonarchAiModeSwitcherStrings.agent == "Agent")
    }

    @Test("The description line takes room only when shown")
    @MainActor
    func descriptionTakesRoom() {
        let with = Host.layout(MonarchAiModeSwitcher(selection: .constant("ask")))
        let without = Host.layout(MonarchAiModeSwitcher(selection: .constant("ask"), showsDescription: false))
        #expect(with.height > without.height)
    }
}
