import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRegionSelectFixtures: ComponentFixtures {
    static let id = "region-select"

    static let fixtures = [
        Fixture("built-in list, chosen, codes") {
            MonarchRegionSelect("State", selection: .constant("US-CA"), country: "us", showsCode: true)
        },
        Fixture("no country") {
            MonarchRegionSelect("Region", selection: .constant(nil), country: nil, helper: "Choose the country above.")
        },
        Fixture("text entry for a country without a list") {
            MonarchRegionSelect("Region", selection: .constant("Lisboa"), country: "PT", error: "Check the region.")
        },
        Fixture("required, error, empty") {
            MonarchRegionSelect(
                "Province or territory of your registered business address",
                selection: .constant(nil),
                country: "CA",
                prompt: "Choose a province",
                error: "Choose a province.",
                isRequired: true
            )
        },
        Fixture("supplied regions, no text entry") {
            MonarchRegionSelect(
                "Canton",
                selection: .constant("CH-ZH"),
                country: "CH",
                regions: [MonarchRegion(code: "CH-ZH", name: "Zürich"), MonarchRegion(code: "CH-GE", name: "Genève")],
                allowsTextEntry: false
            )
        },
        Fixture("with a loader, first drawn while loading") {
            MonarchRegionSelect("State", selection: .constant(nil), country: "US") { country in
                MonarchRegionSelect.builtInRegions(for: country) ?? []
            }
        },
        Fixture("disabled") {
            MonarchRegionSelect("Nation", selection: .constant("GB-SCT"), country: "GB").disabled(true)
        },
    ]
}

@Suite("MonarchRegionSelect")
struct MonarchRegionSelectTests {
    private let zurich = [MonarchRegion(code: "CH-ZH", name: "Zürich")]

    private func source(
        country: String? = "US",
        regions: [MonarchRegion]? = nil,
        hasLoader: Bool = false,
        loaded: [MonarchRegion]? = nil,
        hasFailed: Bool = false,
        allowsTextEntry: Bool = true
    ) -> MonarchRegionSelectTokens.Source {
        MonarchRegionSelectTokens.source(
            country: country,
            regions: regions,
            hasLoader: hasLoader,
            loaded: loaded,
            hasFailed: hasFailed,
            allowsTextEntry: allowsTextEntry
        )
    }

    @Test("The built-in lists have ISO 3166-2 codes and the expected sizes")
    func builtIn() {
        #expect(MonarchRegionSelect.builtInRegions(for: "us")?.count == 51)
        #expect(MonarchRegionSelect.builtInRegions(for: "CA")?.count == 13)
        #expect(MonarchRegionSelect.builtInRegions(for: "AU")?.count == 8)
        #expect(MonarchRegionSelect.builtInRegions(for: "GB")?.map(\.code) == ["GB-ENG", "GB-NIR", "GB-SCT", "GB-WLS"])
        #expect(MonarchRegionSelect.builtInRegions(for: "IN")?.count == 36)
        #expect(MonarchRegionSelect.builtInRegions(for: "PT") == nil)
        #expect(MonarchRegionSelect.builtInRegions(for: "US")?.first == MonarchRegion(code: "US-AL", name: "Alabama"))
    }

    @Test("Each country with a list has its own word for a region")
    func fieldLabels() {
        #expect(MonarchRegionSelect.fieldLabel(for: "us") == "State")
        #expect(MonarchRegionSelect.fieldLabel(for: "CA") == "Province")
        #expect(MonarchRegionSelect.fieldLabel(for: "AU") == "State or territory")
        #expect(MonarchRegionSelect.fieldLabel(for: "GB") == "Nation")
        #expect(MonarchRegionSelect.fieldLabel(for: "IN") == "State or union territory")
        #expect(MonarchRegionSelect.fieldLabel(for: "PT") == nil)
        #expect(MonarchRegionSelect.fieldLabel(for: nil) == nil)
    }

    @Test("No country disables the field")
    func noCountry() {
        #expect(source(country: nil) == .noCountry)
        #expect(source(country: " ") == .noCountry)
    }

    @Test("Supplied regions win over the loader and the built-in lists")
    func supplied() {
        #expect(source(regions: zurich, hasLoader: true) == .list(zurich))
        #expect(source(country: "us", regions: zurich) == .list(zurich))
    }

    @Test("The loader is loading, failed or loaded, and wins over the built-in lists")
    func loader() {
        #expect(source(hasLoader: true) == .loading)
        #expect(source(hasLoader: true, hasFailed: true) == .failed)
        #expect(source(hasLoader: true, loaded: zurich) == .list(zurich))
        #expect(source(hasLoader: true, loaded: []) == .textEntry)
    }

    @Test("A country without a list gets a text field unless text entry is off")
    func textEntry() {
        #expect(source(country: "PT") == .textEntry)
        #expect(source(country: "PT", allowsTextEntry: false) == .list([]))
        #expect(source(country: "us") == .list(MonarchRegionSelect.builtInRegions(for: "US") ?? []))
    }

    @Test("Rows show the name, search the full code and show the short code when asked")
    func options() {
        let rows = MonarchRegionSelectTokens.options(
            [MonarchRegion(code: "US-CA", name: "California")],
            showsCode: true
        )
        #expect(rows.first?.label == "California")
        #expect(rows.first?.hint == "CA")
        #expect(rows.first?.keywords == "US-CA")
        #expect(MonarchRegionSelectTokens.options(zurich, showsCode: false).first?.hint == nil)
        #expect(MonarchRegionSelectTokens.shortCode("AU-NSW") == "NSW")
        #expect(MonarchRegionSelectTokens.shortCode("Lisboa") == "Lisboa")
        #expect(MonarchRegionSelectTokens.shortCode("ABC-1") == "ABC-1")
    }

    @Test("An empty typed region is no region")
    func typed() {
        #expect(MonarchRegionSelectTokens.typedValue("") == nil)
        #expect(MonarchRegionSelectTokens.typedValue("Lisboa") == "Lisboa")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchRegionSelectStrings.chooseCountryFirst == "Choose a country first")
        #expect(MonarchRegionSelectStrings.textEntry == "Type the region")
        #expect(MonarchRegionSelectStrings.empty == "No regions match")
        #expect(MonarchRegionSelectStrings.loadFailed == "Couldn’t load the regions.")
    }
}
