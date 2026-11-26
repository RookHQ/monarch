import SwiftUI

/// A field for choosing one value from a long list, searched in a sheet.
///
/// ```swift
/// MonarchCombobox(
///     "Country",
///     selection: $country,
///     options: countries.map { MonarchComboboxOption(id: $0.code, label: $0.name, hint: $0.code) },
///     prompt: "Choose a country",
///     sortsByMatch: true
/// )
/// ```
///
/// The field shows the chosen label. Tapping it opens a sheet with a
/// standard `List` and `.searchable` field: typing filters the options by
/// label, hint and keywords (ignoring case and accents), and choosing one
/// closes the sheet. Options can have a description, a trailing hint, an
/// icon and a group heading. With `onCreate` the typed text can be added as
/// a new option. For a server search, bind `searchText`, pass a `filter`
/// that keeps every option, and update `options` and `isLoading` from the
/// text, for example with `.task(id: searchText)`. When loading fails, pass
/// the message as `loadError` and `onRetry` to offer Try again in the list.
///
/// Accessibility: the field is a button labelled with `label`, whose value
/// is the chosen label (or "None") and whose hint is the error (read as
/// "Error: …"), the help text and "Required"; a new error is announced. The
/// clear button is a separate button named "Clear". In the sheet each
/// option is a button read with its label, description and hint, and the
/// chosen one has the selected trait and a check mark, not colour alone.
/// Rows and the field are at least 44 points high.
public struct MonarchCombobox: View {
    private let label: String
    @Binding private var selection: String?
    private let options: [MonarchComboboxOption]
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let clearOptionTitle: String?
    private let isRequired: Bool
    private let isLoading: Bool
    private let loadError: String?
    private let onRetry: (@MainActor () -> Void)?
    private let isClearable: Bool
    private let isReadOnly: Bool
    private let sortsByMatch: Bool
    private let limit: Int?
    private let searchText: Binding<String>?
    private let labels: MonarchComboboxLabels
    private let filter: (@Sendable (MonarchComboboxOption, String) -> Bool)?
    private let onCreate: (@MainActor (String) -> MonarchComboboxOption?)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var isPresented = false
    @Environment(\.monarchStartsPresented) private var startsPresented
    @State private var ownSearchText = ""
    @State private var remembered: [String: String] = [:]

    /// A combobox.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It names the field and the sheet.
    ///   - selection: The id of the chosen option, or `nil`.
    ///   - options: The options, in order.
    ///   - prompt: Shown in the field while nothing is chosen.
    ///   - helper: Help text under the field.
    ///   - error: An error message. Shown with an icon and announced.
    ///   - clearOptionTitle: Adds a first row that clears the selection, such as "Unassigned".
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows "Loading…" while there are no options yet, and a progress indicator in the list.
    ///   - isClearable: Shows a clear button in the field while something is chosen.
    ///   - isReadOnly: Shows the chosen label in the field box without a chevron or a clear button, and does not open the list. The text can be selected.
    ///   - sortsByMatch: Lists labels that start with the search first, then labels with a word that does.
    ///   - limit: Shows at most this many matches, with a note to keep typing.
    ///   - searchText: The search text, for apps that search a server. Kept inside when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - filter: Decides whether an option matches the search. By default the label, hint and keywords are searched.
    ///   - onCreate: Offers "Create “text”" when no label equals the typed text. Return the new option to choose it.
    ///   - loadError: Shown in the list in place of the options when they failed to load. Empty uses the built-in message.
    ///   - onRetry: Offers Try again beside `loadError`, to load the options again.
    public init(
        _ label: String,
        selection: Binding<String?>,
        options: [MonarchComboboxOption],
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        clearOptionTitle: String? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        isClearable: Bool = true,
        isReadOnly: Bool = false,
        sortsByMatch: Bool = false,
        limit: Int? = nil,
        searchText: Binding<String>? = nil,
        labels: MonarchComboboxLabels = MonarchComboboxLabels(),
        filter: (@Sendable (MonarchComboboxOption, String) -> Bool)? = nil,
        onCreate: (@MainActor (String) -> MonarchComboboxOption?)? = nil,
        loadError: String? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        _selection = selection
        self.options = options
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.clearOptionTitle = clearOptionTitle
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.isClearable = isClearable
        self.isReadOnly = isReadOnly
        self.sortsByMatch = sortsByMatch
        self.limit = limit
        self.searchText = searchText
        self.labels = labels
        self.filter = filter
        self.onCreate = onCreate
        self.loadError = loadError
        self.onRetry = onRetry
    }

    private var activeError: String? { MonarchFormFieldTokens.activeError(error) }

    private var selectedLabel: String? {
        MonarchComboboxTokens.selectedLabel(selection, options: options, remembered: remembered)
    }

    private var query: Binding<String> { searchText ?? $ownSearchText }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchComboboxTokens.stackSpacing) {
            labelRow
            field
            supportingText
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchAnnouncesError(activeError, labels: labels.field)
        .onAppear { if startsPresented { isPresented = true } }
        .onChange(of: isPresented) { _, presented in
            if !presented { query.wrappedValue = "" }
        }
        .sheet(isPresented: $isPresented) {
            MonarchComboboxList(
                title: label,
                selection: selection,
                options: options,
                query: query,
                clearOptionTitle: clearOptionTitle,
                isLoading: isLoading,
                loadError: loadError,
                onRetry: onRetry,
                sortsByMatch: sortsByMatch,
                limit: limit,
                labels: labels,
                filter: filter,
                canCreate: onCreate != nil,
                onChoose: choose,
                onCreate: create,
                onCancel: { isPresented = false }
            )
        }
    }

    private var labelRow: some View {
        MonarchFieldLabelRow(label: label, isRequired: isRequired)
    }

    @ViewBuilder private var field: some View {
        if isReadOnly { readOnlyField } else { editableField }
    }

    private var readOnlyField: some View {
        Text(selectedLabel ?? prompt ?? "")
            .monarchFieldText()
            .foregroundStyle(
                theme.colors[role: MonarchTextFieldTokens.readOnlyColor(isEmpty: selectedLabel == nil)]
            )
            .textSelection(.enabled)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .monarchFieldBox(
                hasError: activeError != nil,
                isFocused: false,
                isReadOnly: true,
                minHeight: MonarchComboboxTokens.minHeight
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityValue(MonarchComboboxTokens.accessibilityValue(selectedLabel: selectedLabel, labels: labels))
            .accessibilityHint(
                MonarchFieldChrome.accessibilityHint(
                    helper: helper,
                    error: activeError,
                    isRequired: isRequired,
                    isReadOnly: true,
                    labels: labels.field
                )
            )
    }

    private var editableField: some View {
        let shape = MonarchShapes.control
        let border = MonarchTextFieldTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        let background = MonarchTextFieldTokens.background(isEnabled: isEnabled)
        return HStack(spacing: MonarchComboboxTokens.spacing) {
            Button {
                isPresented = true
            } label: {
                HStack(spacing: MonarchComboboxTokens.spacing) {
                    Text(selectedLabel ?? prompt ?? "")
                        .monarchTextStyle(.body)
                        .foregroundStyle(
                            theme.colors[
                                role: MonarchComboboxTokens.valueColor(
                                    hasSelection: selectedLabel != nil,
                                    isEnabled: isEnabled
                                )
                            ]
                        )
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonarchIcon(systemName: MonarchComboboxTokens.chevronSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                }
                .frame(minHeight: MonarchComboboxTokens.minHeight)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityValue(MonarchComboboxTokens.accessibilityValue(selectedLabel: selectedLabel, labels: labels))
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: helper,
                    error: activeError,
                    isRequired: isRequired,
                    labels: labels.field
                )
            )
            if isClearable, selection != nil {
                Button {
                    selection = nil
                } label: {
                    MonarchIcon(
                        systemName: MonarchComboboxTokens.clearSymbol,
                        label: labels.clear ?? MonarchComboboxStrings.clear,
                        size: .sm
                    )
                    .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, MonarchComboboxTokens.horizontalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchComboboxTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth))
    }

    private var supportingText: some View {
        MonarchFieldSupportingText(helper: helper, error: activeError)
    }

    private func choose(_ id: String?) {
        if let id, let option = options.first(where: { $0.id == id }) {
            remembered[id] = option.label
        }
        selection = id
        isPresented = false
    }

    private func create(_ text: String) {
        guard let onCreate else { return }
        if let option = onCreate(text.trimmingCharacters(in: .whitespacesAndNewlines)) {
            remembered[option.id] = option.label
            selection = option.id
        }
        isPresented = false
    }
}

#if DEBUG
    #Preview("MonarchCombobox") {
        let options = [
            MonarchComboboxOption(id: "fr", label: "France", hint: "FR", group: "Europe"),
            MonarchComboboxOption(id: "in", label: "India", hint: "IN", group: "Asia"),
            MonarchComboboxOption(id: "jp", label: "Japan", hint: "JP", group: "Asia"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCombobox("Country", selection: .constant("in"), options: options, helper: "Where you pay tax.")
                MonarchCombobox(
                    "Country",
                    selection: .constant(nil),
                    options: options,
                    prompt: "Choose a country",
                    error: "Choose a country.",
                    isRequired: true
                )
                MonarchCombobox("Disabled", selection: .constant("fr"), options: options).disabled(true)
            }
        }
    }
#endif
