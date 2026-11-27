import SwiftUI
import Testing

@testable import MonarchUI

private let reactions = [
    MonarchReactionBarItem("👍", count: 3, isReacted: true, reactedBy: "You, Ana and Ben"),
    MonarchReactionBarItem("🎉", count: 1),
    MonarchReactionBarItem("❤️", count: 1_204),
    MonarchReactionBarItem("👀", count: 0),
]

enum MonarchReactionBarFixtures: ComponentFixtures {
    static let id = "reaction-bar"
    static let fixtures = [
        Fixture("interactive, small") {
            MonarchReactionBar(reactions) { _, _ in }
        },
        Fixture("interactive, medium") {
            MonarchReactionBar(reactions, size: .md) { _, _ in }
        },
        Fixture("read-only") {
            MonarchReactionBar(reactions)
        },
        Fixture("no reactions, only the add button") {
            MonarchReactionBar([]) { _, _ in }
        },
        Fixture("limit reached, no add button") {
            MonarchReactionBar(reactions, maxReactions: 3) { _, _ in }
        },
        Fixture("disabled") {
            MonarchReactionBar(reactions) { _, _ in }.disabled(true)
        },
        Fixture("many reactions wrap") {
            MonarchReactionBar(
                ["😀", "😂", "😍", "🤔", "😢", "😡", "👏", "🙏", "🔥", "✅", "🚀", "💯"].map {
                    MonarchReactionBarItem($0, count: 7)
                }
            ) { _, _ in }
        },
    ]
}

@Suite("MonarchReactionBar")
@MainActor
struct MonarchReactionBarTests {
    typealias Tokens = MonarchReactionBarTokens

    @Test("Reactions with no count and repeated emoji are not shown")
    func shown() {
        #expect(Tokens.shown(reactions).map(\.emoji) == ["👍", "🎉", "❤️"])
        let repeated = [MonarchReactionBarItem("👍", count: 1), MonarchReactionBarItem("👍", count: 2)]
        #expect(Tokens.shown(repeated).map(\.count) == [1])
    }

    @Test("The add button needs an interactive bar, adding allowed and room under the limit")
    func canAdd() {
        #expect(Tokens.canAdd(isInteractive: true, isAddable: true, shownCount: 3, maxReactions: nil))
        #expect(Tokens.canAdd(isInteractive: true, isAddable: true, shownCount: 2, maxReactions: 3))
        #expect(!Tokens.canAdd(isInteractive: true, isAddable: true, shownCount: 3, maxReactions: 3))
        #expect(!Tokens.canAdd(isInteractive: true, isAddable: false, shownCount: 0, maxReactions: nil))
        #expect(!Tokens.canAdd(isInteractive: false, isAddable: true, shownCount: 0, maxReactions: nil))
    }

    @Test("Picking an emoji you already reacted with changes nothing")
    func reactionToAdd() {
        #expect(Tokens.reactionToAdd("👍", in: reactions) == nil)
        #expect(Tokens.reactionToAdd("🎉", in: reactions) == "🎉")
        #expect(Tokens.reactionToAdd("🚀", in: reactions) == "🚀")
        #expect(Tokens.reactionToAdd("👀", in: reactions) == "👀")
    }

    @Test("A chip is named by the emoji's name and its count")
    func chipLabel() {
        let labels = MonarchReactionBarLabels()
        #expect(
            Tokens.chipLabel(MonarchReactionBarItem("🎉", count: 1, label: "party"), labels: labels)
                == "party, 1 reaction"
        )
        #expect(
            Tokens.chipLabel(MonarchReactionBarItem("🎉", count: 4, label: "party"), labels: labels)
                == "party, 4 reactions"
        )
        let unknown = MonarchReactionBarItem("🫠", count: 2, label: "")
        let name = Tokens.builtInName("🫠") ?? "🫠"
        #expect(Tokens.chipLabel(unknown, labels: labels) == "\(name), 2 reactions")
    }

    @Test("Without a label the name comes from the emoji picker's index")
    func builtInName() {
        let name = Tokens.builtInName("👍")
        #expect(name != nil)
        #expect(name != "👍")
        #expect(Tokens.name(MonarchReactionBarItem("👍", count: 1)) == name)
        #expect(Tokens.builtInName("not an emoji") == nil)
    }

    @Test("A reacted chip differs by more than colour: a thicker border and a bold count")
    func appearance() {
        let reacted = Tokens.appearance(isReacted: true, isEnabled: true)
        let plain = Tokens.appearance(isReacted: false, isEnabled: true)
        #expect(reacted.surface.background == \.accentSubtle)
        #expect(reacted.surface.foreground == \.accentStrong)
        #expect(plain.surface.foreground == \.textDefault)
        #expect(reacted.borderWidth > plain.borderWidth)
        #expect(reacted.isCountBold)
        #expect(!plain.isCountBold)
        let disabled = Tokens.appearance(isReacted: false, isEnabled: false)
        #expect(disabled.surface.foreground == \.textDisabled)
    }

    @Test("Interactive chips reach 44 points; read-only chips keep their size")
    func hitInset() {
        for size in MonarchReactionBarSize.allCases {
            let inset = Tokens.hitInset(size, isInteractive: true)
            #expect(Tokens.minHeight(size) + inset * 2 >= MonarchAccessibility.minimumHitTarget)
            #expect(Tokens.hitInset(size, isInteractive: false) == 0)
        }
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let labels = MonarchReactionBarLabels(reaction: { "\($0) × \($1)" })
        #expect(Tokens.chipLabel(MonarchReactionBarItem("🎉", count: 2, label: "Party"), labels: labels) == "Party × 2")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchReactionBarStrings.label == "Reactions")
        #expect(MonarchReactionBarStrings.add == "Add reaction")
        #expect(MonarchReactionBarStrings.reactedBy == "Reacted by")
    }

    @Test("A read-only bar without reactions draws nothing")
    func empty() {
        #expect(Host.layout(MonarchReactionBar([])).height < 1)
        #expect(Host.layout(MonarchReactionBar([]) { _, _ in }).height > 0)
    }
}
