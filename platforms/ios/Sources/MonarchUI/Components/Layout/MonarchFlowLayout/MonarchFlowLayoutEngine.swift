import CoreGraphics
import MonarchTokens

/// The arithmetic of ``MonarchFlowLayout``, free of SwiftUI so it is tested
/// with plain sizes. Coordinates run from the leading edge; SwiftUI mirrors
/// the placement in a right-to-left layout.
enum MonarchFlowLayoutEngine {
    /// One child: its size and the distance from its top to its alignment guide.
    struct Item: Equatable {
        let size: CGSize
        let guide: CGFloat
    }

    /// Where every child goes, and the size of the whole layout.
    struct Arrangement: Equatable {
        let frames: [CGRect]
        let size: CGSize
    }

    /// The space between children when none is given: 8 points.
    static let defaultSpacing: CGFloat = MonarchTokens.Space.space2
    /// Slack when deciding whether a child fits, so a width that came from
    /// this layout's own measurement does not wrap because of rounding.
    static let tolerance: CGFloat = 0.001

    /// The rows as index ranges. A row breaks before a child that would end
    /// past `width`; a row always has at least one child.
    static func rows(_ widths: [CGFloat], width: CGFloat, spacing: CGFloat) -> [Range<Int>] {
        var rows: [Range<Int>] = []
        var start = 0
        var used: CGFloat = 0
        for (index, itemWidth) in widths.enumerated() {
            if index > start, used + spacing + itemWidth > width + tolerance {
                rows.append(start..<index)
                start = index
                used = itemWidth
            } else {
                used += (index > start ? spacing : 0) + itemWidth
            }
        }
        if start < widths.count {
            rows.append(start..<widths.count)
        }
        return rows
    }

    /// Lays the items out in rows within `width`, which may be infinite.
    static func arrange(
        _ items: [Item],
        width: CGFloat,
        spacing: CGFloat,
        rowSpacing: CGFloat,
        justification: MonarchFlowLayoutJustification
    ) -> Arrangement {
        let rows = rows(items.map(\.size.width), width: width, spacing: spacing)
        let rowWidths = rows.map { row in
            items[row].reduce(0) { $0 + $1.size.width } + spacing * CGFloat(row.count - 1)
        }
        let widest = rowWidths.max() ?? 0
        // A justified layout fills the width it is offered; otherwise it hugs its rows.
        let container = justification != .leading && width.isFinite ? max(width, widest) : widest

        var frames = [CGRect](repeating: .zero, count: items.count)
        var y: CGFloat = 0
        for (row, rowWidth) in zip(rows, rowWidths) {
            let ascent = items[row].map(\.guide).max() ?? 0
            let descent = items[row].map { $0.size.height - $0.guide }.max() ?? 0
            let free = max(0, container - rowWidth)
            var x: CGFloat = 0
            var gap = spacing
            switch justification {
            case .leading: break
            case .center: x = free / 2
            case .trailing: x = free
            case .spaceBetween:
                if row.count > 1 {
                    gap += free / CGFloat(row.count - 1)
                }
            }
            for index in row {
                let item = items[index]
                frames[index] = CGRect(origin: CGPoint(x: x, y: y + ascent - item.guide), size: item.size)
                x += item.size.width + gap
            }
            y += ascent + descent + rowSpacing
        }
        let height = rows.isEmpty ? 0 : y - rowSpacing
        return Arrangement(frames: frames, size: CGSize(width: container, height: height))
    }
}
