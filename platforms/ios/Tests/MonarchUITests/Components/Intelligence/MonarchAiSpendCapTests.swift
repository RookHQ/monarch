import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiSpendCapFixtures: ComponentFixtures {
    static let id = "ai-spend-cap"

    static let resets = Date(timeIntervalSince1970: 1_780_272_000)
    static let breakdown = [
        MonarchAiSpendCapBreakdownItem(id: "research", label: "Research agent", spent: 820),
        MonarchAiSpendCapBreakdownItem(id: "summaries", label: "Meeting summaries", spent: 420),
    ]

    static let fixtures: [Fixture] =
        MonarchAiSpendCapState.allCases.map { state in
            Fixture("state \(state.rawValue)") {
                MonarchAiSpendCap(
                    spent: state == .ok ? 1_240 : state == .warning ? 4_200 : 5_210,
                    limit: state == .noLimit ? nil : 5_000,
                    currencyCode: "GBP",
                    period: .month,
                    resetsAt: resets
                )
            }
        } + [
            Fixture("breakdown") {
                MonarchAiSpendCap(spent: 1_240, limit: 5_000, currencyCode: "GBP", period: .week, breakdown: breakdown)
            },
            Fixture("editing") {
                MonarchAiSpendCap(
                    spent: 1_240,
                    limit: 5_000,
                    currencyCode: "GBP",
                    period: .month,
                    maxLimit: 50_000,
                    isEditing: .constant(true)
                )
            },
            Fixture("editing, saving") {
                MonarchAiSpendCap(
                    spent: 0,
                    limit: nil,
                    currencyCode: "USD",
                    period: .day,
                    behavior: .notify,
                    isEditing: .constant(true),
                    isSaving: true
                )
            },
            Fixture("read only, paused") {
                MonarchAiSpendCap(
                    spent: 5_000,
                    limit: 5_000,
                    currencyCode: "GBP",
                    period: .month,
                    canEdit: false,
                    onRequestIncrease: {}
                )
            },
            Fixture("read only, notify, micros") {
                MonarchAiSpendCap(
                    spent: 12_400_000,
                    limit: 50_000_000,
                    currencyCode: "USD",
                    period: .day,
                    unit: .micros,
                    behavior: .notify,
                    resetsAt: resets,
                    canEdit: false,
                    onRequestIncrease: {}
                )
            },
            Fixture("over the limit, notify") {
                MonarchAiSpendCap(spent: 5_210, limit: 5_000, currencyCode: "JPY", period: .month, behavior: .notify)
            },
        ]
}

@Suite("MonarchAiSpendCap")
struct MonarchAiSpendCapTests {
    private let gb = Locale(identifier: "en_GB")
    private let utc = TimeZone(identifier: "UTC") ?? .current

    private func phrases(
        _ currency: String = "GBP",
        unit: MonarchAiSpendCapUnit = .minor,
        period: MonarchAiSpendCapPeriod = .month,
        labels: MonarchAiSpendCapLabels = MonarchAiSpendCapLabels()
    ) -> MonarchAiSpendCapTokens.Phrases {
        MonarchAiSpendCapTokens.Phrases(currencyCode: currency, unit: unit, period: period, locale: gb, labels: labels)
    }

    @Test("The state follows the limit and the warning share")
    func state() {
        #expect(MonarchAiSpendCapTokens.state(spent: 10, limit: nil, warnAt: 0.8) == .noLimit)
        #expect(MonarchAiSpendCapTokens.state(spent: 79, limit: 100, warnAt: 0.8) == .ok)
        #expect(MonarchAiSpendCapTokens.state(spent: 80, limit: 100, warnAt: 0.8) == .warning)
        #expect(MonarchAiSpendCapTokens.state(spent: 100, limit: 100, warnAt: 0.8) == .reached)
        #expect(MonarchAiSpendCapTokens.state(spent: 150, limit: 100, warnAt: 0.8) == .reached)
        #expect(MonarchAiSpendCapTokens.state(spent: 0, limit: 0, warnAt: 0.8) == .reached)
        #expect(MonarchAiSpendCapTokens.state(spent: -20, limit: 100, warnAt: 0.8) == .ok)
        #expect(MonarchAiSpendCapTokens.state(spent: 70, limit: 100, warnAt: 0.7) == .warning)
    }

    @Test("Each state has a tone, a symbol and a bar colour")
    func looks() {
        let symbols = Set(MonarchAiSpendCapState.allCases.map { MonarchAiSpendCapTokens.look($0).symbol })
        #expect(symbols.count == MonarchAiSpendCapState.allCases.count)
        #expect(MonarchAiSpendCapTokens.look(.reached).tone == .danger)
        #expect(MonarchAiSpendCapTokens.look(.warning).tone == .warning)
        #expect(MonarchAiSpendCapTokens.barTone(.ok) == .neutral)
        #expect(MonarchAiSpendCapTokens.barTone(.warning) == .warning)
        #expect(MonarchAiSpendCapTokens.barTone(.reached) == .danger)
        #expect(MonarchAiSpendCapTokens.fraction(spent: 50, limit: 100) == 0.5)
        #expect(MonarchAiSpendCapTokens.fraction(spent: 150, limit: 100) == 1)
        #expect(MonarchAiSpendCapTokens.fraction(spent: 1, limit: 0) == 1)
        #expect(MonarchAiSpendCapTokens.isPaused(.reached, behavior: .pause))
        #expect(!MonarchAiSpendCapTokens.isPaused(.reached, behavior: .notify))
        #expect(!MonarchAiSpendCapTokens.isPaused(.warning, behavior: .pause))
    }

    @Test("Integer amounts become money without floating point")
    func money() {
        #expect(MonarchAiSpendCapTokens.decimal(1_240, currencyCode: "GBP", unit: .minor) == Decimal(string: "12.40"))
        #expect(
            MonarchAiSpendCapTokens.decimal(12_400_000, currencyCode: "GBP", unit: .micros) == Decimal(string: "12.4")
        )
        #expect(MonarchAiSpendCapTokens.decimal(500, currencyCode: "JPY", unit: .minor) == 500)
        #expect(MonarchAiSpendCapTokens.decimal(-5, currencyCode: "USD", unit: .minor) == Decimal(string: "-0.05"))
        #expect(MonarchAiSpendCapTokens.money(1_240, currencyCode: "GBP", unit: .minor, locale: gb) == "£12.40")
        #expect(MonarchAiSpendCapTokens.money(12_400_000, currencyCode: "GBP", unit: .micros, locale: gb) == "£12.40")
    }

    @Test("The amount field converts between major units and the unit")
    func conversion() {
        #expect(MonarchAiSpendCapTokens.toMajor(1_240, currencyCode: "GBP", unit: .minor) == 12.4)
        #expect(MonarchAiSpendCapTokens.toMajor(12_345_678, currencyCode: "USD", unit: .micros) == 12.35)
        #expect(MonarchAiSpendCapTokens.fromMajor(12.4, currencyCode: "GBP", unit: .minor) == 1_240)
        #expect(MonarchAiSpendCapTokens.fromMajor(12.4, currencyCode: "GBP", unit: .micros) == 12_400_000)
        #expect(MonarchAiSpendCapTokens.fromMajor(0.1 + 0.2, currencyCode: "USD", unit: .minor) == 30)
        #expect(MonarchAiSpendCapTokens.fromMajor(12.345, currencyCode: "USD", unit: .minor) == 1_235)
        #expect(MonarchAiSpendCapTokens.fromMajor(500, currencyCode: "JPY", unit: .minor) == 500)
    }

    @Test("Warn-at choices include the current value and start at the nearest")
    func warnAt() {
        #expect(MonarchAiSpendCapTokens.warnAtShares(options: [0.9, 0.5, 0.5, 1, 0], current: 0.8) == [0.5, 0.8, 0.9])
        #expect(MonarchAiSpendCapTokens.initialWarnAt(shares: [0.5, 0.8], current: 0.8) == 0.8)
        #expect(MonarchAiSpendCapTokens.initialWarnAt(shares: [0.5, 0.75], current: 1.2) == 0.75)
        #expect(MonarchAiSpendCapTokens.initialWarnAt(shares: [], current: 0.8) == 0.8)
        #expect(MonarchAiSpendCapTokens.percent(0.8, locale: gb) == "80%")
    }

    @Test("Saving checks the amount, and a limit below spending needs a second save")
    func validate() {
        func check(_ major: Double?, warned: Int? = nil, max: Int? = 10_000) -> MonarchAiSpendCapTokens.Validation {
            MonarchAiSpendCapTokens.validate(
                major: major,
                spent: 2_000,
                maxLimit: max,
                warnedBelow: warned,
                currencyCode: "GBP",
                unit: .minor
            )
        }
        #expect(check(nil) == .required)
        #expect(check(0) == .notPositive)
        #expect(check(-5) == .notPositive)
        #expect(check(150) == .aboveMaximum)
        #expect(check(150, max: nil) == .valid(15_000))
        #expect(check(10) == .belowSpent(1_000))
        #expect(check(10, warned: 1_000) == .valid(1_000))
        #expect(check(10, warned: 900) == .belowSpent(1_000))
        #expect(check(50) == .valid(5_000))
    }

    @Test("The phrases read the amounts, the period and the state")
    func phrasesText() {
        let text = phrases()
        #expect(text.title == "AI spending limit")
        #expect(text.amountLine(spent: 1_240, limit: 5_000) == "£12.40 of £50.00 this month")
        #expect(text.amountLine(spent: 1_240, limit: nil) == "£12.40 spent this month")
        #expect(text.amountLine(spent: -10, limit: nil) == "£0.00 spent this month")
        #expect(text.remainder(spent: 1_240, limit: 5_000) == "£37.60 left")
        #expect(text.remainder(spent: 5_210, limit: 5_000) == "£2.10 over the limit")
        #expect(text.warnsAt(0.8) == "Warns at 80%")
        #expect(text.warnAtOption(0.75) == "75% of the limit")
        #expect(text.behaviorSummary(.pause) == "At the limit, AI pauses and asks")
        #expect(text.behaviorSummary(.notify) == "At the limit, AI keeps going and tells you")
        #expect(phrases(period: .day).periodText == "today")
        #expect(phrases(period: .week).periodText == "this week")
        #expect(text.state(.noLimit) == "No limit set")
        #expect(text.pausedBody(limit: 5_000).hasPrefix("The £50.00 limit for this month has been reached."))
        #expect(text.amountHint(spent: 1_240) == "£12.40 spent this month so far.")
        #expect(
            text.announcement(.reached, amountLine: "£50.00 of £50.00 this month")
                == "AI spending limit: Limit reached. £50.00 of £50.00 this month"
        )
    }

    @Test("The reset date shows the time only for a daily limit")
    func resets() {
        let date = MonarchAiSpendCapFixtures.resets
        let monthly = phrases().resets(date, timeZone: utc)
        #expect(monthly == "Resets 1 June")
        let daily = phrases(period: .day).resets(date, timeZone: utc)
        #expect(daily.hasPrefix("Resets 1 June"))
        #expect(daily.contains("00:00"))
    }

    @Test("Validation messages")
    func messages() {
        let text = phrases()
        #expect(
            text.message(.required, spent: 0, maxLimit: nil, behavior: .pause)
                == "Enter an amount. To have no limit, use Remove limit."
        )
        #expect(
            text.message(.notPositive, spent: 0, maxLimit: nil, behavior: .pause)
                == "Enter an amount greater than zero."
        )
        #expect(text.message(.aboveMaximum, spent: 0, maxLimit: 50_000, behavior: .pause) == "Enter £500.00 or less.")
        #expect(
            text.message(.belowSpent(100), spent: 2_000, maxLimit: nil, behavior: .pause)
                == "This is below the £20.00 already spent this month, so AI will pause straight away. Save again to confirm."
        )
        #expect(text.message(.valid(100), spent: 0, maxLimit: nil, behavior: .pause) == nil)
        let custom = phrases(labels: MonarchAiSpendCapLabels(title: "Budget", errorMax: { "Max \($0)" }))
        #expect(custom.title == "Budget")
        #expect(custom.message(.aboveMaximum, spent: 0, maxLimit: 100, behavior: .pause) == "Max £1.00")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiSpendCapStrings.meterLabel == "Spent against the limit")
        #expect(MonarchAiSpendCapStrings.edit == "Edit limit")
        #expect(MonarchAiSpendCapStrings.setLimit == "Set a limit")
        #expect(MonarchAiSpendCapStrings.readOnly == "Only a workspace admin can change this limit.")
        #expect(MonarchAiSpendCapStrings.removeConfirmTitle == "Remove the spending limit?")
        #expect(MonarchAiSpendCapStrings.removeCancel == "Keep limit")
        #expect(MonarchAiSpendCapStrings.saveAnyway == "Save anyway")
        for behavior in MonarchAiSpendCapBehavior.allCases {
            #expect(!MonarchAiSpendCapStrings.behaviorOption(behavior).hasPrefix("monarch."))
            #expect(!MonarchAiSpendCapStrings.behaviorDescription(behavior).hasPrefix("monarch."))
        }
        for state in MonarchAiSpendCapState.allCases {
            #expect(!MonarchAiSpendCapStrings.state(state).hasPrefix("monarch."))
        }
    }

    @Test("The form makes the card taller")
    @MainActor
    func layout() {
        let closed = Host.layout(MonarchAiSpendCap(spent: 1_240, limit: 5_000, currencyCode: "GBP", period: .month))
        let open = Host.layout(
            MonarchAiSpendCap(
                spent: 1_240,
                limit: 5_000,
                currencyCode: "GBP",
                period: .month,
                isEditing: .constant(true)
            )
        )
        #expect(open.height > closed.height)
    }
}
