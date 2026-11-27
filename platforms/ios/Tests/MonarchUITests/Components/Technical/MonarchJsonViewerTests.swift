import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let payload: MonarchJsonValue = [
    "id": "evt_1",
    "type": "invoice.paid",
    "amount": 4200,
    "paid": true,
    "refund": nil,
    "lines": [["sku": "A1", "qty": 2], ["sku": "B7", "qty": 1]],
    "customer name": "Ada Lovelace",
]

enum MonarchJsonViewerFixtures: ComponentFixtures {
    static let id = "json-viewer"
    static let fixtures = [
        Fixture("object, one level") {
            MonarchJsonViewer(payload)
        },
        Fixture("expanded two levels, empty root path") {
            MonarchJsonViewer(payload, expandDepth: 2, rootPath: "")
        },
        Fixture("collapsed root, no search, no footer") {
            MonarchJsonViewer(payload, expandDepth: 0, isSearchable: false, isCopyable: false)
        },
        Fixture("large array behind Show more") {
            MonarchJsonViewer(.array((0..<30).map { .number(String($0)) }), maxChildren: 5)
        },
        Fixture("long string cut, primitives, empty containers") {
            MonarchJsonViewer(
                [
                    "text": .string(String(repeating: "A long value that keeps going. ", count: 20)),
                    "empty object": [:],
                    "empty array": [],
                    "pi": 3.14,
                    "no": false,
                ],
                maxStringLength: 40
            )
        },
        Fixture("primitive root") {
            MonarchJsonViewer("just a string")
        },
    ]
}

@Suite("MonarchJsonViewer")
struct MonarchJsonViewerTests {
    typealias Model = MonarchJsonViewerModel

    // Before, the rows were in a LazyVStack with no scroll view. This
    // fixture at the largest text size never finished layout on the iOS
    // Simulator (an hour at full CPU): rows of different heights near the
    // bottom of the 844-point window were swapped between measured sizes and
    // estimates without end. A taller tree of the same rows did lay out, so
    // the test uses the exact fixture and condition.
    @Test("Wrapped rows at the largest text size lay out, and the same each time")
    @MainActor
    func wrappedRowsLayOut() {
        let name = "long string cut, primitives, empty containers"
        guard let fixture = MonarchJsonViewerFixtures.fixtures.first(where: { $0.name == name }) else {
            Issue.record("The long string fixture is missing")
            return
        }
        for theme in ThemeCase.all {
            let view = fixture.make().rendered(in: theme, .largestText)
            let first = Host.layout(view)
            #expect(first.height > 0)
            #expect(Host.layout(view) == first, "\(theme) changes size between layouts")
        }
    }

    @Test("Parsing keeps key order, escapes and number text")
    func parsing() throws {
        let value = try MonarchJsonValue(parsing: #"{"b": 1, "a": [true, null, -1.50e3], "s": "x\n\"y\" é 😀"}"#)
        guard case .object(let members) = value else {
            Issue.record("Expected an object")
            return
        }
        #expect(members.map(\.key) == ["b", "a", "s"])
        #expect(members[0].value == .number("1"))
        #expect(members[1].value == .array([.bool(true), .null, .number("-1.50e3")]))
        #expect(members[2].value == .string("x\n\"y\" é 😀"))
        #expect(try MonarchJsonValue(data: Data(" [ ] ".utf8)) == .array([]))
        #expect(try MonarchJsonValue(parsing: "\u{FEFF}{}") == .object([]))
    }

    @Test("Invalid JSON throws with the position of the problem")
    func invalid() {
        #expect(throws: MonarchJsonParseError(offset: 6)) { try MonarchJsonValue(parsing: #"{"a": }"#) }
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: "[1,]") }
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: "01x") }
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: "\"open") }
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: "tru") }
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: "1 2") }
        let deep = String(repeating: "[", count: 150) + String(repeating: "]", count: 150)
        #expect(throws: MonarchJsonParseError.self) { try MonarchJsonValue(parsing: deep) }
        #expect(MonarchJsonParseError(offset: 3).description == "Invalid JSON at byte 3")
    }

    @Test("Paths are JSONPath, with quoted keys that are not identifiers")
    func paths() {
        let segments: [MonarchJsonViewerSegment] = [.key("users"), .index(0), .key("first name"), .key("id")]
        #expect(Model.path(segments, root: "$") == #"$.users[0]["first name"].id"#)
        #expect(Model.path(segments, root: "") == #"users[0]["first name"].id"#)
        #expect(Model.path([], root: "$") == "$")
        #expect(Model.path([.key("a\"b")], root: "$") == #"$["a\"b"]"#)
        #expect(Model.path([.key("9lives")], root: "$") == #"$["9lives"]"#)
        #expect(Model.path([.key("_ok$")], root: "$") == "$._ok$")
    }

    @Test("Values are copied as their own text or as indented JSON")
    func stringify() {
        #expect(Model.copyText(.string("plain")) == "plain")
        #expect(
            Model.copyText(["a": 1, "b": [true, nil]]) == "{\n  \"a\": 1,\n  \"b\": [\n    true,\n    null\n  ]\n}"
        )
        #expect(Model.copyText([:]) == "{}")
        #expect(Model.copyText([]) == "[]")
        #expect(Model.quoted("tab\there\u{01}") == #""tab\there\u0001""#)
    }

    @Test("Only open containers are flattened, in document order")
    func rows() {
        let closed = Model.rows(payload, isOpen: { _, depth in depth < 0 }, limits: [:], maxChildren: 100, reveal: [:])
        #expect(closed.map(\.id) == ["$"])
        let one = Model.rows(payload, isOpen: { _, depth in depth < 1 }, limits: [:], maxChildren: 100, reveal: [:])
        #expect(
            one.map(\.id) == [
                "$", "$.id", "$.type", "$.amount", "$.paid", "$.refund", "$.lines", #"$["customer name"]"#,
            ]
        )
        #expect(one[6].count == 2)
        #expect(one[6].isExpandable)
        #expect(!one[1].isExpandable)
        #expect(one[6].parentID == "$")
        #expect(one[6].depth == 1)
        let two = Model.rows(payload, isOpen: { _, depth in depth < 2 }, limits: [:], maxChildren: 100, reveal: [:])
        #expect(two.contains { $0.id == "$.lines[1]" })
        #expect(!two.contains { $0.id == "$.lines[1].sku" })
    }

    @Test("Large containers show a Show more row and grow by limits")
    func showMore() {
        let array = MonarchJsonValue.array((0..<12).map { .number(String($0)) })
        let rows = Model.rows(array, isOpen: { _, _ in true }, limits: [:], maxChildren: 5, reveal: [:])
        #expect(rows.count == 1 + 5 + 1)
        let more = rows[6]
        #expect(more.kind == .more)
        #expect(more.shown == 5)
        #expect(more.remaining == 7)
        #expect(more.parentID == "$")
        #expect(Model.firstRevealedID(after: more, in: array) == "$[5]")
        let grown = Model.rows(array, isOpen: { _, _ in true }, limits: ["$": 10], maxChildren: 5, reveal: [:])
        #expect(grown.last?.remaining == 2)
        let revealed = Model.rows(array, isOpen: { _, _ in true }, limits: [:], maxChildren: 5, reveal: ["$": 12])
        #expect(revealed.allSatisfy { $0.kind == .node })
        #expect(Model.firstRevealedID(after: rows[1], in: array) == nil)
    }

    @Test("Search matches keys and primitive values, opens their containers and stops at the cap")
    func search() {
        let result = Model.search(payload, needle: "b7", maxMatches: 500)
        #expect(result.matched == ["$.lines[1].sku"])
        #expect(result.open == ["$", "$.lines", "$.lines[1]"])
        #expect(result.reveal["$.lines"] == 2)
        #expect(result.count == 1)
        #expect(!result.isCapped)
        let keys = Model.search(payload, needle: "name", maxMatches: 500)
        #expect(keys.matched == [#"$["customer name"]"#])
        let literal = Model.search(payload, needle: "null", maxMatches: 500)
        #expect(literal.matched == ["$.refund"])
        let capped = Model.search(.array((0..<50).map { _ in "x" }), needle: "x", maxMatches: 10)
        #expect(capped.count == 10)
        #expect(capped.isCapped)
        #expect(Model.search(payload, needle: "", maxMatches: 500) == .empty)
    }

    @Test("A container opens by choice, then by search, then by depth")
    func isOpen() {
        var search = MonarchJsonViewerSearch()
        search.open = ["$.lines"]
        #expect(Model.isOpen(id: "$.lines", depth: 1, overrides: [:], search: search, baseDepth: 1))
        #expect(!Model.isOpen(id: "$.lines", depth: 1, overrides: ["$.lines": false], search: search, baseDepth: 1))
        #expect(Model.isOpen(id: "$", depth: 0, overrides: [:], search: .empty, baseDepth: 1))
        #expect(!Model.isOpen(id: "$.x", depth: 1, overrides: [:], search: .empty, baseDepth: 1))
        #expect(Model.isOpen(id: "$.x", depth: 1, overrides: ["$.x": true], search: .empty, baseDepth: 0))
    }

    @Test("Summaries and spoken labels")
    func summaries() {
        let labels = MonarchJsonViewerLabels()
        #expect(Model.summary(["a": 1, "b": 2, "c": 3], maxStringLength: 10, labels: labels) == "{ } 3 keys")
        #expect(Model.summary(["a": 1], maxStringLength: 10, labels: labels) == "{ } 1 key")
        #expect(Model.summary([1, 2], maxStringLength: 10, labels: labels) == "[ ] 2 items")
        #expect(Model.summary([1], maxStringLength: 10, labels: labels) == "[ ] 1 item")
        #expect(Model.summary("short", maxStringLength: 10, labels: labels) == "\"short\"")
        #expect(Model.summary("abcdefghijkl", maxStringLength: 4, labels: labels) == "\"abcd… (12 characters)\"")
        #expect(Model.summary(.number("1.50"), maxStringLength: 4, labels: labels) == "1.50")
        #expect(Model.summary(false, maxStringLength: 4, labels: labels) == "false")
        #expect(Model.summary(nil, maxStringLength: 4, labels: labels) == "null")
        #expect(Model.accessibilityLabel(name: .key("id"), summary: "\"x\"") == "id: \"x\"")
        #expect(Model.accessibilityLabel(name: .index(3), summary: "1") == "3: 1")
        #expect(Model.accessibilityLabel(name: nil, summary: "{ } 1 key") == "{ } 1 key")
    }

    @Test("Matched text is found ignoring case, every occurrence")
    func matchRanges() {
        let text = "Ada and ADA"
        let ranges = Model.matchRanges(in: text, needle: "ada")
        #expect(ranges.map { String(text[$0]) } == ["Ada", "ADA"])
        #expect(Model.matchRanges(in: text, needle: "").isEmpty)
        #expect(Model.matchRanges(in: text, needle: "zzz").isEmpty)
    }

    @Test("Built-in strings resolve and every one can be replaced")
    func strings() {
        let labels = MonarchJsonViewerLabels()
        #expect(labels.resolvedLabel == "JSON")
        #expect(labels.resolvedSearch == "Search keys and values")
        #expect(labels.resolvedSearchPrompt == "Search")
        #expect(labels.resolvedClearSearch == "Clear search")
        #expect(labels.resolvedCollapseAll == "Collapse all")
        #expect(labels.resolvedCopyPath == "Copy path")
        #expect(labels.resolvedCopyValue == "Copy value")
        #expect(labels.resolvedSelectedPath == "Selected")
        #expect(labels.resolvedExpanded == "expanded")
        #expect(labels.resolvedCollapsed == "collapsed")
        #expect(labels.matchesText(0, isCapped: false) == "No matches")
        #expect(labels.matchesText(1, isCapped: false) == "1 match")
        #expect(labels.matchesText(12, isCapped: false) == "12 matches")
        #expect(labels.matchesText(500, isCapped: true) == "500+ matches")
        #expect(labels.showMoreText(next: 5, remaining: 7) == "Show 5 more (7 hidden)")

        let custom = MonarchJsonViewerLabels(
            label: "Daten",
            matches: { count, _ in "\(count) Treffer" },
            keys: { "\($0) Schlüssel" },
            items: { "\($0) Einträge" },
            showMore: { next, _ in "+\(next)" },
            truncated: { _ in "…" },
            expanded: "offen",
            collapsed: "zu"
        )
        #expect(custom.resolvedLabel == "Daten")
        #expect(custom.matchesText(2, isCapped: false) == "2 Treffer")
        #expect(custom.keysText(1) == "1 Schlüssel")
        #expect(custom.itemsText(2) == "2 Einträge")
        #expect(custom.showMoreText(next: 3, remaining: 9) == "+3")
        #expect(custom.truncatedText(99) == "…")
        #expect(custom.resolvedExpanded == "offen")
        #expect(custom.resolvedCollapsed == "zu")
    }

    @Test("Colours follow the value kind and the chevron the open state")
    func appearance() {
        #expect(MonarchJsonViewerTokens.valueRole(.string) == \.successStrong)
        #expect(MonarchJsonViewerTokens.valueRole(.number) == \.accentStrong)
        #expect(MonarchJsonViewerTokens.valueRole(.bool) == \.warningStrong)
        #expect(MonarchJsonViewerTokens.valueRole(.null) == \.warningStrong)
        #expect(MonarchJsonViewerTokens.valueRole(.object) == \.textMuted)
        #expect(MonarchJsonViewerTokens.keyRole(.key("a")) == \.infoStrong)
        #expect(MonarchJsonViewerTokens.keyRole(.index(0)) == \.textMuted)
        #expect(MonarchJsonViewerTokens.chevron(isExpandable: false, isOpen: true) == nil)
        #expect(MonarchJsonViewerTokens.chevron(isExpandable: true, isOpen: false) == "chevron.forward")
        #expect(MonarchJsonViewerTokens.chevron(isExpandable: true, isOpen: true) == "chevron.down")
    }

    @Test("Literals build the same values as parsing")
    func literals() throws {
        let literal: MonarchJsonValue = ["a": [1, 2.5, "x", true, nil]]
        #expect(try MonarchJsonValue(parsing: #"{"a":[1,2.5,"x",true,null]}"#) == literal)
        #expect(literal != ["a": []])
        #expect(MonarchJsonValue.object([("a", 1)]) != .object([("b", 1)]))
    }

    @Test("A repeated key keeps its first position and its last value")
    func repeatedKey() throws {
        #expect(try MonarchJsonValue(parsing: #"{"a": 1, "b": 2, "a": 3}"#) == ["a": 3, "b": 2])
    }
}
