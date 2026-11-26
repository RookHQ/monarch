import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPhoneCountrySelectFixtures: ComponentFixtures {
    static let id = "phone-country-select"
    static let english = Locale(identifier: "en_US")

    static let fixtures = [
        Fixture("chosen, preferred countries") {
            MonarchPhoneCountrySelect(
                "Country code",
                selection: .constant("PT"),
                preferred: ["PT", "ES"],
                locale: english
            )
        },
        Fixture("empty with prompt, required, error, clearable") {
            MonarchPhoneCountrySelect(
                "Country code",
                selection: .constant(nil),
                prompt: "Choose a country",
                error: "Choose a country code.",
                isRequired: true,
                isClearable: true
            )
        },
        Fixture("limited list, no flags, custom row text") {
            MonarchPhoneCountrySelect(
                "Code",
                selection: .constant("gb"),
                helper: "We text a code to this number.",
                countries: ["GB", "IE", "US"],
                locale: english,
                showsFlags: false,
                labels: MonarchPhoneCountrySelectLabels(option: { name, code in "\(code) \(name)" })
            )
        },
        Fixture("long label, Arabic names") {
            MonarchPhoneCountrySelect(
                "The country of the mobile number we send sign-in codes to",
                selection: .constant("EG"),
                locale: Locale(identifier: "ar_EG")
            )
        },
        Fixture("disabled") {
            MonarchPhoneCountrySelect("Country code", selection: .constant("US"), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchPhoneCountrySelect")
struct MonarchPhoneCountrySelectTests {
    private let english = Locale(identifier: "en_US")

    private func options(
        countries: [String]? = nil,
        preferred: [String] = [],
        showsFlags: Bool = true
    ) -> [MonarchComboboxOption] {
        MonarchPhoneCountrySelectTokens.options(
            countries: countries,
            preferred: preferred,
            locale: english,
            showsFlags: showsFlags,
            preferredTitle: "Suggested",
            allTitle: "All countries",
            optionText: { MonarchPhoneCountrySelectStrings.option(name: $0, dialCode: $1) }
        )
    }

    @Test("The table parses into unique countries with calling codes and leading digits")
    func table() {
        let all = MonarchPhoneCountry.all
        #expect(all.count == 243)
        #expect(Set(all.map(\.code)).count == all.count)
        #expect(all.allSatisfy { $0.dialCode.hasPrefix("+") && $0.dialCode.dropFirst().allSatisfy(\.isNumber) })
        #expect(MonarchPhoneCountry.country(code: "gb")?.dialCode == "+44")
        #expect(
            MonarchPhoneCountry.country(code: "BS")
                == MonarchPhoneCountry(code: "BS", dialCode: "+1", leadingDigits: ["242"])
        )
        #expect(MonarchPhoneCountry.country(code: "DO")?.leadingDigits == ["809", "829", "849"])
        #expect(MonarchPhoneCountry.country(code: "ZZ") == nil)
    }

    @Test("Numbers resolve to countries by their calling code and leading digits")
    func matching() {
        #expect(MonarchPhoneCountry.matching("+44 20 7946 0000")?.code == "GB")
        #expect(MonarchPhoneCountry.matching("+1 242 555 0100")?.code == "BS")
        #expect(MonarchPhoneCountry.matching("+1 415 555 0100")?.code == "US")
        #expect(MonarchPhoneCountry.matching("+1 416 555 0100", preferred: "CA")?.code == "CA")
        #expect(MonarchPhoneCountry.matching("0044 1534 000000")?.code == "JE")
        #expect(MonarchPhoneCountry.matching("+351 912 345 678")?.code == "PT")
        #expect(MonarchPhoneCountry.matching("+7 7 01 000 0000")?.code == "KZ")
        #expect(MonarchPhoneCountry.matching("020 7946 0000") == nil)
        #expect(MonarchPhoneCountry.matching("+") == nil)
        #expect(MonarchPhoneCountry.matching("+abc") == nil)
    }

    @Test("Other scripts, full-width characters and bidirectional marks are read")
    func otherKeyboards() {
        #expect(MonarchPhoneCountrySelectTokens.compact("\u{202A}＋٤٤ (20) 7946-0000\u{202C}") == "+442079460000")
        #expect(MonarchPhoneCountry.matching("＋９１ ９８７６５ ４３２１０")?.code == "IN")
        #expect(MonarchPhoneCountry.matching("+۹۸ ۹۱۲")?.code == "IR")
    }

    @Test("Rows read name (calling code), sorted by label, with the flag as the hint")
    func rows() {
        let list = options(countries: ["PT", "es", "GB", "PT"])
        #expect(list.map(\.label) == ["Portugal (+351)", "Spain (+34)", "United Kingdom (+44)"])
        #expect(list.first?.hint == "🇵🇹")
        #expect(list.allSatisfy { $0.group == nil })
        #expect(options(countries: ["PT"], showsFlags: false).first?.hint == nil)
    }

    @Test("Preferred countries are listed once, first, under Suggested")
    func preferred() {
        let list = options(countries: ["PT", "ES", "GB"], preferred: ["gb", "XX", "GB"])
        #expect(list.map(\.id) == ["GB", "PT", "ES"])
        #expect(list.map(\.group) == ["Suggested", "All countries", "All countries"])
    }

    @Test("A country is searched by its code and its calling code with and without the plus")
    func search() {
        let bahamas = options(countries: ["BS"])[0]
        #expect(MonarchPhoneCountrySelectTokens.keywords(MonarchPhoneCountry.country(code: "BS")!) == "BS +1 1 +1242")
        for query in ["bahamas", "bs", "+1", "1242", "+1242"] {
            #expect(MonarchGeoSearch.matchesEveryWord(bahamas, query: query))
        }
        #expect(!MonarchGeoSearch.matchesEveryWord(bahamas, query: "+44"))
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchPhoneCountrySelectStrings.suggested == "Suggested")
        #expect(MonarchPhoneCountrySelectStrings.all == "All countries")
        #expect(MonarchPhoneCountrySelectStrings.empty == "No matching country")
        #expect(MonarchPhoneCountrySelectStrings.option(name: "Portugal", dialCode: "+351") == "Portugal (+351)")
    }
}
