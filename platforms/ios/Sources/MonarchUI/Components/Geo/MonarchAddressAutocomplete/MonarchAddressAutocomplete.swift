import SwiftUI

/// An address field with suggestions from the app's own provider: people
/// search, pick a suggestion, and the street, city, region, postal code and
/// country fill in for review. Manual entry is always one step away.
///
/// ```swift
/// MonarchAddressAutocomplete(
///     address: $shipping,
///     search: { query in try await geocoder.suggestions(for: query) },
///     resolve: { suggestion in try await geocoder.address(for: suggestion.id) }
/// )
/// ```
///
/// Monarch makes no requests and ships no map SDK: `search` and `resolve`
/// are closures. `MKLocalSearchCompleter` with `MKLocalSearch` is one
/// provider an app can put behind them. The search field opens a sheet
/// with the system search field; the text is sent after `debounce` once it
/// has `minQueryLength` characters, a newer text cancels the older search,
/// and a failed search shows Try again. After a pick the form shows the
/// address for review. "Enter the address manually" opens the form
/// directly, with the typed text as the street line; "Search for an
/// address" goes back. The street fields use the system address text
/// content types, so AutoFill can offer the person's own addresses.
///
/// Accessibility: the search field is a button labelled "Address" whose
/// value is the address on one line (or "None"). The number of suggestions
/// is announced when they arrive, and "Address filled in. Check the
/// details." after a pick. Each suggestion is a button read with its label
/// and description. The form fields are standard text fields and a country
/// combobox, each with its own label.
@_spi(Experimental) public struct MonarchAddressAutocomplete: View {
    private let label: String?
    @Binding private var address: MonarchAddressAutocompleteAddress
    private let modeBinding: Binding<MonarchAddressAutocompleteMode>?
    private let helper: String?
    private let error: String?
    private let fieldErrors: [MonarchAddressAutocompleteField: String]
    private let isRequired: Bool
    private let allowsManualEntry: Bool
    private let minQueryLength: Int
    private let debounce: Duration
    private let labels: MonarchAddressAutocompleteLabels
    private let search: @Sendable (String) async throws -> [MonarchAddressAutocompleteSuggestion]
    private let resolve:
        (@Sendable (MonarchAddressAutocompleteSuggestion) async throws -> MonarchAddressAutocompleteAddress)?
    private let onSuggestionSelect:
        (@MainActor (MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @State private var ownMode: MonarchAddressAutocompleteMode
    @State private var isSearching = false
    @State private var focusesField = false
    @FocusState private var fieldFocused: Bool
    @State private var query = ""
    @State private var isResolving = false
    @State private var resolveError: String?

    /// An address field.
    ///
    /// - Parameters:
    ///   - label: The search field's label, already localised. "Address" by default.
    ///   - address: The address. A pick replaces it; the form edits it.
    ///   - mode: The search field or the form, when the app owns it. By default the form when the address has a street line.
    ///   - helper: Help text under the search field.
    ///   - initialQuery: Text to search for when the view appears. When it is not empty the search opens with it typed in.
    ///   - error: An error under the search field.
    ///   - fieldErrors: Errors of the form's fields.
    ///   - isRequired: Marks the search field and the country as required.
    ///   - allowsManualEntry: Offers "Enter the address manually" in the list and under the field.
    ///   - minQueryLength: The characters needed before `search` runs.
    ///   - debounce: How long to wait after typing before `search` runs.
    ///   - labels: Replacements for the built-in strings.
    ///   - search: Returns the suggestions for the trimmed text. Throw to show an error with Try again.
    ///   - resolve: Turns a suggestion without an address into one. Throw to show an error.
    ///   - onSuggestionSelect: Receives the picked suggestion and the address it gave.
    public init(
        _ label: String? = nil,
        address: Binding<MonarchAddressAutocompleteAddress>,
        mode: Binding<MonarchAddressAutocompleteMode>? = nil,
        helper: String? = nil,
        initialQuery: String = "",
        error: String? = nil,
        fieldErrors: [MonarchAddressAutocompleteField: String] = [:],
        isRequired: Bool = false,
        allowsManualEntry: Bool = true,
        minQueryLength: Int = 3,
        debounce: Duration = .milliseconds(300),
        labels: MonarchAddressAutocompleteLabels = MonarchAddressAutocompleteLabels(),
        search: @escaping @Sendable (String) async throws -> [MonarchAddressAutocompleteSuggestion],
        resolve: (@Sendable (MonarchAddressAutocompleteSuggestion) async throws -> MonarchAddressAutocompleteAddress)? =
            nil,
        onSuggestionSelect: (
            @MainActor (MonarchAddressAutocompleteSuggestion, MonarchAddressAutocompleteAddress) -> Void
        )? =
            nil
    ) {
        self.label = label
        _address = address
        self.modeBinding = mode
        self.helper = helper
        _query = State(initialValue: initialQuery)
        _isSearching = State(initialValue: !initialQuery.isEmpty)
        self.error = error
        self.fieldErrors = fieldErrors
        self.isRequired = isRequired
        self.allowsManualEntry = allowsManualEntry
        self.minQueryLength = minQueryLength
        self.debounce = debounce
        self.labels = labels
        self.search = search
        self.resolve = resolve
        self.onSuggestionSelect = onSuggestionSelect
        _ownMode = State(initialValue: MonarchAddressAutocompleteTokens.initialMode(address.wrappedValue))
    }

    private var mode: MonarchAddressAutocompleteMode { modeBinding?.wrappedValue ?? ownMode }
    private var title: String { label ?? MonarchAddressAutocompleteStrings.label }
    private var shownError: String? { MonarchFormFieldTokens.activeError(resolveError ?? error) }

    private func setMode(_ next: MonarchAddressAutocompleteMode) {
        ownMode = next
        modeBinding?.wrappedValue = next
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAddressAutocompleteTokens.fieldSpacing) {
            searchField
            if isSearching {
                MonarchAddressAutocompleteSuggestionPanel(
                    query: query,
                    minQueryLength: minQueryLength,
                    debounce: debounce,
                    allowsManualEntry: allowsManualEntry,
                    labels: labels,
                    search: search,
                    onPick: pick,
                    onManual: {
                        address.line1 = MonarchAddressAutocompleteTokens.manualLine1(
                            current: address.line1,
                            query: query
                        )
                        isSearching = false
                        setMode(.manual)
                    }
                )
            }
            if isResolving {
                HStack(spacing: MonarchAddressAutocompleteTokens.spacing) {
                    ProgressView()
                    Text(labels.resolving ?? MonarchAddressAutocompleteStrings.resolving).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityElement(children: .combine)
            }
            if mode == .manual {
                form
                MonarchButton(
                    labels.backToSearch ?? MonarchAddressAutocompleteStrings.backToSearch,
                    systemImage: MonarchAddressAutocompleteTokens.searchSymbol,
                    variant: .ghost,
                    size: .sm
                ) {
                    setMode(.search)
                }
            } else if allowsManualEntry {
                MonarchButton(
                    labels.enterManually ?? MonarchAddressAutocompleteStrings.enterManually,
                    variant: .ghost,
                    size: .sm
                ) {
                    setMode(.manual)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: isSearching) { _, presented in
            if !presented {
                query = ""
                focusesField = false
            }
        }
    }

    private var searchField: some View {
        let summary = MonarchAddressAutocompleteTokens.summary(address, locale: locale)
        let shape = MonarchShapes.control
        return VStack(alignment: .leading, spacing: MonarchComboboxTokens.stackSpacing) {
            MonarchFieldLabelRow(label: title, isRequired: isRequired)
            fieldBox(shape: shape, summary: summary)
            MonarchFieldSupportingText(helper: helper, error: shownError)
        }
    }

    /// The field: a tap target that shows the address while closed, the search text while open.
    @ViewBuilder
    private func fieldBox(shape: some InsettableShape, summary: String?) -> some View {
        let chrome = HStack(spacing: MonarchAddressAutocompleteTokens.spacing) {
            MonarchIcon(systemName: MonarchAddressAutocompleteTokens.searchSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
            if isSearching {
                TextField(
                    title,
                    text: $query,
                    prompt: Text(labels.placeholder ?? MonarchAddressAutocompleteStrings.placeholder)
                        .foregroundStyle(theme.colors.textSubtle)
                )
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .focused($fieldFocused)
                .task { if focusesField { fieldFocused = true } }
                if !query.isEmpty {
                    Button {
                        isSearching = false
                    } label: {
                        MonarchIcon(systemName: "xmark", size: .sm)
                            .foregroundStyle(theme.colors.textMuted)
                            .frame(
                                width: MonarchAccessibility.minimumHitTarget,
                                height: MonarchAccessibility.minimumHitTarget
                            )
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(labels.cancel ?? MonarchAddressAutocompleteStrings.cancel)
                    .padding(.trailing, -MonarchComboboxTokens.horizontalPadding / 2)
                }
            } else {
                Text(summary ?? labels.placeholder ?? MonarchAddressAutocompleteStrings.placeholder)
                    .monarchTextStyle(.body)
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchAddressAutocompleteTokens.valueColor(
                                hasAddress: summary != nil,
                                isEnabled: isEnabled
                            )
                        ]
                    )
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.horizontal, MonarchComboboxTokens.horizontalPadding)
        .padding(.vertical, MonarchComboboxTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchComboboxTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchTextFieldTokens.border(hasError: shownError != nil, isEnabled: isEnabled)],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .contentShape(Rectangle())
        if isSearching {
            chrome
        } else {
            Button {
                resolveError = nil
                focusesField = true
                isSearching = true
            } label: {
                chrome
            }
            .buttonStyle(.plain)
            .disabled(isResolving)
            .accessibilityLabel(title)
            .accessibilityValue(summary ?? labels.noAddress ?? MonarchAddressAutocompleteStrings.noAddress)
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: helper,
                    error: shownError,
                    isRequired: isRequired,
                    labels: MonarchFormFieldLabels()
                )
            )
        }
    }

    private var form: some View {
        let optional = MonarchAddressAutocompleteTokens.optionalText(labels: labels)
        return VStack(alignment: .leading, spacing: MonarchAddressAutocompleteTokens.fieldSpacing) {
            MonarchTextField(
                labels.line1 ?? MonarchAddressAutocompleteStrings.line1,
                text: $address.line1,
                error: fieldErrors[.line1]
            )
            .monarchAddressAutocompleteContentType(.line1)
            MonarchTextField(
                labels.line2 ?? MonarchAddressAutocompleteStrings.line2,
                text: $address.line2,
                helper: optional,
                error: fieldErrors[.line2]
            )
            .monarchAddressAutocompleteContentType(.line2)
            MonarchTextField(
                labels.city ?? MonarchAddressAutocompleteStrings.city,
                text: $address.city,
                error: fieldErrors[.city]
            )
            .monarchAddressAutocompleteContentType(.city)
            MonarchTextField(
                labels.region ?? MonarchAddressAutocompleteStrings.region,
                text: $address.region,
                error: fieldErrors[.region]
            )
            .monarchAddressAutocompleteContentType(.region)
            MonarchTextField(
                labels.postalCode ?? MonarchAddressAutocompleteStrings.postalCode,
                text: $address.postalCode,
                error: fieldErrors[.postalCode]
            )
            .monarchAddressAutocompleteContentType(.postalCode)
            MonarchCombobox(
                labels.country ?? MonarchAddressAutocompleteStrings.country,
                selection: $address.countryCode,
                options: MonarchAddressAutocompleteTokens.countries(locale: locale),
                error: fieldErrors[.country],
                isRequired: isRequired,
                sortsByMatch: true
            )
        }
    }

    private func pick(_ suggestion: MonarchAddressAutocompleteSuggestion) {
        isSearching = false
        resolveError = nil
        if let picked = suggestion.address {
            fill(picked, from: suggestion)
            return
        }
        guard let resolve else {
            fill(MonarchAddressAutocompleteAddress(line1: suggestion.label), from: suggestion)
            return
        }
        isResolving = true
        Task { @MainActor in
            do {
                let resolved = try await resolve(suggestion)
                isResolving = false
                fill(resolved, from: suggestion)
            } catch {
                isResolving = false
                let message = labels.resolveFailed ?? MonarchAddressAutocompleteStrings.resolveFailed
                resolveError = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func fill(_ next: MonarchAddressAutocompleteAddress, from suggestion: MonarchAddressAutocompleteSuggestion)
    {
        address = next
        setMode(.manual)
        onSuggestionSelect?(suggestion, next)
        AccessibilityNotification.Announcement(labels.filled ?? MonarchAddressAutocompleteStrings.filled).post()
    }
}

extension View {
    /// The system text content type of an address field, so AutoFill can
    /// offer the person's own addresses. iOS only; other platforms have no
    /// matching types.
    @ViewBuilder
    fileprivate func monarchAddressAutocompleteContentType(_ field: MonarchAddressAutocompleteField) -> some View {
        #if os(iOS)
            switch field {
            case .line1: textContentType(.streetAddressLine1)
            case .line2: textContentType(.streetAddressLine2)
            case .city: textContentType(.addressCity)
            case .region: textContentType(.addressState)
            case .postalCode: textContentType(.postalCode)
            case .country: textContentType(.countryName)
            }
        #else
            self
        #endif
    }
}

#if DEBUG
    #Preview("MonarchAddressAutocomplete") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAddressAutocomplete(address: .constant(MonarchAddressAutocompleteAddress()), search: { _ in [] })
                MonarchAddressAutocomplete(
                    address: .constant(
                        MonarchAddressAutocompleteAddress(
                            line1: "10 Market Street",
                            city: "Lisbon",
                            postalCode: "1100-148",
                            countryCode: "PT"
                        )
                    ),
                    search: { _ in [] }
                )
            }
        }
    }
#endif
