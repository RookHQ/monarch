import Foundation
import SwiftUI

/// Country, region, city and postal code as one address block, with each
/// country's own words for its fields and lists from an injected adapter.
///
/// ```swift
/// MonarchLocationFields(
///     "Billing address",
///     value: $address,
///     adapter: MonarchLocationAdapter(
///         cities: { query in try await places.cities(query.country, query.region, query.query) }
///     ),
///     required: [.country, .city]
/// )
/// ```
///
/// The country is a ``MonarchCountrySelect`` (or a ``MonarchCombobox`` of
/// the adapter's countries); choosing one clears the other parts. The region
/// is a ``MonarchRegionSelect`` labelled with the country's word for it
/// ("State", "Province or territory", or the adapter's word) and becomes a
/// text field for a country without a list. With a city search the city is
/// a ``MonarchCombobox`` searched as people type, which can keep typed text
/// as the city and fills in the region of a chosen city; without one it is
/// a text field. The postal code is a text field labelled with the
/// country's word ("ZIP code" for US). Monarch makes no network calls: the
/// adapter's closures call the app's service and are cancelled when their
/// answer is no longer needed. Disable the whole block with `disabled(_:)`.
///
/// Accessibility: the legend is a header, and the block is a container. Each
/// field keeps its own label, value, hint, error and required mark from the
/// component it is built on. The text fields tell AutoFill they hold the
/// city and the postal code. A failed country load is an inline error with
/// Try again; a failed city search is the city list's empty text.
public struct MonarchLocationFields: View {
    private let legend: String?
    @Binding private var value: MonarchLocationValue
    private let adapter: MonarchLocationAdapter
    private let fields: [MonarchLocationField]
    private let required: Set<MonarchLocationField>
    private let errors: [MonarchLocationField: String]
    private let priorityCountries: [String]
    private let locale: Locale?
    private let cityMinQuery: Int
    private let cityDebounce: Duration
    private let allowsCustomCity: Bool
    private let labels: MonarchLocationFieldsLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var countriesLoad = MonarchLocationFieldsTokens.CountriesLoad.loading
    @State private var countriesAttempt = 0
    @State private var regionCache = MonarchKeyedCache<String, MonarchLocationRegions>()
    @State private var citySearch = MonarchDebouncedSearch<MonarchLocationCity>()
    @State private var cityQuery = ""

    private typealias Tokens = MonarchLocationFieldsTokens

    /// An address block.
    ///
    /// - Parameters:
    ///   - legend: A visible heading over the fields, already localised, such as "Shipping address".
    ///   - value: The address. Changing the country clears the region, city and postal code.
    ///   - adapter: Where the countries, regions and cities come from. Built-in lists and text fields by default.
    ///   - fields: The fields to show, in order. All four by default.
    ///   - required: The fields that must be filled; they show a required mark where the field has one.
    ///   - errors: An error message per field, shown with an icon and announced.
    ///   - priorityCountries: Country codes listed first in the built-in country list.
    ///   - locale: The language of the built-in country names. The environment's locale by default.
    ///   - cityMinQuery: Letters needed before cities are searched while no region is chosen.
    ///   - cityDebounce: How long to wait after typing before cities are searched.
    ///   - allowsCustomCity: Offers to keep the typed text as the city when it is not in the results.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ legend: String? = nil,
        value: Binding<MonarchLocationValue>,
        adapter: MonarchLocationAdapter = MonarchLocationAdapter(),
        fields: [MonarchLocationField] = MonarchLocationField.allCases,
        required: Set<MonarchLocationField> = [],
        errors: [MonarchLocationField: String] = [:],
        priorityCountries: [String] = [],
        locale: Locale? = nil,
        cityMinQuery: Int = 2,
        cityDebounce: Duration = .milliseconds(250),
        allowsCustomCity: Bool = true,
        labels: MonarchLocationFieldsLabels = MonarchLocationFieldsLabels()
    ) {
        self.legend = legend
        _value = value
        self.adapter = adapter
        self.fields = fields
        self.required = required
        self.errors = errors
        self.priorityCountries = priorityCountries
        self.locale = locale
        self.cityMinQuery = cityMinQuery
        self.cityDebounce = cityDebounce
        self.allowsCustomCity = allowsCustomCity
        self.labels = labels
    }

    private var countryCode: String? { MonarchGeoSearch.normalized(value.country) }

    private var chooseCountryFirst: String {
        labels.chooseCountryFirst ?? MonarchLocationFieldsStrings.chooseCountryFirst
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.fieldSpacing) {
            if let legend, !legend.isEmpty {
                Text(legend)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityAddTraits(.isHeader)
            }
            // As on the web: columns at least 14rem wide, so a narrow screen has one and a wide one has several.
            LazyVGrid(
                columns: [
                    GridItem(
                        .adaptive(minimum: Tokens.columnMinimumWidth),
                        spacing: Tokens.fieldSpacing,
                        alignment: .top
                    )
                ],
                alignment: .leading,
                spacing: Tokens.fieldSpacing
            ) {
                ForEach(fields, id: \.self) { field in
                    self.field(field)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .task(id: countriesAttempt) { await loadCountries() }
        .task(
            id: MonarchLocationFieldsCitySearchKey(country: countryCode, region: value.region, query: cityQuery)
        ) {
            await searchCities()
        }
    }

    @ViewBuilder
    private func field(_ field: MonarchLocationField) -> some View {
        switch field {
        case .country: countryField
        case .region: regionField
        case .city: cityField
        case .postalCode: postalCodeField
        }
    }

    // MARK: Country

    private var countryBinding: Binding<String?> {
        Binding(
            get: { value.country },
            set: { code in
                let next = Tokens.choosingCountry(value, code)
                if next != value { cityQuery = "" }
                value = next
            }
        )
    }

    @ViewBuilder private var countryField: some View {
        let label = labels.country ?? MonarchLocationFieldsStrings.country
        if adapter.countries == nil {
            MonarchCountrySelect(
                label,
                selection: countryBinding,
                error: errors[.country],
                priority: priorityCountries,
                locale: locale,
                isRequired: required.contains(.country),
                isClearable: false
            )
        } else {
            VStack(alignment: .leading, spacing: Tokens.retrySpacing) {
                MonarchCombobox(
                    label,
                    selection: countryBinding,
                    options: Tokens.countryOptions(countriesLoad, selection: value.country),
                    error: errors[.country],
                    isRequired: required.contains(.country),
                    isLoading: countriesLoad == .loading,
                    isClearable: false,
                    sortsByMatch: true
                )
                if countriesLoad == .failed {
                    MonarchInlineErrorRetry(
                        labels.countriesLoadFailed ?? MonarchLocationFieldsStrings.countriesLoadFailed
                    ) {
                        countriesAttempt += 1
                    }
                }
            }
        }
    }

    private func loadCountries() async {
        guard let load = adapter.countries else { return }
        countriesLoad = .loading
        do {
            let countries = try await load()
            guard !Task.isCancelled else { return }
            countriesLoad = .ready(countries)
        } catch {
            guard !Task.isCancelled else { return }
            countriesLoad = .failed
        }
    }

    // MARK: Region

    /// The regions the field chooses codes from, for telling a code from typed text.
    private var knownRegions: [MonarchRegion] {
        guard let code = countryCode else { return [] }
        if adapter.regions != nil { return regionCache.answers[code]?.regions ?? [] }
        return MonarchRegionSelect.builtInRegions(for: code) ?? []
    }

    private var regionLoader: (@Sendable (String) async throws -> [MonarchRegion])? {
        guard let load = adapter.regions else { return nil }
        let cache = regionCache
        return { country in
            let answer = try await load(country)
            await cache.store(answer, for: country)
            return answer.regions
        }
    }

    private var regionField: some View {
        let label = Tokens.regionLabel(
            country: countryCode,
            adapterLabel: countryCode.flatMap { regionCache.answers[$0]?.label },
            labels: labels
        )
        return MonarchRegionSelect(
            label,
            selection: Binding(
                get: { Tokens.regionSelection(value) },
                set: { value = Tokens.choosingRegion(value, $0, regions: knownRegions) }
            ),
            country: countryCode,
            error: errors[.region],
            isRequired: required.contains(.region),
            isClearable: false,
            labels: MonarchRegionSelectLabels(
                chooseCountryFirst: chooseCountryFirst,
                loadFailed: labels.regionsLoadFailed
            ),
            loadRegions: regionLoader
        )
    }

    // MARK: City

    @ViewBuilder private var cityField: some View {
        let label = labels.city ?? MonarchLocationFieldsStrings.city
        if adapter.cities == nil {
            MonarchTextField(
                label,
                text: Binding(
                    get: { value.city },
                    set: { text in
                        value.city = text
                        value.cityID = nil
                    }
                ),
                error: errors[.city]
            )
            .monarchAddressContentType(.city)
        } else {
            let minimum = Tokens.cityMinimumLength(value, minimum: cityMinQuery)
            let labels = labels
            MonarchCombobox(
                label,
                selection: Binding(
                    get: { Tokens.citySelection(value) },
                    set: { value = Tokens.choosingCity(value, $0, cities: citySearch.results) }
                ),
                options: Tokens.cityOptions(citySearch.results, value: value),
                prompt: value.country == nil ? chooseCountryFirst : nil,
                error: errors[.city],
                isRequired: required.contains(.city),
                isLoading: citySearch.phase == .loading,
                isClearable: false,
                searchText: $cityQuery,
                labels: MonarchComboboxLabels(
                    empty: Tokens.cityEmptyMessage(citySearch.phase, minimum: minimum, labels: labels),
                    create: { text in Tokens.useTyped(text, labels: labels) }
                ),
                filter: { _, _ in true },
                onCreate: typedCity
            )
            .disabled(value.country == nil)
        }
    }

    /// Keeps typed text as the city, when allowed.
    private var typedCity: (@MainActor (String) -> MonarchComboboxOption?)? {
        guard allowsCustomCity else { return nil }
        return { text in Tokens.typedCityOption(text) }
    }

    private func searchCities() async {
        guard let cities = adapter.cities, let country = countryCode else { return }
        let region = value.region
        await citySearch.run(
            cityQuery,
            minLength: Tokens.cityMinimumLength(value, minimum: cityMinQuery),
            debounce: cityDebounce,
            search: { query in
                try await cities(MonarchLocationCityQuery(country: country, region: region, query: query))
            }
        )
    }

    // MARK: Postal code

    private var postalCodeField: some View {
        MonarchTextField(
            Tokens.postalCodeLabel(country: countryCode, labels: labels),
            text: $value.postalCode,
            prompt: "",
            error: errors[.postalCode]
        )
        .monarchAddressContentType(.postalCode)
    }
}

#if DEBUG
    #Preview("MonarchLocationFields") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchLocationFields(
                    "Shipping address",
                    value: .constant(
                        MonarchLocationValue(
                            country: "US",
                            region: "US-CA",
                            regionName: "California",
                            city: "Oakland",
                            postalCode: "94612"
                        )
                    ),
                    required: [.country, .city]
                )
                MonarchLocationFields(value: .constant(MonarchLocationValue()), errors: [.country: "Choose a country."])
                MonarchLocationFields(value: .constant(MonarchLocationValue(country: "PT"))).disabled(true)
            }
        }
    }
#endif
