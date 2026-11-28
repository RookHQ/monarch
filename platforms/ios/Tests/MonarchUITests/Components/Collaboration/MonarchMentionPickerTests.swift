import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let query = MonarchMentionQuery.find(in: "Thanks @an")
private let items = [
    MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
    MonarchMentionItem(id: "u2", label: "Andre Kim", detail: "No access to this conversation", isDisabled: true),
    MonarchMentionItem(id: "c1", label: "announcements", kind: .channel, detail: "214 members"),
]

enum MonarchMentionPickerFixtures: ComponentFixtures {
    static let id = "mention-picker"
    static let fixtures = [
        Fixture("people and channels, one disabled") {
            MonarchMentionPicker(query, items: items) { _ in }
        },
        Fixture("people only") {
            MonarchMentionPicker(query, items: Array(items.prefix(2))) { _ in }
        },
        Fixture("many, scrolling") {
            MonarchMentionPicker(
                query,
                items: (1...8).map { MonarchMentionItem(id: "\($0)", label: "Person \($0)", detail: "Team \($0)") }
            ) { _ in }
        },
        Fixture("loading") {
            MonarchMentionPicker(query, items: [], status: .loading) { _ in }
        },
        Fixture("failed with retry, failed without") {
            VStack {
                MonarchMentionPicker(query, items: [], status: .failed, onRetry: {}, onSelect: { _ in })
                MonarchMentionPicker(query, items: [], status: .failed) { _ in }
            }
        },
        Fixture("no matches") {
            MonarchMentionPicker(query, items: []) { _ in }
        },
        Fixture("closed") {
            MonarchMentionPicker(nil, items: items) { _ in }
        },
    ]
}

@Suite("MonarchMentionPicker")
@MainActor
struct MonarchMentionPickerTests {
    typealias Tokens = MonarchMentionPickerTokens
    let labels = MonarchMentionPickerLabels()

    @Test("A mention is found at the start, after a space or a parenthesis")
    func findsQuery() throws {
        let start = try #require(MonarchMentionQuery.find(in: "@an"))
        #expect(start.trigger == "@")
        #expect(start.text == "an")
        #expect(MonarchMentionQuery.find(in: "hi @ana")?.text == "ana")
        #expect(MonarchMentionQuery.find(in: "(@ana")?.text == "ana")
        #expect(MonarchMentionQuery.find(in: "hi @")?.text == "")
        #expect(MonarchMentionQuery.find(in: "hi @ana lo")?.text == "ana lo")
    }

    @Test("No mention inside a word, after two words, across a line break or for a long word")
    func rejectsQuery() {
        #expect(MonarchMentionQuery.find(in: "mail@example") == nil)
        #expect(MonarchMentionQuery.find(in: "hi @ana lopez now") == nil)
        #expect(MonarchMentionQuery.find(in: "hi @ana\nlopez") == nil)
        #expect(MonarchMentionQuery.find(in: "@" + String(repeating: "a", count: 41)) == nil)
        #expect(MonarchMentionQuery.find(in: "no trigger") == nil)
    }

    @Test("Other triggers and a caret before the end are honoured")
    func triggersAndCaret() throws {
        #expect(MonarchMentionQuery.find(in: "see #gen", triggers: ["@", "#"])?.trigger == "#")
        #expect(MonarchMentionQuery.find(in: "see #gen") == nil)
        let text = "hi @an and more"
        let caret = try #require(text.range(of: "@an")?.upperBound)
        #expect(MonarchMentionQuery.find(in: text, caret: caret)?.text == "an")
    }

    @Test("Replacing a query writes the mention and one space, without doubling a space")
    func replacing() throws {
        let text = "Thanks @an"
        let found = try #require(MonarchMentionQuery.find(in: text))
        #expect(found.replacing(in: text, with: "@Ana Lopez") == "Thanks @Ana Lopez ")
        let middle = "hi @an and more"
        let caret = try #require(middle.range(of: "@an")?.upperBound)
        let inner = try #require(MonarchMentionQuery.find(in: middle, caret: caret))
        #expect(inner.replacing(in: middle, with: "@Ana") == "hi @Ana and more")
    }

    @Test("The token matches the web format and drops brackets from the label")
    func token() {
        #expect(MonarchMentionItem(id: "u_123", label: "Ana Lopez").token() == "@[Ana Lopez](u_123)")
        #expect(MonarchMentionItem(id: "c1", label: "general (all)").token(trigger: "#") == "#[general all](c1)")
    }

    @Test("The picker hides when closed, and after a query with a space found nothing")
    func isOpen() {
        let spaced = MonarchMentionQuery.find(in: "@ana lo")
        #expect(!MonarchMentionSuggestions.isOpen(query: nil, status: .ready, itemCount: 3))
        #expect(MonarchMentionSuggestions.isOpen(query: query, status: .ready, itemCount: 0))
        #expect(!MonarchMentionSuggestions.isOpen(query: spaced, status: .ready, itemCount: 0))
        #expect(MonarchMentionSuggestions.isOpen(query: spaced, status: .loading, itemCount: 0))
        #expect(MonarchMentionSuggestions.isOpen(query: spaced, status: .ready, itemCount: 1))
    }

    @Test("People and channels are grouped with headings only when both are present")
    func groups() {
        let both = Tokens.groups(items)
        #expect(both.map(\.kind) == [.person, .channel])
        #expect(both[0].items.count == 2)
        #expect(Tokens.heading(.person, groupCount: 2, labels: labels) == "People")
        #expect(Tokens.heading(.channel, groupCount: 2, labels: labels) == "Channels")
        let people = Tokens.groups(Array(items.prefix(2)))
        #expect(people.count == 1)
        #expect(Tokens.heading(.person, groupCount: 1, labels: labels) == nil)
        #expect(Tokens.groups([MonarchMentionItem(id: "c", label: "c", kind: .channel)]).first?.kind == .channel)
    }

    @Test("Each state of the search has its own announcement")
    func announcement() {
        #expect(Tokens.announcement(status: .loading, count: 0, labels: labels) == "Searching…")
        #expect(Tokens.announcement(status: .failed, count: 0, labels: labels) == "Couldn’t load suggestions.")
        #expect(Tokens.announcement(status: .ready, count: 0, labels: labels) == "No matches")
        #expect(Tokens.announcement(status: .ready, count: 1, labels: labels) == "1 suggestion")
        #expect(Tokens.announcement(status: .ready, count: 4, labels: labels) == "4 suggestions")
    }

    @Test("Disabled suggestions use the disabled text colour")
    func colors() {
        #expect(Tokens.textColors(isDisabled: true).label == \.textDisabled)
        #expect(Tokens.textColors(isDisabled: false).label == \.textDefault)
        #expect(Tokens.textColors(isDisabled: false).detail == \.textMuted)
    }

    @Test("More than four suggestions scroll")
    func scrolls() {
        #expect(!Tokens.scrolls(itemCount: 4))
        #expect(Tokens.scrolls(itemCount: 5))
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchMentionPickerLabels(
            people: "Personen",
            empty: "Keine Treffer",
            count: { "\($0) Vorschläge" }
        )
        #expect(Tokens.heading(.person, groupCount: 2, labels: custom) == "Personen")
        #expect(Tokens.announcement(status: .ready, count: 0, labels: custom) == "Keine Treffer")
        #expect(Tokens.announcement(status: .ready, count: 2, labels: custom) == "2 Vorschläge")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchMentionPickerStrings.label == "Suggestions")
        #expect(MonarchMentionPickerStrings.people == "People")
    }

    @Test("A closed picker draws nothing")
    func closed() {
        #expect(Host.layout(MonarchMentionPicker(nil, items: items) { _ in }).height < 1)
        #expect(Host.layout(MonarchMentionPicker(query, items: items) { _ in }).height > 0)
    }
}
