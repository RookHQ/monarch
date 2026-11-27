import MonarchTokens
import SwiftUI

/// The position of a cell, counted from 0.
struct MonarchTableBlockCell: Hashable, Sendable {
    let row: Int
    let column: Int
}

/// Component tokens for ``MonarchTableBlock``.
enum MonarchTableBlockTokens {
    /// The least width of a column of a read-only table, before Dynamic Type.
    static let minColumnWidth: CGFloat = MonarchTokens.Space.space24
    /// The width at which the text of a read-only column wraps, before Dynamic Type.
    static let maxColumnWidth: CGFloat = MonarchTokens.Space.space24 * 2.5
    /// The width of every column of an editable table, before Dynamic Type.
    static let editableColumnWidth: CGFloat = MonarchTokens.Space.space24 * 1.5
    /// Padding before and after the text of a cell.
    static let cellHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the text of a cell.
    static let cellVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The least height of an editable cell, which is its touch target.
    static let editableCellMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The gap between the controls, the caption and the table.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol on the controls menu.
    static let controlsSymbol = "tablecells"

    /// The lines between cells and around the table.
    static var line: MonarchColorRole { \.borderDefault }
    /// The colour of the caption.
    static var caption: MonarchColorRole { \.textMuted }

    /// The fill of a cell.
    static func background(isHeader: Bool) -> MonarchColorRole {
        isHeader ? \.bgSubtle : \.bgSurface
    }

    /// The colour of the text of a cell.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The type role of a cell: header cells are stronger, so the header
    /// does not depend on its fill.
    static func textStyle(isHeader: Bool) -> MonarchTextStyle {
        isHeader ? .bodyStrong : .body
    }

    /// The spoken name of the table: the label, else the caption, else the
    /// built-in "Table".
    static func regionLabel(label: String?, caption: String?) -> String {
        if let label, !label.isEmpty { return label }
        if let caption, !caption.isEmpty { return caption }
        return MonarchTableBlockStrings.region
    }

    /// Whether the visible caption is hidden from VoiceOver because it is
    /// already the spoken name of the table.
    static func captionIsSpokenName(label: String?) -> Bool {
        label?.isEmpty ?? true
    }

    /// The spoken position of a cell, counted from 1.
    static func position(of cell: MonarchTableBlockCell, labels: MonarchTableBlockLabels) -> String {
        if let custom = labels.cellPosition { return custom(cell.row + 1, cell.column + 1) }
        return MonarchTableBlockStrings.cellPosition(row: cell.row + 1, column: cell.column + 1)
    }

    /// What VoiceOver reads for a read-only cell: its text, or "Empty".
    static func cellLabel(text: String, labels: MonarchTableBlockLabels) -> String {
        if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return text }
        return labels.emptyCell ?? MonarchTableBlockStrings.emptyCell
    }

    /// The cell the controls act on: the cell that last had focus if it is
    /// still inside the table, otherwise the last cell.
    static func target(active: MonarchTableBlockCell?, in data: MonarchTableBlockData) -> MonarchTableBlockCell {
        let last = MonarchTableBlockCell(row: data.rowCount - 1, column: data.columnCount - 1)
        guard let active, active.row >= 0, active.column >= 0 else { return last }
        return MonarchTableBlockCell(row: min(active.row, last.row), column: min(active.column, last.column))
    }
}
