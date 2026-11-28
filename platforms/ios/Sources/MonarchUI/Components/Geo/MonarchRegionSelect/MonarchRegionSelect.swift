import Foundation
import SwiftUI

/// A picker for a state, province or territory of a country.
///
/// ```swift
/// MonarchRegionSelect(
///     MonarchRegionSelect.fieldLabel(for: country) ?? "Region",
///     selection: $region,
///     country: country
/// )
/// ```
///
/// Regions come from `regions`, from an async `loadRegions(country)`, or
/// from the compact built-in lists (US, CA, AU, GB and IN, with ISO 3166-2
/// codes and English names). A country with no list gets a text field
/// instead, so any address can be entered. With no country the field is
/// disabled and asks for one. Changing the country starts a fresh list.
/// Lists are searched with ``MonarchCombobox``.
///
/// The loader runs once per country and its result is kept while the view
/// is shown; it is cancelled when the view goes away. If it throws, an
/// inline error with "Try again" is shown under the field.
///
/// Accessibility: the field is the combobox's button, or a text field for a
/// country without a list, labelled with `label`. A failed load is an
/// inline error with a retry button, announced by ``MonarchInlineErrorRetry``.
public struct MonarchRegionSelect: View {
    private let label: String
    @Binding private var selection: String?
    private let country: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let regions: [MonarchRegion]?
    private let isRequired: Bool
    private let isClearable: Bool
    private let allowsTextEntry: Bool
    private let showsCode: Bool
    private let labels: MonarchRegionSelectLabels
    private let loadRegions: (@Sendable (String) async throws -> [MonarchRegion])?

    @State private var loaded: [String: [MonarchRegion]] = [:]
    @State private var failed: Set<String> = []
    @State private var attempt = 0

    /// A region picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. ``fieldLabel(for:)`` gives the country's own word for it.
    ///   - selection: The chosen region's code, or the typed text for a country without a list; `nil` when empty.
    ///   - country: The ISO 3166-1 alpha-2 code of the country whose regions are listed. `nil` disables the field.
    ///   - prompt: Shown in the field while nothing is chosen or typed.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - regions: The country's regions from the app's own data. They win over the loader and the built-in lists.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint of the list field.
    ///   - isClearable: Shows a clear button in the field while a region is chosen.
    ///   - allowsTextEntry: Shows a text field when the country has no list, instead of an empty list.
    ///   - showsCode: Shows each region's code, without the country, at the end of its row.
    ///   - labels: Replacements for the built-in strings.
    ///   - loadRegions: Loads a country's regions from the app's service. Return an empty list for a text field. It wins over the built-in lists, so call ``builtInRegions(for:)`` inside it to keep them.
    public init(
        _ label: String,
        selection: Binding<String?>,
        country: String?,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        regions: [MonarchRegion]? = nil,
        isRequired: Bool = false,
        isClearable: Bool = true,
        allowsTextEntry: Bool = true,
        showsCode: Bool = false,
        labels: MonarchRegionSelectLabels = MonarchRegionSelectLabels(),
        loadRegions: (@Sendable (String) async throws -> [MonarchRegion])? = nil
    ) {
        self.label = label
        _selection = selection
        self.country = country
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.regions = regions
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.allowsTextEntry = allowsTextEntry
        self.showsCode = showsCode
        self.labels = labels
        self.loadRegions = loadRegions
    }

    /// The built-in regions of a country (US, CA, AU, GB or IN), or `nil` when Monarch has no list for it.
    public nonisolated static func builtInRegions(for country: String) -> [MonarchRegion]? {
        MonarchRegionSelectTokens.builtIn[country.uppercased()]
    }

    /// The country's word for its regions, such as "State" for US or
    /// "Province or territory" for CA, from the built-in strings; `nil` when
    /// Monarch has no list for the country.
    public nonisolated static func fieldLabel(for country: String?) -> String? {
        MonarchGeoSearch.normalized(country).flatMap(MonarchRegionSelectStrings.fieldLabel)
    }

    private var code: String? { MonarchGeoSearch.normalized(country) }

    private var comboboxLabels: MonarchComboboxLabels {
        var comboboxLabels = labels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchRegionSelectStrings.empty
        return comboboxLabels
    }

    /// The content of the view.
    public var body: some View {
        let source = MonarchRegionSelectTokens.source(
            country: code,
            regions: regions,
            hasLoader: loadRegions != nil,
            loaded: code.flatMap { loaded[$0] },
            hasFailed: code.map(failed.contains) ?? false,
            allowsTextEntry: allowsTextEntry
        )
        VStack(alignment: .leading, spacing: MonarchRegionSelectTokens.retrySpacing) {
            switch source {
            case .noCountry:
                MonarchCombobox(
                    label,
                    selection: .constant(nil),
                    options: [],
                    prompt: labels.chooseCountryFirst ?? MonarchRegionSelectStrings.chooseCountryFirst,
                    helper: helper,
                    isRequired: isRequired,
                    isClearable: false,
                    labels: comboboxLabels
                )
                .disabled(true)
            case .textEntry:
                MonarchTextField(
                    label,
                    text: Binding(
                        get: { selection ?? "" },
                        set: { selection = MonarchRegionSelectTokens.typedValue($0) }
                    ),
                    prompt: prompt ?? labels.textEntry ?? MonarchRegionSelectStrings.textEntry,
                    helper: helper,
                    error: error
                )
                .monarchRegionSelectContentType()
            case .loading:
                combobox(options: [], isLoading: true)
            case .failed:
                combobox(options: [], isLoading: false)
                MonarchInlineErrorRetry(labels.loadFailed ?? MonarchRegionSelectStrings.loadFailed) {
                    attempt += 1
                }
            case .list(let regions):
                combobox(options: MonarchRegionSelectTokens.options(regions, showsCode: showsCode), isLoading: false)
            }
        }
        .id(code)
        .task(id: MonarchRegionSelectLoad(country: code, attempt: attempt)) {
            await load(code)
        }
    }

    private func combobox(options: [MonarchComboboxOption], isLoading: Bool) -> some View {
        MonarchCombobox(
            label,
            selection: $selection,
            options: options,
            prompt: prompt ?? MonarchRegionSelectStrings.choose,
            helper: helper,
            error: error,
            isRequired: isRequired,
            isLoading: isLoading,
            isClearable: isClearable,
            sortsByMatch: true,
            labels: comboboxLabels,
            filter: { option, query in MonarchGeoSearch.matchesEveryWord(option, query: query) }
        )
    }

    private func load(_ country: String?) async {
        guard let loadRegions, regions == nil, let country, loaded[country] == nil else { return }
        failed.remove(country)
        do {
            let list = try await loadRegions(country)
            loaded[country] = list
        } catch is CancellationError {
            return
        } catch {
            failed.insert(country)
        }
    }
}

/// What starts a load: the country and the number of retries.
private struct MonarchRegionSelectLoad: Hashable {
    let country: String?
    let attempt: Int
}

extension View {
    /// Tells AutoFill the field holds the state or region of an address. iOS only.
    @ViewBuilder
    fileprivate func monarchRegionSelectContentType() -> some View {
        #if os(iOS)
            textContentType(.addressState)
        #else
            self
        #endif
    }
}

#if DEBUG
    #Preview("MonarchRegionSelect") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRegionSelect("State", selection: .constant("US-CA"), country: "US", showsCode: true)
                MonarchRegionSelect("Region", selection: .constant(nil), country: nil)
                MonarchRegionSelect("Region", selection: .constant("Lisboa"), country: "PT")
                MonarchRegionSelect(
                    "Province",
                    selection: .constant(nil),
                    country: "CA",
                    error: "Choose a province.",
                    isRequired: true
                )
            }
        }
    }
#endif
