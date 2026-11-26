import MonarchTokens
import SwiftUI

/// How a column's card count stands against its work-in-progress limit.
enum MonarchKanbanBoardLimitState: Hashable, Sendable {
    /// The column has no limit.
    case none
    /// Fewer cards than the limit.
    case under
    /// As many cards as the limit.
    case at
    /// More cards than the limit.
    case over
}

/// Component tokens and colour decisions for ``MonarchKanbanBoard``.
enum MonarchKanbanBoardTokens {
    /// The narrowest a column gets, before Dynamic Type scaling: one column per page on a phone.
    static let minimumColumnWidth: CGFloat = MonarchTokens.Space.space24 * 3
    /// The gap between columns, and the board's leading and trailing inset.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding inside a column.
    static let columnPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between cards in a column.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a header or a card row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the column tabs and the columns.
    static let tabsSpacing: CGFloat = MonarchTokens.Space.space2
    /// The leading and trailing padding of a column tab.
    static let tabPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of a column.
    static let columnRadius: CGFloat = MonarchTokens.Radius.lg
    /// The width of a column's border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The width of the drop-target border and the insertion line: twice the hairline.
    static let targetWidth: CGFloat = MonarchShapes.borderWidth * 2
    /// The height of the drop zone at the end of a column, before Dynamic Type scaling.
    static let endZoneHeight: CGFloat = MonarchTokens.Space.space20
    /// The symbol of a card's Move menu.
    static let moveSymbol = "arrow.up.arrow.down"
    /// The symbol shown with the count at or over the limit, so the state is not only a colour.
    static let limitSymbol = "exclamationmark.triangle.fill"

    /// The fill of a column.
    static var columnFill: MonarchColorRole { \.bgSubtle }
    /// The role of quiet text: counts, the empty text and unselected tabs.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of the insertion line and the selected tab.
    static var accent: MonarchColorRole { \.accentStrong }

    /// The border of a column: the accent while a card is dragged over it.
    static func columnBorder(isTargeted: Bool) -> MonarchColorRole {
        isTargeted ? \.accentStrong : \.borderSubtle
    }

    /// The width of a column's border.
    static func columnBorderWidth(isTargeted: Bool) -> CGFloat {
        isTargeted ? targetWidth : borderWidth
    }

    /// The role of a tab: the accent when it shows the column in view.
    static func tab(isSelected: Bool) -> MonarchColorRole {
        isSelected ? \.accentStrong : \.textMuted
    }

    /// How a count stands against a limit.
    static func limitState(count: Int, limit: Int?) -> MonarchKanbanBoardLimitState {
        guard let limit else { return .none }
        if count > limit { return .over }
        return count == limit ? .at : .under
    }

    /// The role of the count: muted, the strong warning colour at the limit, the strong danger
    /// colour over it.
    static func countRole(_ state: MonarchKanbanBoardLimitState) -> MonarchColorRole {
        switch state {
        case .none, .under: \.textMuted
        case .at: \.warningStrong
        case .over: \.dangerStrong
        }
    }

    /// The fill of the count's pill: subtle warning at the limit, subtle danger over it.
    static func countFill(_ state: MonarchKanbanBoardLimitState) -> MonarchColorRole {
        switch state {
        case .none, .under: \.bgMuted
        case .at: \.warningSubtle
        case .over: \.dangerSubtle
        }
    }

    /// Whether the count shows the limit symbol.
    static func showsLimitSymbol(_ state: MonarchKanbanBoardLimitState) -> Bool {
        state == .at || state == .over
    }
}
