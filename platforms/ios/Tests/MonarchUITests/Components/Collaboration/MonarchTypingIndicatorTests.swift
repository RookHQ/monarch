import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTypingIndicatorFixtures: ComponentFixtures {
    static let id = "typing-indicator"
    static let fixtures = [
        Fixture("one, two and several people") {
            VStack(alignment: .leading) {
                MonarchTypingIndicator(["Ana"])
                MonarchTypingIndicator(["Ana", "Ben"])
                MonarchTypingIndicator(["Ana", "Ben", "Chen"])
            }
        },
        Fixture("nobody, space reserved") {
            MonarchTypingIndicator([])
        },
        Fixture("nobody, no space reserved, under a message") {
            VStack(alignment: .leading) {
                Text("See you at ten.")
                MonarchTypingIndicator([], reservesSpace: false)
            }
        },
        Fixture("custom sentence, long names") {
            MonarchTypingIndicator(
                ["Maximilian Alexander von Hohenberg-Lichtenstein", "Anastasia Konstantinopolskaya"],
                text: "Maximilian Alexander von Hohenberg-Lichtenstein and one other person are writing a reply",
                announcesChanges: false
            )
        },
    ]
}

@Suite("MonarchTypingIndicator")
@MainActor
struct MonarchTypingIndicatorTests {
    @Test("The sentence is worded for one, two and several people")
    func sentence() {
        typealias Tokens = MonarchTypingIndicatorTokens
        #expect(Tokens.sentence(names: [], text: nil).isEmpty)
        #expect(Tokens.sentence(names: ["Ana"], text: nil) == "Ana is typing…")
        #expect(Tokens.sentence(names: ["Ana", "Ben"], text: nil) == "Ana and Ben are typing…")
        #expect(Tokens.sentence(names: ["Ana", "Ben", "Chen"], text: nil) == "Several people are typing…")
    }

    @Test("Empty names do not count as people")
    func emptyNames() {
        #expect(MonarchTypingIndicatorTokens.sentence(names: ["", "  "], text: nil).isEmpty)
        #expect(MonarchTypingIndicatorTokens.sentence(names: ["", "Ana"], text: nil) == "Ana is typing…")
    }

    @Test("A custom sentence replaces the built-in one only while somebody is typing")
    func customText() {
        #expect(MonarchTypingIndicatorTokens.sentence(names: ["Ana"], text: "Ana schreibt…") == "Ana schreibt…")
        #expect(MonarchTypingIndicatorTokens.sentence(names: [], text: "Ana schreibt…").isEmpty)
        #expect(MonarchTypingIndicatorTokens.sentence(names: ["Ana"], text: "") == "Ana is typing…")
    }

    @Test("A new sentence is announced unless announcements are off or nobody is typing")
    func announcement() {
        typealias Tokens = MonarchTypingIndicatorTokens
        #expect(Tokens.announcement(sentence: "Ana is typing…", announcesChanges: true) == "Ana is typing…")
        #expect(Tokens.announcement(sentence: "Ana is typing…", announcesChanges: false) == nil)
        #expect(Tokens.announcement(sentence: "", announcesChanges: true) == nil)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchTypingIndicatorStrings.one("Ana") == "Ana is typing…")
        #expect(MonarchTypingIndicatorStrings.two("Ana", "Ben") == "Ana and Ben are typing…")
        #expect(MonarchTypingIndicatorStrings.several == "Several people are typing…")
    }

    @Test("The line keeps its height while nobody is typing, unless asked not to")
    func reservedSpace() {
        let active = Host.layout(MonarchTypingIndicator(["Ana"]))
        let reserved = Host.layout(MonarchTypingIndicator([]))
        let collapsed = Host.layout(MonarchTypingIndicator([], reservesSpace: false))
        #expect(reserved.height > 0)
        #expect(abs(reserved.height - active.height) < 1)
        #expect(collapsed.height < 1)
    }
}
