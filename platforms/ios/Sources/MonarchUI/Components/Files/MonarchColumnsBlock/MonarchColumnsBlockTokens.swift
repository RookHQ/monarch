import CoreGraphics
import MonarchTokens

/// Component tokens and the layout rules of ``MonarchColumnsBlock``.
enum MonarchColumnsBlockTokens {
    /// The gap between columns side by side.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between stacked columns.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// Below this width the columns stack: 480 points, the web's 30rem.
    static let stackWidth: CGFloat = MonarchTokens.Space.space24 * 5
    /// The smallest share of the width for the first of two columns.
    static let minimumRatio: Double = 0.2
    /// The largest share of the width for the first of two columns.
    static let maximumRatio: Double = 0.8
    /// The share used when the ratio is not a number.
    static let defaultRatio: Double = 0.5

    /// Keeps a ratio inside 0.2 to 0.8, and 0.5 when it is not finite.
    static func clampRatio(_ ratio: Double) -> Double {
        guard ratio.isFinite else { return defaultRatio }
        return min(maximumRatio, max(minimumRatio, ratio))
    }

    /// Whether the columns stack: in a narrow container, or when asked.
    static func isStacked(width: CGFloat, forcesStack: Bool) -> Bool {
        forcesStack || width < stackWidth
    }

    /// The leading offset and width of each column side by side. Two columns
    /// split the width by `ratio`; three or more are equal. Offsets run from
    /// the leading edge; SwiftUI mirrors them in a right-to-left layout.
    static func columns(count: Int, width: CGFloat, ratio: Double) -> [(x: CGFloat, width: CGFloat)] {
        guard count > 0 else { return [] }
        let available = max(0, width - columnSpacing * CGFloat(count - 1))
        let widths: [CGFloat]
        if count == 2 {
            let first = available * CGFloat(clampRatio(ratio))
            widths = [first, available - first]
        } else {
            widths = Array(repeating: available / CGFloat(count), count: count)
        }
        var x: CGFloat = 0
        return widths.map { columnWidth in
            defer { x += columnWidth + columnSpacing }
            return (x, columnWidth)
        }
    }
}
