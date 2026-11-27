import SwiftUI

/// The searchable list of ``MonarchEntityPicker``, shown in a sheet.
struct MonarchEntityPickerList: View {
    /// The search text and the retry count, so Try again reruns the same text.
    private struct SearchKey: Equatable {
        let query: String
        let attempt: Int
    }

    let title: String
    let isMultiple: Bool
    let selection: [String]
    let entities: [MonarchEntityPickerEntity]
    @Binding var query: String
    let clearOptionTitle: String?
    let isLoading: Bool
    let minSearchLength: Int
    let debounce: Duration
    let labels: MonarchEntityPickerLabels
    let search: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])?
    let canCreate: Bool
    var footerTitle: String?
    var onFooter: (@MainActor () -> Void)?
    let onChoose: @MainActor (MonarchEntityPickerEntity?) -> Void
    let onCreate: @MainActor (String) async -> Void
    let onClose: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var searcher = MonarchDebouncedSearch<MonarchEntityPickerEntity>()
    @State private var attempt = 0
    @State private var isCreating = false

    var body: some View {
        NavigationStack {
            list
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    if let footerTitle {
                        MonarchButton(footerTitle, variant: .ghost, size: .sm) { onFooter?() }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(theme.colors.bgSurface)
                    }
                }
                .navigationTitle(title)
                .monarchEntityPickerInlineTitle()
                .monarchEntityPickerSearchable(text: $query, prompt: labels.search ?? MonarchEntityPickerStrings.search)
                .toolbar {
                    if isMultiple {
                        ToolbarItem(placement: .confirmationAction) {
                            Button(labels.done ?? MonarchEntityPickerStrings.done, action: onClose)
                        }
                    } else {
                        ToolbarItem(placement: .cancellationAction) {
                            MonarchButton(
                                labels.cancel ?? MonarchEntityPickerStrings.cancel,
                                variant: .ghost,
                                size: .sm,
                                action: onClose
                            )
                            .fixedSize()
                        }
                        .monarchFlat()
                    }
                }
        }
        .presentationDetents([.medium, .large])
        .task(id: SearchKey(query: query, attempt: attempt)) {
            guard let search else { return }
            await searcher.run(query, minLength: minSearchLength, debounce: debounce, search: search)
        }
    }

    private var shownEntities: [MonarchEntityPickerEntity] {
        let content = content
        guard case .rows(let ids) = content else { return [] }
        let pool = search == nil || searcher.phase == .idle ? entities : searcher.results
        return ids.compactMap { id in pool.first { $0.id == id } }
    }

    private var content: MonarchEntityPickerContent {
        MonarchEntityPickerTokens.content(
            entities: entities,
            query: query,
            hasSearch: search != nil,
            phase: searcher.phase,
            results: searcher.results,
            isLoading: isLoading
        )
    }

    private var list: some View {
        let content = content
        let shown = shownEntities
        let showsCreate =
            content != .loading
            && MonarchEntityPickerTokens.showsCreate(query: query, shown: shown, canCreate: canCreate)
        return List {
            Group {
                if let clearOptionTitle, !isMultiple, query.isEmpty {
                    Button {
                        onChoose(nil)
                    } label: {
                        HStack(spacing: MonarchEntityPickerTokens.spacing) {
                            // As on the web: a dashed circle, the empty seat, before the name.
                            Circle()
                                .strokeBorder(
                                    theme.colors.textMuted,
                                    style: StrokeStyle(lineWidth: MonarchShapes.borderWidth, dash: [2, 2])
                                )
                                .frame(width: 16, height: 16)
                                .accessibilityHidden(true)
                            Text(clearOptionTitle)
                                .monarchTextStyle(.body)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            checkmark(selection.isEmpty)
                        }
                        .foregroundStyle(theme.colors.textDefault)
                        .frame(minHeight: MonarchComboboxTokens.minHeight)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selection.isEmpty ? .isSelected : [])
                }
                ForEach(MonarchEntityPickerTokens.sections(shown), id: \.title) { section in
                    if let heading = section.title {
                        Section(heading) { rows(section.ids, in: shown) }
                    } else {
                        rows(section.ids, in: shown)
                    }
                }
                if showsCreate { createRow }
                switch content {
                case .rows:
                    EmptyView()
                case .loading:
                    HStack(spacing: MonarchComboboxTokens.spacing) {
                        ProgressView()
                        Text(message(content)).monarchTextStyle(.body)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityElement(children: .combine)
                case .failed:
                    MonarchInlineErrorRetry(message(content)) { attempt += 1 }
                case .tooShort, .empty:
                    if !showsCreate {
                        Text(message(content))
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    @ViewBuilder private func rows(_ ids: [String], in shown: [MonarchEntityPickerEntity]) -> some View {
        ForEach(ids, id: \.self) { id in
            if let entity = shown.first(where: { $0.id == id }) { row(entity) }
        }
    }

    private func message(_ content: MonarchEntityPickerContent) -> String {
        MonarchEntityPickerTokens.message(content, minLength: minSearchLength, labels: labels) ?? ""
    }

    private func row(_ entity: MonarchEntityPickerEntity) -> some View {
        let isSelected = selection.contains(entity.id)
        let isEnabled = !entity.isDisabled
        return Button {
            onChoose(entity)
        } label: {
            HStack(spacing: MonarchEntityPickerTokens.spacing) {
                MonarchEntityPickerAvatar(
                    entity: entity,
                    size: MonarchEntityPickerTokens.rowAvatarSize,
                    showsStatus: true
                )
                VStack(alignment: .leading, spacing: MonarchComboboxTokens.textSpacing) {
                    Text(entity.name)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors[role: MonarchComboboxTokens.optionColor(isEnabled: isEnabled)])
                    if let description = entity.description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)]
                            )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if let hint = entity.hint, !hint.isEmpty {
                    Text(hint)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                }
                checkmark(isSelected)
            }
            .frame(minHeight: MonarchComboboxTokens.minHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(entity.isDisabled)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchEntityPickerTokens.accessibilityLabel(entity))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }

    private var createRow: some View {
        Button {
            isCreating = true
            Task {
                await onCreate(query)
                isCreating = false
            }
        } label: {
            HStack(spacing: MonarchEntityPickerTokens.spacing) {
                MonarchIcon(systemName: MonarchComboboxTokens.createSymbol, size: .md)
                Text(labels.create?(query) ?? MonarchEntityPickerStrings.create(query))
                    .monarchTextStyle(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if isCreating { ProgressView() }
            }
            .foregroundStyle(theme.colors.textDefault)
            .frame(minHeight: MonarchComboboxTokens.minHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(isCreating)
    }

    @ViewBuilder private func checkmark(_ isSelected: Bool) -> some View {
        if isSelected {
            MonarchIcon(systemName: MonarchComboboxTokens.checkSymbol, size: .sm)
                .foregroundStyle(theme.colors.accentStrong)
        }
    }
}

/// The avatar of an entity, from its picture or its address.
struct MonarchEntityPickerAvatar: View {
    let entity: MonarchEntityPickerEntity
    let size: MonarchAvatarSize
    /// Whether the presence dot shows (in the list rows, not in the field).
    var showsStatus = false

    var body: some View {
        if let url = entity.url {
            MonarchAvatar(
                url: url,
                name: entity.name,
                seed: entity.id,
                systemImage: entity.systemImage,
                shape: entity.shape,
                size: size,
                status: showsStatus ? entity.status : nil,
                isDecorative: true,
                statusLabel: entity.statusLabel
            )
        } else {
            MonarchAvatar(
                image: entity.image,
                name: entity.name,
                seed: entity.id,
                systemImage: entity.systemImage,
                shape: entity.shape,
                size: size,
                status: showsStatus ? entity.status : nil,
                isDecorative: true,
                statusLabel: entity.statusLabel
            )
        }
    }
}

extension View {
    /// An inline navigation title on iOS; other platforms have one style.
    @ViewBuilder
    fileprivate func monarchEntityPickerInlineTitle() -> some View {
        #if os(iOS)
            navigationBarTitleDisplayMode(.inline)
        #else
            self
        #endif
    }

    /// The search field, always shown in the navigation bar on iOS.
    @ViewBuilder
    fileprivate func monarchEntityPickerSearchable(text: Binding<String>, prompt: String) -> some View {
        #if os(iOS)
            searchable(text: text, placement: .navigationBarDrawer(displayMode: .always), prompt: Text(prompt))
        #else
            searchable(text: text, prompt: Text(prompt))
        #endif
    }
}
