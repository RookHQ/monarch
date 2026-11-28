import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPageOutlineFixtures: ComponentFixtures {
    static let id = "page-outline"
    static let headings = [
        MonarchPageOutlineHeading("Overview", level: 1, id: "overview"),
        MonarchPageOutlineHeading("Goals for the next quarter of the planning cycle", level: 2, id: "goals"),
        MonarchPageOutlineHeading("Risks", level: 3, id: "risks"),
        MonarchPageOutlineHeading("Appendix detail", level: 4, id: "appendix"),
        MonarchPageOutlineHeading("Timeline", level: 2, id: "timeline"),
    ]

    static let fixtures = [
        Fixture("nested, current heading, stats") {
            MonarchPageOutline(
                headings,
                currentID: "goals",
                stats: MonarchPageOutlineStats(words: 1_234, minutes: 6)
            ) { _ in }
        },
        Fixture("no current heading") {
            MonarchPageOutline(headings) { _ in }
        },
        Fixture("deeper levels, title hidden") {
            MonarchPageOutline(headings, currentID: "appendix", maxLevel: 6, isTitleHidden: true) { _ in }
        },
        Fixture("empty") {
            MonarchPageOutline([]) { _ in }
        },
        Fixture("custom labels") {
            MonarchPageOutline(
                [],
                stats: MonarchPageOutlineStats(words: 1, minutes: 1),
                labels: MonarchPageOutlineLabels(
                    title: "Contents",
                    empty: "No sections yet.",
                    stats: { "\($0.words)w" }
                )
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchPageOutline(headings, currentID: "risks") { _ in }.disabled(true)
        },
    ]
}

@Suite("MonarchPageOutline")
struct MonarchPageOutlineTests {
    private let headings = MonarchPageOutlineFixtures.headings

    @Test("Headings deeper than maxLevel and blank headings are left out")
    func visible() {
        let withBlank = headings + [MonarchPageOutlineHeading("  ", level: 2, id: "blank")]
        #expect(
            MonarchPageOutlineTokens.visible(withBlank, maxLevel: 3).map(\.id) == [
                "overview", "goals", "risks", "timeline",
            ]
        )
        #expect(MonarchPageOutlineTokens.visible(withBlank, maxLevel: 1).map(\.id) == ["overview"])
    }

    @Test("Depth follows the nearest lower heading, so skipped levels add no indent")
    func depths() {
        #expect(MonarchPageOutlineTokens.depths(headings) == [0, 1, 2, 3, 1])
        let skipped = [
            MonarchPageOutlineHeading("A", level: 1, id: "a"),
            MonarchPageOutlineHeading("B", level: 3, id: "b"),
            MonarchPageOutlineHeading("C", level: 2, id: "c"),
            MonarchPageOutlineHeading("D", level: 2, id: "d"),
        ]
        #expect(MonarchPageOutlineTokens.depths(skipped) == [0, 1, 1, 1])
        let flat = [
            MonarchPageOutlineHeading("A", level: 2, id: "a"), MonarchPageOutlineHeading("B", level: 2, id: "b"),
        ]
        #expect(MonarchPageOutlineTokens.depths(flat) == [0, 0])
        #expect(MonarchPageOutlineTokens.depths([]).isEmpty)
    }

    @Test("Levels are clamped to 1 through 6")
    func clamp() {
        #expect(MonarchPageOutlineHeading("A", level: 0, id: "a").level == 1)
        #expect(MonarchPageOutlineHeading("A", level: 9, id: "a").level == 6)
    }

    @Test("The current section is accent, stronger and has a bar")
    func current() {
        #expect(MonarchPageOutlineTokens.textRole(isCurrent: true, isEnabled: true) == \MonarchColorTokens.accentStrong)
        #expect(MonarchPageOutlineTokens.textRole(isCurrent: false, isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(
            MonarchPageOutlineTokens.textRole(isCurrent: true, isEnabled: false) == \MonarchColorTokens.textDisabled
        )
        #expect(MonarchPageOutlineTokens.barRole(isCurrent: true) == \MonarchColorTokens.accentSolid)
        #expect(MonarchPageOutlineTokens.barRole(isCurrent: false) == \MonarchColorTokens.borderSubtle)
        #expect(MonarchPageOutlineTokens.textStyle(isCurrent: true) == .body)
        #expect(MonarchPageOutlineTokens.textStyle(isCurrent: false) == .body)
        #expect(MonarchPageOutlineTokens.accessibilityValue(isCurrent: true, currentLabel: "Current") == "Current")
        #expect(MonarchPageOutlineTokens.accessibilityValue(isCurrent: false, currentLabel: "Current") == "")
    }

    @Test("Stats count words and round the reading time, at least one minute")
    func stats() {
        #expect(MonarchPageOutlineStats(text: "One two\nthree  four") == MonarchPageOutlineStats(words: 4, minutes: 1))
        #expect(MonarchPageOutlineStats(text: "").words == 0)
        #expect(MonarchPageOutlineStats(text: "").minutes == 1)
        let long = Array(repeating: "word", count: 1_100).joined(separator: " ")
        #expect(MonarchPageOutlineStats(text: long) == MonarchPageOutlineStats(words: 1_100, minutes: 5))
    }

    @Test("Built-in strings resolve and format the count for the locale")
    func strings() {
        #expect(MonarchPageOutlineStrings.title == "On this page")
        #expect(MonarchPageOutlineStrings.empty == "Headings you add appear here.")
        #expect(MonarchPageOutlineStrings.current == "Current section")
        let english = Locale(identifier: "en_US")
        #expect(
            MonarchPageOutlineStrings.stats(MonarchPageOutlineStats(words: 1_234, minutes: 6), locale: english)
                == "1,234 words · 6 min read"
        )
        #expect(
            MonarchPageOutlineStrings.stats(MonarchPageOutlineStats(words: 1, minutes: 1), locale: english)
                == "1 word · 1 min read"
        )
    }
}
