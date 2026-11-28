import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDeltaIndicatorFixtures: ComponentFixtures {
    static let id = "delta-indicator"
    static let fixtures = [
        Fixture("every direction in both variants and sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchDeltaIndicatorVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchDeltaIndicatorSize.allCases, id: \.self) { size in
                        MonarchDeltaIndicator(0.12, comparison: "vs last month", variant: variant, size: size)
                        MonarchDeltaIndicator(-0.08, comparison: "vs last month", variant: variant, size: size)
                        MonarchDeltaIndicator(0, comparison: "vs last month", variant: variant, size: size)
                        MonarchDeltaIndicator(nil, comparison: "vs last month", variant: variant, size: size)
                    }
                }
            }
        },
        Fixture("good direction, no icon, custom format and labels") {
            VStack(alignment: .leading) {
                MonarchDeltaIndicator(-0.2, goodDirection: .down)
                MonarchDeltaIndicator(0.3, goodDirection: .neither, variant: .subtle)
                MonarchDeltaIndicator(0.3, showsIcon: false)
                MonarchDeltaIndicator(-42, variant: .subtle, formatValue: { "\(Int($0)) ms" })
                MonarchDeltaIndicator(0.004, flatThreshold: 0.01, flatLabel: "Steady")
                MonarchDeltaIndicator(
                    current: 120,
                    previous: 100,
                    comparison: "compared with the same week of last year"
                )
                MonarchDeltaIndicator(current: 5, previous: 0, noDataLabel: "New", spokenLabel: "New this week")
            }
        },
    ]
}

@Suite("MonarchDeltaIndicator")
struct MonarchDeltaIndicatorTests {
    private let english = Locale(identifier: "en_US")

    private func resolve(
        _ value: Double?,
        comparison: String? = nil,
        good: MonarchDeltaIndicatorGoodDirection = .up,
        flatThreshold: Double = 0,
        format: ((Double) -> String)? = nil
    ) -> MonarchDeltaIndicatorTokens.Result {
        let locale = english
        return MonarchDeltaIndicatorTokens.resolve(
            value: value,
            comparison: comparison,
            goodDirection: good,
            flatThreshold: flatThreshold,
            words: MonarchDeltaIndicatorTokens.Words(),
            format: format ?? { MonarchDeltaIndicatorTokens.percent($0, locale: locale) }
        )
    }

    @Test("An increase is signed with a plus and spoken as up and better")
    func increase() {
        let delta = resolve(0.12, comparison: "vs last month")
        #expect(delta.direction == .up)
        #expect(delta.tone == .positive)
        #expect(delta.text == "+12%")
        #expect(delta.label == "up 12% vs last month, better")
    }

    @Test("A decrease is signed with a real minus sign and spoken as down and worse")
    func decrease() {
        let delta = resolve(-0.085)
        #expect(delta.direction == .down)
        #expect(delta.tone == .negative)
        #expect(delta.text == "−8.5%")
        #expect(delta.label == "down 8.5%, worse")
    }

    @Test("The tone follows which direction is better")
    func goodDirection() {
        #expect(resolve(-0.2, good: .down).tone == .positive)
        #expect(resolve(-0.2, good: .down).label == "down 20%, better")
        #expect(resolve(0.2, good: .down).tone == .negative)
        #expect(resolve(0.2, good: .neither).tone == .neutral)
        #expect(resolve(0.2, good: .neither).label == "up 20%")
        #expect(resolve(-0.2, good: .neither).tone == .neutral)
    }

    @Test("Zero, a change inside the threshold and a change written as zero are no change")
    func flat() {
        #expect(resolve(0).direction == .flat)
        #expect(resolve(0).text == "No change")
        #expect(resolve(0, comparison: "vs last month").label == "No change vs last month")
        #expect(resolve(0.004, flatThreshold: 0.01).direction == .flat)
        // 0.0001 is written "0%", so it is never shown as "+0%".
        #expect(resolve(0.000_1).direction == .flat)
        #expect(resolve(-0.000_1).tone == .neutral)
    }

    @Test("Nothing to compare with shows the no-data text and no comparison")
    func noData() {
        for value in [nil, Double.nan, Double.infinity] {
            let delta = resolve(value, comparison: "vs last month")
            #expect(delta.direction == nil)
            #expect(delta.tone == .neutral)
            #expect(delta.text == "No earlier data")
            #expect(delta.label == "No earlier data")
        }
    }

    @Test("A custom format writes the size without a sign")
    func customFormat() {
        let delta = resolve(-42, format: { "\(Int($0)) ms" })
        #expect(delta.text == "−42 ms")
        #expect(delta.label == "down 42 ms, worse")
    }

    @Test("The flat and no-data texts can be replaced")
    func overrides() {
        let words = MonarchDeltaIndicatorTokens.Words(flat: "Steady", noData: "New")
        #expect(words.flat == "Steady")
        #expect(words.noData == "New")
        #expect(words.up == "up")
    }

    @Test("The ratio of two periods is relative to the previous one")
    func ratio() {
        #expect(MonarchDeltaIndicatorTokens.ratio(current: 120, previous: 100) == 0.2)
        #expect(MonarchDeltaIndicatorTokens.ratio(current: 50, previous: 100) == -0.5)
        // A negative baseline: moving from −100 to −50 is an increase.
        #expect(MonarchDeltaIndicatorTokens.ratio(current: -50, previous: -100) == 0.5)
        #expect(MonarchDeltaIndicatorTokens.ratio(current: 5, previous: 0) == nil)
        #expect(MonarchDeltaIndicatorTokens.ratio(current: nil, previous: 100) == nil)
        #expect(MonarchDeltaIndicatorTokens.ratio(current: 5, previous: nil) == nil)
        #expect(MonarchDeltaIndicatorTokens.ratio(current: .infinity, previous: 1) == nil)
    }

    @Test("Each direction has its own arrow, and no data has none")
    func symbols() {
        let up = MonarchDeltaIndicatorTokens.symbol(for: .up)
        let down = MonarchDeltaIndicatorTokens.symbol(for: .down)
        let flat = MonarchDeltaIndicatorTokens.symbol(for: .flat)
        #expect(Set([up, down, flat]).count == 3)
        #expect(up != nil && down != nil && flat != nil)
        #expect(MonarchDeltaIndicatorTokens.symbol(for: nil) == nil)
    }

    @Test("Better is the success colour, worse the danger colour, and only the pill has a fill")
    func appearance() {
        let better = MonarchDeltaIndicatorTokens.appearance(tone: .positive, variant: .plain)
        #expect(better.foreground == \MonarchColorTokens.successStrong)
        #expect(better.background == nil)
        let worse = MonarchDeltaIndicatorTokens.appearance(tone: .negative, variant: .subtle)
        #expect(worse.foreground == \MonarchColorTokens.dangerStrong)
        #expect(worse.background == \MonarchColorTokens.dangerSubtle)
        let neutral = MonarchDeltaIndicatorTokens.appearance(tone: .neutral, variant: .subtle)
        #expect(neutral.foreground == \MonarchColorTokens.textMuted)
        #expect(neutral.background == \MonarchColorTokens.bgMuted)
    }

    @Test("Only the pill has padding, and the sizes use different type roles")
    func tokens() {
        #expect(MonarchDeltaIndicatorTokens.horizontalPadding(.plain) == 0)
        #expect(MonarchDeltaIndicatorTokens.verticalPadding(.plain) == 0)
        #expect(MonarchDeltaIndicatorTokens.horizontalPadding(.subtle) > 0)
        #expect(MonarchDeltaIndicatorTokens.textStyle(.sm) == .caption)
        #expect(MonarchDeltaIndicatorTokens.textStyle(.md) == .label)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchDeltaIndicatorStrings.up == "up")
        #expect(MonarchDeltaIndicatorStrings.down == "down")
        #expect(MonarchDeltaIndicatorStrings.flat == "No change")
        #expect(MonarchDeltaIndicatorStrings.better == "better")
        #expect(MonarchDeltaIndicatorStrings.worse == "worse")
        #expect(MonarchDeltaIndicatorStrings.noData == "No earlier data")
    }
}
