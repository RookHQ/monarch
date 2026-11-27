import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCountrySelectFixtures: ComponentFixtures {
    static let id = "country-select"
    static let english = Locale(identifier: "en_US")

    static let fixtures = [
        Fixture("chosen, with helper") {
            MonarchCountrySelect("Country", selection: .constant("PT"), helper: "Where you pay tax.", locale: english)
        },
        Fixture("empty with prompt, required, error") {
            MonarchCountrySelect(
                "Country",
                selection: .constant(nil),
                prompt: "Choose a country",
                error: "Choose a country.",
                locale: english,
                isRequired: true
            )
        },
        Fixture("priority, limited list, no flags or codes") {
            MonarchCountrySelect(
                "Ship to",
                selection: .constant("gb"),
                countries: ["GB", "US", "FR", "DE"],
                priority: ["US"],
                locale: english,
                showsFlag: false,
                showsCode: false,
                isClearable: false
            )
        },
        Fixture("French names, long override") {
            MonarchCountrySelect(
                "Pays de résidence fiscale pour la déclaration annuelle",
                selection: .constant("DE"),
                locale: Locale(identifier: "fr_FR"),
                countryName: { $0 == "DE" ? "République fédérale d’Allemagne (nom officiel complet)" : nil }
            )
        },
        Fixture("disabled") {
            MonarchCountrySelect("Country", selection: .constant("JP"), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchCountrySelect")
struct MonarchCountrySelectTests {
    private let english = Locale(identifier: "en_US")
    private let french = Locale(identifier: "fr_FR")

    private func options(
        countries: [String]? = nil,
        excluded: [String] = [],
        priority: [String] = [],
        selection: String? = nil,
        query: String = "",
        locale: Locale? = nil,
        showsFlag: Bool = true,
        showsCode: Bool = true,
        countryName: (@Sendable (String) -> String?)? = nil
    ) -> [MonarchComboboxOption] {
        MonarchCountrySelectTokens.options(
            countries: countries,
            excluded: excluded,
            priority: priority,
            selection: selection,
            query: query,
            locale: locale ?? english,
            showsFlag: showsFlag,
            showsCode: showsCode,
            suggested: "Suggested",
            all: "All countries",
            countryName: countryName
        )
    }

    @Test("The default list has the 249 ISO 3166-1 codes, each two upper-case letters, once")
    func codes() {
        let codes = MonarchCountrySelect.countryCodes
        #expect(codes.count == 249)
        #expect(Set(codes).count == 249)
        #expect(codes.allSatisfy { $0.count == 2 && $0 == $0.uppercased() })
        #expect(options().count == 249)
    }

    @Test("Flags are built from regional indicator symbols; anything else gives no flag")
    func flags() {
        #expect(MonarchCountrySelect.flag(for: "PT") == "🇵🇹")
        #expect(MonarchCountrySelect.flag(for: "gb") == "🇬🇧")
        #expect(MonarchCountrySelect.flag(for: "GBR").isEmpty)
        #expect(MonarchCountrySelect.flag(for: "1A").isEmpty)
        #expect(MonarchCountrySelect.flag(for: "").isEmpty)
    }

    @Test("Names come from the locale and are sorted in it")
    func namesAndOrder() {
        let english = options(countries: ["DE", "AT", "FR"])
        #expect(english.map(\.label) == ["Austria", "France", "Germany"])
        #expect(english.map(\.id) == ["AT", "FR", "DE"])
        let french = options(countries: ["DE", "AT", "FR"], locale: french)
        #expect(french.map(\.label) == ["Allemagne", "Autriche", "France"])
        #expect(french.first?.keywords == "Germany")
        #expect(english.first?.keywords == nil)
    }

    @Test("Codes are upper-cased, deduplicated and filtered")
    func filtering() {
        let list = options(countries: ["de", "DE", "fr", "at"], excluded: ["at"])
        #expect(list.map(\.id) == ["FR", "DE"])
    }

    @Test("The hint has the code and the flag as asked")
    func hints() {
        #expect(MonarchCountrySelectTokens.hint("PT", showsCode: true, showsFlag: true) == "PT 🇵🇹")
        #expect(MonarchCountrySelectTokens.hint("PT", showsCode: true, showsFlag: false) == "PT")
        #expect(MonarchCountrySelectTokens.hint("PT", showsCode: false, showsFlag: true) == "🇵🇹")
        #expect(MonarchCountrySelectTokens.hint("PT", showsCode: false, showsFlag: false) == nil)
    }

    @Test("Priority countries come first under Suggested only while nothing is typed")
    func priority() {
        let idle = options(countries: ["DE", "FR", "US"], priority: ["us", "XX", "US"])
        #expect(idle.map(\.id) == ["US", "FR", "DE", "US"])
        #expect(idle.map(\.group) == ["Suggested", "All countries", "All countries", "All countries"])
        let searching = options(countries: ["DE", "FR", "US"], priority: ["US"], query: "fr")
        #expect(searching.map(\.id) == ["FR", "DE", "US"])
        #expect(searching.allSatisfy { $0.group == nil })
    }

    @Test("A selection outside the list is kept so it can be shown")
    func extraSelection() {
        let list = options(countries: ["DE", "FR"], selection: "PT")
        #expect(list.first?.id == "PT")
        #expect(list.first?.label == "Portugal")
        #expect(options(countries: ["DE", "FR"], selection: "DE").map(\.id) == ["FR", "DE"])
    }

    @Test("The name override wins; nil keeps Foundation's name")
    func override() {
        let list = options(countries: ["DE", "FR"], countryName: { $0 == "DE" ? "Deutschland" : nil })
        #expect(list.map(\.label) == ["Deutschland", "France"])
    }

    @Test("Search needs every word in the name, code, hint or keywords, ignoring case and accents")
    func search() {
        let option = MonarchComboboxOption(id: "CI", label: "Côte d’Ivoire", hint: "CI", keywords: "Ivory Coast")
        #expect(MonarchGeoSearch.matchesEveryWord(option, query: "cote"))
        #expect(MonarchGeoSearch.matchesEveryWord(option, query: "ivory coast"))
        #expect(MonarchGeoSearch.matchesEveryWord(option, query: "ci"))
        #expect(MonarchGeoSearch.matchesEveryWord(option, query: "  "))
        #expect(!MonarchGeoSearch.matchesEveryWord(option, query: "ivory france"))
    }

    @Test("Codes are normalised to upper case; blanks are no selection")
    func normalized() {
        #expect(MonarchGeoSearch.normalized("pt") == "PT")
        #expect(MonarchGeoSearch.normalized(" ") == nil)
        #expect(MonarchGeoSearch.normalized(nil) == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchCountrySelectStrings.suggested, MonarchCountrySelectStrings.all, MonarchCountrySelectStrings.empty,
        ]
        #expect(all == ["Suggested", "All countries", "No countries match"])
    }
}
