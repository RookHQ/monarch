import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLoadingStateFixtures: ComponentFixtures {
    static let id = "loading-state"
    static let fixtures = [
        Fixture("list") { MonarchLoadingState("Loading contacts") },
        Fixture("rows") { MonarchLoadingState("Loading tickets", variant: .rows, rows: 6) },
        Fixture("cards") { MonarchLoadingState("Loading projects", variant: .cards, rows: 3) },
        Fixture("detail") { MonarchLoadingState("Loading the article", variant: .detail) },
        Fixture("spinner, long label") {
            MonarchLoadingState(
                "Loading the invoices of every workspace you belong to",
                variant: .spinner
            )
        },
        Fixture("no rows") { MonarchLoadingState("Loading", rows: 0) },
    ]
}

@Suite("MonarchLoadingState")
struct MonarchLoadingStateTests {
    @Test("The row count is never negative")
    func rowCount() {
        #expect(MonarchLoadingStateTokens.rowCount(4) == 4)
        #expect(MonarchLoadingStateTokens.rowCount(0) == 0)
        #expect(MonarchLoadingStateTokens.rowCount(-3) == 0)
    }

    @Test("Line widths vary between rows and stay within the row")
    func lineWidths() {
        let widths = (0..<8).map { MonarchLoadingStateTokens.lineWidths($0) }
        #expect(widths[0].first == 55)
        #expect(widths[0].second == 30)
        #expect(Set(widths.map(\.first)).count > 1)
        for width in widths {
            #expect((55..<85).contains(width.first))
            #expect((30..<55).contains(width.second))
        }
        #expect(MonarchLoadingStateTokens.metaWidth(0) == 15)
    }

    @Test("Placeholder text is as long as its width")
    func placeholder() {
        #expect(MonarchLoadingStateTokens.placeholder(percent: 100).count == MonarchLoadingStateTokens.fullLineGlyphs)
        let full = MonarchLoadingStateTokens.fullLineGlyphs
        #expect(MonarchLoadingStateTokens.placeholder(percent: 50).count == full / 2)
        #expect(MonarchLoadingStateTokens.placeholder(percent: 0).count == 1)
    }

    @Test("Placeholders use the theme's muted roles")
    func colours() {
        #expect(MonarchLoadingStateTokens.blockFill == \MonarchColorTokens.bgMuted)
        #expect(MonarchLoadingStateTokens.lineFill == \MonarchColorTokens.textSubtle)
        #expect(MonarchLoadingStateTokens.label == \MonarchColorTokens.textMuted)
    }

    @Test("More rows take more height")
    @MainActor
    func height() {
        let two = Host.layout(MonarchLoadingState("Loading", rows: 2))
        let six = Host.layout(MonarchLoadingState("Loading", rows: 6))
        #expect(six.height > two.height)
    }
}
