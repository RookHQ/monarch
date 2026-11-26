import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiConfidenceMeterFixtures: ComponentFixtures {
    static let id = "ai-confidence-meter"
    static let fixtures = [
        Fixture("badge: every level and size") {
            VStack(alignment: .leading) {
                ForEach(MonarchAiConfidenceMeterSize.allCases, id: \.self) { size in
                    HStack {
                        MonarchAiConfidenceMeter(value: 0.42, size: size)
                        MonarchAiConfidenceMeter(value: 0.7, size: size)
                        MonarchAiConfidenceMeter(value: 0.93, size: size)
                    }
                }
            }
        },
        Fixture("badge: with value and explanation") {
            MonarchAiConfidenceMeter(value: 0.42, explanation: "Only one source mentions it.", showsValue: true)
        },
        Fixture("bar: every level") {
            VStack(alignment: .leading) {
                MonarchAiConfidenceMeter(value: 0.42, explanation: "Only one source mentions it.", variant: .bar)
                MonarchAiConfidenceMeter(value: 0.7, variant: .bar)
                MonarchAiConfidenceMeter(value: 0.93, explanation: "Matches two sources.", variant: .bar, size: .sm)
            }
        },
        Fixture("bar: extremes, no value, custom labels") {
            VStack(alignment: .leading) {
                MonarchAiConfidenceMeter(value: 0, variant: .bar)
                MonarchAiConfidenceMeter(value: 1, variant: .bar, showsValue: false)
                MonarchAiConfidenceMeter(value: .nan, variant: .bar)
                MonarchAiConfidenceMeter(
                    value: 0.5,
                    variant: .bar,
                    lowThreshold: 0.3,
                    highThreshold: 0.5,
                    levelLabel: "Looks right",
                    meterLabel: "Certainty",
                    valueText: { label, percent in "\(percent), \(label)" }
                )
            }
        },
    ]
}

@Suite("MonarchAiConfidenceMeter")
struct MonarchAiConfidenceMeterTests {
    typealias Tokens = MonarchAiConfidenceMeterTokens

    @Test("Below the low threshold is low, at or above the high threshold is high")
    func levels() {
        #expect(MonarchAiConfidenceLevel.level(for: 0) == .low)
        #expect(MonarchAiConfidenceLevel.level(for: 0.59) == .low)
        #expect(MonarchAiConfidenceLevel.level(for: 0.6) == .medium)
        #expect(MonarchAiConfidenceLevel.level(for: 0.84) == .medium)
        #expect(MonarchAiConfidenceLevel.level(for: 0.85) == .high)
        #expect(MonarchAiConfidenceLevel.level(for: 1) == .high)
    }

    @Test("Values outside 0 to 1 are clamped, and not-a-number counts as 0")
    func clamping() {
        #expect(Tokens.clamped(-1) == 0)
        #expect(Tokens.clamped(2) == 1)
        #expect(Tokens.clamped(.nan) == 0)
        #expect(Tokens.clamped(.infinity) == 0)
        #expect(Tokens.clamped(0.3) == 0.3)
        #expect(MonarchAiConfidenceLevel.level(for: 7) == .high)
        #expect(MonarchAiConfidenceLevel.level(for: -7) == .low)
        #expect(MonarchAiConfidenceLevel.level(for: .nan) == .low)
    }

    @Test("Custom thresholds move the bands")
    func thresholds() {
        #expect(MonarchAiConfidenceLevel.level(for: 0.5, lowThreshold: 0.3, highThreshold: 0.5) == .high)
        #expect(MonarchAiConfidenceLevel.level(for: 0.4, lowThreshold: 0.3, highThreshold: 0.5) == .medium)
        #expect(MonarchAiConfidenceLevel.level(for: 0.2, lowThreshold: 0.3, highThreshold: 0.5) == .low)
    }

    @Test("Every band has its own label, icon and colours")
    func bands() {
        let levels = MonarchAiConfidenceLevel.allCases
        #expect(
            levels.map(MonarchAiConfidenceMeterStrings.label) == [
                "Needs check", "Medium confidence", "High confidence",
            ]
        )
        #expect(Set(levels.map(Tokens.symbol)).count == levels.count)
        #expect(Tokens.foreground(.low) == \MonarchColorTokens.confidenceLow)
        #expect(Tokens.foreground(.medium) == \MonarchColorTokens.confidenceMedium)
        #expect(Tokens.foreground(.high) == \MonarchColorTokens.confidenceHigh)
    }

    @Test("The badge is a tinted pill with a border in the colours of the band")
    func badge() {
        #expect(
            Tokens.badgeAppearance(.low)
                == MonarchSurfaceAppearance(
                    background: \.dangerSubtle,
                    foreground: \.confidenceLow,
                    border: \.dangerBorder
                )
        )
        #expect(Tokens.badgeAppearance(.medium).background == \MonarchColorTokens.warningSubtle)
        #expect(Tokens.badgeAppearance(.medium).border == \MonarchColorTokens.warningBorder)
        #expect(Tokens.badgeAppearance(.high).background == \MonarchColorTokens.successSubtle)
        #expect(Tokens.badgeAppearance(.high).foreground == \MonarchColorTokens.confidenceHigh)
    }

    @Test("The percentage shows in the bar by default, and the app's choice wins")
    func showsValue() {
        #expect(!Tokens.showsValue(nil, variant: .badge))
        #expect(Tokens.showsValue(nil, variant: .bar))
        #expect(Tokens.showsValue(true, variant: .badge))
        #expect(!Tokens.showsValue(false, variant: .bar))
    }

    @Test("VoiceOver reads the band in words with the percentage")
    func valueText() {
        let percent = Tokens.percent(0.42)
        #expect(percent.contains("42"))
        #expect(Tokens.valueText(value: 0.42, level: .low) == "Needs check, \(percent) confidence")
        #expect(
            Tokens.valueText(value: 0.42, level: .low, levelLabel: "Verify") == "Verify, \(percent) confidence"
        )
        #expect(
            Tokens.valueText(value: 0.42, level: .low, format: { label, value in "\(value): \(label)" })
                == "\(percent): Needs check"
        )
        #expect(Tokens.percent(7).contains("100"))
    }

    @Test("The small size is tighter")
    func sizes() {
        #expect(Tokens.trackHeight(.sm) < Tokens.trackHeight(.md))
        #expect(Tokens.badgeHorizontalPadding(.sm) < Tokens.badgeHorizontalPadding(.md))
        #expect(Tokens.barSpacing(.sm) < Tokens.barSpacing(.md))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiConfidenceMeterStrings.name == "Confidence")
        #expect(MonarchAiConfidenceMeterStrings.valueText("Needs check", "42%") == "Needs check, 42% confidence")
    }
}
