import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCurrencySelectFixtures: ComponentFixtures {
    static let id = "currency-select"
    static let english = Locale(identifier: "en_US")

    static let fixtures = [
        Fixture("chosen by name, with helper") {
            MonarchCurrencySelect("Currency", selection: .constant("EUR"), helper: "Used on invoices.", locale: english)
        },
        Fixture("empty with prompt, required, error") {
            MonarchCurrencySelect(
                "Currency",
                selection: .constant(nil),
                prompt: "Choose a currency",
                error: "Choose a currency.",
                locale: english,
                isRequired: true
            )
        },
        Fixture("code display, priority, no symbol") {
            MonarchCurrencySelect(
                "Code",
                selection: .constant("usd"),
                currencies: ["USD", "EUR", "GBP"],
                priority: ["GBP"],
                locale: english,
                display: .code,
                showsSymbol: false,
                isClearable: false
            )
        },
        Fixture("code and name in German, long label") {
            MonarchCurrencySelect(
                "Währung für alle künftigen Rechnungen und Gutschriften",
                selection: .constant("CHF"),
                locale: Locale(identifier: "de_DE"),
                display: .codeAndName
            )
        },
        Fixture("disabled") {
            MonarchCurrencySelect("Currency", selection: .constant("JPY"), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchCurrencySelect")
struct MonarchCurrencySelectTests {
    private let english = Locale(identifier: "en_US")
    private let french = Locale(identifier: "fr_FR")

    private func options(
        currencies: [String]? = nil,
        priority: [String] = [],
        selection: String? = nil,
        query: String = "",
        locale: Locale? = nil,
        display: MonarchCurrencySelectDisplay = .name,
        showsSymbol: Bool = true
    ) -> [MonarchComboboxOption] {
        MonarchCurrencySelectTokens.options(
            currencies: currencies,
            priority: priority,
            selection: selection,
            query: query,
            locale: locale ?? english,
            display: display,
            showsSymbol: showsSymbol,
            suggested: "Suggested",
            all: "All currencies"
        )
    }

    @Test("The default list has unique three-letter codes and includes the newest ones")
    func codes() {
        let codes = MonarchCurrencySelect.currencyCodes
        #expect(Set(codes).count == codes.count)
        #expect(codes.allSatisfy { $0.count == 3 && $0 == $0.uppercased() })
        #expect(codes.contains("XCG") && codes.contains("ZWG") && !codes.contains("HRK"))
        #expect(options().count == codes.count)
    }

    @Test("Names and narrow symbols come from the locale")
    func namesAndSymbols() {
        #expect(MonarchCurrencySelectTokens.name("EUR", locale: english) == "Euro")
        #expect(MonarchCurrencySelectTokens.symbol("USD", locale: english) == "$")
        #expect(MonarchCurrencySelectTokens.symbol("EUR", locale: english) == "€")
        #expect(MonarchCurrencySelectTokens.symbol("INR", locale: english) == "₹")
    }

    @Test("The label follows the display; the name moves to the description")
    func display() {
        #expect(MonarchCurrencySelectTokens.label(code: "EUR", name: "Euro", display: .name) == "Euro")
        #expect(MonarchCurrencySelectTokens.label(code: "EUR", name: "Euro", display: .code) == "EUR")
        #expect(MonarchCurrencySelectTokens.label(code: "EUR", name: "Euro", display: .codeAndName) == "EUR · Euro")
        let coded = options(currencies: ["EUR"], display: .code)
        #expect(coded.first?.label == "EUR")
        #expect(coded.first?.description == "Euro")
        #expect(options(currencies: ["EUR"]).first?.description == nil)
    }

    @Test("The hint is the code, with the symbol when it differs and is shown")
    func hint() {
        #expect(MonarchCurrencySelectTokens.hint(code: "EUR", symbol: "€", showsSymbol: true) == "EUR €")
        #expect(MonarchCurrencySelectTokens.hint(code: "EUR", symbol: "€", showsSymbol: false) == "EUR")
        #expect(MonarchCurrencySelectTokens.hint(code: "XYZ", symbol: "XYZ", showsSymbol: true) == "XYZ")
    }

    @Test("Rows are sorted by name in the locale; other languages search the English name")
    func order() {
        let list = options(currencies: ["USD", "EUR", "GBP"])
        #expect(list.map(\.id) == ["GBP", "EUR", "USD"])
        let french = options(currencies: ["USD"], locale: french)
        #expect(french.first?.keywords?.contains("US Dollar") == true)
        #expect(french.first?.keywords?.contains("$") == true)
    }

    @Test("Priority currencies come first under Suggested only while nothing is typed")
    func priority() {
        let idle = options(currencies: ["USD", "EUR"], priority: ["usd"])
        #expect(idle.map(\.id) == ["USD", "EUR", "USD"])
        #expect(idle.first?.group == "Suggested")
        #expect(idle.last?.group == "All currencies")
        let searching = options(currencies: ["USD", "EUR"], priority: ["USD"], query: "dollar")
        #expect(searching.map(\.id) == ["EUR", "USD"])
        #expect(searching.allSatisfy { $0.group == nil })
    }

    @Test("Search finds a currency by symbol, and a selection outside the list is kept")
    func searchAndExtra() {
        let list = options(currencies: ["USD", "EUR"], selection: "JPY")
        #expect(list.first?.id == "JPY")
        let euro = list.first { $0.id == "EUR" }
        #expect(euro.map { MonarchGeoSearch.matchesEveryWord($0, query: "€") } == true)
        #expect(euro.map { MonarchGeoSearch.matchesEveryWord($0, query: "eur") } == true)
        #expect(euro.map { MonarchGeoSearch.matchesEveryWord($0, query: "yen") } == false)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchCurrencySelectStrings.suggested == "Suggested")
        #expect(MonarchCurrencySelectStrings.all == "All currencies")
        #expect(MonarchCurrencySelectStrings.empty == "No currencies match")
    }
}
