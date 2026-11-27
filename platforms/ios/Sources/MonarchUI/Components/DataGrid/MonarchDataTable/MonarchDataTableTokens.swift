import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDataTable``.
enum MonarchDataTableTokens {
    /// The gap between the lines of a compact row and between bar items.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a field's name and its value in a compact row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The ideal width of the selection column, before Dynamic Type scaling.
    static let selectionWidth: CGFloat = MonarchTokens.Control.xl
    /// The ideal width of the actions column.
    static let actionsWidth: CGFloat = MonarchTokens.Control.xl
    /// The number of placeholder rows while the first rows load.
    static let loadingRows = 5
    /// The actions menu symbol.
    static let actionsSymbol = "ellipsis"
    /// The symbol of an ascending sort in the sort menu.
    static let ascendingSymbol = "chevron.up"
    /// The symbol of a descending sort in the sort menu.
    static let descendingSymbol = "chevron.down"
    /// The sort menu symbol.
    static let sortSymbol = "arrow.up.arrow.down"
    /// The text drawn for an empty cell; VoiceOver reads the no-value label instead.
    static let emptyCell = "—"
    /// The colour of field names, counts and empty cells.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of cell text.
    static var textRole: MonarchColorRole { \.textDefault }
    /// The colour of a row title that opens the row.
    static var linkRole: MonarchColorRole { \.textLink }
    /// The weight of a row's name in the list layout.
    static let nameWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The insets of a list row: none at the sides, so the rows share the
    /// toolbar's left edge, and 8 points above and below.
    static let listRowInsets = EdgeInsets(
        top: MonarchTokens.Space.space2,
        leading: 0,
        bottom: MonarchTokens.Space.space2,
        trailing: 0
    )
    /// The colour of a destructive action.
    static var destructiveRole: MonarchColorRole { \.dangerStrong }

    // MARK: Layout

    /// The layout used: a table only at regular width, below the accessibility
    /// text sizes, and when the system has dynamic table columns.
    static func resolvedLayout(
        _ layout: MonarchDataTableLayout,
        isRegularWidth: Bool,
        isAccessibilitySize: Bool,
        supportsTable: Bool
    ) -> MonarchDataTableLayout {
        guard supportsTable else { return .list }
        switch layout {
        case .automatic: return isRegularWidth && !isAccessibilitySize ? .table : .list
        case .table, .list: return layout
        }
    }

    /// What the table shows: with no rows a failure wins, then loading, then
    /// the filtered empty state, then the empty state.
    enum Phase: Equatable {
        /// The rows.
        case ready
        /// The first rows are loading.
        case loading
        /// The rows failed to load.
        case failed
        /// Filters match nothing.
        case filtered
        /// There are no rows.
        case empty
    }

    /// The phase from the inputs.
    static func phase(rowCount: Int, isLoading: Bool, errorMessage: String?, isFiltered: Bool) -> Phase {
        guard rowCount == 0 else { return .ready }
        if errorMessage != nil { return .failed }
        if isLoading { return .loading }
        return isFiltered ? .filtered : .empty
    }

    // MARK: Columns

    /// The columns shown, in order: `order` first, then the rest; hidden
    /// columns are dropped unless they cannot be hidden.
    static func visibleColumns<Row>(
        _ columns: [MonarchDataTableColumn<Row>],
        hidden: Set<String>,
        order: [String]
    ) -> [MonarchDataTableColumn<Row>] {
        let byID = Dictionary(columns.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        return MonarchColumnChooserTokens.ordered(columns.map(\.chooserColumn), order: order)
            .filter { MonarchColumnChooserTokens.isShown($0, hidden: hidden) }
            .compactMap { byID[$0.id] }
    }

    // MARK: Sorting

    /// Whether `lhs` sorts before `rhs` ascending by a column: its own rule,
    /// or the cell texts compared as Finder does.
    static func ascends<Row>(_ lhs: Row, _ rhs: Row, by column: MonarchDataTableColumn<Row>) -> Bool {
        if let sortBy = column.sortBy { return sortBy(lhs, rhs) }
        return column.value(lhs).localizedStandardCompare(column.value(rhs)) == .orderedAscending
    }

    /// The rows sorted by the sort's column, stable for equal rows. Rows stay
    /// in their order when there is no sort, the column is unknown or not sortable.
    static func sorted<Row>(
        _ rows: [Row],
        by sort: MonarchDataTableSort?,
        columns: [MonarchDataTableColumn<Row>]
    ) -> [Row] {
        guard let sort, let column = columns.first(where: { $0.id == sort.columnID }), column.isSortable else {
            return rows
        }
        return rows.enumerated()
            .sorted { lhs, rhs in
                let (first, second) = sort.order == .forward ? (lhs.element, rhs.element) : (rhs.element, lhs.element)
                if ascends(first, second, by: column) { return true }
                if ascends(second, first, by: column) { return false }
                return lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    /// The sort after a column is chosen: the same column flips its order,
    /// another column sorts ascending. `nil` for a column that cannot sort.
    static func toggledSort<Row>(
        _ current: MonarchDataTableSort?,
        columnID: String,
        columns: [MonarchDataTableColumn<Row>]
    ) -> MonarchDataTableSort? {
        guard columns.contains(where: { $0.id == columnID && $0.isSortable }) else { return current }
        if let current, current.columnID == columnID {
            return MonarchDataTableSort(columnID, order: current.order == .forward ? .reverse : .forward)
        }
        return MonarchDataTableSort(columnID)
    }

    /// The sort the table's header asks for, from its first comparator.
    /// Requests for columns that cannot sort keep the current sort.
    static func sort<Row>(
        fromHeader request: MonarchDataTableSort?,
        current: MonarchDataTableSort?,
        columns: [MonarchDataTableColumn<Row>]
    ) -> MonarchDataTableSort? {
        guard let request else { return nil }
        return columns.contains(where: { $0.id == request.columnID && $0.isSortable }) ? request : current
    }

    /// The spoken sort state of a column, or `nil` when it is not the sorted one.
    static func sortState(columnID: String, sort: MonarchDataTableSort?, labels: MonarchDataTableLabels) -> String? {
        guard let sort, sort.columnID == columnID else { return nil }
        return sort.order == .forward
            ? labels.sortedAscending ?? MonarchDataTableStrings.sortedAscending
            : labels.sortedDescending ?? MonarchDataTableStrings.sortedDescending
    }

    // MARK: Selection

    /// The ids that can be selected.
    static func selectableIDs<Row: Identifiable>(_ rows: [Row], isRowSelectable: ((Row) -> Bool)?) -> [Row.ID] {
        rows.filter { isRowSelectable?($0) ?? true }.map(\.id)
    }

    /// Whether every selectable row is selected (and there is at least one).
    static func isAllSelected<ID: Hashable>(_ selection: Set<ID>, selectable: [ID]) -> Bool {
        !selectable.isEmpty && selectable.allSatisfy(selection.contains)
    }

    /// The selection after the select-all checkbox: adds or removes every selectable row,
    /// keeping selected rows that are not on screen.
    static func selectingAll<ID: Hashable>(_ selection: Set<ID>, selectable: [ID], isOn: Bool) -> Set<ID> {
        isOn ? selection.union(selectable) : selection.subtracting(selectable)
    }

    /// The selection after a row's checkbox.
    static func selecting<ID: Hashable>(_ selection: Set<ID>, id: ID, isOn: Bool) -> Set<ID> {
        var result = selection
        if isOn {
            result.insert(id)
        } else {
            result.remove(id)
        }
        return result
    }

    // MARK: Text

    /// A row's name: the app's, or the first shown column's text.
    static func rowName<Row>(_ row: Row, columns: [MonarchDataTableColumn<Row>], rowLabel: ((Row) -> String)?) -> String
    {
        rowLabel?(row) ?? columns.first.map { $0.value(row) } ?? ""
    }

    /// What VoiceOver reads for a compact row: its name, then "Field: value"
    /// for every other shown column, "No value" for empty cells.
    static func rowSummary<Row>(
        _ row: Row,
        name: String,
        columns: [MonarchDataTableColumn<Row>],
        labels: MonarchDataTableLabels
    ) -> String {
        let noValue = labels.noValue ?? MonarchDataTableStrings.noValue
        let fields = columns.dropFirst().map { column in
            let value = column.value(row)
            return "\(column.title): \(value.isEmpty ? noValue : value)"
        }
        return ([name] + fields).joined(separator: ", ")
    }

    /// The spoken text of one cell: the value, or "No value".
    static func cellLabel(_ value: String, labels: MonarchDataTableLabels) -> String {
        value.isEmpty ? labels.noValue ?? MonarchDataTableStrings.noValue : value
    }

    /// The visible text of one cell: the value, or a dash.
    static func cellText(_ value: String, labels: MonarchDataTableLabels = MonarchDataTableLabels()) -> String {
        value.isEmpty ? labels.emptyCell ?? emptyCell : value
    }

    /// The symbol before the sorted column in the sort menu.
    static func sortMark(_ order: SortOrder, labels: MonarchDataTableLabels = MonarchDataTableLabels()) -> String {
        order == .forward ? labels.ascendingSymbol ?? ascendingSymbol : labels.descendingSymbol ?? descendingSymbol
    }

    /// The selected count.
    static func selectedCountText(_ count: Int, labels: MonarchDataTableLabels) -> String {
        labels.selectedCount?(count) ?? MonarchDataTableStrings.selectedCount(count)
    }
}
