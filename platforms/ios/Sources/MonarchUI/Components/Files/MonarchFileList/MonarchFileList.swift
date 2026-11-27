import SwiftUI

/// A list or grid of files with optional selection, a menu of actions per
/// file and a bar of actions for the selected files.
///
/// ```swift
/// MonarchFileList(
///     files,
///     selection: $selection,
///     layout: .grid,
///     bulkActions: [
///         MonarchBulkAction(id: "download", title: "Download", systemImage: "arrow.down.circle") { download() },
///         MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) { delete() },
///     ],
///     onOpen: { file in preview(file) },
///     menu: { file in
///         Button("Rename", systemImage: "pencil") { rename(file) }
///     }
/// )
/// ```
///
/// Each file is a ``MonarchFileCard``: rows in the list layout, cards in an
/// adaptive `LazyVGrid` in the grid layout. With a `selection`, each file
/// has a checkbox, a "Select all files" checkbox heads the list, and
/// ``MonarchBulkActionBar`` appears while files are selected, with the count,
/// the actions and Clear selection. Without files it shows
/// ``MonarchEmptyState``. Put it in a `ScrollView`; it does not scroll by
/// itself. A hardware keyboard's Escape clears the selection.
///
/// Accessibility: the files are a container named by `label`. Each file is
/// read as in ``MonarchFileCard``: the name, then the type, size, meta and
/// badges; the checkbox and the menu are separate controls with 44-point
/// touch targets. Select all is a standard toggle with on, off and mixed
/// values. The bar announces every change of the selection.
public struct MonarchFileList<MenuItems: View>: View {
    private let items: [MonarchFileListItem]
    private let selection: Binding<Set<String>>?
    private let layout: MonarchFileListLayout
    private let label: String?
    private let bulkActions: [MonarchBulkAction]
    private let labels: MonarchFileListLabels
    private let onOpen: (@MainActor (MonarchFileListItem) -> Void)?
    private let menu: (MonarchFileListItem) -> MenuItems
    private let hasMenu: Bool

    @ScaledMetric(relativeTo: .body) private var minimumCardWidth = MonarchFileListTokens.minimumCardWidth

    /// A file list with a menu of actions on each file.
    ///
    /// - Parameters:
    ///   - items: The files, in order.
    ///   - selection: The ids of the selected files. Adds checkboxes, select all and the bulk action bar.
    ///   - layout: Rows, or cards in a grid.
    ///   - label: The spoken name of the list, such as "Project files".
    ///   - bulkActions: The actions for the selected files, shown in the bar while any file is selected.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens a file. Makes each file a button.
    ///   - menu: The items of a file's actions menu.
    public init(
        _ items: [MonarchFileListItem],
        selection: Binding<Set<String>>? = nil,
        layout: MonarchFileListLayout = .list,
        label: String? = nil,
        bulkActions: [MonarchBulkAction] = [],
        labels: MonarchFileListLabels = MonarchFileListLabels(),
        onOpen: (@MainActor (MonarchFileListItem) -> Void)? = nil,
        @ViewBuilder menu: @escaping (MonarchFileListItem) -> MenuItems
    ) {
        self.init(
            items: items,
            selection: selection,
            layout: layout,
            label: label,
            bulkActions: bulkActions,
            labels: labels,
            onOpen: onOpen,
            menu: menu,
            hasMenu: true
        )
    }

    init(
        items: [MonarchFileListItem],
        selection: Binding<Set<String>>?,
        layout: MonarchFileListLayout,
        label: String?,
        bulkActions: [MonarchBulkAction],
        labels: MonarchFileListLabels,
        onOpen: (@MainActor (MonarchFileListItem) -> Void)?,
        menu: @escaping (MonarchFileListItem) -> MenuItems,
        hasMenu: Bool
    ) {
        self.items = items
        self.selection = selection
        self.layout = layout
        self.label = label
        self.bulkActions = bulkActions
        self.labels = labels
        self.onOpen = onOpen
        self.menu = menu
        self.hasMenu = hasMenu
    }

    private var selectedCount: Int {
        MonarchFileListTokens.selectedCount(items, selection: selection?.wrappedValue ?? [])
    }

    /// The content of the view.
    public var body: some View {
        if items.isEmpty {
            MonarchEmptyState(
                labels.empty ?? MonarchFileListStrings.empty,
                systemImage: MonarchFileListTokens.emptySymbol,
                size: .sm
            )
        } else {
            VStack(alignment: .leading, spacing: MonarchFileListTokens.sectionSpacing) {
                if let selection {
                    selectAll(selection)
                    MonarchBulkActionBar(
                        count: selectedCount,
                        totalCount: items.count,
                        actions: bulkActions,
                        labels: labels.bulkActionBar,
                        onClear: { selection.wrappedValue = MonarchFileListTokens.cleared(selection.wrappedValue) }
                    )
                }
                files
                    .accessibilityElement(children: .contain)
                    .accessibilityLabel(label ?? "")
            }
            .onKeyPress(.escape) {
                guard let selection, selectedCount > 0 else { return .ignored }
                selection.wrappedValue = MonarchFileListTokens.cleared(selection.wrappedValue)
                return .handled
            }
        }
    }

    private func selectAll(_ selection: Binding<Set<String>>) -> some View {
        let ids = MonarchFileListTokens.selectableIDs(items)
        return Toggle(sources: ids.map { binding(for: $0, in: selection) }, isOn: \.self) {
            Text(labels.selectAll ?? MonarchFileListStrings.selectAll)
        }
        .toggleStyle(.monarchCheckbox)
        .disabled(ids.isEmpty)
    }

    @ViewBuilder private var files: some View {
        switch layout {
        case .list:
            LazyVStack(alignment: .leading, spacing: MonarchFileListTokens.spacing) {
                ForEach(items) { card($0, layout: .row) }
            }
        case .grid:
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: minimumCardWidth), spacing: MonarchFileListTokens.spacing)],
                alignment: .leading,
                spacing: MonarchFileListTokens.spacing
            ) {
                ForEach(items) { card($0, layout: .card) }
            }
        }
    }

    private func card(_ item: MonarchFileListItem, layout: MonarchFileCardLayout) -> some View {
        let open = onOpen.map { onOpen in { @MainActor in onOpen(item) } }
        return MonarchFileCard(
            item.name,
            isSelected: selection.map { binding(for: item.id, in: $0) },
            size: item.size,
            contentType: item.contentType,
            meta: item.meta,
            image: item.thumbnail,
            url: item.thumbnail == nil ? item.thumbnailURL : nil,
            badges: item.badges,
            layout: layout,
            labels: labels.card,
            onOpen: open,
            menu: menu(item),
            hasMenu: hasMenu
        )
        .disabled(item.isDisabled)
    }

    private func binding(for id: String, in selection: Binding<Set<String>>) -> Binding<Bool> {
        Binding(
            get: { selection.wrappedValue.contains(id) },
            set: { selection.wrappedValue = MonarchFileListTokens.setting(id, isOn: $0, in: selection.wrappedValue) }
        )
    }
}

extension MonarchFileList where MenuItems == EmptyView {
    /// A file list without a menu on each file.
    ///
    /// - Parameters:
    ///   - items: The files, in order.
    ///   - selection: The ids of the selected files. Adds checkboxes, select all and the bulk action bar.
    ///   - layout: Rows, or cards in a grid.
    ///   - label: The spoken name of the list, such as "Project files".
    ///   - bulkActions: The actions for the selected files, shown in the bar while any file is selected.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens a file. Makes each file a button.
    public init(
        _ items: [MonarchFileListItem],
        selection: Binding<Set<String>>? = nil,
        layout: MonarchFileListLayout = .list,
        label: String? = nil,
        bulkActions: [MonarchBulkAction] = [],
        labels: MonarchFileListLabels = MonarchFileListLabels(),
        onOpen: (@MainActor (MonarchFileListItem) -> Void)? = nil
    ) {
        self.init(
            items: items,
            selection: selection,
            layout: layout,
            label: label,
            bulkActions: bulkActions,
            labels: labels,
            onOpen: onOpen,
            menu: { _ in EmptyView() },
            hasMenu: false
        )
    }
}

#if DEBUG
    #Preview("MonarchFileList") {
        let files = [
            MonarchFileListItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, meta: "Edited yesterday"),
            MonarchFileListItem(id: "2", name: "Budget 2027.xlsx", size: 48_000, badges: ["Shared"]),
            MonarchFileListItem(id: "3", name: "Locked.zip", size: 9_800_000, isDisabled: true),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFileList(
                    files,
                    selection: .constant(["1"]),
                    onOpen: { _ in },
                    menu: { _ in Button("Rename") {} }
                )
                MonarchFileList(files, layout: .grid)
                MonarchFileList([])
            }
        }
    }
#endif
