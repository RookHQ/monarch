import MonarchTokens
import SwiftUI

/// A section of the person's pinned destinations (projects, views,
/// documents) for a sidebar, with an Edit button that lets people reorder
/// and remove them.
///
/// ```swift
/// List {
///     MonarchFavoritesList(
///         items: favorites,
///         onRemove: { item in favorites.removeAll { $0.id == item.id } },
///         onReorder: { favorites = $0 },
///         onSelect: { open($0) }
///     )
/// }
/// ```
///
/// The list is a `Section` with a starred heading, made to sit in a `List`.
/// Rows are buttons that report the selection; the app opens the
/// destination and keeps the favorites. With `onRemove` or `onReorder`, the
/// heading has an Edit button that puts this section in edit mode: rows show
/// the system delete control and reorder handle (`onDelete` and `onMove`).
/// Swipe to delete works outside edit mode, as everywhere in iOS. With no
/// items and no `emptyMessage` the section is left out.
///
/// Accessibility: the heading has the header trait and the star is
/// decorative. The current destination has the selected trait and is set in
/// a semibold weight on the accent's subtle fill, so it is not marked by
/// colour alone. In edit mode each row also has Move up and Move down
/// actions. After a removal "Roadmap removed from favorites." is announced.
public struct MonarchFavoritesList: View {
    private let items: [MonarchFavoriteItem]
    private let externalIsEditing: Binding<Bool>?
    private let showsIcon: Bool
    private let emptyMessage: String?
    private let labels: MonarchFavoritesListLabels
    private let onRemove: (@MainActor (MonarchFavoriteItem) -> Void)?
    private let onReorder: (@MainActor ([MonarchFavoriteItem]) -> Void)?
    private let onSelect: @MainActor (MonarchFavoriteItem) -> Void

    @State private var internalIsEditing = false
    @AccessibilityFocusState private var focusedRow: String?
    @State private var pendingRemoval: (ids: Set<String>, index: Int)?
    @Environment(\.monarchTheme) private var theme

    /// A favorites list.
    ///
    /// - Parameters:
    ///   - items: The pinned destinations, in order.
    ///   - isEditing: Edit mode, when the app controls it. Without it the
    ///     Edit button keeps its own state.
    ///   - showsIcon: Whether a star comes before the heading.
    ///   - emptyMessage: Shown when there are no items. Without it the
    ///     section is left out when empty.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemove: Removes an item. With it, rows can be deleted.
    ///   - onReorder: Saves a new order. With it, rows can be moved in edit mode.
    ///   - onSelect: Opens a destination.
    public init(
        items: [MonarchFavoriteItem],
        isEditing: Binding<Bool>? = nil,
        showsIcon: Bool = true,
        emptyMessage: String? = nil,
        labels: MonarchFavoritesListLabels = MonarchFavoritesListLabels(),
        onRemove: (@MainActor (MonarchFavoriteItem) -> Void)? = nil,
        onReorder: (@MainActor ([MonarchFavoriteItem]) -> Void)? = nil,
        onSelect: @escaping @MainActor (MonarchFavoriteItem) -> Void
    ) {
        self.items = items
        self.externalIsEditing = isEditing
        self.showsIcon = showsIcon
        self.emptyMessage = emptyMessage
        self.labels = labels
        self.onRemove = onRemove
        self.onReorder = onReorder
        self.onSelect = onSelect
    }

    private var isEditable: Bool {
        MonarchFavoritesListTokens.isEditable(canRemove: onRemove != nil, canReorder: onReorder != nil)
    }

    private var isEditing: Bool {
        MonarchFavoritesListTokens.isEditing(
            requested: externalIsEditing?.wrappedValue ?? internalIsEditing,
            isEditable: isEditable,
            count: items.count
        )
    }

    private func setEditing(_ value: Bool) {
        if let externalIsEditing {
            externalIsEditing.wrappedValue = value
        } else {
            internalIsEditing = value
        }
    }

    /// The content of the view.
    public var body: some View {
        if !MonarchFavoritesListTokens.isHidden(count: items.count, hasEmptyMessage: emptyMessage != nil) {
            section
        }
    }

    private var section: some View {
        Section {
            sectionContent
        } header: {
            header
        }
        #if os(iOS)
            .environment(\.editMode, editMode)
        #endif
        .onChange(of: items.map(\.id)) { _, ids in
            // As on the web: after a removal focus stays in the list, on the next row.
            guard let pending = pendingRemoval, pending.ids.isDisjoint(with: ids) else { return }
            pendingRemoval = nil
            focusedRow = MonarchFocusAfterRemoval.target(removedIndex: pending.index, remaining: ids)
        }
    }

    @ViewBuilder
    private var sectionContent: some View {
        if items.isEmpty, let emptyMessage {
            Text(emptyMessage)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
        } else {
            ForEach(items) { item in
                row(item)
            }
            .onMove(perform: moveAction)
            .onDelete(perform: deleteAction)
        }
    }

    private var moveAction: ((IndexSet, Int) -> Void)? {
        guard onReorder != nil else { return nil }
        return { source, destination in move(from: source, to: destination) }
    }

    private var deleteAction: ((IndexSet) -> Void)? {
        guard onRemove != nil else { return nil }
        return { offsets in delete(at: offsets) }
    }

    #if os(iOS)
        private var editMode: Binding<EditMode> {
            Binding(
                get: { isEditing ? .active : .inactive },
                set: { setEditing($0.isEditing) }
            )
        }
    #endif

    private var header: some View {
        HStack(spacing: MonarchFavoritesListTokens.headerSpacing) {
            HStack(spacing: MonarchFavoritesListTokens.headerSpacing) {
                if showsIcon {
                    MonarchIcon(systemName: "star", size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                }
                Text(MonarchFavoritesListTokens.title(labels: labels))
                    .monarchTextStyle(.caption, weight: .semibold)
                    .textCase(.uppercase)
                    .tracking(0.6)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            if isEditable && !items.isEmpty {
                MonarchButton(
                    MonarchFavoritesListTokens.editTitle(isEditing: isEditing, labels: labels),
                    variant: .ghost,
                    size: .sm
                ) {
                    setEditing(!isEditing)
                }
                .accessibilityLabel(
                    MonarchFavoritesListTokens.editAccessibilityLabel(isEditing: isEditing, labels: labels)
                )
            }
        }
        .textCase(nil)
    }

    @ViewBuilder
    private func row(_ item: MonarchFavoriteItem) -> some View {
        let colors = MonarchFavoritesListTokens.colors(isCurrent: item.isCurrent)
        let content = HStack(spacing: MonarchFavoritesListTokens.rowSpacing) {
            if let systemImage = item.systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(theme.colors[role: colors.icon])
            }
            Text(item.title)
                .monarchTextStyle(item.isCurrent ? .bodyStrong : .body)
                .foregroundStyle(theme.colors[role: colors.text])
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
        Group {
            if isEditing {
                content
                    .accessibilityElement(children: .combine)
                    .accessibilityActions { moveActions(item) }
            } else {
                Button {
                    onSelect(item)
                } label: {
                    content
                }
                .buttonStyle(.plain)
            }
        }
        .accessibilityAddTraits(item.isCurrent ? .isSelected : [])
        .accessibilityFocused($focusedRow, equals: item.id)
        // As on the web: no dividers, and the current page in a rounded tinted pill.
        .listRowSeparator(.hidden)
        .listRowBackground(
            // A row with no fill is clear, so the canvas shows in dark and not the system's black.
            Group {
                if let fill = colors.fill {
                    RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                        .fill(theme.colors[role: fill])
                        .padding(.horizontal, MonarchTokens.Space.space1)
                } else {
                    Color.clear
                }
            }
        )
    }

    @ViewBuilder
    private func moveActions(_ item: MonarchFavoriteItem) -> some View {
        if let onReorder {
            let names = MonarchFavoritesListTokens.moveActions(labels: labels)
            if let up = MonarchFavoritesListTokens.moved(items, id: item.id, by: -1) {
                Button(names.up) { onReorder(up) }
            }
            if let down = MonarchFavoritesListTokens.moved(items, id: item.id, by: 1) {
                Button(names.down) { onReorder(down) }
            }
        }
    }

    private func move(from source: IndexSet, to destination: Int) {
        onReorder?(MonarchFavoritesListTokens.moved(items, fromOffsets: source, toOffset: destination))
    }

    private func delete(at offsets: IndexSet) {
        guard let onRemove else { return }
        let removed = MonarchFavoritesListTokens.removed(items, atOffsets: offsets)
        pendingRemoval = (Set(removed.map(\.id)), offsets.min() ?? 0)
        for item in removed {
            onRemove(item)
        }
        if let last = removed.last {
            AccessibilityNotification.Announcement(
                MonarchFavoritesListTokens.removedAnnouncement(last.title, labels: labels)
            )
            .post()
        }
        if removed.count == items.count {
            setEditing(false)
        }
    }
}

#if DEBUG
    #Preview("MonarchFavoritesList") {
        MonarchPreviewMatrix {
            List {
                Group {
                    MonarchFavoritesList(
                        items: [
                            MonarchFavoriteItem("Roadmap", id: "doc:1", systemImage: "doc.text", isCurrent: true),
                            MonarchFavoriteItem("Q3 launch", id: "project:2", systemImage: "folder"),
                            MonarchFavoriteItem(
                                "My open tickets",
                                id: "view:3",
                                systemImage: "line.3.horizontal.decrease"
                            ),
                        ],
                        onRemove: { _ in },
                        onReorder: { _ in },
                        onSelect: { _ in }
                    )
                    MonarchFavoritesList(items: [], emptyMessage: "Star a page to pin it here.") { _ in }

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 8)
        }
    }
#endif
