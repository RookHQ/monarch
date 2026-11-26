import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchUsageMeterFixtures: ComponentFixtures {
    static let id = "usage-meter"
    static let fixtures = [
        Fixture("every level") {
            MonarchUsageMeter(
                "Usage",
                items: [
                    MonarchUsageMeterItem("Requests", used: 4_200, limit: 10_000),
                    MonarchUsageMeterItem("Storage", used: 42, limit: 50, unit: "GB"),
                    MonarchUsageMeterItem("Minutes", used: 130, limit: 100, description: "Extra minutes are billed."),
                    MonarchUsageMeterItem("Seats", used: 4, unit: "seats"),
                ],
                period: "This month",
                resetsAt: Date(timeIntervalSince1970: 1_796_083_200)
            )
        },
        Fixture("no header, custom labels and long text") {
            MonarchUsageMeter(
                items: [
                    MonarchUsageMeterItem(
                        "Automated workflow runs across every workspace of the organisation",
                        used: 99_999.5,
                        limit: 100_000,
                        unit: "runs",
                        description: "A run counts once it starts, whether or not it finishes without an error."
                    )
                ],
                warningAt: 0.5,
                labels: MonarchUsageMeterLabels(nearLimit: "Almost used up", retry: "Reload")
            )
        },
        Fixture("loading") {
            MonarchUsageMeter("Usage", items: [], period: "This month", isLoading: true)
        },
        Fixture("error with and without retry") {
            VStack {
                MonarchUsageMeter("Usage", items: [], error: "Usage could not be loaded.") {}
                MonarchUsageMeter(items: [], error: "Usage could not be loaded.")
            }
        },
        Fixture("loading with items") {
            MonarchUsageMeter(
                "Usage",
                items: [MonarchUsageMeterItem("Requests", used: 1, limit: 2), MonarchUsageMeterItem("Seats", used: 1)],
                isLoading: true
            )
        },
        Fixture("retrying") {
            MonarchUsageMeter("Usage", items: [], error: "Usage could not be loaded.", isRetrying: true) {}
        },
        Fixture("empty") {
            MonarchUsageMeter("Usage", items: [])
        },
    ]
}

@Suite("MonarchUsageMeter")
struct MonarchUsageMeterTests {
    private let english = Locale(identifier: "en_US")
    private let german = Locale(identifier: "de_DE")
    private let labels = MonarchUsageMeterLabels()

    @Test("The level follows the amount, the limit and the warning fraction")
    func level() {
        #expect(MonarchUsageMeterTokens.level(used: 10, limit: 100, warningAt: 0.8) == .normal)
        #expect(MonarchUsageMeterTokens.level(used: 79.9, limit: 100, warningAt: 0.8) == .normal)
        #expect(MonarchUsageMeterTokens.level(used: 80, limit: 100, warningAt: 0.8) == .warning)
        // Exactly at the limit is near it, not over it.
        #expect(MonarchUsageMeterTokens.level(used: 100, limit: 100, warningAt: 0.8) == .warning)
        #expect(MonarchUsageMeterTokens.level(used: 100.1, limit: 100, warningAt: 0.8) == .over)
        #expect(MonarchUsageMeterTokens.level(used: 50, limit: 100, warningAt: 0.5) == .warning)
    }

    @Test("No limit, or a limit of zero or less, is unlimited")
    func unlimited() {
        #expect(MonarchUsageMeterTokens.level(used: 10, limit: nil, warningAt: 0.8) == .unlimited)
        #expect(MonarchUsageMeterTokens.level(used: 10, limit: 0, warningAt: 0.8) == .unlimited)
        #expect(MonarchUsageMeterTokens.level(used: 10, limit: -5, warningAt: 0.8) == .unlimited)
        #expect(MonarchUsageMeterTokens.level(used: 10, limit: .nan, warningAt: 0.8) == .unlimited)
    }

    @Test("Near and over the limit have their own colour and their own icon")
    func roles() {
        #expect(MonarchUsageMeterTokens.roles(for: .normal).fill == \MonarchColorTokens.textMuted)
        #expect(MonarchUsageMeterTokens.roles(for: .warning).fill == \MonarchColorTokens.warningStrong)
        #expect(MonarchUsageMeterTokens.roles(for: .warning).status == \MonarchColorTokens.warningStrong)
        #expect(MonarchUsageMeterTokens.roles(for: .over).fill == \MonarchColorTokens.dangerSolid)
        #expect(MonarchUsageMeterTokens.roles(for: .over).status == \MonarchColorTokens.dangerStrong)

        #expect(MonarchUsageMeterTokens.symbol(for: .normal) == nil)
        #expect(MonarchUsageMeterTokens.symbol(for: .unlimited) == nil)
        let warning = MonarchUsageMeterTokens.symbol(for: .warning)
        let over = MonarchUsageMeterTokens.symbol(for: .over)
        #expect(warning != nil && over != nil && warning != over)
    }

    @Test("A normal item shows the figures and the share used")
    func normalText() {
        let item = MonarchUsageMeterItem("Requests", used: 4_200, limit: 10_000)
        let text = MonarchUsageMeterTokens.text(for: item, level: .normal, locale: english, labels: labels)
        #expect(text.figures == "4,200 of 10,000")
        #expect(text.status == "42% used")
        #expect(text.accessibilityValue == "4,200 of 10,000, 42% used")
    }

    @Test("Near and over the limit are written, with the unit after the figures")
    func levelText() {
        let near = MonarchUsageMeterItem("Storage", used: 42, limit: 50, unit: "GB")
        let nearText = MonarchUsageMeterTokens.text(for: near, level: .warning, locale: english, labels: labels)
        #expect(nearText.figures == "42 of 50 GB")
        #expect(nearText.status == "Near the limit · 84% used")
        #expect(nearText.accessibilityValue == "42 of 50 GB, Near the limit, 84% used")

        let over = MonarchUsageMeterItem("Minutes", used: 130, limit: 100)
        let overText = MonarchUsageMeterTokens.text(for: over, level: .over, locale: english, labels: labels)
        #expect(overText.status == "Over the limit · 130% used")
        #expect(overText.accessibilityValue == "130 of 100, Over the limit, 130% used")
    }

    @Test("An item without a limit shows the count alone")
    func unlimitedText() {
        let item = MonarchUsageMeterItem("Seats", used: 4, unit: "seats")
        let text = MonarchUsageMeterTokens.text(for: item, level: .unlimited, locale: english, labels: labels)
        #expect(text.figures == "4 seats")
        #expect(text.status.isEmpty)
        #expect(text.accessibilityValue == "4 seats")
    }

    @Test("Figures follow the locale and keep at most one fraction digit")
    func numbers() {
        #expect(MonarchUsageMeterTokens.number(7_200, locale: english) == "7,200")
        #expect(MonarchUsageMeterTokens.number(7_200, locale: german) == "7.200")
        #expect(MonarchUsageMeterTokens.number(1.26, locale: english) == "1.3")
        #expect(MonarchUsageMeterTokens.percent(used: 82, limit: 100, locale: english) == "82%")
    }

    @Test("Custom labels replace the built-in ones")
    func customLabels() {
        let custom = MonarchUsageMeterLabels(
            amount: { "\($0) / \($1)" },
            percent: { "\($0) consumed" },
            nearLimit: "Almost used up"
        )
        let item = MonarchUsageMeterItem("Storage", used: 45, limit: 50)
        let text = MonarchUsageMeterTokens.text(for: item, level: .warning, locale: english, labels: custom)
        #expect(text.figures == "45 / 50")
        #expect(text.status == "Almost used up · 90% consumed")
        // The ones that were not replaced stay built in.
        #expect(custom.overLimit == "Over the limit")
    }

    @Test("The subtitle joins the period and the reset date, in the given time zone")
    func subtitle() throws {
        let utc = try #require(TimeZone(identifier: "UTC"))
        let tokyo = try #require(TimeZone(identifier: "Asia/Tokyo"))
        // 1 December 2026, 20:00 UTC, which is 2 December in Tokyo.
        let date = Date(timeIntervalSince1970: 1_796_155_200)
        func subtitle(period: String?, resetsAt: Date?, timeZone: TimeZone) -> String? {
            MonarchUsageMeterTokens.subtitle(
                period: period,
                resetsAt: resetsAt,
                locale: english,
                timeZone: timeZone,
                labels: labels
            )
        }
        #expect(subtitle(period: nil, resetsAt: nil, timeZone: utc) == nil)
        #expect(subtitle(period: "", resetsAt: nil, timeZone: utc) == nil)
        #expect(subtitle(period: "This month", resetsAt: nil, timeZone: utc) == "This month")
        #expect(subtitle(period: nil, resetsAt: date, timeZone: utc) == "Resets Dec 1, 2026")
        #expect(subtitle(period: "This month", resetsAt: date, timeZone: utc) == "This month · Resets Dec 1, 2026")
        #expect(subtitle(period: nil, resetsAt: date, timeZone: tokyo) == "Resets Dec 2, 2026")
    }

    @Test("Only the items that just crossed a threshold are announced, once, and not when the meter first appears")
    func crossings() {
        func levels(_ items: [MonarchUsageMeterItem]) -> [MonarchUsageMeterLevel] {
            items.map { MonarchUsageMeterTokens.level(used: $0.used, limit: $0.limit, warningAt: 0.8) }
        }
        let before = [
            MonarchUsageMeterItem("Requests", id: "r", used: 1, limit: 10),
            MonarchUsageMeterItem("Storage", id: "s", used: 9, limit: 10),
            MonarchUsageMeterItem("Minutes", id: "m", used: 5, limit: 10),
        ]
        let after = [
            MonarchUsageMeterItem("Requests", id: "r", used: 8, limit: 10),
            MonarchUsageMeterItem("Storage", id: "s", used: 9, limit: 10),
            MonarchUsageMeterItem("Minutes", id: "m", used: 12, limit: 10),
        ]
        let previous = Dictionary(uniqueKeysWithValues: zip(before.map(\.id), levels(before)))
        let crossed = MonarchUsageMeterTokens.crossings(previous: previous, items: after, levels: levels(after))
        // Storage was already near the limit: not read again.
        #expect(crossed.items.map(\.label) == ["Requests", "Minutes"])
        #expect(crossed.levels == [.warning, .over])
        #expect(MonarchUsageMeterTokens.crossings(previous: [:], items: after, levels: levels(after)).items.isEmpty)
        let unchanged = MonarchUsageMeterTokens.crossings(previous: previous, items: before, levels: levels(before))
        #expect(unchanged.items.isEmpty)
        // A drop below a threshold is not announced.
        let drop = MonarchUsageMeterTokens.crossings(
            previous: Dictionary(uniqueKeysWithValues: zip(after.map(\.id), levels(after))),
            items: before,
            levels: levels(before)
        )
        #expect(drop.items.isEmpty)
    }

    @Test("Only items near or over their limit are announced")
    func announcement() {
        let items = [
            MonarchUsageMeterItem("Requests", used: 1, limit: 10),
            MonarchUsageMeterItem("Storage", used: 9, limit: 10),
            MonarchUsageMeterItem("Minutes", used: 11, limit: 10),
            MonarchUsageMeterItem("Seats", used: 4),
        ]
        let levels = items.map { MonarchUsageMeterTokens.level(used: $0.used, limit: $0.limit, warningAt: 0.8) }
        #expect(
            MonarchUsageMeterTokens.announcement(items: items, levels: levels, labels: labels)
                == "Storage: Near the limit. Minutes: Over the limit"
        )
        #expect(
            MonarchUsageMeterTokens.announcement(
                items: [items[0], items[3]],
                levels: [.normal, .unlimited],
                labels: labels
            )
            .isEmpty
        )
    }

    @Test("The bar is limited to the track")
    func clamped() {
        #expect(MonarchUsageMeterTokens.clamped(-0.2) == 0)
        #expect(MonarchUsageMeterTokens.clamped(0.4) == 0.4)
        #expect(MonarchUsageMeterTokens.clamped(1.3) == 1)
        #expect(MonarchUsageMeterTokens.clamped(.nan) == 0)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchUsageMeterStrings.amount(used: "1", limit: "2") == "1 of 2")
        #expect(MonarchUsageMeterStrings.percent("82%") == "82% used")
        #expect(MonarchUsageMeterStrings.nearLimit == "Near the limit")
        #expect(MonarchUsageMeterStrings.overLimit == "Over the limit")
        #expect(MonarchUsageMeterStrings.resets("1 Dec") == "Resets 1 Dec")
        #expect(
            MonarchUsageMeterStrings.announcement(label: "Seats", level: "Over the limit") == "Seats: Over the limit"
        )
        #expect(MonarchUsageMeterStrings.loading == "Loading usage")
        #expect(MonarchUsageMeterStrings.retry == "Try again")
    }

    @Test("An item's identity is its label unless another is given")
    func identity() {
        #expect(MonarchUsageMeterItem("Seats", used: 1).id == "Seats")
        #expect(MonarchUsageMeterItem("Seats", id: "seats-eu", used: 1).id == "seats-eu")
    }

    @Test("Placeholder rows follow the item count: three when there are none, at most eight")
    func placeholders() {
        #expect(MonarchUsageMeterTokens.placeholderItems(count: 0).count == 3)
        #expect(MonarchUsageMeterTokens.placeholderItems(count: 2).count == 2)
        #expect(MonarchUsageMeterTokens.placeholderItems(count: 20).count == 8)
        let items = MonarchUsageMeterTokens.placeholderItems(count: 3)
        #expect(Set(items.map(\.id)).count == 3)
        #expect(items.allSatisfy { $0.limit != nil })
    }
}
