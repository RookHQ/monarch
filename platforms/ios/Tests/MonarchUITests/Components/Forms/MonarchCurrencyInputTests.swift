import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCurrencyInputFixtures: ComponentFixtures {
    static let id = "currency-input"

    static let fixtures = [
        Fixture("major units with help text") {
            MonarchCurrencyInput("Amount", value: .constant(19.99), currencyCode: "USD", helper: "Charged today.")
        },
        Fixture("minor units, zero-decimal currency, code") {
            MonarchCurrencyInput("Price", minorUnits: .constant(1500), currencyCode: "JPY", currencyDisplay: .code)
        },
        Fixture("three decimals, name, stepper") {
            MonarchCurrencyInput(
                "Fee",
                value: .constant(1.25),
                currencyCode: "KWD",
                currencyDisplay: .name,
                step: 0.5,
                showsStepper: true
            )
        },
        Fixture("negative allowed, narrow symbol") {
            MonarchCurrencyInput(
                "Adjustment",
                value: .constant(-5),
                currencyCode: "EUR",
                currencyDisplay: .narrowSymbol,
                allowsNegative: true
            )
        },
        Fixture("below zero without allowsNegative") {
            MonarchCurrencyInput("Budget", value: .constant(-1), currencyCode: "USD")
        },
        Fixture("above the maximum with a custom message") {
            MonarchCurrencyInput(
                "Tip",
                value: .constant(500),
                currencyCode: "USD",
                maximum: 100,
                rangeMessage: "Tips are up to $100."
            )
        },
        Fixture("empty with prompt") {
            MonarchCurrencyInput("Amount", value: .constant(nil), currencyCode: "INR", prompt: "0.00")
        },
        Fixture("error") {
            MonarchCurrencyInput("Amount", value: .constant(10), currencyCode: "GBP", error: "Card declined.")
        },
        Fixture("disabled") {
            MonarchCurrencyInput("Amount", value: .constant(10), currencyCode: "USD").disabled(true)
        },
    ]
}

@Suite("MonarchCurrencyInput")
struct MonarchCurrencyInputTests {
    private typealias Math = MonarchCurrencyInputMath

    @Test("Each currency keeps its own number of decimals")
    func digits() {
        #expect(Math.fractionDigits(currencyCode: "USD") == 2)
        #expect(Math.fractionDigits(currencyCode: "JPY") == 0)
        #expect(Math.fractionDigits(currencyCode: "KWD") == 3)
    }

    @Test("Typed amounts are rounded to the currency's decimals, half away from zero")
    func rounding() {
        #expect(Math.rounded(19.999, digits: 2) == 20)
        #expect(Math.rounded(1.005, digits: 2) == 1.01)
        #expect(Math.rounded(-2.345, digits: 2) == -2.35)
        #expect(Math.rounded(1499.5, digits: 0) == 1500)
        #expect(Math.rounded(0.1 + 0.2, digits: 2) == 0.3)
    }

    @Test("Minor units convert without floating-point loss")
    func minorUnits() {
        #expect(Math.minorUnits(19.99, digits: 2) == 1999)
        #expect(Math.minorUnits(0.29, digits: 2) == 29)
        #expect(Math.minorUnits(1.2345, digits: 3) == 1235)
        #expect(Math.minorUnits(-4.1, digits: 2) == -410)
        #expect(Math.minorUnits(.nan, digits: 2) == nil)
        #expect(Math.majorUnits(1999, digits: 2) == 19.99)
        #expect(Math.majorUnits(-410, digits: 2) == -4.1)
        #expect(Math.majorUnits(1500, digits: 0) == 1500)
        for cents in [1, 7, 29, 1999, 123_456_789] {
            #expect(Math.minorUnits(Math.majorUnits(cents, digits: 2), digits: 2) == cents)
        }
    }

    @Test("The floor is zero unless negatives are allowed or a minimum is given")
    func lowest() {
        #expect(Math.lowest(minimum: nil, allowsNegative: false) == 0)
        #expect(Math.lowest(minimum: nil, allowsNegative: true) == nil)
        #expect(Math.lowest(minimum: -50, allowsNegative: false) == -50)
        #expect(Math.lowest(minimum: 5, allowsNegative: true) == 5)
    }

    @Test("The format writes the locale's currency with the currency's decimals")
    func format() {
        let usd = MonarchCurrencyInputTokens.format(
            currencyCode: "USD",
            display: .symbol,
            locale: Locale(identifier: "en_US")
        )
        #expect(usd.format(1234.5) == "$1,234.50")
        let jpy = MonarchCurrencyInputTokens.format(
            currencyCode: "JPY",
            display: .code,
            locale: Locale(identifier: "en_US")
        )
        #expect(jpy.format(1500).contains("JPY"))
        #expect(!jpy.format(1500).contains("."))
        let euro = MonarchCurrencyInputTokens.format(
            currencyCode: "EUR",
            display: .symbol,
            locale: Locale(identifier: "de_DE")
        )
        #expect(euro.format(1234.5).hasPrefix("1.234,50"))
    }

    @Test("Typed text is read with or without the symbol")
    func parsing() throws {
        let usd = MonarchCurrencyInputTokens.format(
            currencyCode: "USD",
            display: .symbol,
            locale: Locale(identifier: "en_US")
        )
        #expect(try usd.parseStrategy.parse("$19.99") == 19.99)
        #expect(try usd.parseStrategy.parse("19.99") == 19.99)
    }
}
