import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMoneyFixtures: ComponentFixtures {
    static let id = "money"
    static let fixtures = [
        Fixture("currencies and minor units") {
            VStack(alignment: .trailing) {
                MonarchMoney(1_234.5, currencyCode: "USD")
                MonarchMoney(minorUnits: 129_900, currencyCode: "INR")
                MonarchMoney(minorUnits: 1_999, currencyCode: "JPY")
                MonarchMoney(minorUnits: 12_345, currencyCode: "KWD")
            }
        },
        Fixture("compact, signs and currency displays") {
            VStack(alignment: .trailing) {
                MonarchMoney(1_250_000, currencyCode: "EUR", notation: .compact)
                MonarchMoney(42, currencyCode: "USD", signDisplay: .exceptZero)
                MonarchMoney(0, currencyCode: "USD", signDisplay: .always)
                MonarchMoney(-42, currencyCode: "USD", signDisplay: .never)
                MonarchMoney(42, currencyCode: "USD", currencyDisplay: .code)
                MonarchMoney(42, currencyCode: "USD", currencyDisplay: .name)
                MonarchMoney(42, currencyCode: "USD", currencyDisplay: .narrowSymbol)
            }
        },
        Fixture("negative toned, accounting, empty") {
            VStack(alignment: .trailing) {
                MonarchMoney(-1_042.75, currencyCode: "USD", tonesNegative: true)
                MonarchMoney(-42, currencyCode: "USD", usesAccountingSign: true, tonesNegative: true)
                MonarchMoney(nil, currencyCode: "USD")
                MonarchMoney(minorUnits: nil, currencyCode: "USD", emptyText: "n/a", emptyLabel: "Not billed")
            }
        },
        Fixture("type roles and a large amount") {
            VStack(alignment: .leading) {
                MonarchMoney(98_765_432.1, currencyCode: "GBP", textStyle: .display)
                MonarchMoney(12.5, currencyCode: "CHF", fractionDigits: 0...0, textStyle: .caption)
            }
        },
    ]
}

@Suite("MonarchMoney")
struct MonarchMoneyTests {
    private let english = Locale(identifier: "en_US")

    private func format(
        _ code: String,
        locale: Locale? = nil,
        notation: MonarchMoneyNotation = .standard,
        sign: MonarchMoneySignDisplay = .automatic,
        display: MonarchMoneyCurrencyDisplay = .symbol,
        accounting: Bool = false,
        digits: ClosedRange<Int>? = nil
    ) -> MonarchMoneyTokens.Format {
        MonarchMoneyTokens.Format(
            currencyCode: code,
            notation: notation,
            signDisplay: sign,
            currencyDisplay: display,
            usesAccountingSign: accounting,
            fractionDigits: digits,
            locale: locale ?? english
        )
    }

    @Test("Each currency has its own minor-unit digits")
    func digits() {
        #expect(MonarchMoneyTokens.fractionDigits(currencyCode: "USD") == 2)
        #expect(MonarchMoneyTokens.fractionDigits(currencyCode: "JPY") == 0)
        #expect(MonarchMoneyTokens.fractionDigits(currencyCode: "KWD") == 3)
    }

    @Test("Minor units convert exactly")
    func minorUnits() {
        #expect(MonarchMoneyTokens.decimal(minorUnits: 1_999, currencyCode: "USD") == Decimal(string: "19.99"))
        #expect(MonarchMoneyTokens.decimal(minorUnits: -5, currencyCode: "USD") == Decimal(string: "-0.05"))
        #expect(MonarchMoneyTokens.decimal(minorUnits: 1_999, currencyCode: "JPY") == 1_999)
        #expect(MonarchMoneyTokens.decimal(minorUnits: 12_345, currencyCode: "KWD") == Decimal(string: "12.345"))
        let large = MonarchMoneyTokens.decimal(minorUnits: 9_007_199_254_740_991, currencyCode: "USD")
        #expect(large == Decimal(string: "90071992547409.91"))
    }

    @Test("Amounts are written by the system's currency format")
    func standard() {
        #expect(MonarchMoneyTokens.string(1_234.5, format: format("USD")) == "$1,234.50")
        #expect(MonarchMoneyTokens.string(1_999, format: format("JPY")) == "¥1,999")
        #expect(MonarchMoneyTokens.string(12.5, format: format("USD", digits: 0...0)).hasPrefix("$1"))
    }

    @Test("Sign displays and the accounting sign")
    func signs() {
        #expect(MonarchMoneyTokens.string(42, format: format("USD", sign: .exceptZero)) == "+$42.00")
        #expect(MonarchMoneyTokens.string(0, format: format("USD", sign: .exceptZero)) == "$0.00")
        #expect(MonarchMoneyTokens.string(0, format: format("USD", sign: .always)).hasPrefix("+"))
        #expect(MonarchMoneyTokens.string(-42, format: format("USD", sign: .never)) == "$42.00")
        #expect(MonarchMoneyTokens.string(-42, format: format("USD", accounting: true)) == "($42.00)")
        #expect(MonarchMoneyTokens.string(-42, format: format("USD")) == "-$42.00")
    }

    @Test("Currency displays")
    func displays() {
        #expect(MonarchMoneyTokens.string(42, format: format("USD", display: .code)).contains("USD"))
        #expect(MonarchMoneyTokens.string(42, format: format("USD", display: .name)).contains("dollars"))
        #expect(MonarchMoneyTokens.string(42, format: format("USD", display: .narrowSymbol)) == "$42.00")
    }

    @Test("A compact amount is read in full")
    func compact() {
        let texts = MonarchMoneyTokens.texts(1_250_000, format: format("EUR", notation: .compact))
        #expect(texts.spoken == "€1,250,000.00")
        #expect(texts.shown != texts.spoken)
        #expect(texts.shown.count < texts.spoken.count)
        let standard = MonarchMoneyTokens.texts(1_250_000, format: format("EUR"))
        #expect(standard.shown == standard.spoken)
    }

    @Test("The compact form follows the locale: de_DE and ja_JP name the magnitude their own way (C-092)")
    func compactInOtherLocales() {
        let german = Locale(identifier: "de_DE")
        let japanese = Locale(identifier: "ja_JP")
        // Both the iOS 18 style and the assembled fallback (used before iOS 18) are checked.
        let germanFormat = format("EUR", locale: german, notation: .compact)
        let japaneseFormat = format("JPY", locale: japanese, notation: .compact)
        for text in [
            MonarchMoneyTokens.string(1_250_000, format: germanFormat),
            MonarchMoneyTokens.assembledCompact(1_250_000, format: germanFormat),
        ] {
            #expect(text.contains("Mio"))
            #expect(text.contains("€"))
            #expect(text.contains("1,") || text.hasPrefix("1"))
            #expect(text.hasSuffix("€"))
        }
        for text in [
            MonarchMoneyTokens.string(1_250_000, format: japaneseFormat),
            MonarchMoneyTokens.assembledCompact(1_250_000, format: japaneseFormat),
        ] {
            #expect(text.contains("万"))
            #expect(text.contains("¥") || text.contains("￥"))
            #expect(text.contains("125"))
        }
        // VoiceOver reads the full value in the locale's own grouping.
        let spoken = MonarchMoneyTokens.texts(1_250_000, format: germanFormat).spoken
        #expect(spoken.contains("1.250.000,00"))
        #expect(MonarchMoneyTokens.texts(1_250_000, format: japaneseFormat).spoken.contains("1,250,000"))
    }

    @Test("Before iOS 18 the compact form is assembled around the system's compact number")
    func assembledCompact() {
        let compact = MonarchMoneyTokens.assembledCompact(1_250_000, format: format("EUR", notation: .compact))
        #expect(compact.hasPrefix("€1"))
        #expect(compact.hasSuffix("M"))
        let negative = MonarchMoneyTokens.assembledCompact(-1_250_000, format: format("USD", notation: .compact))
        #expect(negative.hasPrefix("-$1"))
        let accounting = MonarchMoneyTokens.assembledCompact(
            -1_250_000,
            format: format("USD", notation: .compact, accounting: true)
        )
        #expect(accounting.hasPrefix("($1"))
        #expect(accounting.hasSuffix("M)"))
        #expect(MonarchMoneyTokens.assembledCompact(0, format: format("USD", notation: .compact)) == "$0")
    }

    @Test("The public formatter matches the view")
    func publicFormatter() {
        #expect(MonarchMoney.formatted(1_234.5, currencyCode: "USD", locale: english) == "$1,234.50")
    }

    @Test("Colour: muted when empty, danger only for toned negatives")
    func colours() {
        #expect(MonarchMoneyTokens.foreground(amount: nil, tonesNegative: false) == \.textMuted)
        #expect(MonarchMoneyTokens.foreground(amount: -1, tonesNegative: true) == \.dangerStrong)
        #expect(MonarchMoneyTokens.foreground(amount: -1, tonesNegative: false) == nil)
        #expect(MonarchMoneyTokens.foreground(amount: 1, tonesNegative: true) == nil)
        #expect(MonarchMoneyTokens.foreground(amount: 0, tonesNegative: true) == nil)
    }

    @Test("The built-in strings resolve and can be replaced")
    func strings() {
        #expect(MonarchMoneyStrings.empty == "—")
        #expect(MonarchMoneyStrings.emptyLabel == "No amount")
        #expect(MonarchMoneyTokens.emptyText("n/a") == "n/a")
        #expect(MonarchMoneyTokens.emptyLabel("Not billed") == "Not billed")
    }
}
