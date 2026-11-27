import SwiftUI

/// The doc picker of ``MonarchLinkedDocsPanel``: a sheet with a system
/// search field, New doc, and the docs not linked yet.
struct MonarchLinkedDocsPanelPicker: View {
    let options: [MonarchLinkedDocsPanelOption]
    let items: [MonarchLinkedDocsPanelItem]
    let labels: MonarchLinkedDocsPanelLabels
    let onQueryChange: (@MainActor (String) -> Void)?
    let onCreateDoc: (@MainActor () -> Void)?
    let onSelect: @MainActor (String) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private typealias Strings = MonarchLinkedDocsPanelStrings

    var body: some View {
        let available = MonarchLinkedDocsPanelTokens.availableOptions(
            options,
            items: items,
            query: onQueryChange == nil ? query : "",
            locale: locale
        )
        NavigationStack {
            List {
                Group {
                    if let onCreateDoc {
                        Button {
                            dismiss()
                            onCreateDoc()
                        } label: {
                            Label(
                                labels.newDoc ?? Strings.newDoc,
                                systemImage: MonarchLinkedDocsPanelTokens.newDocSymbol
                            )
                            .monarchTextStyle(.body)
                        }
                    }
                    if available.isEmpty {
                        Text(labels.noDocs ?? Strings.noDocs)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                    } else {
                        ForEach(available) { option in
                            Button {
                                onSelect(option.docID)
                            } label: {
                                VStack(alignment: .leading, spacing: MonarchLinkedDocsPanelTokens.lineSpacing) {
                                    Text(option.label)
                                        .monarchTextStyle(.body)
                                        .foregroundStyle(theme.colors.textDefault)
                                    if let description = option.description {
                                        Text(description)
                                            .monarchTextStyle(.caption)
                                            .foregroundStyle(theme.colors.textMuted)
                                    }
                                }
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .searchable(text: $query, prompt: labels.searchDocs ?? Strings.searchDocs)
            .onChange(of: query) { _, newValue in
                onQueryChange?(newValue)
            }
            .navigationTitle(labels.linkDoc ?? Strings.linkDoc)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.cancel ?? Strings.cancel) { dismiss() }
                }
            }
        }
    }
}
