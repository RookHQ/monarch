import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiResponseSkeletonFixtures: ComponentFixtures {
    static let id = "ai-response-skeleton"
    static let fixtures = [
        Fixture("text: default") { MonarchAiResponseSkeleton() },
        Fixture("text: one line and many lines") {
            VStack {
                MonarchAiResponseSkeleton(lines: 1)
                MonarchAiResponseSkeleton(lines: 9)
            }
        },
        Fixture("cards") { MonarchAiResponseSkeleton(shape: .cards) },
        Fixture("table") { MonarchAiResponseSkeleton(shape: .table, lines: 3, columns: 4) },
        Fixture("out of range counts are clamped, custom label") {
            VStack {
                MonarchAiResponseSkeleton(shape: .table, lines: 0, columns: 0, label: "Building the table")
                MonarchAiResponseSkeleton(shape: .cards, lines: -2)
            }
        },
    ]
}

@Suite("MonarchAiResponseSkeleton")
struct MonarchAiResponseSkeletonTests {
    typealias Tokens = MonarchAiResponseSkeletonTokens

    @Test("Default counts follow the shape: 3 lines, 3 cards, 4 table rows")
    func defaults() {
        #expect(Tokens.count(nil, shape: .text) == 3)
        #expect(Tokens.count(nil, shape: .cards) == 3)
        #expect(Tokens.count(nil, shape: .table) == 4)
        #expect(Tokens.count(6, shape: .text) == 6)
    }

    @Test("Counts and columns are at least 1", arguments: [0, -1, -40])
    func clamped(value: Int) {
        for shape in MonarchAiResponseSkeletonShape.allCases {
            #expect(Tokens.count(value, shape: shape) == 1)
        }
        #expect(Tokens.columns(value) == 1)
        #expect(Tokens.columns(5) == 5)
    }

    @Test("The last of several lines is short; a single line is full width")
    func lineWidths() {
        #expect(Tokens.lineWidth(index: 0, count: 1) == 1)
        #expect(Tokens.lineWidth(index: 2, count: 3) == Tokens.lastLineWidth)
        #expect(Tokens.lineWidth(index: 1, count: 3) == Tokens.widths[1])
        for index in 0..<20 {
            let width = Tokens.lineWidth(index: index, count: 21)
            #expect(width > 0 && width <= 1)
        }
    }

    @Test("Cell widths are fixed and vary between neighbours")
    func cellWidths() {
        #expect(Tokens.cellWidth(row: 0, column: 0) == Tokens.cellWidth(row: 0, column: 0))
        #expect(Tokens.cellWidth(row: 0, column: 0) != Tokens.cellWidth(row: 0, column: 1))
        #expect(Tokens.cellWidth(row: 40, column: 9) <= 1)
    }

    @Test("The pulse rises and falls within 0...1 and is off under Reduce Motion")
    func pulse() {
        #expect(MonarchPulse.value(at: nil) == 0)
        #expect(abs(MonarchPulse.value(at: 0)) < 0.0001)
        #expect(abs(MonarchPulse.value(at: MonarchPulse.period / 2) - 1) < 0.0001)
        for step in 0..<50 {
            let value = MonarchPulse.value(at: Double(step) * 0.07)
            #expect(value >= 0 && value <= 1)
        }
    }

    @Test("The bars use the muted roles with the assistant tint")
    func roles() {
        #expect(Tokens.bar == \MonarchColorTokens.bgMuted)
        #expect(Tokens.strongBar == \MonarchColorTokens.bgEmphasis)
        #expect(Tokens.shimmer == \MonarchColorTokens.aiSubtle)
        #expect(Tokens.border == \MonarchColorTokens.borderSubtle)
    }

    @Test("The built-in label comes from the strings table")
    func strings() {
        #expect(MonarchAiResponseSkeletonStrings.label == "Generating response…")
    }

    @Test("More lines take more height")
    @MainActor
    func linesTakeRoom() {
        let three = Host.layout(MonarchAiResponseSkeleton(lines: 3))
        let six = Host.layout(MonarchAiResponseSkeleton(lines: 6))
        #expect(six.height > three.height)
        let table = Host.layout(MonarchAiResponseSkeleton(shape: .table, lines: 2))
        let longer = Host.layout(MonarchAiResponseSkeleton(shape: .table, lines: 5))
        #expect(longer.height > table.height)
    }
}
