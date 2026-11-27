import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSortableGrid``.
enum MonarchSortableGridTokens {
    /// The gap between tiles, in both directions.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The smallest tile width when the number of columns is not fixed,
    /// before Dynamic Type scaling: two columns on a phone in portrait.
    static let minimumItemWidth: CGFloat = MonarchTokens.Space.space20 * 2
    /// The inset of the drag handle from the tile's top trailing corner.
    static let handleInset: CGFloat = MonarchTokens.Space.space1
    /// The colour of the outline around the tile a drag is over.
    static var dropTargetRole: MonarchColorRole { \.accentStrong }
    /// The width of that outline: twice the hairline, so it is seen in every theme.
    static let dropTargetWidth: CGFloat = MonarchShapes.borderWidth * 2

    /// The number of columns: the fixed number, or as many tiles of the
    /// minimum width as fit. Always at least one.
    static func columnCount(width: CGFloat, columns: Int?, minimumItemWidth: CGFloat, spacing: CGFloat) -> Int {
        if let columns { return max(1, columns) }
        guard width > 0, minimumItemWidth > 0 else { return 1 }
        return max(1, Int(((width + spacing) / (minimumItemWidth + spacing)).rounded(.down)))
    }

    /// Moves one element, as the web's `moveItem`: removed at `from`,
    /// inserted at `to`. Out-of-range indices leave the list unchanged.
    static func move<Element>(_ list: [Element], from: Int, to: Int) -> [Element] {
        guard list.indices.contains(from), list.indices.contains(to), from != to else { return list }
        var result = list
        let element = result.remove(at: from)
        result.insert(element, at: to)
        return result
    }

    /// Where an accessibility action moves the tile at `index`: backward and
    /// forward one place in reading order, up and down one row. `nil` when
    /// the move would leave the grid.
    static func target(from index: Int, direction: MonarchDragHandleDirection, columns: Int, count: Int) -> Int? {
        let step =
            switch direction {
            case .backward: -1
            case .forward: 1
            case .up: -max(1, columns)
            case .down: max(1, columns)
            }
        let target = index + step
        return (0..<count).contains(target) ? target : nil
    }

    /// The axis of the handles: up and down only in one column, all four
    /// directions otherwise.
    static func axis(columns: Int) -> MonarchDragHandleAxis {
        columns <= 1 ? .vertical : .both
    }

    /// The drag payload of a tile: the grid's own namespace and the index,
    /// so drops from another grid or another app are ignored.
    static func payload(namespace: String, index: Int) -> String {
        "\(namespace)#\(index)"
    }

    /// The index in a payload from this grid, or `nil`.
    static func index(payload: String, namespace: String, count: Int) -> Int? {
        let prefix = "\(namespace)#"
        guard payload.hasPrefix(prefix), let index = Int(payload.dropFirst(prefix.count)) else { return nil }
        return (0..<count).contains(index) ? index : nil
    }

    /// The spoken name of a handle.
    static func handleLabel(_ item: String, labels: MonarchSortableGridLabels) -> String {
        labels.handle?(item) ?? MonarchSortableGridStrings.handle(item)
    }

    /// The announcement after an accessibility move, positions counted from 1.
    static func movedAnnouncement(_ item: String, to index: Int, count: Int, labels: MonarchSortableGridLabels)
        -> String
    {
        labels.moved?(item, index + 1, count) ?? MonarchSortableGridStrings.moved(item, index + 1, count)
    }

    /// The announcement after a drop, positions counted from 1.
    static func droppedAnnouncement(_ item: String, to index: Int, count: Int, labels: MonarchSortableGridLabels)
        -> String
    {
        labels.dropped?(item, index + 1, count) ?? MonarchSortableGridStrings.dropped(item, index + 1, count)
    }
}
