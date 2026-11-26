import SwiftUI

/// The sheet that adds a relation: a type picker and a searchable list of
/// documents. Choosing a document adds the relation.
struct MonarchDocumentRelationsAddSheet: View {
    let types: [MonarchDocumentRelationType]
    let candidates: [MonarchDocumentRelationTarget]
    let relations: [MonarchDocumentRelation]
    let labels: MonarchDocumentRelationsLabels
    let onSearch: (@MainActor (String) -> Void)?
    let onAdd: @MainActor (MonarchDocumentRelationTarget, String) async throws -> Void
    @State var type: String

    @State private var query = ""
    @State private var pending: String?
    @State private var error: String?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchDocumentRelationsTokens
    private typealias Strings = MonarchDocumentRelationsStrings

    var body: some View {
        let results = Tokens.results(candidates, relations: relations, query: query, searchesRemotely: onSearch != nil)
        NavigationStack {
            List {
                Group {
                    Section {
                        Picker(labels.type ?? Strings.type, selection: $type) {
                            ForEach(types) { type in
                                Label(type.label, systemImage: Tokens.symbol(type.value)).tag(type.value)
                            }
                        }
                        .monarchTextStyle(.body)
                    } footer: {
                        Text(labels.sheetDescription ?? Strings.sheetDescription)
                            .monarchTextStyle(.caption)
                    }
                    Section {
                        if let error {
                            Label {
                                Text(error)
                            } icon: {
                                MonarchIcon(systemName: "exclamationmark.circle", size: .sm)
                            }
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: Tokens.errorColor])
                        }
                        if results.isEmpty {
                            Text(labels.noResults ?? Strings.noResults)
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                        }
                        ForEach(results) { target in
                            Button {
                                add(target)
                            } label: {
                                result(target)
                            }
                            .disabled(pending != nil)
                        }
                    } header: {
                        Text(labels.results ?? Strings.results)
                    }

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .searchable(text: $query, prompt: labels.search ?? Strings.search)
            .onChange(of: query) { _, new in onSearch?(new) }
            .navigationTitle(labels.sheetTitle ?? Strings.sheetTitle)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.cancel ?? Strings.cancel) { dismiss() }
                }
            }
        }
    }

    private func result(_ target: MonarchDocumentRelationTarget) -> some View {
        HStack(spacing: Tokens.rowSpacing) {
            MonarchDocumentRelationsTargetIcon(target: target)
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                Text(target.title)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.titleColor])
                if let description = target.description {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                }
            }
            Spacer(minLength: 0)
            if pending == target.id {
                ProgressView()
            }
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }

    private func add(_ target: MonarchDocumentRelationTarget) {
        guard pending == nil else { return }
        pending = target.id
        error = nil
        Task {
            do {
                try await onAdd(target, type)
                pending = nil
                dismiss()
            } catch {
                pending = nil
                self.error = Tokens.failureMessage(error, labels: labels)
            }
        }
    }
}
