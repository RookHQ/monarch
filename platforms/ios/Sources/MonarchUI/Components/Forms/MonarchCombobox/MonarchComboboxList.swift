import SwiftUI

/// The searchable list of ``MonarchCombobox``, shown in a sheet.
struct MonarchComboboxList: View {
    let title: String
    let selection: String?
    let options: [MonarchComboboxOption]
    @Binding var query: String
    let clearOptionTitle: String?
    let isLoading: Bool
    var loadError: String? = nil
    var onRetry: (@MainActor () -> Void)? = nil
    let sortsByMatch: Bool
    let limit: Int?
    let labels: MonarchComboboxLabels
    let filter: (@Sendable (MonarchComboboxOption, String) -> Bool)?
    let canCreate: Bool
    let onChoose: @MainActor (String?) -> Void
    let onCreate: @MainActor (String) -> Void
    let onCancel: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    private var results: MonarchComboboxTokens.Results {
        MonarchComboboxTokens.results(
            options,
            query: query,
            filter: filter,
            sortsByMatch: sortsByMatch,
            limit: limit
        )
    }

    var body: some View {
        NavigationStack {
            list
                .navigationTitle(title)
                .monarchInlineNavigationTitle()
                .monarchSheetSearchable(text: $query, prompt: labels.search ?? MonarchComboboxStrings.search)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(labels.cancel ?? MonarchComboboxStrings.cancel, action: onCancel)
                    }
                }
        }
        .presentationDetents([.medium, .large])
    }

    private var list: some View {
        let results = results
        let showsCreate = MonarchComboboxTokens.showsCreate(query: query, options: options, canCreate: canCreate)
        return List {
            Group {
                if let clearOptionTitle, query.isEmpty {
                    row(title: clearOptionTitle, systemImage: nil, isSelected: selection == nil) { onChoose(nil) }
                }
                ForEach(Array(results.sections.enumerated()), id: \.offset) { _, section in
                    if let heading = section.title {
                        Section(heading) { rows(section.options) }
                    } else {
                        rows(section.options)
                    }
                }
                if showsCreate {
                    row(
                        title: labels.create?(query) ?? MonarchComboboxStrings.create(query),
                        systemImage: MonarchComboboxTokens.createSymbol,
                        isSelected: false
                    ) { onCreate(query) }
                }
                if let loadError {
                    let message = MonarchComboboxTokens.loadFailedMessage(loadError, labels: labels)
                    if let onRetry {
                        MonarchInlineErrorRetry(message, onRetry: onRetry)
                    } else {
                        MonarchFieldErrorRow(message: message, isSpoken: true)
                    }
                } else if isLoading {
                    HStack(spacing: MonarchComboboxTokens.spacing) {
                        ProgressView()
                        Text(labels.loading ?? MonarchComboboxStrings.loading).monarchTextStyle(.body)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityElement(children: .combine)
                } else if results.isEmpty && !showsCreate {
                    Text(labels.empty ?? MonarchComboboxStrings.empty)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if results.isTruncated {
                    Text(
                        labels.more?(results.shown, results.total)
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
            Button {
                onChoose(option.id)
            } label: {
                HStack(spacing: MonarchComboboxTokens.spacing) {
                    if let systemImage = option.systemImage {
                        MonarchIcon(systemName: systemImage, size: .md)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: !option.isDisabled)]
                            )
                    }
                    if let leadingText = option.leadingText, !leadingText.isEmpty {
                        Text(leadingText)
                            .monarchTextStyle(.body)
                            .accessibilityHidden(true)
                    }
                    VStack(alignment: .leading, spacing: MonarchComboboxTokens.textSpacing) {
                        Text(option.label)
                            .monarchLabelLanguage(option.labelLanguage)
                            .monarchTextStyle(.body)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.optionColor(isEnabled: !option.isDisabled)]
                            )
                        if let description = option.description, !description.isEmpty {
                            Text(description)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(
                                    theme.colors[
                                        role: MonarchComboboxTokens.secondaryColor(isEnabled: !option.isDisabled)
                                    ]
                                )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if let hint = option.hint, !hint.isEmpty {
                        Text(hint)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: !option.isDisabled)]
                            )
                    }
                    checkmark(option.id == selection)
                }
                .frame(minHeight: MonarchComboboxTokens.minHeight)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(option.isDisabled)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(MonarchComboboxTokens.spokenLabel(option)))
            .accessibilityAddTraits(option.id == selection ? [.isButton, .isSelected] : .isButton)
        }
    }

    private func row(
        title: String,
        systemImage: String?,
        isSelected: Bool,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: MonarchComboboxTokens.spacing) {
                if let systemImage { MonarchIcon(systemName: systemImage, size: .md) }
                Text(title)
                    .monarchTextStyle(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                checkmark(isSelected)
            }
            .foregroundStyle(theme.colors.textDefault)
            .frame(minHeight: MonarchComboboxTokens.minHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    @ViewBuilder private func checkmark(_ isSelected: Bool) -> some View {
        if isSelected {
            MonarchIcon(systemName: MonarchComboboxTokens.checkSymbol, size: .sm)
                .foregroundStyle(theme.colors.accentStrong)
        }
    }
}

extension View {
    /// Lays out text in the language it is written in, so an endonym gets
    /// that language's glyph forms and line breaking.
    @ViewBuilder
    fileprivate func monarchLabelLanguage(_ language: Locale.Language?) -> some View {
        if let language {
            environment(\.locale, Locale(identifier: language.maximalIdentifier))
        } else {
            self
        }
    }
}
