import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchProgressViewStyleFixtures: ComponentFixtures {
    static let id = "progress-bar"
    static let fixtures = [
        Fixture("determinate") {
            VStack {
                ProgressView("Uploading", value: 0.4)
                ProgressView(value: 0)
                ProgressView(value: 3, total: 2) {
                    Text("Over")
                } currentValueLabel: {
                    Text("3 of 2")
                }
            }
            .progressViewStyle(.monarchLinear)
        },
        Fixture("indeterminate") {
            ProgressView("Working").progressViewStyle(.monarchLinear)
        },
    ]
}

@Suite("MonarchProgressViewStyle")
struct MonarchProgressViewStyleTests {
    @Test("Fractions are limited to 0 through 1")
    func clamped() {
        #expect(MonarchProgressTokens.clamped(-1) == 0)
        #expect(MonarchProgressTokens.clamped(0.4) == 0.4)
        #expect(MonarchProgressTokens.clamped(1.5) == 1)
        #expect(MonarchProgressTokens.clamped(.nan) == 0)
    }

    @Test("The indeterminate cycle uses the shimmer duration token")
    func indeterminate() {
        #expect(MonarchProgressTokens.indeterminateDuration == 1.6)
        #expect(MonarchProgressTokens.indeterminateSegment > 0 && MonarchProgressTokens.indeterminateSegment < 1)
    }
}

@Suite("MonarchProgressViewStyle tone")
@MainActor
struct MonarchProgressViewStyleToneTests {
    @Test(
        "The fill is the strong colour of the tone, muted text for neutral; the default is the accent",
        arguments: MonarchTone.allCases
    )
    func fill(tone: MonarchTone) {
        #expect(
            MonarchProgressTokens.fill(tone: tone)
                == (tone == .neutral ? \MonarchColorTokens.textMuted : MonarchTonePalette.palette(for: tone).strong)
        )
    }

    @Test("The default fill is the accent")
    func accent() {
        #expect(MonarchProgressTokens.fill(tone: .accent) == \MonarchColorTokens.accentStrong)
        let toned = ProgressView(value: 0.5).progressViewStyle(.monarchLinear(tone: .success))
        #expect(Host.layout(toned).height > 0)
    }
}
