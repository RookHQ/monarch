import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiThinkingIndicatorFixtures: ComponentFixtures {
    static let id = "ai-thinking-indicator"
    static let fixtures = [
        Fixture("shimmer: just started") {
            MonarchAiThinkingIndicator(isAnnounced: false)
        },
        Fixture("mark: long wait with elapsed time") {
            MonarchAiThinkingIndicator(
                label: "Searching your files…",
                startedAt: Date(timeIntervalSinceNow: -65),
                variant: .mark,
                isAnnounced: false
            )
        },
        Fixture("shimmer: long label, custom elapsed text") {
            MonarchAiThinkingIndicator(
                label: "Reading the twelve attached contracts and comparing their renewal clauses…",
                startedAt: Date(timeIntervalSinceNow: -8),
                isAnnounced: false,
                elapsedLabel: { "\($0) seconds" }
            )
        },
        Fixture("elapsed time never shown") {
            MonarchAiThinkingIndicator(
                startedAt: Date(timeIntervalSinceNow: -600),
                showsElapsedAfter: .infinity,
                isAnnounced: false
            )
        },
    ]
}

@Suite("MonarchAiThinkingIndicator")
struct MonarchAiThinkingIndicatorTests {
    typealias Tokens = MonarchAiThinkingIndicatorTokens
    let english = Locale(identifier: "en_US")

    @Test("Elapsed seconds are whole and never negative")
    func elapsedSeconds() {
        let start = Date(timeIntervalSinceReferenceDate: 1000)
        #expect(Tokens.elapsedSeconds(now: start.addingTimeInterval(4.9), start: start) == 4)
        #expect(Tokens.elapsedSeconds(now: start.addingTimeInterval(65), start: start) == 65)
        #expect(Tokens.elapsedSeconds(now: start.addingTimeInterval(-3), start: start) == 0)
    }

    @Test("The elapsed time appears after the delay, never with an infinite delay")
    func showsElapsed() {
        #expect(!Tokens.showsElapsed(seconds: 2, after: 3))
        #expect(Tokens.showsElapsed(seconds: 3, after: 3))
        #expect(Tokens.showsElapsed(seconds: 0, after: 0))
        #expect(!Tokens.showsElapsed(seconds: 10_000, after: .infinity))
        #expect(Tokens.defaultElapsedDelay == 3)
    }

    @Test("Elapsed time reads 5s, 1m 5s and 2m in English")
    func elapsedText() {
        #expect(Tokens.elapsedText(seconds: 5, locale: english) == "5s")
        #expect(Tokens.elapsedText(seconds: 65, locale: english) == "1m 5s")
        #expect(Tokens.elapsedText(seconds: 120, locale: english) == "2m")
    }

    @Test("The shimmer band sweeps from before the leading edge to past the trailing edge")
    func band() {
        #expect(abs(Tokens.bandCentre(at: 0) + Tokens.bandHalfWidth) < 0.0001)
        let late = Tokens.bandCentre(at: MonarchPulse.period * 0.999)
        #expect(late > 1)
        for step in 0..<40 {
            let centre = Tokens.bandCentre(at: Double(step) * 0.05)
            let locations = Tokens.bandLocations(centre: centre)
            #expect(locations.start <= locations.centre && locations.centre <= locations.end)
            #expect(locations.start >= 0 && locations.end <= 1)
        }
    }

    @Test("The mark pulses between its minimum and full opacity, and is still under Reduce Motion")
    func mark() {
        #expect(Tokens.markOpacity(at: nil) == 1)
        #expect(abs(Tokens.markOpacity(at: 0) - 1) < 0.0001)
        #expect(abs(Tokens.markOpacity(at: MonarchPulse.period / 2) - Tokens.markMinimumOpacity) < 0.0001)
    }

    @Test("The label is muted and the accent is the assistant colour")
    func roles() {
        #expect(Tokens.label == \MonarchColorTokens.textMuted)
        #expect(Tokens.accent == \MonarchColorTokens.aiStrong)
        #expect(Tokens.elapsed == \MonarchColorTokens.textSubtle)
    }

    @Test("The built-in label comes from the strings table")
    func strings() {
        #expect(MonarchAiThinkingIndicatorStrings.thinking == "Thinking…")
    }
}
