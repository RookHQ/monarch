import SwiftUI

/// A phone number as a country calling code plus the national number, with
/// one E.164 value out (`+442079460958`).
///
/// ```swift
/// @State private var phone: String?
///
/// MonarchPhoneInput("Mobile number", value: $phone, preferred: ["GB", "PT"])
/// ```
///
/// The country is a button showing the flag and calling code; it opens the
/// searchable country list of ``MonarchPhoneCountrySelect`` in a sheet. The
/// number is a standard `TextField` with the phone pad keyboard and the
/// telephone number content type, so AutoFill can offer the person's own
/// number. Typing or pasting a full international number ("+44 20 7946
/// 0958" or "0044…") picks the country and keeps the national digits.
///
/// The value is written when the field loses focus, when Return is pressed
/// and when the country changes, never on every keystroke. Separators and
/// one trunk 0 are removed (Italy, San Marino and Vatican City keep it).
/// A number outside 8 to 15 digits is kept in the field, flagged and
/// explained, and the value is not changed. Clearing the field writes
/// `nil`. Read the parts of a value with ``parse(_:preferred:)``.
///
/// Accessibility: the label is hidden and given to the number field, whose
/// hint is the error (read as "Error: …"), the helper text and "Required".
/// The country button is named "Country code" and its value is the
/// country with its calling code ("Portugal (+351)"). A new error is
/// announced and shown with an icon and text. The number is laid out left
/// to right in every language, as phone numbers are written. At the
/// accessibility text sizes the button sits above the field. Both are at
/// least 44 points high.
public struct MonarchPhoneInput: View {
    private let label: String
    @Binding private var value: String?
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let countries: [String]?
    private let preferred: [String]
    private let locale: Locale?
    private let showsFlags: Bool
    private let isRequired: Bool
    private let labels: MonarchPhoneInputLabels
    private let onCountryChange: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var environmentLocale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var country: String
    @State private var draft: String
    @State private var isInvalid = false
    @State private var isPickingCountry = false
    @State private var countrySearch = ""
    @FocusState private var isFocused: Bool

    private typealias Tokens = MonarchPhoneInputTokens

    /// A phone number field.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It names the number field.
    ///   - value: The number in E.164 (`+442079460958`), or `nil` when empty. Any international form is read.
    ///   - prompt: Placeholder text shown while the number is empty.
    ///   - helper: Help text under the field.
    ///   - error: An error message from the app. Shown with an icon and announced.
    ///   - defaultCountry: The ISO code of the country when the value does not give one. "US" by default.
    ///   - countries: Limits the country list to these ISO codes. Every country by default.
    ///   - preferred: ISO codes listed first in the country list, such as the person's own country.
    ///   - locale: The language of the country names. The environment's locale by default.
    ///   - showsFlags: Shows the country's flag on the button and in the list.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCountryChange: Receives the ISO code when the country changes, from the list or a pasted number.
    public init(
        _ label: String,
        value: Binding<String?>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        defaultCountry: String = "US",
        countries: [String]? = nil,
        preferred: [String] = [],
        locale: Locale? = nil,
        showsFlags: Bool = true,
        isRequired: Bool = false,
        labels: MonarchPhoneInputLabels = MonarchPhoneInputLabels(),
        onCountryChange: (@MainActor (String) -> Void)? = nil
    ) {
        self.label = label
        _value = value
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.countries = countries
        self.preferred = preferred
        self.locale = locale
        self.showsFlags = showsFlags
        self.isRequired = isRequired
        self.labels = labels
        self.onCountryChange = onCountryChange
        let parts = MonarchPhoneInputTokens.split(value.wrappedValue, country: defaultCountry.uppercased())
        _country = State(initialValue: parts.country)
        _draft = State(initialValue: parts.national)
    }

    /// Joins a calling code and a national number into E.164:
    /// `toE164(dialCode: "+44", national: "020 7946 0958")` is
    /// `"+442079460958"`. Separators are removed and one leading trunk 0 is
    /// dropped, except for Italy, San Marino and Vatican City when `country`
    /// says so.
    ///
    /// - Parameters:
    ///   - dialCode: The calling code, such as "+44".
    ///   - national: The national number in any form.
    ///   - country: The ISO code, which decides whether the leading 0 is kept.
    /// - Returns: The E.164 number, or `nil` when there are no digits.
    nonisolated public static func toE164(dialCode: String, national: String, country: String? = nil) -> String? {
        Tokens.toE164(dialCode: dialCode, national: national, country: country)
    }

    /// Whether a text is a plausible E.164 number: "+", then 8 to 15 ASCII
    /// digits, not starting with 0.
    ///
    /// - Parameter value: The text to check.
    /// - Returns: `true` for a plausible number.
    nonisolated public static func isValidE164(_ value: String) -> Bool {
        Tokens.isValidE164(value)
    }

    /// Splits an international number ("+44 20 7946 0958", "0044…" or
    /// E.164) into its country, calling code and national digits.
    ///
    /// - Parameters:
    ///   - phone: The number as typed or stored.
    ///   - preferred: The ISO code to prefer for a shared calling code (+1, +7, +44).
    /// - Returns: The parts, or `nil` when no calling code matches.
    nonisolated public static func parse(_ phone: String, preferred: String? = nil) -> MonarchPhoneInputDetails? {
        Tokens.parse(phone, preferred: preferred)
    }

    private var activeError: String? { Tokens.activeError(isInvalid: isInvalid, error: error, labels: labels) }

    private var countryTitle: String { labels.country ?? MonarchPhoneInputStrings.country }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            labelRow
            let layout =
                dynamicTypeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.rowSpacing))
                : AnyLayout(HStackLayout(alignment: .center, spacing: Tokens.rowSpacing))
            layout {
                countryButton
                numberField
            }
            supportingText
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: value) { _, newValue in show(newValue) }
        .onChange(of: isFocused) { _, focused in
            if !focused { commit(draft, for: country) }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(
                    MonarchFormFieldTokens.spokenError(newError, labels: labels.field)
                )
                .post()
            }
        }
        .onChange(of: isPickingCountry) { _, presented in
            if !presented { countrySearch = "" }
        }
        .sheet(isPresented: $isPickingCountry) { countryList }
    }

    private var labelRow: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.labelColor(isEnabled: isEnabled)])
            if isRequired {
                Text(MonarchFormFieldTokens.requiredMark)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.requiredColor(isEnabled: isEnabled)])
            }
        }
        .accessibilityHidden(true)
    }

    private var countryButton: some View {
        let shape = MonarchShapes.control
        return Button {
            isPickingCountry = true
        } label: {
            HStack(spacing: Tokens.buttonSpacing) {
                // As on the web, the button reads the country's name and calling code, such as "Portugal (+351)".
                Text(
                    Tokens.countryValue(
                        country: country,
                        locale: locale ?? environmentLocale,
                        option: labels.countrySelect.option
                    )
                )
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
            }
            .padding(.horizontal, Tokens.horizontalPadding)
            .frame(minHeight: Tokens.minHeight)
            .background(shape.fill(theme.colors[role: Tokens.background(isEnabled: isEnabled)]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: Tokens.border(hasError: activeError != nil, isEnabled: isEnabled)],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .contentShape(shape)
        }
        .buttonStyle(.plain)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityLabel(countryTitle)
        .accessibilityValue(
            Tokens.countryValue(
                country: country,
                locale: locale ?? environmentLocale,
                option: labels.countrySelect.option
            )
        )
    }

    private var numberField: some View {
        let shape = MonarchShapes.control
        return TextField(label, text: $draft, prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) })
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
            .autocorrectionDisabled()
            #if os(iOS)
                .keyboardType(.phonePad)
                .textContentType(.telephoneNumber)
            #endif
            .focused($isFocused)
            .layoutPriority(1)
            .onSubmit { commit(draft, for: country) }
            .onChange(of: draft) { _, text in typed(text) }
            .accessibilityLabel(label)
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: helper,
                    error: activeError,
                    isRequired: isRequired,
                    labels: labels.field
                )
            )
            .padding(.horizontal, Tokens.horizontalPadding)
            .padding(.vertical, Tokens.verticalPadding)
            .frame(maxWidth: .infinity, minHeight: Tokens.minHeight, alignment: .leading)
            .background(shape.fill(theme.colors[role: Tokens.background(isEnabled: isEnabled)]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: Tokens.border(hasError: activeError != nil, isEnabled: isEnabled)],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .monarchFocusRing(isFocused, in: shape)
            .contentShape(Rectangle())
            .onTapGesture { isFocused = true }
            // Phone numbers are written left to right in every language.
            .environment(\.layoutDirection, .leftToRight)
    }

    @ViewBuilder private var supportingText: some View {
        if let activeError {
            HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.messageIconSpacing) {
                MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
                Text(activeError).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.errorColor])
            .accessibilityHidden(true)
        } else if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.helperColor(isEnabled: isEnabled)])
                .accessibilityHidden(true)
        }
    }

    private var countryList: some View {
        let selectLabels = labels.countrySelect
        var comboboxLabels = selectLabels.combobox
        comboboxLabels.empty = comboboxLabels.empty ?? MonarchPhoneCountrySelectStrings.empty
        let option = selectLabels.option
        return MonarchComboboxList(
            title: countryTitle,
            selection: country,
            options: MonarchPhoneCountrySelectTokens.options(
                countries: countries,
                preferred: preferred,
                locale: locale ?? environmentLocale,
                showsFlags: showsFlags,
                preferredTitle: selectLabels.suggested ?? MonarchPhoneCountrySelectStrings.suggested,
                allTitle: selectLabels.all ?? MonarchPhoneCountrySelectStrings.all,
                optionText: { name, dialCode in
                    option?(name, dialCode) ?? MonarchPhoneCountrySelectStrings.option(name: name, dialCode: dialCode)
                }
            ),
            query: $countrySearch,
            clearOptionTitle: nil,
            isLoading: false,
            sortsByMatch: true,
            limit: nil,
            labels: comboboxLabels,
            filter: { option, query in MonarchGeoSearch.matchesEveryWord(option, query: query) },
            canCreate: false,
            onChoose: { code in
                isPickingCountry = false
                guard let code else { return }
                choose(code)
            },
            onCreate: { _ in },
            onCancel: { isPickingCountry = false }
        )
    }

    // MARK: Actions

    private func choose(_ code: String) {
        let code = code.uppercased()
        changeCountry(code)
        if !draft.trimmingCharacters(in: .whitespaces).isEmpty { commit(draft, for: code) }
    }

    private func changeCountry(_ code: String) {
        guard code != country else { return }
        country = code
        onCountryChange?(code)
    }

    private func typed(_ text: String) {
        if let pasted = Tokens.pasted(text, country: country) {
            changeCountry(pasted.country)
            if draft != pasted.national { draft = pasted.national }
            isInvalid = false
            return
        }
        if isInvalid { isInvalid = false }
    }

    private func commit(_ text: String, for code: String) {
        let result = Tokens.commit(text, country: code)
        changeCountry(result.country)
        if draft != result.draft { draft = result.draft }
        isInvalid = result.isInvalid
        guard !result.isInvalid, result.value != value else { return }
        value = result.value
    }

    /// Shows a value that changed, from the app or from a commit.
    private func show(_ newValue: String?) {
        let parts = Tokens.split(newValue, country: country)
        changeCountry(parts.country)
        if draft != parts.national { draft = parts.national }
        isInvalid = false
    }
}

#if DEBUG
    #Preview("MonarchPhoneInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPhoneInput("Mobile number", value: .constant("+442079460958"), helper: "For delivery updates.")
                MonarchPhoneInput(
                    "Phone",
                    value: .constant(nil),
                    prompt: "Number",
                    error: "Enter a phone number.",
                    defaultCountry: "PT",
                    isRequired: true
                )
                MonarchPhoneInput("Disabled", value: .constant("+12025550100"), showsFlags: false).disabled(true)
            }
        }
    }
#endif
