import Foundation
import SwiftUI

/// A searchable picker for an ISO 4217 currency.
///
/// ```swift
/// MonarchCurrencySelect(
///     "Billing currency",
///     selection: $currency,
///     priority: ["USD", "EUR", "GBP"]
/// )
/// ```
///
/// The value is an upper-case ISO 4217 code, such as "EUR". Names come from
/// Foundation (`Locale.localizedString(forCurrencyCode:)`) and symbols from
/// its currency format, in the environment's locale or in `locale`; Monarch
/// ships only the list of active codes. Each row shows the name, with the
/// code and the symbol at the end. `priority` currencies are listed first
/// under "Suggested" while nothing is typed. Search matches every word typed
/// against the name, the code, the symbol and, in other languages, the
/// English name. Built on ``MonarchCombobox``.
///
/// Accessibility: the field is the combobox's button, labelled with `label`
/// and valued with what `display` shows. Each row is read with its label,
/// the name when the label is a code, then the code and the symbol.
public struct MonarchCurrencySelect: View {
    private let label: String
    @Binding private var selection: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let currencies: [String]?
    private let priority: [String]
    private let locale: Locale?
    private let display: MonarchCurrencySelectDisplay
    private let showsSymbol: Bool
    private let isRequired: Bool
    private let isClearable: Bool
    private let labels: MonarchCurrencySelectLabels

    @Environment(\.locale) private var environmentLocale
    @State private var query = ""

    /// A currency picker.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - selection: The chosen ISO 4217 code, or `nil`. Any case is read; the picker writes upper case.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - currencies: Limits the list to these codes, such as the currencies a payment provider supports. The active currencies by default.
    ///   - priority: Codes listed first under "Suggested" while nothing is typed.
    ///   - locale: The language of the names and symbols and the sort order. The environment's locale by default.
    ///   - display: What the field shows for the chosen currency: the name, the code, or both.
    ///   - showsSymbol: Shows each currency's symbol after its code.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isClearable: Shows a clear button in the field while a currency is chosen.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        currencies: [String]? = nil,
        priority: [String] = [],
        locale: Locale? = nil,
        display: MonarchCurrencySelectDisplay = .name,
        showsSymbol: Bool = true,
        isRequired: Bool = false,
        isClearable: Bool = true,
        labels: MonarchCurrencySelectLabels = MonarchCurrencySelectLabels()
    ) {
        self.label = label
        _selection = selection
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.currencies = currencies
        self.priority = priority
        self.locale = locale
        self.display = display
        self.showsSymbol = showsSymbol
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.labels = labels
    }

    /// The active ISO 4217 codes the picker lists by default.
    public nonisolated static var currencyCodes: [String] { MonarchCurrencySelectTokens.currencyCodes }

    /// The content of the view.
    public var body: some View {
        let code = MonarchGeoSearch.normalized(selection)
        var comboboxLabels = labels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchCurrencySelectStrings.empty
        return MonarchCombobox(
            label,
            selection: Binding(get: { code }, set: { selection = $0 }),
            options: MonarchCurrencySelectTokens.options(
                currencies: currencies,
                priority: priority,
                selection: code,
                query: query,
                locale: locale ?? environmentLocale,
                display: display,
                showsSymbol: showsSymbol,
                suggested: labels.suggested ?? MonarchCurrencySelectStrings.suggested,
                all: labels.all ?? MonarchCurrencySelectStrings.all
            ),
            prompt: prompt,
            helper: helper,
            error: error,
            isRequired: isRequired,
            isClearable: isClearable,
            sortsByMatch: true,
            searchText: $query,
            labels: comboboxLabels,
            filter: { option, query in MonarchGeoSearch.matchesEveryWord(option, query: query) }
        )
    }
}

#if DEBUG
    #Preview("MonarchCurrencySelect") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCurrencySelect("Currency", selection: .constant("EUR"), helper: "Used on invoices.")
                MonarchCurrencySelect(
                    "Payout currency",
                    selection: .constant(nil),
                    prompt: "Choose a currency",
                    error: "Choose a currency.",
                    priority: ["USD", "EUR"],
                    isRequired: true
                )
                MonarchCurrencySelect("Code", selection: .constant("JPY"), display: .codeAndName).disabled(true)
            }
        }
    }
#endif
