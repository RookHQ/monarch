import SwiftUI

/// A field for choosing several values from a long list, searched in a
/// sheet. The chosen values show as removable tags.
///
/// ```swift
/// MonarchMultiSelect(
///     "Labels",
///     selection: $labels,
///     options: allLabels.map { MonarchComboboxOption(id: $0.id, label: $0.name) },
///     prompt: "Choose labels",
///     maxSelections: 3
/// )
/// ```
///
/// The chosen values are tags in the order they were picked; each tag has a
/// remove button. The button under them opens a sheet with a standard
/// `List` and `.searchable` field. Each row adds or removes its value and
/// the sheet stays open until Done. With `maxSelections`, the other rows are
/// disabled once the limit is reached and "2 of 3 selected" shows under the
/// field. For a server search, bind `searchText`, pass a `filter` that keeps
/// every option, and update `options` and `isLoading` from the text.
///
/// Accessibility: the button is labelled with `label`; its value is the
/// chosen labels (or "None") and its hint is the error, the help text,
/// "Required" and the count. Each tag is read with its text and has a
/// separate "Remove" button. In the sheet each row is a button read with its
/// label, description and hint; chosen rows have the selected trait and a
/// check mark, not colour alone. Rows and buttons are at least 44 points high.
public struct MonarchMultiSelect: View {
    private let label: String
    @Binding private var selection: [String]
    private let options: [MonarchComboboxOption]
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let maxSelections: Int?
    private let isRequired: Bool
    private let isLoading: Bool
    private let sortsByMatch: Bool
    private let limit: Int?
    private let searchText: Binding<String>?
    private let labels: MonarchMultiSelectLabels
    private let filter: (@Sendable (MonarchComboboxOption, String) -> Bool)?
    private let onCreate: (@MainActor (String) -> MonarchComboboxOption?)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var isPresented = false
    @State private var ownSearchText = ""
    @State private var remembered: [String: String] = [:]

    /// A multi-select field.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It names the field and the sheet.
    ///   - selection: The ids of the chosen options, in the order they were picked.
    ///   - options: The options, in order.
    ///   - prompt: The button's text while nothing is chosen. "Select…" by default.
    ///   - helper: Help text under the field.
    ///   - error: An error message. Shown with an icon and announced.
    ///   - maxSelections: The most values that can be chosen. Shows a count under the field.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows a progress row with "Loading…" in the list.
    ///   - sortsByMatch: Lists labels that start with the search first, then labels with a word that does.
    ///   - limit: Shows at most this many matches, with a note to keep typing.
    ///   - searchText: The search text, for apps that search a server. Kept inside when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - filter: Decides whether an option matches the search. By default the label, hint and keywords are searched.
    ///   - onCreate: Offers "Create “text”" when no label equals the typed text. Return the new option to choose it.
    public init(
        _ label: String,
        selection: Binding<[String]>,
        options: [MonarchComboboxOption],
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        maxSelections: Int? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        sortsByMatch: Bool = false,
        limit: Int? = nil,
        searchText: Binding<String>? = nil,
        labels: MonarchMultiSelectLabels = MonarchMultiSelectLabels(),
        filter: (@Sendable (MonarchComboboxOption, String) -> Bool)? = nil,
        onCreate: (@MainActor (String) -> MonarchComboboxOption?)? = nil
    ) {
        self.label = label
        _selection = selection
        self.options = options
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.maxSelections = maxSelections
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.sortsByMatch = sortsByMatch
        self.limit = limit
        self.searchText = searchText
        self.labels = labels
        self.filter = filter
        self.onCreate = onCreate
    }

    private var activeError: String? { MonarchFormFieldTokens.activeError(error) }
    private var query: Binding<String> { searchText ?? $ownSearchText }
    private var picks: [MonarchMultiSelectTokens.Pick] {
        MonarchMultiSelectTokens.picks(selection, options: options, remembered: remembered)
    }
    private var countText: String? {
        MonarchMultiSelectTokens.countText(count: selection.count, maxSelections: maxSelections, labels: labels)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchComboboxTokens.stackSpacing) {
            labelRow
            field
            supportingText
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchAnnouncesError(activeError, labels: labels.list.field)
        .onChange(of: isPresented) { _, presented in
            if !presented { query.wrappedValue = "" }
        }
        .onChange(of: selection) { _, ids in
            for id in ids {
                if let option = options.first(where: { $0.id == id }) { remembered[id] = option.label }
            }
        }
        .sheet(isPresented: $isPresented) {
            MonarchMultiSelectList(
                title: label,
                selection: $selection,
                options: options,
                query: query,
                maxSelections: maxSelections,
                isLoading: isLoading,
                sortsByMatch: sortsByMatch,
                limit: limit,
                labels: labels,
                filter: filter,
                canCreate: onCreate != nil,
                onCreate: create,
                onDone: { isPresented = false }
            )
            .environment(\.monarchTheme, theme)
        }
    }

    private var labelRow: some View {
        MonarchFieldLabelRow(label: label, isRequired: isRequired)
    }

    private var field: some View {
        let shape = MonarchShapes.control
        let border = MonarchTextFieldTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        let background = MonarchTextFieldTokens.background(isEnabled: isEnabled)
        let picks = picks
        return HStack(alignment: .center, spacing: MonarchComboboxTokens.spacing) {
            if picks.isEmpty {
                Text(MonarchMultiSelectTokens.buttonTitle(hasSelection: false, prompt: prompt, labels: labels))
                    .monarchTextStyle(.body)
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchMultiSelectTokens.buttonColor(hasSelection: false, isEnabled: isEnabled)
                        ]
                    )
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityHidden(true)
            } else {
                MonarchFlowLayout(spacing: MonarchMultiSelectTokens.tagSpacing) {
                    ForEach(picks) { pick in
                        MonarchTag(pick.label, size: MonarchMultiSelectTokens.tagSize) {
                            selection.removeAll { $0 == pick.id }
                        }
                    }
                }
                .padding(.vertical, MonarchMultiSelectTokens.verticalPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button {
                isPresented = true
            } label: {
                MonarchIcon(systemName: MonarchMultiSelectTokens.chevronSymbol, size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                    .frame(minHeight: MonarchComboboxTokens.minHeight)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(picks.isEmpty ? label : labels.change ?? MonarchMultiSelectStrings.change)
            .accessibilityValue(MonarchMultiSelectTokens.accessibilityValue(picks, labels: labels))
            .accessibilityHint(
                MonarchMultiSelectTokens.accessibilityHint(
                    helper: helper,
                    error: activeError,
                    isRequired: isRequired,
                    count: countText,
                    labels: labels
                )
            )
        }
        .contentShape(Rectangle())
        .onTapGesture { isPresented = true }
        .padding(.horizontal, MonarchComboboxTokens.horizontalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchComboboxTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth))
    }

    @ViewBuilder private var supportingText: some View {
        MonarchFieldSupportingText(helper: helper, error: activeError)
        if let countText {
            MonarchFieldHelperText(text: countText)
        }
    }

    private func create(_ text: String) {
        guard let onCreate,
            let option = onCreate(text.trimmingCharacters(in: .whitespacesAndNewlines))
        else { return }
        remembered[option.id] = option.label
        selection = MonarchMultiSelectTokens.toggled(
            selection.filter { $0 != option.id },
            id: option.id,
            maxSelections: maxSelections
        )
        query.wrappedValue = ""
    }
}

#if DEBUG
    #Preview("MonarchMultiSelect") {
        let options = [
            MonarchComboboxOption(id: "bug", label: "Bug"),
            MonarchComboboxOption(id: "docs", label: "Documentation"),
            MonarchComboboxOption(id: "ui", label: "Interface"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMultiSelect("Labels", selection: .constant(["bug", "ui"]), options: options, maxSelections: 2)
                MonarchMultiSelect(
                    "Labels",
                    selection: .constant([]),
                    options: options,
                    prompt: "Choose labels",
                    error: "Choose at least one label.",
                    isRequired: true
                )
                MonarchMultiSelect("Disabled", selection: .constant(["docs"]), options: options).disabled(true)
            }
        }
    }
#endif
