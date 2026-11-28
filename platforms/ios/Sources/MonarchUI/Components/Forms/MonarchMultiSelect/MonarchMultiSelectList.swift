import SwiftUI

/// The searchable list of ``MonarchMultiSelect``, shown in a sheet. Each row
/// adds or removes its value; the list stays open until Done.
struct MonarchMultiSelectList: View {
    let title: String
    @Binding var selection: [String]
    let options: [MonarchComboboxOption]
    @Binding var query: String
    let maxSelections: Int?
    let isLoading: Bool
    let sortsByMatch: Bool
    let limit: Int?
    let labels: MonarchMultiSelectLabels
    let filter: (@Sendable (MonarchComboboxOption, String) -> Bool)?
    let canCreate: Bool
    let onCreate: @MainActor (String) -> Void
    let onDone: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        NavigationStack {
            list
                .navigationTitle(title)
                .monarchMultiSelectInlineTitle()
                .monarchMultiSelectSearchable(
                    text: $query,
                    prompt: labels.list.search ?? MonarchComboboxStrings.search
                )
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(labels.done ?? MonarchMultiSelectStrings.done, action: onDone)
                    }
                }
        }
        .presentationDetents([.medium, .large])
    }

    private var list: some View {
        let results = MonarchComboboxTokens.results(
            options,
            query: query,
            filter: filter,
            sortsByMatch: sortsByMatch,
            limit: limit
        )
        let isFull = MonarchMultiSelectTokens.isFull(count: selection.count, maxSelections: maxSelections)
        let showsCreate =
            !isFull && MonarchComboboxTokens.showsCreate(query: query, options: options, canCreate: canCreate)
        return List {
            Group {
                if let count = MonarchMultiSelectTokens.countText(
                    count: selection.count,
                    maxSelections: maxSelections,
                    labels: labels
                ) {
                    Text(count)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                ForEach(Array(results.sections.enumerated()), id: \.offset) { _, section in
                    if let heading = section.title {
                        Section(heading) { rows(section.options) }
                    } else {
                        rows(section.options)
                    }
                }
                if showsCreate {
                    Button {
                        onCreate(query)
                    } label: {
                        HStack(spacing: MonarchComboboxTokens.spacing) {
                            MonarchIcon(systemName: MonarchComboboxTokens.createSymbol, size: .md)
                            Text(labels.list.create?(query) ?? MonarchComboboxStrings.create(query))
                                .monarchTextStyle(.body)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .foregroundStyle(theme.colors.textDefault)
                        .frame(minHeight: MonarchComboboxTokens.minHeight)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                if isLoading {
                    HStack(spacing: MonarchComboboxTokens.spacing) {
                        ProgressView()
                        Text(labels.list.loading ?? MonarchComboboxStrings.loading).monarchTextStyle(.body)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityElement(children: .combine)
                } else if results.isEmpty && !showsCreate {
                    Text(labels.list.empty ?? MonarchComboboxStrings.empty)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if results.isTruncated {
                    Text(
                        labels.list.more?(results.shown, results.total)
                            ?? MonarchComboboxStrings.more(shown: results.shown, total: results.total)
                    )
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    private func rows(_ options: [MonarchComboboxOption]) -> some View {
        ForEach(options) { option in
            let isSelected = selection.contains(option.id)
            let isDisabled = MonarchMultiSelectTokens.isRowDisabled(
                option,
                selection: selection,
                maxSelections: maxSelections
            )
            Button {
                selection = MonarchMultiSelectTokens.toggled(selection, id: option.id, maxSelections: maxSelections)
            } label: {
                HStack(spacing: MonarchComboboxTokens.spacing) {
                    if let systemImage = option.systemImage {
                        MonarchIcon(systemName: systemImage, size: .md)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: !isDisabled)]
                            )
                    }
                    VStack(alignment: .leading, spacing: MonarchComboboxTokens.textSpacing) {
                        Text(option.label)
                            .monarchTextStyle(.body)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.optionColor(isEnabled: !isDisabled)]
                            )
                        if let description = option.description, !description.isEmpty {
                            Text(description)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(
                                    theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: !isDisabled)]
                                )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if let hint = option.hint, !hint.isEmpty {
                        Text(hint)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: !isDisabled)]
                            )
                    }
                    if isSelected {
                        MonarchIcon(systemName: MonarchMultiSelectTokens.checkSymbol, size: .sm)
                            .foregroundStyle(theme.colors.accentStrong)
                    }
                }
                .frame(minHeight: MonarchComboboxTokens.minHeight)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(isDisabled)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(MonarchComboboxTokens.accessibilityLabel(option))
            .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        }
    }
}

extension View {
    /// An inline navigation title on iOS; other platforms have one style.
    @ViewBuilder
    fileprivate func monarchMultiSelectInlineTitle() -> some View {
        #if os(iOS)
            navigationBarTitleDisplayMode(.inline)
        #else
            self
        #endif
    }

    /// The search field, always shown in the navigation bar on iOS.
    @ViewBuilder
    fileprivate func monarchMultiSelectSearchable(text: Binding<String>, prompt: String) -> some View {
        #if os(iOS)
            searchable(text: text, placement: .navigationBarDrawer(displayMode: .always), prompt: Text(prompt))
        #else
            searchable(text: text, prompt: Text(prompt))
        #endif
    }
}
