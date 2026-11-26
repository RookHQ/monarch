import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSearchResultItemFixtures: ComponentFixtures {
    static let id = "search-result-item"
    static let fixtures = [
        Fixture("interactive, with path, snippet, file icon and meta") {
            MonarchSearchResultItem(
                "Travel policy",
                path: ["Handbook", "Finance", "Travel"],
                snippet: "Book travel at least two weeks ahead and keep every receipt for the claim.",
                terms: ["travel", "receipt"],
                fileName: "travel-policy.pdf",
                action: {},
                meta: { MonarchBadge("PDF") }
            )
        },
        Fixture("plain, title only") {
            MonarchSearchResultItem("Quarterly planning")
        },
        Fixture("mark tags and ranges") {
            VStack {
                MonarchSearchResultItem(
                    "Roadmap",
                    snippet: "The <mark>roadmap</mark> for <b>next</b> quarter.",
                    snippetHasMarkTags: true,
                    systemImage: "doc.text"
                )
                MonarchSearchResultItem("Design review", titleRanges: [0..<6], systemImage: "person.2", action: {})
            }
        },
        Fixture("long text and overridden path label") {
            MonarchSearchResultItem(
                "A result whose title is long enough to wrap onto a second and a third line on a phone",
                path: ["A very long folder name", "Another folder", "Yet another folder", "Leaf"],
                snippet: String(repeating: "A snippet that goes on and on. ", count: 10),
                terms: ["snippet"],
                pathLabel: "Ort",
                action: {},
                meta: { Text("2 days ago") }
            )
        },
        Fixture("disabled") {
            MonarchSearchResultItem("Archived", path: ["Archive"], action: {}).disabled(true)
        },
    ]
}

@Suite("MonarchSearchResultItem")
struct MonarchSearchResultItemTests {
    typealias Tokens = MonarchSearchResultItemTokens
    typealias Segment = MonarchSearchResultItemTokens.Segment

    @Test("Query words are letters and digits, lower-cased, without repeats, at most eight")
    func terms() {
        #expect(MonarchSearchResultItem.terms(from: "Travel, travel POLICY 2024!") == ["travel", "policy", "2024"])
        #expect(MonarchSearchResultItem.terms(from: "  ").isEmpty)
        #expect(MonarchSearchResultItem.terms(from: "a b c d e f g h i j").count == 8)
        #expect(MonarchSearchResultItem.terms(from: "Café über") == ["café", "über"])
    }

    @Test("Terms match without regard to case and overlapping matches merge")
    func termSegments() {
        let segments = Tokens.segments(
            "Travel and TRAVELLING",
            terms: ["travel", "trav"],
            ranges: nil,
            markTags: false
        )
        #expect(
            segments == [
                Segment(text: "Travel", isMatch: true),
                Segment(text: " and ", isMatch: false),
                Segment(text: "TRAVEL", isMatch: true),
                Segment(text: "LING", isMatch: false),
            ]
        )
    }

    @Test("No terms give one unmatched run; empty text gives none")
    func noTerms() {
        #expect(
            Tokens.segments("Hello", terms: [], ranges: nil, markTags: false) == [
                Segment(text: "Hello", isMatch: false)
            ]
        )
        #expect(Tokens.segments("Hello", terms: ["  "], ranges: nil, markTags: false).count == 1)
        #expect(Tokens.segments("", terms: ["a"], ranges: nil, markTags: false).isEmpty)
    }

    @Test("Ranges are UTF-16 offsets, clamped, merged and widened to whole characters")
    func ranges() {
        #expect(
            Tokens.segments("abcdef", terms: ["zzz"], ranges: [3..<5, 0..<1, 4..<6], markTags: false) == [
                Segment(text: "a", isMatch: true),
                Segment(text: "bc", isMatch: false),
                Segment(text: "def", isMatch: true),
            ]
        )
        #expect(
            Tokens.segments("abc", terms: [], ranges: [-4..<1, 2..<99], markTags: false) == [
                Segment(text: "a", isMatch: true),
                Segment(text: "b", isMatch: false),
                Segment(text: "c", isMatch: true),
            ]
        )
        // The emoji is two UTF-16 units; an offset inside it covers the whole emoji.
        #expect(
            Tokens.segments("a😀b", terms: [], ranges: [1..<2], markTags: false) == [
                Segment(text: "a", isMatch: false),
                Segment(text: "😀", isMatch: true),
                Segment(text: "b", isMatch: false),
            ]
        )
    }

    @Test("Only mark markers are read; other markup stays text")
    func markTags() {
        let segments = Tokens.segments(
            "The <MARK>road</mark> to <b>x</b> < y <mark>end",
            terms: ["the"],
            ranges: nil,
            markTags: true
        )
        #expect(
            segments == [
                Segment(text: "The ", isMatch: false),
                Segment(text: "road", isMatch: true),
                Segment(text: " to <b>x</b> < y ", isMatch: false),
                Segment(text: "end", isMatch: true),
            ]
        )
        #expect(Tokens.plainText("a <mark>b</mark> c", markTags: true) == "a b c")
        #expect(Tokens.plainText("a <mark>b</mark> c", markTags: false) == "a <mark>b</mark> c")
    }

    @Test("The path is read with its label and without separators")
    func pathLabel() {
        #expect(
            Tokens.pathAccessibilityLabel(["Handbook", "Travel"], pathLabel: "Location") == "Location: Handbook, Travel"
        )
        #expect(Tokens.pathAccessibilityLabel(["", "Travel"], pathLabel: "Ort") == "Ort: Travel")
        #expect(Tokens.pathAccessibilityLabel([], pathLabel: "Location") == nil)
    }

    @Test("The built-in path label resolves")
    func strings() {
        #expect(MonarchSearchResultItemStrings.path == "Location")
    }

    @Test("Colours: link title when interactive, subtle fill while pressed, warning highlight")
    func colours() {
        #expect(Tokens.titleRole(isInteractive: true) == \MonarchColorTokens.textLink)
        #expect(Tokens.titleRole(isInteractive: false) == \MonarchColorTokens.textDefault)
        #expect(Tokens.rowFill(isPressed: true) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.rowFill(isPressed: false) == nil)
        #expect(Tokens.highlightBackground == \MonarchColorTokens.warningSubtle)
        #expect(Tokens.highlightForeground == \MonarchColorTokens.textDefault)
    }

    #if os(iOS)
        @Test("An interactive row is at least 44 points high")
        @MainActor
        func touchTarget() {
            let size = Host.layout(MonarchSearchResultItem("A", action: {}))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
