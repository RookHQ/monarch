import MonarchTokens
import SwiftUI

/// Everything the assistant remembers, grouped by the person, organization
/// or project it is about, with search, edit, forget one, and select
/// several to forget after a confirmation.
///
/// ```swift
/// NavigationStack {
///     MonarchAiMemoryPanel(
///         memories,
///         onEdit: { id, text in store.update(id, text: text) },
///         onForget: { id in store.forget([id]) },
///         onForgetMany: { ids in store.forget(ids) }
///     )
///     .navigationTitle("Memories")
/// }
/// ```
///
/// A product pattern, experimental until a Rook app adopts it, built from
/// system controls and Monarch parts: a standard `List` with one `Section`
/// per entity (people, then organizations, projects and other, each by
/// name; newest memory first), `searchable` for the search field, a
/// "Select" toolbar button that puts the list in edit mode for multiple
/// selection, ``MonarchBulkActionBar`` at the bottom with Forget, and a
/// confirmation dialog. Each row is a ``MonarchAiMemoryChip`` row whose
/// menu offers Edit and Forget, with the date it was last updated; swiping
/// a row also forgets it. Search and the toolbar need a `NavigationStack`
/// around the panel. The app owns the memories: change them in the
/// callbacks, and keep them in step with a conversation with
/// ``MonarchAiMemoryItem/apply(_:event:now:defaultEntityName:entity:)``.
///
/// Accessibility: the list is named "Memories" and each group name is a
/// header followed by its kind and count. Each row reads the memory, with
/// Edit and Forget as actions. The number of matches is announced after the
/// search settles, and "Memory updated" or "2 memories forgotten" after a
/// change. Selection is the system's edit mode, so VoiceOver reads each
/// row's selected state.
@_spi(Experimental) public struct MonarchAiMemoryPanel: View {
    private let memories: [MonarchAiMemoryItem]
    private let labels: MonarchAiMemoryPanelLabels
    private let onEdit: @MainActor (_ id: String, _ text: String) -> Void
    private let onForget: @MainActor (_ id: String) -> Void
    private let onForgetMany: @MainActor (_ ids: [String]) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @State private var query = ""
    @State private var selection = Set<String>()
    @State private var isSelecting: Bool
    @State private var isConfirming = false

    private typealias Tokens = MonarchAiMemoryPanelTokens

    /// A memory panel.
    ///
    /// - Parameters:
    ///   - memories: Everything remembered, in any order.
    ///   - labels: Replaces the built-in strings, the row strings and the bulk bar strings.
    ///   - startsSelecting: Opens in selection mode, with the select-all and forget actions showing.
    ///   - onEdit: Saves an edited memory. Receives the id and the trimmed new text.
    ///   - onForget: Forgets one memory straight away, from its menu or a swipe.
    ///   - onForgetMany: Forgets the selected memories after the person confirms.
    public init(
        _ memories: [MonarchAiMemoryItem],
        labels: MonarchAiMemoryPanelLabels = MonarchAiMemoryPanelLabels(),
        startsSelecting: Bool = false,
        onEdit: @escaping @MainActor (_ id: String, _ text: String) -> Void,
        onForget: @escaping @MainActor (_ id: String) -> Void,
        onForgetMany: @escaping @MainActor (_ ids: [String]) -> Void
    ) {
        self.memories = memories
        self.labels = labels
        _isSelecting = State(initialValue: startsSelecting)
        self.onEdit = onEdit
        self.onForget = onForget
        self.onForgetMany = onForgetMany
    }

    /// The content of the view.
    public var body: some View {
        let groups = Tokens.groups(memories, query: query, locale: locale)
        let selected = Tokens.selected(selection, groups: groups)
        list(groups)
            .monarchSearchable(text: $query, prompt: Text(labels.search))
            .toolbar {
                if !memories.isEmpty {
                    ToolbarItem(placement: .primaryAction) {
                        Button(isSelecting ? labels.done : labels.select) {
                            isSelecting.toggle()
                            if !isSelecting { selection = [] }
                        }
                    }
                    .monarchFlat()
                }
            }
            .confirmationDialog(
                labels.confirmTitle(selected.count),
                isPresented: $isConfirming,
                titleVisibility: .visible
            ) {
                Button(labels.forget, role: .destructive) { manyForgetter(selected) }
                Button(labels.cancel, role: .cancel) {}
            } message: {
                Text(labels.confirmMessage)
            }
            .task(id: query) {
                let count = Tokens.shownIDs(groups).count
                guard let text = Tokens.searchAnnouncement(query: query, count: count, labels: labels) else { return }
                // Announce once the search settles, not on every keystroke, as the web does.
                try? await Task.sleep(for: Tokens.searchDebounce)
                guard !Task.isCancelled else { return }
                AccessibilityNotification.Announcement(text).post()
            }
    }

    private func list(_ groups: [Tokens.Group]) -> some View {
        List {
            Group {
                if isSelecting {
                    // As the web: Select all with its count and Forget selected, under the search field.
                    selectionBar(groups)
                        .listRowBackground(Color.clear)
                }
                if groups.isEmpty {
                    emptyState.listRowBackground(Color.clear)
                }
                ForEach(groups) { group in
                    Section {
                        ForEach(group.items) { row($0) }
                    } header: {
                        header(group)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
        .accessibilityLabel(labels.label)
    }

    private func selectionBar(_ groups: [Tokens.Group]) -> some View {
        let isAll = Tokens.isAllSelected(selection, groups: groups)
        let selected = Tokens.selected(selection, groups: groups)
        return HStack {
            // A square checkbox like the web's, not the system's round edit-mode mark.
            Toggle(
                Tokens.selectAllTitle(
                    isAllSelected: isAll,
                    shown: Tokens.shownIDs(groups).count,
                    labels: labels
                ),
                isOn: Binding(
                    get: { isAll },
                    set: { selection = $0 ? Set(Tokens.shownIDs(groups)) : [] }
                )
            )
            .toggleStyle(.monarchCheckbox)
            .monarchTextStyle(.body)
            .disabled(groups.isEmpty)
            Spacer(minLength: 0)
            Button(labels.forgetSelected, role: .destructive) { isConfirming = true }
                .buttonStyle(.monarch(.dangerSubtle, size: .sm))
                .disabled(selected.isEmpty)
        }
    }

    @ViewBuilder private var emptyState: some View {
        if memories.isEmpty {
            MonarchEmptyState(labels.empty, kind: .firstRun, size: .sm)
        } else {
            MonarchEmptyState(
                labels.noMatches(query.trimmingCharacters(in: .whitespacesAndNewlines)),
                kind: .filtered,
                size: .sm
            ) {
                query = ""
            }
        }
    }

    private func header(_ group: Tokens.Group) -> some View {
        let name = Text(group.entity.name)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors[role: Tokens.headingRole])
            .accessibilityAddTraits(.isHeader)
        let detail = Text(Tokens.groupDetail(group, labels: labels))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: Tokens.mutedRole])
        return HStack(alignment: .firstTextBaseline, spacing: MonarchTokens.Space.space2) {
            if isSelecting {
                let ids = group.items.map(\.id)
                Toggle(
                    group.entity.name,
                    isOn: Binding(
                        get: { ids.allSatisfy(selection.contains) },
                        set: { isOn in
                            if isOn { selection.formUnion(ids) } else { selection.subtract(ids) }
                        }
                    )
                )
                .toggleStyle(.monarchCheckbox)
                .labelsHidden()
                .accessibilityLabel(group.entity.name)
            }
            name
            detail
        }
        .textCase(nil)
    }

    private func row(_ memory: MonarchAiMemoryItem) -> some View {
        HStack(alignment: .top, spacing: MonarchTokens.Space.space2) {
            if isSelecting {
                Toggle(
                    memory.text,
                    isOn: Binding(
                        get: { selection.contains(memory.id) },
                        set: { isOn in
                            if isOn { selection.insert(memory.id) } else { selection.remove(memory.id) }
                        }
                    )
                )
                .toggleStyle(.monarchCheckbox)
                .labelsHidden()
                .accessibilityLabel(memory.text)
            }
            rowContent(memory)
        }
        .tag(memory.id)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                forgetter(memory.id)
            } label: {
                Label(labels.forget, systemImage: Tokens.forgetSymbol)
            }
        }
    }

    private func rowContent(_ memory: MonarchAiMemoryItem) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineGap) {
            MonarchAiMemoryChip(
                Tokens.part(memory),
                variant: .row,
                showsScope: false,
                labels: labels.chip,
                onEdit: editor,
                onForget: forgetter
            )
            Text(Tokens.updatedLine(memory.updatedAt, locale: locale, timeZone: timeZone, labels: labels))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                .accessibilityHidden(isSelecting)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Saves an edit and announces it. Captures only sendable values, so
    /// the row's chip can keep it.
    private var editor: @MainActor (String, String) -> Void {
        let onEdit = onEdit
        let edited = labels.edited
        return { id, text in
            onEdit(id, text)
            AccessibilityNotification.Announcement(edited).post()
        }
    }

    /// Forgets one memory from a row.
    private var forgetter: @MainActor (String) -> Void {
        let forgetMany = manyForgetter
        return { id in forgetMany([id]) }
    }

    /// Forgets memories (one through `onForget`, several through
    /// `onForgetMany`), updates the selection and announces it.
    private var manyForgetter: @MainActor ([String]) -> Void {
        let onForget = onForget
        let onForgetMany = onForgetMany
        let forgotten = labels.forgotten
        let selection = $selection
        let isSelecting = $isSelecting
        return { ids in
            guard !ids.isEmpty else { return }
            if ids.count == 1 {
                onForget(ids[0])
            } else {
                onForgetMany(ids)
            }
            selection.wrappedValue.subtract(ids)
            if selection.wrappedValue.isEmpty { isSelecting.wrappedValue = false }
            AccessibilityNotification.Announcement(forgotten(ids.count)).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchAiMemoryPanel") {
        let now = Date()
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchAiMemoryPanel(
                    [
                        MonarchAiMemoryItem(
                            id: "1",
                            text: "Prefers summaries under 200 words",
                            entity: MonarchAiMemoryEntity(kind: .person, name: "Ada Lovelace"),
                            updatedAt: now
                        ),
                        MonarchAiMemoryItem(
                            id: "2",
                            text: "Fiscal year starts in April",
                            entity: MonarchAiMemoryEntity(kind: .organization, name: "Rook"),
                            updatedAt: now
                        ),
                    ],
                    onEdit: { _, _ in },
                    onForget: { _ in },
                    onForgetMany: { _ in }
                )
            }
        }
    }
#endif
