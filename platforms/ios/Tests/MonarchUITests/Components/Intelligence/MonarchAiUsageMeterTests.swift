import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiUsageMeterFixtures: ComponentFixtures {
    static let id = "ai-usage-meter"
    static let fixtures = [
        Fixture("scopes: no limit, normal, near, over") {
            MonarchAiUsageMeter(
                scopes: [
                    MonarchAiUsageScope("This turn", used: 1_850),
                    MonarchAiUsageScope("Conversation", used: 12_400, limit: 50_000),
                    MonarchAiUsageScope("Today", used: 42_000, limit: 50_000),
                    MonarchAiUsageScope("This month", used: 1_040, limit: 1_000, unit: .credits),
                ]
            )
        },
        Fixture("titled, period and reset, one credit") {
            MonarchAiUsageMeter(
                "AI usage",
                scopes: [MonarchAiUsageScope("Image generation", used: 1, unit: .credits)],
                period: "November",
                resetsAt: Date(timeIntervalSince1970: 0)
            )
        },
        Fixture("loading and error") {
            VStack {
                MonarchAiUsageMeter(scopes: [], isLoading: true)
                MonarchAiUsageMeter(scopes: [], error: "Usage could not be loaded.") {}
            }
        },
        Fixture("long labels, repeated labels, own strings") {
            MonarchAiUsageMeter(
                scopes: [
                    MonarchAiUsageScope(
                        "Tokens used by the research assistant in this workspace",
                        used: 900,
                        limit: 1_000
                    ),
                    MonarchAiUsageScope(
                        "Tokens used by the research assistant in this workspace",
                        used: 3,
                        limit: 10,
                        unit: .credits
                    ),
                ],
                warningAt: 0.5,
                labels: MonarchAiUsageMeterLabels(label: "Usage", unit: { unit, _ in unit == .tokens ? "tk" : "cr" })
            )
        },
    ]
}

@Suite("MonarchAiUsageMeter")
struct MonarchAiUsageMeterTests {
    typealias Tokens = MonarchAiUsageMeterTokens

    @Test("Scopes become usage meter items with unit names and positional ids")
    func items() {
        let items = Tokens.items(
            scopes: [
                MonarchAiUsageScope("This turn", used: 1_850),
                MonarchAiUsageScope("This turn", used: 2, limit: 1_000, unit: .credits),
                MonarchAiUsageScope("Month", id: "month", used: 10, limit: 1, unit: .credits),
            ],
            labels: MonarchAiUsageMeterLabels()
        )
        #expect(items.map(\.id) == ["0", "1", "month"])
        #expect(items[0].unit == "tokens")
        #expect(items[0].limit == nil)
        #expect(items[1].unit == "credits")
        #expect(items[1].limit == 1_000)
        #expect(items[2].unit == "credit")
    }

    @Test("The unit agrees with the limit, or with the amount when there is no limit")
    func unitCount() {
        let one = Tokens.items(scopes: [MonarchAiUsageScope("A", used: 1)], labels: MonarchAiUsageMeterLabels())
        #expect(one[0].unit == "token")
        let limited = Tokens.items(
            scopes: [MonarchAiUsageScope("A", used: 1, limit: 100)],
            labels: MonarchAiUsageMeterLabels()
        )
        #expect(limited[0].unit == "tokens")
    }

    @Test("Negative amounts count as zero; a zero or negative limit is no limit")
    func clamping() {
        let items = Tokens.items(
            scopes: [
                MonarchAiUsageScope("A", used: -5, limit: 10),
                MonarchAiUsageScope("B", used: 5, limit: 0),
                MonarchAiUsageScope("C", used: 5, limit: -1),
            ],
            labels: MonarchAiUsageMeterLabels()
        )
        #expect(items[0].used == 0)
        #expect(items[1].limit == nil)
        #expect(items[2].limit == nil)
    }

    @Test("Items take the usage meter's levels: near at the warning share, over above the limit")
    func levels() {
        let items = Tokens.items(
            scopes: [
                MonarchAiUsageScope("A", used: 100, limit: 1_000),
                MonarchAiUsageScope("B", used: 850, limit: 1_000),
                MonarchAiUsageScope("C", used: 1_200, limit: 1_000),
                MonarchAiUsageScope("D", used: 1_200),
            ],
            labels: MonarchAiUsageMeterLabels()
        )
        let levels = items.map { MonarchUsageMeterTokens.level(used: $0.used, limit: $0.limit, warningAt: 0.8) }
        #expect(levels == [.normal, .warning, .over, .unlimited])
    }

    @Test("The figures read as an amount of a limit with the unit")
    func figures() {
        let item = Tokens.items(
            scopes: [MonarchAiUsageScope("Conversation", used: 12_400, limit: 50_000)],
            labels: MonarchAiUsageMeterLabels()
        )[0]
        let text = MonarchUsageMeterTokens.text(
            for: item,
            level: .normal,
            locale: Locale(identifier: "en_US"),
            labels: MonarchUsageMeterLabels()
        )
        #expect(text.figures == "12,400 of 50,000 tokens")
        #expect(text.accessibilityValue == "12,400 of 50,000 tokens, 25% used")
    }

    @Test("Own labels replace the unit names and the group name")
    func overrides() {
        let labels = MonarchAiUsageMeterLabels(label: "Usage", unit: { _, _ in "units" })
        #expect(labels.label == "Usage")
        #expect(Tokens.items(scopes: [MonarchAiUsageScope("A", used: 1)], labels: labels)[0].unit == "units")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiUsageMeterStrings.label == "AI usage")
        #expect(MonarchAiUsageMeterStrings.unit(.tokens, count: 1) == "token")
        #expect(MonarchAiUsageMeterStrings.unit(.tokens, count: 0) == "tokens")
        #expect(MonarchAiUsageMeterStrings.unit(.credits, count: 1) == "credit")
        #expect(MonarchAiUsageMeterStrings.unit(.credits, count: 2.5) == "credits")
    }
}
