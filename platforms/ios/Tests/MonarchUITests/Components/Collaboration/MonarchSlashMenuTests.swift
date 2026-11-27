import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let items = [
    MonarchSlashMenuItem(
        id: "heading",
        label: "Heading",
        description: "Large section title",
        systemImage: "textformat.size",
        group: "Text",
        keywords: ["title", "h2"]
    ),
    MonarchSlashMenuItem(
        id: "quote",
        label: "Quote",
        description: "Indented quotation",
        systemImage: "text.quote",
        group: "Text"
    ),
    MonarchSlashMenuItem(
        id: "bullets",
        label: "Bulleted list",
        description: "A simple list",
        systemImage: "list.bullet",
        group: "Lists"
    ),
    MonarchSlashMenuItem(
        id: "checklist",
        label: "Checklist",
        description: "Items to tick off",
        systemImage: "checklist",
        group: "Lists",
        keywords: ["todo", "task"]
    ),
    MonarchSlashMenuItem(
        id: "date",
        label: "Date",
        description: "Insert today’s date",
        systemImage: "calendar",
        group: "Insert"
    ),
    MonarchSlashMenuItem(
        id: "table",
        label: "Table",
        description: "Not available in replies",
        systemImage: "tablecells",
        group: "Insert",
        isDisabled: true
    ),
]

enum MonarchSlashMenuFixtures: ComponentFixtures {
    static let id = "slash-menu"
    static let fixtures = [
        Fixture("every item, grouped, scrolling, one disabled") {
            MonarchSlashMenu(
                MonarchSlashMenuQuery.find(in: "/"),
                items: items,
                groupOrder: ["Text", "Lists", "Insert"]
            ) { _ in }
        },
        Fixture("filtered") {
            MonarchSlashMenu(MonarchSlashMenuQuery.find(in: "/li"), items: items) { _ in }
        },
        Fixture("no groups, no icons, no descriptions") {
            MonarchSlashMenu(
                MonarchSlashMenuQuery.find(in: "/"),
                items: [
                    MonarchSlashMenuItem(id: "a", label: "Divider"), MonarchSlashMenuItem(id: "b", label: "Page break"),
                ]
            ) { _ in }
        },
        Fixture("long text") {
            MonarchSlashMenu(
                MonarchSlashMenuQuery.find(in: "/"),
                items: [
                    MonarchSlashMenuItem(
                        id: "long",
                        label: "A command with a very long name that has to wrap",
                        description: "And a description that is long enough to wrap onto a second line at any size",
                        systemImage: "text.alignleft",
                        group: "A group heading that is long too"
                    )
                ]
            ) { _ in }
        },
        Fixture("nothing matches") {
            MonarchSlashMenu(MonarchSlashMenuQuery.find(in: "/zzz"), items: items) { _ in }
        },
        Fixture("closed") {
            MonarchSlashMenu(nil, items: items) { _ in }
        },
    ]
}

@Suite("MonarchSlashMenu")
@MainActor
struct MonarchSlashMenuTests {
    typealias Tokens = MonarchSlashMenuTokens
    let labels = MonarchSlashMenuLabels()

    @Test("A command is found at the start of a line, with letters, digits and hyphens")
    func findsQuery() throws {
        let start = try #require(MonarchSlashMenuQuery.find(in: "/"))
        #expect(start.trigger == "/")
        #expect(start.text == "")
        #expect(MonarchSlashMenuQuery.find(in: "/head")?.text == "head")
        #expect(MonarchSlashMenuQuery.find(in: "Intro\n/to-do2")?.text == "to-do2")
        let text = "Intro\n/h"
        let found = try #require(MonarchSlashMenuQuery.find(in: text))
        #expect(text[found.range] == "/h")
    }

    @Test("No command inside a line, after a space, in a path or past the longest query")
    func rejectsQuery() {
        #expect(MonarchSlashMenuQuery.find(in: "1/2") == nil)
        #expect(MonarchSlashMenuQuery.find(in: "see /head") == nil)
        #expect(MonarchSlashMenuQuery.find(in: "/usr/bin") == nil)
        #expect(MonarchSlashMenuQuery.find(in: "/head ing") == nil)
        #expect(MonarchSlashMenuQuery.find(in: "/" + String(repeating: "a", count: 25)) == nil)
        #expect(MonarchSlashMenuQuery.find(in: "/" + String(repeating: "a", count: 24)) != nil)
        #expect(MonarchSlashMenuQuery.find(in: "no trigger") == nil)
    }

    @Test("Word start also opens after a space; other triggers and a caret are honoured")
    func positionTriggerCaret() throws {
        #expect(MonarchSlashMenuQuery.find(in: "see /head", position: .wordStart)?.text == "head")
        #expect(MonarchSlashMenuQuery.find(in: "1/2", position: .wordStart) == nil)
        #expect(MonarchSlashMenuQuery.find(in: "!cmd", trigger: "!")?.trigger == "!")
        #expect(MonarchSlashMenuQuery.find(in: "/cmd", maxLength: 2) == nil)
        let text = "/he and more"
        let caret = try #require(text.range(of: "/he")?.upperBound)
        #expect(MonarchSlashMenuQuery.find(in: text, caret: caret)?.text == "he")
    }

    @Test("Replacing a query writes the replacement in place of the slash and the query")
    func replacing() throws {
        let text = "Intro\n/head"
        let found = try #require(MonarchSlashMenuQuery.find(in: text))
        #expect(found.replacing(in: text, with: "## ") == "Intro\n## ")
        #expect(found.replacing(in: text) == "Intro\n")
        let middle = "/he tail"
        let caret = try #require(middle.range(of: "/he")?.upperBound)
        let inner = try #require(MonarchSlashMenuQuery.find(in: middle, caret: caret))
        #expect(inner.replacing(in: middle, with: "> ") == ">  tail")
    }

    @Test("Filtering ranks label prefixes, then label matches, then ids, descriptions and keywords")
    func filter() {
        #expect(MonarchSlashMenu.filter(items, query: "").map(\.id) == items.map(\.id))
        #expect(MonarchSlashMenu.filter(items, query: "li").map(\.id) == ["bullets", "checklist", "table"])
        #expect(MonarchSlashMenu.filter(items, query: "list").map(\.id) == ["bullets", "checklist"])
        #expect(MonarchSlashMenu.filter(items, query: "todo").map(\.id) == ["checklist"])
        #expect(MonarchSlashMenu.filter(items, query: "DATE").map(\.id) == ["date"])
        #expect(MonarchSlashMenu.filter(items, query: "zzz").isEmpty)
        let ranked = MonarchSlashMenu.filter(items, query: "t")
        #expect(ranked.first?.id == "table")
        #expect(ranked.map(\.id).firstIndex(of: "heading")! > ranked.map(\.id).firstIndex(of: "table")!)
    }

    @Test("Groups follow the given order, then the order they first appear")
    func groups() {
        let ordered = Tokens.groups(items, order: ["Insert"])
        #expect(ordered.map(\.heading) == ["Insert", "Text", "Lists"])
        #expect(ordered[0].items.map(\.id) == ["date", "table"])
        #expect(Tokens.groups(items, order: ["Missing"]).map(\.heading) == ["Text", "Lists", "Insert"])
        let mixed = [MonarchSlashMenuItem(id: "a", label: "A"), MonarchSlashMenuItem(id: "b", label: "B", group: "G")]
        #expect(Tokens.groups(mixed, order: []).map(\.heading) == [nil, "G"])
        #expect(Tokens.ordered(items, order: ["Lists"]).first?.id == "bullets")
    }

    @Test("A row is read as its label and description")
    func accessibilityLabel() {
        #expect(Tokens.accessibilityLabel(items[0]) == "Heading, Large section title")
        #expect(Tokens.accessibilityLabel(MonarchSlashMenuItem(id: "a", label: "Divider")) == "Divider")
    }

    @Test("The count and the empty text are announced, with the slash in the empty text")
    func announcement() throws {
        let query = try #require(MonarchSlashMenuQuery.find(in: "/zzz"))
        #expect(Tokens.announcement(count: 0, query: query, labels: labels) == "Nothing matches “/zzz”")
        #expect(Tokens.announcement(count: 1, query: query, labels: labels) == "1 option")
        #expect(Tokens.announcement(count: 4, query: query, labels: labels) == "4 options")
    }

    @Test("Overrides replace the built-in strings")
    func overrides() throws {
        let query = try #require(MonarchSlashMenuQuery.find(in: "/x"))
        let custom = MonarchSlashMenuLabels(
            label: "Einfügen",
            empty: { "Nichts für \($0)" },
            count: { "\($0) Optionen" }
        )
        #expect(Tokens.announcement(count: 0, query: query, labels: custom) == "Nichts für /x")
        #expect(Tokens.announcement(count: 2, query: query, labels: custom) == "2 Optionen")
    }

    @Test("Disabled items use the disabled text colour")
    func colors() {
        #expect(Tokens.colors(isDisabled: true).label == \.textDisabled)
        #expect(Tokens.colors(isDisabled: true).icon == \.textDisabled)
        #expect(Tokens.colors(isDisabled: false).label == \.textDefault)
        #expect(Tokens.colors(isDisabled: false).description == \.textMuted)
    }

    @Test("More than four rows scroll")
    func scrolls() {
        #expect(!Tokens.scrolls(itemCount: 4))
        #expect(Tokens.scrolls(itemCount: 5))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchSlashMenuStrings.label == "Insert block")
        #expect(MonarchSlashMenuStrings.empty("/a") == "Nothing matches “/a”")
        #expect(MonarchSlashMenuStrings.count(3) == "3 options")
    }

    @Test("A closed menu draws nothing")
    func closed() {
        #expect(Host.layout(MonarchSlashMenu(nil, items: items) { _ in }).height < 1)
        #expect(Host.layout(MonarchSlashMenu(MonarchSlashMenuQuery.find(in: "/"), items: items) { _ in }).height > 0)
    }
}
