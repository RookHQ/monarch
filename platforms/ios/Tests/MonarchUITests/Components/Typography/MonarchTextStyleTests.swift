import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTextStyleFixtures: ComponentFixtures {
    static let id = "text"
    static let fixtures = [
        Fixture("every role") {
            VStack(alignment: .leading) {
                ForEach(MonarchTextStyle.allCases, id: \.self) { style in
                    Text("The quick brown fox").monarchTextStyle(style)
                }
            }
        },
        Fixture("long text wraps") {
            Text("A paragraph that is long enough to wrap onto several lines at every text size.")
                .monarchTextStyle(.body)
        },
    ]
}

@Suite("MonarchTextStyle")
struct MonarchTextStyleTests {
    @Test("Every type role takes its values from the tokens")
    func specs() {
        #expect(MonarchTextStyle.allCases.count == 12)
        #expect(MonarchTextStyle.allCases.map(\.spec.size) == [36, 24, 20, 18, 16, 16, 14, 12, 14, 12, 14, 12])
        for style in MonarchTextStyle.allCases {
            #expect(style.spec.lineHeight >= MonarchTextStyleSpec.systemLineHeight, "\(style)")
            #expect(style.spec.isMonospaced == (style == .code || style == .codeSmall), "\(style)")
        }
        #expect(MonarchTextStyle.body.spec.weight == MonarchTokens.FontWeight.regular)
        #expect(MonarchTextStyle.bodyStrong.spec.weight == MonarchTokens.FontWeight.semibold)
    }

    @Test("Token weights map to SwiftUI weights")
    func fontWeights() {
        #expect(MonarchTextStyleSpec.fontWeight(400) == .regular)
        #expect(MonarchTextStyleSpec.fontWeight(500) == .medium)
        #expect(MonarchTextStyleSpec.fontWeight(600) == .semibold)
        #expect(MonarchTextStyleSpec.fontWeight(700) == .bold)
    }

    #if os(iOS)
        // Dynamic Type is an iOS setting. On the macOS host build
        // `ScaledMetric` does not scale, so this runs on the simulator only.
        @Test("Text grows with Dynamic Type", arguments: MonarchTextStyle.allCases)
        @MainActor
        func scales(style: MonarchTextStyle) {
            let text = Text("Monarch").monarchTextStyle(style)
            let standard = Host.layout(text.environment(\.dynamicTypeSize, .large))
            let largest = Host.layout(text.environment(\.dynamicTypeSize, .accessibility5))
            #expect(largest.height > standard.height, "\(style) did not grow")
        }
    #endif
}
