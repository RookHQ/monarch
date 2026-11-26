import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLocationFieldsFixtures: ComponentFixtures {
    static let id = "location-fields"

    private static let english = Locale(identifier: "en_US")

    private static let filled = MonarchLocationValue(
        country: "US",
        region: "US-CA",
        regionName: "California",
        city: "Oakland",
        postalCode: "94612"
    )

    static let fixtures = [
        Fixture("filled US address, legend, required") {
            MonarchLocationFields(
                "Shipping address",
                value: .constant(filled),
                required: [.country, .city],
                locale: english
            )
        },
        Fixture("empty, errors on every field") {
            MonarchLocationFields(
                value: .constant(MonarchLocationValue()),
                errors: [
                    .country: "Choose a country.",
                    .region: "Choose a region.",
                    .city: "Enter a city.",
                    .postalCode: "Enter a postal code.",
                ],
                locale: english
            )
        },
        Fixture("country without a region list, typed region") {
            MonarchLocationFields(
                value: .constant(MonarchLocationValue(country: "PT", regionName: "Lisboa", city: "Lisboa")),
                locale: english
            )
        },
        Fixture("adapter lists, first drawn while loading, typed city") {
            MonarchLocationFields(
                "Office",
                value: .constant(MonarchLocationValue(country: "AE", city: "Al Ain")),
                adapter: MonarchLocationAdapter(
                    countries: { [MonarchLocationOption(id: "AE", label: "United Arab Emirates")] },
                    regions: { _ in
                        MonarchLocationRegions([MonarchRegion(code: "AE-AZ", name: "Abu Dhabi")], label: "Emirate")
                    },
                    cities: { _ in [MonarchLocationCity(id: "aa", name: "Al Ain")] }
                )
            )
        },
        Fixture("city search with no country") {
            MonarchLocationFields(
                value: .constant(MonarchLocationValue()),
                adapter: MonarchLocationAdapter(cities: { _ in [] }),
                locale: english
            )
        },
        Fixture("some fields, long legend") {
            MonarchLocationFields(
                "Where the company is registered for tax purposes in its home jurisdiction",
                value: .constant(MonarchLocationValue(country: "GB", postalCode: "SW1A 1AA")),
                fields: [.country, .postalCode],
                locale: english
            )
        },
        Fixture("disabled") {
            MonarchLocationFields(value: .constant(filled), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchLocationFields")
struct MonarchLocationFieldsTests {
    private typealias Tokens = MonarchLocationFieldsTokens

    private let california = MonarchRegion(code: "US-CA", name: "California")
    private let texas = MonarchRegion(code: "US-TX", name: "Texas")

    private var filled: MonarchLocationValue {
        MonarchLocationValue(
            country: "US",
            region: "US-CA",
            regionName: "California",
            city: "Oakland",
            cityID: "oak",
            postalCode: "94612"
        )
    }

    @Test("A new country clears every other part; the same country keeps them")
    func country() {
        #expect(Tokens.choosingCountry(filled, "US") == filled)
        #expect(Tokens.choosingCountry(filled, "CA") == MonarchLocationValue(country: "CA"))
        #expect(Tokens.choosingCountry(filled, nil) == MonarchLocationValue())
    }

    @Test("The adapter's countries become options; the chosen code shows while they load")
    func countryOptions() {
        #expect(Tokens.countryOptions(.loading, selection: "AE").map(\.id) == ["AE"])
        #expect(Tokens.countryOptions(.failed, selection: nil).isEmpty)
        let ready = Tokens.countryOptions(
            .ready([MonarchLocationOption(id: "AE", label: "United Arab Emirates", description: "Gulf")]),
            selection: nil
        )
        #expect(ready.first?.label == "United Arab Emirates")
        #expect(ready.first?.description == "Gulf")
    }

    @Test("The region field shows the code, or the typed name")
    func regionSelection() {
        #expect(Tokens.regionSelection(filled) == "US-CA")
        #expect(Tokens.regionSelection(MonarchLocationValue(country: "PT", regionName: "Lisboa")) == "Lisboa")
        #expect(Tokens.regionSelection(MonarchLocationValue(country: "PT")) == nil)
    }

    @Test("Choosing a listed region stores its code and name and clears a city of another region")
    func choosingListedRegion() {
        let same = Tokens.choosingRegion(filled, "US-CA", regions: [california, texas])
        #expect(same == filled)
        let other = Tokens.choosingRegion(filled, "US-TX", regions: [california, texas])
        #expect(other.region == "US-TX")
        #expect(other.regionName == "Texas")
        #expect(other.city.isEmpty)
        #expect(other.cityID == nil)
        #expect(other.postalCode == "94612")
    }

    @Test("Typed or cleared regions keep no code")
    func choosingTypedRegion() {
        let typed = Tokens.choosingRegion(
            MonarchLocationValue(country: "PT", city: "Porto"),
            "Norte",
            regions: []
        )
        #expect(typed.region == nil)
        #expect(typed.regionName == "Norte")
        #expect(typed.city == "Porto")
        let cleared = Tokens.choosingRegion(filled, nil, regions: [california])
        #expect(cleared.region == nil)
        #expect(cleared.regionName.isEmpty)
        #expect(cleared.city.isEmpty)
    }

    @Test("The region label prefers the adapter's word, then the app's, then the country's, then the default")
    func regionLabel() {
        let none = MonarchLocationFieldsLabels()
        #expect(Tokens.regionLabel(country: "AE", adapterLabel: "Emirate", labels: none) == "Emirate")
        #expect(
            Tokens.regionLabel(country: "US", adapterLabel: nil, labels: MonarchLocationFieldsLabels(region: "Area"))
                == "Area"
        )
        #expect(Tokens.regionLabel(country: "US", adapterLabel: nil, labels: none) == "State")
        #expect(Tokens.regionLabel(country: "CA", adapterLabel: nil, labels: none) == "Province")
        #expect(Tokens.regionLabel(country: "PT", adapterLabel: nil, labels: none) == "State or region")
        #expect(Tokens.regionLabel(country: nil, adapterLabel: nil, labels: none) == "State or region")
    }

    @Test("The postal code label follows the country")
    func postalCodeLabel() {
        let none = MonarchLocationFieldsLabels()
        #expect(Tokens.postalCodeLabel(country: "us", labels: none) == "ZIP code")
        #expect(Tokens.postalCodeLabel(country: "GB", labels: none) == "Postcode")
        #expect(Tokens.postalCodeLabel(country: "AU", labels: none) == "Postcode")
        #expect(Tokens.postalCodeLabel(country: "IN", labels: none) == "PIN code")
        #expect(Tokens.postalCodeLabel(country: "CA", labels: none) == "Postal code")
        #expect(Tokens.postalCodeLabel(country: nil, labels: none) == "Postal code")
        #expect(
            Tokens.postalCodeLabel(country: "US", labels: MonarchLocationFieldsLabels(postalCode: "Post")) == "Post"
        )
    }

    @Test("The city field shows the code, or the typed text marked as typed")
    func citySelection() {
        #expect(Tokens.citySelection(filled) == "oak")
        let typed = MonarchLocationValue(country: "PT", city: "Porto")
        #expect(Tokens.citySelection(typed) == Tokens.typedCityPrefix + "Porto")
        #expect(Tokens.citySelection(MonarchLocationValue(country: "PT")) == nil)
    }

    @Test("Typed text becomes a typed city option, trimmed; blank text does not")
    func typedOption() {
        let option = Tokens.typedCityOption("  Al Ain ")
        #expect(option?.id == Tokens.typedCityPrefix + "Al Ain")
        #expect(option?.label == "Al Ain")
        #expect(Tokens.typedCityOption("   ") == nil)
    }

    @Test("City options are the results plus the chosen city when missing")
    func cityOptions() {
        let results = [MonarchLocationCity(id: "sf", name: "San Francisco", description: "California")]
        let options = Tokens.cityOptions(results, value: filled)
        #expect(options.map(\.id) == ["sf", "oak"])
        #expect(options.last?.label == "Oakland")
        #expect(options.first?.description == "California")
        #expect(Tokens.cityOptions(results, value: MonarchLocationValue(country: "US")).map(\.id) == ["sf"])
    }

    @Test("Choosing a city stores it and fills in its region only when none is chosen")
    func choosingCity() {
        let austin = MonarchLocationCity(id: "aus", name: "Austin", region: texas)
        let start = MonarchLocationValue(country: "US")
        let chosen = Tokens.choosingCity(start, "aus", cities: [austin])
        #expect(chosen.city == "Austin")
        #expect(chosen.cityID == "aus")
        #expect(chosen.region == "US-TX")
        #expect(chosen.regionName == "Texas")
        let keepsRegion = Tokens.choosingCity(filled, "aus", cities: [austin])
        #expect(keepsRegion.region == "US-CA")
        #expect(keepsRegion.cityID == "aus")
        let typed = Tokens.choosingCity(filled, Tokens.typedCityPrefix + "Alameda", cities: [])
        #expect(typed.city == "Alameda")
        #expect(typed.cityID == nil)
        let cleared = Tokens.choosingCity(filled, nil, cities: [])
        #expect(cleared.city.isEmpty)
        #expect(cleared.cityID == nil)
        #expect(Tokens.choosingCity(filled, "unknown", cities: [austin]) == filled)
    }

    @Test("Cities need letters until a region is chosen")
    func cityMinimum() {
        #expect(Tokens.cityMinimumLength(MonarchLocationValue(country: "US"), minimum: 2) == 2)
        #expect(Tokens.cityMinimumLength(filled, minimum: 2) == 0)
        #expect(Tokens.cityMinimumLength(MonarchLocationValue(country: "US"), minimum: -1) == 0)
    }

    @Test("The city list's empty text follows the search phase and can be replaced")
    func cityEmpty() {
        let none = MonarchLocationFieldsLabels()
        #expect(Tokens.cityEmptyMessage(.failed, minimum: 2, labels: none) == "Couldn’t load the cities.")
        #expect(Tokens.cityEmptyMessage(.tooShort, minimum: 3, labels: none) == "Type at least 3 letters to search.")
        #expect(Tokens.cityEmptyMessage(.idle, minimum: 2, labels: none) == "Type at least 2 letters to search.")
        #expect(Tokens.cityEmptyMessage(.done, minimum: 2, labels: none) == "No cities match")
        let custom = MonarchLocationFieldsLabels(
            citiesLoadFailed: "Offline",
            typeMore: { "More than \($0)" },
            noMatch: "Nothing"
        )
        #expect(Tokens.cityEmptyMessage(.failed, minimum: 2, labels: custom) == "Offline")
        #expect(Tokens.cityEmptyMessage(.tooShort, minimum: 2, labels: custom) == "More than 2")
        #expect(Tokens.cityEmptyMessage(.done, minimum: 2, labels: custom) == "Nothing")
        #expect(Tokens.useTyped("Porto", labels: none) == "Use “Porto”")
        #expect(
            Tokens.useTyped("Porto", labels: MonarchLocationFieldsLabels(useTyped: { "Keep \($0)" })) == "Keep Porto"
        )
    }

    @Test("The region cache keeps the adapter's answer per country")
    @MainActor
    func regionCache() {
        let cache = MonarchKeyedCache<String, MonarchLocationRegions>()
        cache.store(MonarchLocationRegions([california], label: "State"), for: "US")
        #expect(cache.answers["US"]?.regions == [california])
        #expect(cache.answers["US"]?.label == "State")
        #expect(cache.answers["CA"] == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchLocationFieldsStrings.country == "Country")
        #expect(MonarchLocationFieldsStrings.region == "State or region")
        #expect(MonarchLocationFieldsStrings.city == "City")
        #expect(MonarchLocationFieldsStrings.postalCode == "Postal code")
        #expect(MonarchLocationFieldsStrings.countriesLoadFailed == "Couldn’t load the countries.")
        #expect(MonarchLocationFieldsStrings.chooseCountryFirst == "Choose a country first")
        #expect(MonarchLocationFieldsStrings.postalCodeLabel("FR") == nil)
    }
}
