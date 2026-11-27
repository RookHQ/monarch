import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchStatCardFixtures: ComponentFixtures {
    static let id = "stat-card"
    static let fixtures = [
        Fixture("number, change, hint, help, sparkline, drill-down") {
            MonarchStatCard(
                "Open tickets",
                value: 128,
                change: MonarchMetricChange(0.12, comparison: "vs last week", goodDirection: .down),
                hint: "38 waiting on a customer",
                help: "Tickets that are not solved or closed.",
                systemImage: "tray",
                sparkline: [96, 104, nil, 121, 128],
                action: {}
            )
        },
        Fixture("every size") {
            VStack {
                ForEach(MonarchMetricSize.allCases, id: \.self) { size in
                    MonarchStatCard("Response time", value: 840, unit: "ms", size: size)
                }
            }
        },
        Fixture("every tone") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchStatCard(
                        "Error rate",
                        value: 0.042,
                        format: .percent,
                        hint: "Above the 2% target",
                        tone: tone
                    )
                }
            }
        },
        Fixture("currency, compact") {
            MonarchStatCard("Revenue", value: 1_284_000, format: .currency(code: "EUR"), isCompact: true)
        },
        Fixture("text value, plain variant") {
            MonarchStatCard("Rating", text: "4.8 / 5", spokenText: "4.8 out of 5", variant: .plain)
        },
        Fixture("empty value") {
            MonarchStatCard("Churn", value: nil)
        },
        Fixture("loading") {
            MonarchStatCard("Churn", value: nil, isLoading: true)
        },
        Fixture("error with retry, error without") {
            VStack {
                MonarchStatCard(
                    "Uptime",
                    value: nil,
                    errorMessage: "Couldn’t load uptime.",
                    isRetrying: true,
                    onRetry: {}
                )
                MonarchStatCard("Uptime", value: nil, errorMessage: "Couldn’t load uptime.")
            }
        },
        Fixture("long label and hint") {
            MonarchStatCard(
                "Median time from ticket creation to the first reply by an agent",
                value: 3_600,
                unit: "seconds",
                hint: "Measured over business hours only, excluding weekends and public holidays"
            )
        },
    ]
}

@Suite("MonarchStatCard")
struct MonarchStatCardTests {
    @Test("Loading wins over an error, and an empty message is no error")
    func phase() {
        #expect(MonarchStatCardTokens.phase(isLoading: false, errorMessage: nil) == .value)
        #expect(MonarchStatCardTokens.phase(isLoading: true, errorMessage: "Failed") == .loading)
        #expect(MonarchStatCardTokens.phase(isLoading: false, errorMessage: "Failed") == .error)
        #expect(MonarchStatCardTokens.phase(isLoading: false, errorMessage: "") == .value)
    }

    @Test("A neutral card has the card border and no tone icon")
    func neutral() {
        #expect(MonarchStatCardTokens.border(.neutral) == MonarchCardTokens.border)
        #expect(MonarchStatCardTokens.toneSymbol(.neutral) == nil)
        #expect(MonarchStatCardTokens.sparklineTone(.neutral) == nil)
    }

    @Test("Every other tone has its own border and an icon")
    func tones() {
        let others = MonarchTone.allCases.filter { $0 != .neutral }
        for tone in others {
            #expect(MonarchStatCardTokens.border(tone) == MonarchTonePalette.palette(for: tone).border)
            #expect(MonarchStatCardTokens.toneSymbol(tone) != nil)
            #expect(MonarchStatCardTokens.toneColor(tone) == MonarchTonePalette.palette(for: tone).strong)
            #expect(MonarchStatCardTokens.sparklineTone(tone) == tone)
        }
        #expect(Set(others.compactMap(MonarchStatCardTokens.toneSymbol)).count == others.count)
    }

    @Test("Only the card variant draws a surface")
    func surface() {
        #expect(MonarchStatCardTokens.hasSurface(.card))
        #expect(!MonarchStatCardTokens.hasSurface(.plain))
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchStatCardStrings.loading == "Loading")
        #expect(MonarchStatCardStrings.helpLabel("Open tickets") == "About Open tickets")
        #expect(!MonarchStatCardStrings.drillDownHint.hasPrefix("monarch."))
    }
}
