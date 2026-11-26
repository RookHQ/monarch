import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFlowLayoutFixtures: ComponentFixtures {
    static let id = "inline"
    static let fixtures = [
        Fixture("tags that wrap") {
            MonarchFlowLayout {
                ForEach(MonarchFlowLayoutFixtures.tags, id: \.self) { tag in
                    MonarchBadge(tag)
                }
            }
        },
        Fixture("every justification") {
            VStack(alignment: .leading) {
                ForEach(MonarchFlowLayoutJustification.allCases, id: \.self) { justification in
                    MonarchFlowLayout(spacing: 12, rowSpacing: 4, justification: justification) {
                        ForEach(MonarchFlowLayoutFixtures.tags, id: \.self) { tag in
                            MonarchBadge(tag, tone: .info)
                        }
                    }
                }
            }
        },
        Fixture("buttons, baseline alignment, a child wider than the row") {
            MonarchFlowLayout(alignment: .firstTextBaseline) {
                MonarchButton("Save", variant: .primary) {}
                MonarchButton("Cancel", size: .sm) {}
                Text(String(repeating: "A label much longer than one row of the layout. ", count: 4))
                    .monarchTextStyle(.caption)
            }
        },
        Fixture("empty and one child") {
            VStack {
                MonarchFlowLayout {}
                MonarchFlowLayout(justification: .spaceBetween) { MonarchBadge("Only") }
            }
        },
    ]

    static let tags = ["Design", "Research", "Engineering", "Customer support", "Sales", "Finance", "Legal"]
}

@Suite("MonarchFlowLayout")
struct MonarchFlowLayoutTests {
    typealias Engine = MonarchFlowLayoutEngine

    private func items(_ widths: [CGFloat], height: CGFloat = 20) -> [Engine.Item] {
        widths.map { Engine.Item(size: CGSize(width: $0, height: height), guide: height / 2) }
    }

    private func arrange(
        _ widths: [CGFloat],
        width: CGFloat,
        spacing: CGFloat = 8,
        rowSpacing: CGFloat = 8,
        justification: MonarchFlowLayoutJustification = .leading
    ) -> Engine.Arrangement {
        Engine.arrange(
            items(widths),
            width: width,
            spacing: spacing,
            rowSpacing: rowSpacing,
            justification: justification
        )
    }

    @Test("Children that fit stay on one row, separated by the spacing")
    func oneRow() {
        let result = arrange([40, 50, 60], width: 200)
        #expect(result.frames.map(\.minX) == [0, 48, 106])
        #expect(result.frames.allSatisfy { $0.minY == 0 })
        #expect(result.size == CGSize(width: 166, height: 20))
    }

    @Test("A child that would pass the edge starts a new row")
    func wraps() {
        #expect(Engine.rows([40, 50, 60], width: 100, spacing: 8) == [0..<2, 2..<3])
        let result = arrange([40, 50, 60], width: 100, rowSpacing: 4)
        #expect(result.frames[2].origin == CGPoint(x: 0, y: 24))
        #expect(result.size == CGSize(width: 98, height: 44))
    }

    @Test("A row that fits exactly does not wrap, and one point less does")
    func exactFit() {
        #expect(Engine.rows([46, 46], width: 100, spacing: 8) == [0..<2])
        #expect(Engine.rows([46, 46], width: 99, spacing: 8) == [0..<1, 1..<2])
    }

    @Test("A child wider than the layout gets a row of its own")
    func oversize() {
        #expect(Engine.rows([30, 300, 30], width: 100, spacing: 8) == [0..<1, 1..<2, 2..<3])
        #expect(Engine.rows([300], width: 100, spacing: 8) == [0..<1])
    }

    @Test("With no width limit everything is one row")
    func unlimited() {
        let result = arrange([40, 50, 60], width: .infinity, justification: .center)
        #expect(Engine.rows([40, 50, 60], width: .infinity, spacing: 8).count == 1)
        #expect(result.size.width == 166)
        #expect(result.frames[0].minX == 0)
    }

    @Test("No children take no room")
    func empty() {
        let result = arrange([], width: 100)
        #expect(result.frames.isEmpty)
        #expect(result.size == .zero)
        #expect(Engine.rows([], width: 100, spacing: 8).isEmpty)
    }

    @Test("Centre, trailing and space-between share the left-over width of each row")
    func justification() {
        let centre = arrange([40, 40, 80], width: 100, justification: .center)
        #expect(centre.size.width == 100)
        #expect(centre.frames.map(\.minX) == [6, 54, 10])

        let trailing = arrange([40, 40, 80], width: 100, justification: .trailing)
        #expect(trailing.frames.map(\.maxX) == [52, 100, 100])

        let between = arrange([20, 20, 20, 80], width: 100, justification: .spaceBetween)
        #expect(between.frames.map(\.minX) == [0, 40, 80, 0])
        #expect(between.frames[2].maxX == 100)
    }

    @Test("Leading rows hug their content; the order of children never changes")
    func order() {
        let result = arrange([30, 30, 30, 30, 30], width: 80)
        #expect(result.size.width == 68)
        let positions = result.frames.map { ($0.minY, $0.minX) }
        #expect(positions.map(\.0) == [0, 0, 28, 28, 56])
        for (earlier, later) in zip(positions, positions.dropFirst()) {
            #expect(earlier.0 < later.0 || (earlier.0 == later.0 && earlier.1 < later.1))
        }
    }

    @Test("Children of different heights line up on their alignment guides")
    func alignment() {
        let centred = Engine.arrange(
            [
                Engine.Item(size: CGSize(width: 10, height: 40), guide: 20),
                Engine.Item(size: CGSize(width: 10, height: 10), guide: 5),
            ],
            width: 100,
            spacing: 8,
            rowSpacing: 8,
            justification: .leading
        )
        #expect(centred.frames.map(\.minY) == [0, 15])
        #expect(centred.size.height == 40)

        // Baselines: the second child's guide is lower, so the first is pushed down.
        let baseline = Engine.arrange(
            [
                Engine.Item(size: CGSize(width: 10, height: 20), guide: 12),
                Engine.Item(size: CGSize(width: 10, height: 30), guide: 24),
            ],
            width: 100,
            spacing: 8,
            rowSpacing: 8,
            justification: .leading
        )
        #expect(baseline.frames.map(\.minY) == [12, 0])
        #expect(baseline.size.height == 32)
    }

    @Test("The default spacing is 8 points, and negative spacing counts as none")
    @MainActor
    func spacing() {
        #expect(Engine.defaultSpacing == 8)
        let block = Color.clear.frame(width: 100, height: 10)
        let negative = Host.layout(
            MonarchFlowLayout(spacing: -20) {
                block
                block
            }
            .fixedSize()
        )
        #expect(abs(negative.height - 10) < 0.5)
    }

    @Test("The layout wraps real views at the width it is given")
    @MainActor
    func wrapsViews() {
        let block = Color.clear.frame(width: 150, height: 30)
        let size = Host.layout(
            MonarchFlowLayout(spacing: 10, rowSpacing: 6) {
                block
                block
                block
            }
        )
        // 390 points hold two 150-point blocks and a 10-point gap; the third wraps.
        #expect(abs(size.height - 66) < 0.5)
    }

    @Test("Text wider than the layout wraps inside it instead of overflowing")
    @MainActor
    func longText() {
        let line = Host.layout(Text("Short").fixedSize())
        let size = Host.layout(
            MonarchFlowLayout {
                Text(String(repeating: "A sentence that is longer than the canvas. ", count: 6))
            }
        )
        #expect(size.height > line.height * 2)
    }
}
