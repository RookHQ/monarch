import Foundation

/// Everything the grid and day-page layouts of ``MonarchTimesheetGrid``
/// show, in one value, with the state of the cells being saved.
struct MonarchTimesheetGridModel {
    let days: [MonarchTimesheetGridDay]
    let rows: [MonarchTimesheetGridRow]
    let summaryRows: [MonarchTimesheetGridSummaryRow]
    let maximum: Int
    let isEditable: Bool
    let emptyMessage: String?
    let labels: MonarchTimesheetGridLabels
    let errors: [MonarchTimesheetGridCellKey: String]
    let saving: Set<MonarchTimesheetGridCellKey>
    let rowActions: ((MonarchTimesheetGridRow) -> [MonarchDataTableRowAction])?
    let onDayActivate: (@MainActor (Date) -> Void)?
    let onCellActivate: (@MainActor (String, Date) -> Void)?
    /// Saves a value typed in a cell.
    let commit: @MainActor (MonarchTimesheetGridCellKey, Int) -> Void
    /// Records or clears the problem of a cell's text.
    let report: @MainActor (MonarchTimesheetGridCellKey, String?) -> Void

    typealias Tokens = MonarchTimesheetGridTokens

    func isCellEditable(_ row: MonarchTimesheetGridRow, _ day: MonarchTimesheetGridDay) -> Bool {
        Tokens.isCellEditable(isEditable: isEditable, row: row, day: day)
    }

    func actions(for row: MonarchTimesheetGridRow) -> [MonarchDataTableRowAction] {
        rowActions?(row) ?? []
    }

    var emptyText: String {
        emptyMessage ?? MonarchTimesheetGridStrings.empty(labels)
    }
}
