import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiScoreFixtures: ComponentFixtures {
    static let id = "ai-score"
    static let factors = [
        MonarchAiScoreFactor("Replied to the last two emails", impact: 18),
        MonarchAiScoreFactor("No budget confirmed", impact: -9, detail: "Asked on the last call"),
        MonarchAiScoreFactor("Company size", impact: 0),
    ]

    static let fixtures = [
        Fixture("ring, high, every section") {
            MonarchAiScore(
                "Lead score",
                value: 82,
                rationale: "Replied twice this week and matches the target industry.",
                factors: factors
            )
        },
        Fixture("ring, medium, small") {
            MonarchAiScore("Fit", value: 55, size: .sm)
        },
        Fixture("bar, low") {
            MonarchAiScore("Churn risk", value: 12, factors: factors, variant: .bar)
        },
        Fixture("no score, ring") {
            MonarchAiScore(value: nil)
        },
        Fixture("no score, bar") {
            MonarchAiScore(value: nil, variant: .bar, showsBands: false)
        },
        Fixture("custom bands, out of range") {
            MonarchAiScore(
                "Match",
                value: 140,
                bands: [
                    MonarchAiScoreBand(minimum: 0, label: "Weak", tone: .neutral),
                    MonarchAiScoreBand(minimum: 50, label: "Strong", tone: .info),
                ],
                variant: .bar
            )
        },
        Fixture("long text") {
            MonarchAiScore(
                "Likelihood that this customer renews the annual contract before the end of the quarter",
                value: 64,
                rationale:
                    "The account grew its seat count twice this year, but the main contact left in March and the replacement has not replied yet."
            )
        },
    ]
}

@Suite("MonarchAiScore")
struct MonarchAiScoreTests {
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchAiScoreLabels()
    private let bands = MonarchAiScoreBand.defaults

    @Test("Scores are limited to 0 through 100; missing or invalid scores are nil")
    func clamping() {
        #expect(MonarchAiScoreTokens.clamped(140) == 100)
        #expect(MonarchAiScoreTokens.clamped(-3) == 0)
        #expect(MonarchAiScoreTokens.clamped(72) == 72)
        #expect(MonarchAiScoreTokens.clamped(nil) == nil)
        #expect(MonarchAiScoreTokens.clamped(.nan) == nil)
        #expect(MonarchAiScoreTokens.fraction(nil) == 0)
        #expect(MonarchAiScoreTokens.fraction(25) == 0.25)
    }

    @Test("Bands are sorted with upper bounds; a score takes the highest band at or below it")
    func banding() {
        let ranges = MonarchAiScoreTokens.ranges(Array(bands.reversed()))
        #expect(ranges.map(\.band.label) == ["Low", "Medium", "High"])
        #expect(ranges.map(\.maximum) == [39, 69, 100])
        #expect(MonarchAiScoreTokens.band(for: 0, in: bands)?.label == "Low")
        #expect(MonarchAiScoreTokens.band(for: 39.5, in: bands)?.label == "Low")
        #expect(MonarchAiScoreTokens.band(for: 40, in: bands)?.label == "Medium")
        #expect(MonarchAiScoreTokens.band(for: 100, in: bands)?.label == "High")
        let shifted = [MonarchAiScoreBand(minimum: 20, label: "Some")]
        #expect(MonarchAiScoreTokens.band(for: 5, in: shifted)?.label == "Some")
        #expect(MonarchAiScoreTokens.band(for: 5, in: []) == nil)
        #expect(MonarchAiScoreTokens.ticks(bands) == [0.4, 0.7])
    }

    @Test("The tone follows the band, neutral without a score; fills are the tone's solid role")
    func tones() {
        #expect(MonarchAiScoreTokens.tone(for: 82, in: bands) == .success)
        #expect(MonarchAiScoreTokens.tone(for: 55, in: bands) == .warning)
        #expect(MonarchAiScoreTokens.tone(for: 10, in: bands) == .danger)
        #expect(MonarchAiScoreTokens.tone(for: nil, in: bands) == .neutral)
        #expect(MonarchAiScoreTokens.fill(.success) == \MonarchColorTokens.successSolid)
        #expect(MonarchAiScoreTokens.fill(.danger) == \MonarchColorTokens.dangerSolid)
        #expect(MonarchAiScoreTokens.impactRole(.up) == \MonarchColorTokens.successStrong)
        #expect(MonarchAiScoreTokens.impactRole(.down) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchAiScoreTokens.impactRole(.none) == \MonarchColorTokens.textMuted)
    }

    @Test("Spoken values name the score and the band")
    func spoken() {
        #expect(MonarchAiScoreTokens.valueText(72, locale: english, labels: labels) == "72 out of 100")
        #expect(MonarchAiScoreTokens.valueText(nil, locale: english, labels: labels) == "No score yet")
        #expect(
            MonarchAiScoreTokens.barValue(72, band: bands[2], locale: english, labels: labels)
                == "72 out of 100, High"
        )
        #expect(MonarchAiScoreTokens.barValue(nil, band: nil, locale: english, labels: labels) == "No score yet")
        let custom = MonarchAiScoreLabels(valueText: { "\($0) sur \($1)" })
        #expect(MonarchAiScoreTokens.valueText(40, locale: english, labels: custom) == "40 sur 100")
    }

    @Test("Factors are signed, spoken by direction and sized against the largest")
    func factors() {
        #expect(MonarchAiScoreTokens.signed(18, locale: english) == "+18")
        #expect(MonarchAiScoreTokens.signed(-9, locale: english) == "-9")
        #expect(MonarchAiScoreTokens.signed(0, locale: english) == "0")
        #expect(MonarchAiScoreTokens.signed(2.5, locale: english) == "+2.5")
        #expect(MonarchAiScoreTokens.direction(3) == .up)
        #expect(MonarchAiScoreTokens.direction(-3) == .down)
        #expect(MonarchAiScoreTokens.direction(0) == .none)
        #expect(MonarchAiScoreTokens.impactText(18, locale: english, labels: labels) == "Raised the score by 18")
        #expect(MonarchAiScoreTokens.impactText(-9, locale: english, labels: labels) == "Lowered the score by 9")
        #expect(MonarchAiScoreTokens.impactText(0, locale: english, labels: labels) == "No effect on the score")
        let factors = MonarchAiScoreFixtures.factors
        #expect(MonarchAiScoreTokens.impactFraction(18, factors: factors) == 1)
        #expect(MonarchAiScoreTokens.impactFraction(-9, factors: factors) == 0.5)
        #expect(MonarchAiScoreTokens.impactFraction(0.5, factors: []) == 0.5)
    }

    @Test("Ranges and numbers are written in the locale")
    func formatting() {
        let ranges = MonarchAiScoreTokens.ranges(bands)
        #expect(MonarchAiScoreTokens.rangeText(ranges[1], locale: english) == "40–69")
        #expect(MonarchAiScoreTokens.number(71.6, locale: english) == "72")
        #expect(MonarchAiScoreTokens.ringSize(.sm) == .sm)
        #expect(MonarchAiScoreTokens.ringSize(.md) == .md)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(bands.map(\.label) == ["Low", "Medium", "High"])
        #expect(MonarchAiScoreStrings.label == "Score")
        #expect(MonarchAiScoreStrings.aiGenerated == "AI score")
        #expect(MonarchAiScoreStrings.factors == "What affects this score")
        #expect(MonarchAiScoreStrings.bands == "How to read this score")
        #expect(MonarchAiScoreStrings.currentBand == "this score")
    }
}
