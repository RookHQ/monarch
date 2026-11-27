import MonarchTokens
import SwiftUI

/// A panel to show, hide, find and reorder the columns of a table, for a
/// sheet, a settings screen or a saved-view editor.
///
/// ```swift
/// @State private var hidden: Set<String> = []
/// @State private var order: [String] = []
///
/// MonarchColumnChooser(
///     columns.map(\.chooserColumn),
///     hiddenColumns: $hidden,
///     order: $order
/// )
/// ```
///
/// It takes the same state as ``MonarchDataTable`` (`hiddenColumns` and
/// `columnOrder`), so the table follows it. The panel is a `List`: each
/// column is a standard `Toggle`, and with `order` the rows reorder with
/// `onMove` (drag a row, or use the reorder controls in edit mode). At
/// `searchThreshold` columns and more a field finds columns by name; while it
/// filters, reordering is off. Columns that cannot be hidden are always on
/// and say so, and the last `minimumVisible` columns cannot be switched off.
///
/// Accessibility: the title is a header read with the count ("Columns, 5 of 8
/// shown"). Each switch reads the column's name, its note and its state. With
/// `order`, each row has the accessibility actions "Move Status up" and "Move
/// Status down", and every move is announced ("Status moved to position 3 of
/// 8.").
public struct MonarchColumnChooser: View {
    private let columns: [MonarchColumnChooserColumn]
    @Binding private var hiddenColumns: Set<String>
    private let order: Binding<[String]>?
    private let searchThreshold: Int
    private let minimumVisible: Int
    private let labels: MonarchColumnChooserLabels
    private let onReset: (@MainActor () -> Void)?

    @State private var query = ""
    @FocusState private var isSearchFocused: Bool
    @Environment(\.monarchTheme) private var theme

    /// A column chooser.
    ///
    /// - Parameters:
    ///   - columns: The columns, in their default order.
    ///   - hiddenColumns: The ids of hidden columns. Ids of columns that cannot be hidden are ignored.
    ///   - order: The column ids in display order. With it, columns can be reordered.
    ///     Ids missing from it follow in their default order.
    ///   - searchThreshold: Offers the search field from this many columns.
    ///   - minimumVisible: The fewest columns that must stay shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - onReset: Adds a Reset button that runs this, to restore the app's default columns.
    public init(
        _ columns: [MonarchColumnChooserColumn],
        hiddenColumns: Binding<Set<String>>,
        order: Binding<[String]>? = nil,
        searchThreshold: Int = 8,
        minimumVisible: Int = 1,
        labels: MonarchColumnChooserLabels = MonarchColumnChooserLabels(),
        onReset: (@MainActor () -> Void)? = nil
    ) {
        self.columns = columns
        _hiddenColumns = hiddenColumns
        self.order = order
        self.searchThreshold = searchThreshold
        self.minimumVisible = minimumVisible
        self.labels = labels
        self.onReset = onReset
    }

    private typealias Tokens = MonarchColumnChooserTokens

    /// The content of the view.
    public var body: some View {
        let ordered = Tokens.ordered(columns, order: order?.wrappedValue)
        let shown = Tokens.shownCount(columns, hidden: hiddenColumns)
        let isSearchable = Tokens.isSearchable(columnCount: columns.count, threshold: searchThreshold)
        let visible = Tokens.filtered(ordered, query: query, isSearchable: isSearchable)
        let isFiltering = Tokens.isFiltering(query: query, isSearchable: isSearchable)
        let canMove = order != nil && !isFiltering
        List {
            Group {
                Section {
                    if isSearchable {
                        searchField
                    }
                    if visible.isEmpty {
                        Text(labels.noMatches ?? MonarchColumnChooserStrings.noMatches)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                    }
                    ForEach(visible) { column in
                        row(column, shown: shown, ordered: ordered, canMove: canMove)
                    }
                    .onMove(perform: canMove ? { move(fromOffsets: $0, toOffset: $1, ordered: ordered) } : nil)
                } header: {
                    header(shown: shown)
                } footer: {
                    footer(shown: shown)
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    // MARK: Parts

    private func header(shown: Int) -> some View {
        let title = labels.title ?? MonarchColumnChooserStrings.title
        let count = Tokens.countText(shown: shown, total: columns.count, labels: labels)
        return VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: Tokens.labelRole])
            Text(count)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedRole])
        }
        .textCase(nil)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title), \(count)")
        .accessibilityAddTraits(.isHeader)
    }

    private var searchField: some View {
        let prompt = labels.search ?? MonarchColumnChooserStrings.search
        return HStack(spacing: Tokens.spacing) {
            MonarchIcon(systemName: Tokens.searchSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: Tokens.mutedRole])
            TextField(prompt, text: $query)
                .textFieldStyle(.plain)
                .monarchFieldText()
                .focused($isSearchFocused)
                .autocorrectionDisabled()
                .submitLabel(.search)
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    MonarchIcon(
                        systemName: Tokens.clearSymbol,
                        label: MonarchColumnChooserStrings.clearSearch,
                        size: .sm
                    )
                    .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .padding(.vertical, -MonarchTextFieldTokens.verticalPadding)
            }
        }
        // The Monarch search box: the field chrome with the magnifier and the
        // clear button inside it, as the web search input.
        .monarchFieldBox(hasError: false, isFocused: isSearchFocused)
        .accessibilityElement(children: .contain)
    }

    private func row(
        _ column: MonarchColumnChooserColumn,
        shown: Int,
        ordered: [MonarchColumnChooserColumn],
        canMove: Bool
    ) -> some View {
        let ids = ordered.map(\.id)
        let isOn = Binding(
            get: { Tokens.isShown(column, hidden: hiddenColumns) },
            set: { hiddenColumns = Tokens.setting(column.id, isShown: $0, in: hiddenColumns) }
        )
        let toggle = Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 0) {
                Text(column.label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.labelRole])
                if !column.isHideable {
                    Text(labels.required ?? MonarchColumnChooserStrings.required)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                }
            }
        }
        .toggleStyle(.monarchCheckbox)
        .disabled(
            Tokens.isToggleDisabled(column, hidden: hiddenColumns, shownCount: shown, minimumVisible: minimumVisible)
        )
        return HStack(spacing: Tokens.spacing) {
            toggle
                .frame(maxWidth: .infinity, alignment: .leading)
            if order != nil {
                // As on the web: an up and a down arrow on every row, disabled at the ends.
                Button {
                    move(column, by: -1, ids: ids)
                } label: {
                    MonarchIcon(
                        systemName: "chevron.up",
                        label: labels.moveUp?(column.label) ?? MonarchColumnChooserStrings.moveUp(column.label),
                        size: .sm
                    )
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .disabled(!canMove || Tokens.moving(column.id, by: -1, in: ids) == nil)
                Button {
                    move(column, by: 1, ids: ids)
                } label: {
                    MonarchIcon(
                        systemName: "chevron.down",
                        label: labels.moveDown?(column.label) ?? MonarchColumnChooserStrings.moveDown(column.label),
                        size: .sm
                    )
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .disabled(!canMove || Tokens.moving(column.id, by: 1, in: ids) == nil)
            }
        }
        .accessibilityActions {
            if canMove {
                if Tokens.moving(column.id, by: -1, in: ids) != nil {
                    Button(labels.moveUp?(column.label) ?? MonarchColumnChooserStrings.moveUp(column.label)) {
                        move(column, by: -1, ids: ids)
                    }
                }
                if Tokens.moving(column.id, by: 1, in: ids) != nil {
                    Button(labels.moveDown?(column.label) ?? MonarchColumnChooserStrings.moveDown(column.label)) {
                        move(column, by: 1, ids: ids)
                    }
                }
            }
        }
    }

    @ViewBuilder private func footer(shown: Int) -> some View {
        let showsShowAll = Tokens.showsShowAll(shown: shown, total: columns.count)
        if showsShowAll || onReset != nil {
            HStack(spacing: Tokens.spacing) {
                if showsShowAll {
                    MonarchButton(labels.showAll ?? MonarchColumnChooserStrings.showAll, variant: .ghost, size: .sm) {
                        hiddenColumns = []
                    }
                }
                if let onReset {
                    MonarchButton(
                        labels.reset ?? MonarchColumnChooserStrings.reset,
                        variant: .ghost,
                        size: .sm,
                        action: onReset
                    )
                }
            }
            .textCase(nil)
        }
    }

    // MARK: Moves

    private func move(_ column: MonarchColumnChooserColumn, by step: Int, ids: [String]) {
        guard let next = Tokens.moving(column.id, by: step, in: ids), let index = next.firstIndex(of: column.id)
        else { return }
        order?.wrappedValue = next
        announce(column.label, at: index, count: next.count)
    }

    private func move(fromOffsets source: IndexSet, toOffset destination: Int, ordered: [MonarchColumnChooserColumn]) {
        let ids = ordered.map(\.id)
        let next = Tokens.moving(fromOffsets: source, toOffset: destination, in: ids)
        order?.wrappedValue = next
        if source.count == 1, let first = source.first, let index = next.firstIndex(of: ids[first]) {
            announce(ordered[first].label, at: index, count: next.count)
        }
    }

    private func announce(_ label: String, at index: Int, count: Int) {
        AccessibilityNotification.Announcement(
            Tokens.movedAnnouncement(label, to: index, count: count, labels: labels)
        )
        .post()
    }
}

#if DEBUG
    #Preview("MonarchColumnChooser") {
        MonarchPreviewMatrix {
            MonarchColumnChooser(
                [
                    MonarchColumnChooserColumn("Name", isHideable: false),
                    MonarchColumnChooserColumn("Status"),
                    MonarchColumnChooserColumn("Owner"),
                    MonarchColumnChooserColumn("Updated"),
                ],
                hiddenColumns: .constant(["Owner"]),
                order: .constant([]),
                onReset: {}
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        }
    }
#endif
