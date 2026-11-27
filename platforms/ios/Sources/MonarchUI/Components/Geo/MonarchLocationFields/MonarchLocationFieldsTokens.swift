import MonarchTokens
import Observation
import SwiftUI

/// Layout tokens and the pure value logic of ``MonarchLocationFields``.
enum MonarchLocationFieldsTokens {
    /// The narrowest a column may be, 14rem as on the web.
    static let columnMinimumWidth: CGFloat = 224
    /// The gap between the fields.
    static let fieldSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between a field and the load error under it.
    static let retrySpacing: CGFloat = MonarchTokens.Space.space2
    /// The prefix of a city option that keeps typed text, so it never clashes with a code.
    static let typedCityPrefix = "\u{0}typed:"

    /// Where the countries from the adapter stand.
    enum CountriesLoad: Equatable {
        case loading
        case failed
        case ready([MonarchLocationOption])
    }

    // MARK: Country

    /// The value after choosing a country: a new country clears every other part.
    static func choosingCountry(_ value: MonarchLocationValue, _ country: String?) -> MonarchLocationValue {
        guard country != value.country else { return value }
        return MonarchLocationValue(country: country)
    }

    /// The combobox options for the countries from the adapter. The chosen
    /// code is kept as an option while the list loads, so the field shows it.
    static func countryOptions(_ load: CountriesLoad, selection: String?) -> [MonarchComboboxOption] {
        if case .ready(let countries) = load {
            return countries.map { MonarchComboboxOption(id: $0.id, label: $0.label, description: $0.description) }
        }
        return selection.map { [MonarchComboboxOption(id: $0, label: $0)] } ?? []
    }

    // MARK: Region

    /// What the region field shows as chosen: the code, or the typed text.
    static func regionSelection(_ value: MonarchLocationValue) -> String? {
        if let region = value.region { return region }
        return value.regionName.isEmpty ? nil : value.regionName
    }

    /// The value after the region field changed. A code from `regions`
    /// stores the code and the name and, when it differs, clears the city;
    /// anything else is typed text.
    static func choosingRegion(
        _ value: MonarchLocationValue,
        _ selection: String?,
        regions: [MonarchRegion]
    ) -> MonarchLocationValue {
        var next = value
        if let selection, let region = regions.first(where: { $0.code == selection }) {
            if region.code != value.region {
                next.city = ""
                next.cityID = nil
            }
            next.region = region.code
            next.regionName = region.name
        } else {
            if value.region != nil {
                next.city = ""
                next.cityID = nil
            }
            next.region = nil
            next.regionName = selection ?? ""
        }
        return next
    }

    /// The region field's label: the adapter's word for the country, the
    /// app's label, Monarch's word for the country, then "State or region".
    static func regionLabel(country: String?, adapterLabel: String?, labels: MonarchLocationFieldsLabels) -> String {
        adapterLabel ?? labels.region ?? MonarchRegionSelect.fieldLabel(for: country)
            ?? MonarchLocationFieldsStrings.region
    }

    // MARK: Postal code

    /// The postal code field's label: the app's label, the country's word, then "Postal code".
    static func postalCodeLabel(country: String?, labels: MonarchLocationFieldsLabels) -> String {
        if let label = labels.postalCode { return label }
        return MonarchGeoSearch.normalized(country).flatMap(MonarchLocationFieldsStrings.postalCodeLabel)
            ?? MonarchLocationFieldsStrings.postalCode
    }

    // MARK: City

    /// What the city field shows as chosen: the city's code, or the typed text marked as typed.
    static func citySelection(_ value: MonarchLocationValue) -> String? {
        if let cityID = value.cityID { return cityID }
        return value.city.isEmpty ? nil : typedCityPrefix + value.city
    }

    /// The option that keeps typed text as the city.
    static func typedCityOption(_ text: String) -> MonarchComboboxOption? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        return MonarchComboboxOption(id: typedCityPrefix + trimmed, label: trimmed)
    }

    /// The city options: the search results, then the chosen city when it is
    /// not among them, so the field always shows it.
    static func cityOptions(_ cities: [MonarchLocationCity], value: MonarchLocationValue) -> [MonarchComboboxOption] {
        var options = cities.map { MonarchComboboxOption(id: $0.id, label: $0.name, description: $0.description) }
        if let selection = citySelection(value), !options.contains(where: { $0.id == selection }) {
            options.append(MonarchComboboxOption(id: selection, label: value.city))
        }
        return options
    }

    /// The value after the city field changed: cleared, typed text, or a city
    /// from the results, which also fills in its region when none is chosen.
    static func choosingCity(
        _ value: MonarchLocationValue,
        _ selection: String?,
        cities: [MonarchLocationCity]
    ) -> MonarchLocationValue {
        var next = value
        guard let selection else {
            next.city = ""
            next.cityID = nil
            return next
        }
        if selection.hasPrefix(typedCityPrefix) {
            next.city = String(selection.dropFirst(typedCityPrefix.count))
            next.cityID = nil
            return next
        }
        guard let city = cities.first(where: { $0.id == selection }) else { return value }
        next.city = city.name
        next.cityID = city.id
        if let region = city.region, value.region == nil {
            next.region = region.code
            next.regionName = region.name
        }
        return next
    }

    /// The fewest letters the city search runs for: none once a region is
    /// chosen (its cities are listed at once), otherwise `minimum`.
    static func cityMinimumLength(_ value: MonarchLocationValue, minimum: Int) -> Int {
        value.region == nil ? max(minimum, 0) : 0
    }

    /// The text the city list shows when it has no rows.
    static func cityEmptyMessage(
        _ phase: MonarchDebouncedSearchPhase,
        minimum: Int,
        labels: MonarchLocationFieldsLabels
    ) -> String {
        switch phase {
        case .failed:
            labels.citiesLoadFailed ?? MonarchLocationFieldsStrings.citiesLoadFailed
        case .idle, .tooShort:
            labels.typeMore?(minimum) ?? MonarchLocationFieldsStrings.typeMore(minimum)
        case .loading, .done:
            labels.noMatch ?? MonarchLocationFieldsStrings.noMatch
        }
    }

    /// The text of the row that keeps the typed city.
    static func useTyped(_ text: String, labels: MonarchLocationFieldsLabels) -> String {
        labels.useTyped?(text) ?? MonarchLocationFieldsStrings.useTyped(text)
    }
}

/// What starts a city search: the country, the region and the text.
struct MonarchLocationFieldsCitySearchKey: Hashable {
    let country: String?
    let region: String?
    let query: String
}
