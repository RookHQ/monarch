import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let wide = MonarchColumnsBlockTokens.stackWidth * 1.5

enum MonarchColumnsBlockFixtures: ComponentFixtures {
    static let id = "columns-block"
    static let fixtures = [
        Fixture("two columns, wide, uneven split") {
            MonarchColumnsBlock(ratio: 0.35) {
                Text("Before the launch").monarchTextStyle(.bodyStrong)
                Text("Sign-ups doubled after the launch, mostly from the new referral page.")
                    .monarchTextStyle(.body)
            }
            .frame(width: wide)
        },
        Fixture("three columns, wide") {
            MonarchColumnsBlock {
                Text("Plan").monarchTextStyle(.body)
                Text("Build").monarchTextStyle(.body)
                Text("Ship the first version to the customers who asked for it").monarchTextStyle(.body)
            }
            .frame(width: wide)
        },
        Fixture("stacked at phone width, ratio out of range") {
            MonarchColumnsBlock(ratio: 2) {
                VStack(alignment: .leading) {
                    Text("Notes").monarchTextStyle(.bodyStrong)
                    Text("The column holds several views in a stack.").monarchTextStyle(.body)
                }
                Text("Second column").monarchTextStyle(.body)
            }
        },
        Fixture("one column") {
            MonarchColumnsBlock { Text("Only one").monarchTextStyle(.body) }
        },
    ]
}

@Suite("MonarchColumnsBlock")
@MainActor
struct MonarchColumnsBlockTests {
    @Test("The ratio stays between 0.2 and 0.8, and 0.5 when it is not a number")
    func clamp() {
        #expect(MonarchColumnsBlockTokens.clampRatio(0.4) == 0.4)
        #expect(MonarchColumnsBlockTokens.clampRatio(0.05) == 0.2)
        #expect(MonarchColumnsBlockTokens.clampRatio(2) == 0.8)
        #expect(MonarchColumnsBlockTokens.clampRatio(.nan) == 0.5)
        #expect(MonarchColumnsBlockTokens.clampRatio(.infinity) == 0.5)
    }

    @Test("Two columns split the width by the ratio, after the gap")
    func twoColumns() {
        let gap = MonarchColumnsBlockTokens.columnSpacing
        let columns = MonarchColumnsBlockTokens.columns(count: 2, width: 600 + gap, ratio: 0.25)
        #expect(columns.count == 2)
        #expect(columns[0].x == 0)
        #expect(columns[0].width == 150)
        #expect(columns[1].x == 150 + gap)
        #expect(columns[1].width == 450)
    }

    @Test("Three or more columns are equal, whatever the ratio")
    func threeColumns() {
        let gap = MonarchColumnsBlockTokens.columnSpacing
        let columns = MonarchColumnsBlockTokens.columns(count: 3, width: 300 + 2 * gap, ratio: 0.2)
        #expect(columns.map(\.width) == [100, 100, 100])
        #expect(columns.map(\.x) == [0, 100 + gap, 200 + 2 * gap])
        #expect(MonarchColumnsBlockTokens.columns(count: 0, width: 300, ratio: 0.5).isEmpty)
    }

    @Test("Columns stack below 480 points or when asked")
    func stacking() {
        #expect(MonarchColumnsBlockTokens.isStacked(width: 390, forcesStack: false))
        #expect(!MonarchColumnsBlockTokens.isStacked(width: 768, forcesStack: false))
        #expect(MonarchColumnsBlockTokens.isStacked(width: 768, forcesStack: true))
        #expect(MonarchColumnsBlockTokens.stackWidth == 480)
    }

    @Test("Stacked columns are taller than the same columns side by side")
    func layoutHeight() {
        let block = MonarchColumnsBlock {
            Text("First").monarchTextStyle(.body)
            Text("Second").monarchTextStyle(.body)
        }
        let stacked = Host.layout(block)
        let single = Host.layout(Text("First").monarchTextStyle(.body))
        #expect(stacked.height > single.height * 1.5)
    }
}
