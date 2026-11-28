import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCalloutBlockFixtures: ComponentFixtures {
    static let id = "callout-block"
    static let fixtures = [
        Fixture("every tone, one paragraph") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCalloutBlock(
                        "A paragraph inside a callout block, long enough to wrap onto a second line.",
                        tone: tone
                    )
                }
            }
        },
        Fixture("custom icon, tone label and mixed content") {
            MonarchCalloutBlock(systemImage: "lightbulb", tone: .info, toneLabel: "Tip") {
                Text("Before you migrate")
                    .monarchTextStyle(.bodyStrong)
                Text("Back up the workspace. The migration cannot be undone.")
                Button("Read the guide") {}
                    .buttonStyle(.monarch(.secondary, size: .sm))
            }
        },
        Fixture("empty content") {
            MonarchCalloutBlock(tone: .warning) {}
        },
    ]
}

@Suite("MonarchCalloutBlock")
struct MonarchCalloutBlockTests {
    @Test("Colours follow the tone")
    func appearance() {
        let danger = MonarchCalloutBlockTokens.appearance(tone: .danger)
        #expect(danger.background == \MonarchColorTokens.dangerSubtle)
        #expect(danger.icon == \MonarchColorTokens.dangerStrong)
        #expect(danger.border == \MonarchColorTokens.dangerBorder)
        #expect(danger.text == \MonarchColorTokens.textDefault)

        let neutral = MonarchCalloutBlockTokens.appearance(tone: .neutral)
        #expect(neutral.background == \MonarchColorTokens.bgSubtle)
        #expect(neutral.border == \MonarchColorTokens.borderDefault)
    }

    @Test("Every tone has its own background", arguments: MonarchTone.allCases)
    func backgrounds(tone: MonarchTone) {
        let others = MonarchTone.allCases.filter { $0 != tone }
        let background = MonarchCalloutBlockTokens.appearance(tone: tone).background
        #expect(others.allSatisfy { MonarchCalloutBlockTokens.appearance(tone: $0).background != background })
    }

    @Test("The neutral tone is a small dot; other tones use the shared tone icon; the app's icon wins")
    func symbol() {
        #expect(MonarchCalloutBlockTokens.symbol(tone: .neutral, override: nil) == "circle.fill")
        #expect(MonarchCalloutBlockTokens.iconSize(tone: .neutral, override: nil) == .sm)
        #expect(
            MonarchCalloutBlockTokens.symbol(tone: .warning, override: nil)
                == MonarchTonePalette.palette(for: .warning).symbol
        )
        #expect(MonarchCalloutBlockTokens.iconSize(tone: .warning, override: nil) == .md)
        #expect(MonarchCalloutBlockTokens.symbol(tone: .neutral, override: "lightbulb") == "lightbulb")
        #expect(MonarchCalloutBlockTokens.iconSize(tone: .neutral, override: "lightbulb") == .md)
        let symbols = MonarchTone.allCases.map { MonarchCalloutBlockTokens.symbol(tone: $0, override: nil) }
        #expect(Set(symbols).count == MonarchTone.allCases.count)
    }

    @Test("A block fills the width, and more content makes it taller")
    @MainActor
    func layout() {
        let one = Host.layout(MonarchCalloutBlock("One paragraph."))
        let two = Host.layout(
            MonarchCalloutBlock {
                Text("One paragraph.")
                Text("Another paragraph.")
            }
        )
        #expect(one.width == Host.width)
        #expect(two.height > one.height)
    }
}
