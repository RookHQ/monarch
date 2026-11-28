import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchScoreRingFixtures: ComponentFixtures {
    static let id = "score-ring"
    static let fixtures = [
        Fixture("every size") {
            HStack(alignment: .top) {
                ForEach(MonarchScoreRingSize.allCases, id: \.self) { size in
                    MonarchScoreRing("Score", value: 78, size: size)
                }
            }
        },
        Fixture("every band and colour") {
            HStack(alignment: .top) {
                MonarchScoreRing(value: 92, size: .sm)
                MonarchScoreRing(value: 64, size: .sm)
                MonarchScoreRing(value: 45, size: .sm)
                MonarchScoreRing(value: 12, size: .sm)
                MonarchScoreRing(value: 50, color: .accent, size: .sm)
                MonarchScoreRing(value: 50, color: .tone(.neutral), size: .sm)
            }
        },
        Fixture("no score, zero and full") {
            HStack(alignment: .top) {
                MonarchScoreRing("New", value: nil)
                MonarchScoreRing("Empty", value: 0)
                MonarchScoreRing("Full", value: 140)
            }
        },
        Fixture("custom scale, format and long text") {
            MonarchScoreRing(
                "Average rating from customers this quarter",
                value: 4.25,
                max: 5,
                description: "Based on 1,204 reviews collected since the start of April",
                size: .lg,
                showsMax: true,
                format: { String(format: "%.1f", $0) },
                labels: MonarchScoreRingLabels(ofMax: { "of \($0)" })
            )
        },
    ]
}

@Suite("MonarchScoreRing")
struct MonarchScoreRingTests {
    private let english = Locale(identifier: "en_US")

    @Test("The default bands give success, info, warning and danger")
    func bands() {
        let bands = MonarchScoreRingThreshold.defaults
        #expect(MonarchScoreRingTokens.tone(for: 100, thresholds: bands) == .success)
        #expect(MonarchScoreRingTokens.tone(for: 80, thresholds: bands) == .success)
        #expect(MonarchScoreRingTokens.tone(for: 79.9, thresholds: bands) == .info)
        #expect(MonarchScoreRingTokens.tone(for: 60, thresholds: bands) == .info)
        #expect(MonarchScoreRingTokens.tone(for: 40, thresholds: bands) == .warning)
        #expect(MonarchScoreRingTokens.tone(for: 0, thresholds: bands) == .danger)
        #expect(MonarchScoreRingTokens.tone(for: 10, thresholds: []) == .neutral)
        let unsorted = [
            MonarchScoreRingThreshold(minimum: 0, tone: .danger),
            MonarchScoreRingThreshold(minimum: 50, tone: .success),
        ]
        #expect(MonarchScoreRingTokens.tone(for: 70, thresholds: unsorted) == .success)
    }

    @Test("The arc takes the band, the accent or a fixed tone")
    func fill() {
        let bands = MonarchScoreRingThreshold.defaults
        func fill(_ color: MonarchScoreRingColor, _ value: Double?) -> MonarchColorRole {
            MonarchScoreRingTokens.fill(color: color, value: value, thresholds: bands)
        }
        #expect(fill(.auto, 90) == \MonarchColorTokens.successSolid)
        #expect(fill(.auto, 20) == \MonarchColorTokens.dangerSolid)
        #expect(fill(.auto, nil) == \MonarchColorTokens.textMuted)
        #expect(fill(.accent, 20) == \MonarchColorTokens.accentSolid)
        #expect(
            MonarchScoreRingTokens.fill(color: .tone(.warning), value: 99, thresholds: bands)
                == \MonarchColorTokens.warningSolid
        )
        #expect(MonarchScoreRingTokens.track == \MonarchColorTokens.bgMuted)
    }

    @Test("Scores are limited to the scale, and a bad scale falls back to 100")
    func clamping() {
        #expect(MonarchScoreRingTokens.clamped(140, max: 100) == 100)
        #expect(MonarchScoreRingTokens.clamped(-5, max: 100) == 0)
        #expect(MonarchScoreRingTokens.clamped(nil, max: 100) == nil)
        #expect(MonarchScoreRingTokens.clamped(.nan, max: 100) == nil)
        #expect(MonarchScoreRingTokens.safeMax(0) == 100)
        #expect(MonarchScoreRingTokens.safeMax(-3) == 100)
        #expect(MonarchScoreRingTokens.safeMax(5) == 5)
    }

    @Test("Sizes set the diameter, the ring width and the number's type")
    func sizes() {
        #expect(MonarchScoreRingTokens.diameter(.sm) == 48)
        #expect(MonarchScoreRingTokens.diameter(.md) == 80)
        #expect(MonarchScoreRingTokens.diameter(.lg) == 120)
        #expect(MonarchScoreRingTokens.lineWidth(diameter: 120) == 12)
        #expect(MonarchScoreRingTokens.lineWidth(diameter: 30) == 4)
        #expect(MonarchScoreRingTokens.valueStyle(.lg) == .display)
        #expect(MonarchScoreRingTokens.valueStyle(.md) == .title)
        #expect(!MonarchScoreRingTokens.showsMax(nil, size: .sm, hasValue: true))
        #expect(MonarchScoreRingTokens.showsMax(nil, size: .md, hasValue: true))
        #expect(MonarchScoreRingTokens.showsMax(true, size: .sm, hasValue: true))
        #expect(!MonarchScoreRingTokens.showsMax(true, size: .lg, hasValue: false))
    }

    @Test("VoiceOver reads the name and the score out of the maximum")
    func accessibility() {
        let labels = MonarchScoreRingLabels()
        #expect(MonarchScoreRingTokens.formatted(77.6, locale: english) == "78")
        #expect(MonarchScoreRingTokens.accessibilityValue(text: "78", max: "100", labels: labels) == "78 out of 100")
        #expect(MonarchScoreRingTokens.accessibilityValue(text: nil, max: "100", labels: labels) == "No score yet")
        #expect(MonarchScoreRingTokens.accessibilityLabel(label: "SEO", labels: labels) == "SEO")
        #expect(MonarchScoreRingTokens.accessibilityLabel(label: nil, labels: labels) == "Score")
        #expect(MonarchScoreRingTokens.accessibilityLabel(label: "", labels: labels) == "Score")
        let custom = MonarchScoreRingLabels(name: "Rating", valueText: { "\($0) of \($1)" }, noValue: "Unrated")
        #expect(MonarchScoreRingTokens.accessibilityValue(text: "4", max: "5", labels: custom) == "4 of 5")
        #expect(MonarchScoreRingTokens.accessibilityValue(text: nil, max: "5", labels: custom) == "Unrated")
        #expect(MonarchScoreRingTokens.accessibilityLabel(label: nil, labels: custom) == "Rating")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchScoreRingStrings.name == "Score")
        #expect(MonarchScoreRingStrings.valueText("78", "100") == "78 out of 100")
        #expect(MonarchScoreRingStrings.ofMax("100") == "/ 100")
        #expect(MonarchScoreRingStrings.noValue == "No score yet")
    }
}
