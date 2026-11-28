import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchTableOfContents``.
enum MonarchTableOfContentsTokens {
    /// The type role of an entry.
    static let textStyle: MonarchTextStyle = .label
    /// The type role of the title.
    static let titleStyle: MonarchTextStyle = .caption
    /// The weight of the title and of the current entry.
    static let emphasisWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The gap between the title and the entries.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space2
    /// The indent of each nesting depth.
    static let indent: CGFloat = MonarchTokens.Space.space4
    /// The gap between the marker line and the entry's text.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The width of the marker line beside each entry.
    static let markerWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The vertical padding of an entry.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The share of the visible height, from the top, in which a heading becomes current.
    static let activationRatio: CGFloat = 0.4
    /// The name of the coordinate space the scroll spy measures headings in.
    static let coordinateSpace = "monarch.tableOfContents"
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textSubtle }

    /// The colours of an entry: its text and the marker line beside it.
    static func colors(isCurrent: Bool, isEnabled: Bool) -> (text: MonarchColorRole, marker: MonarchColorRole) {
        let text: MonarchColorRole
        let marker: MonarchColorRole
        if !isEnabled {
            text = \.textDisabled
            marker = \.borderSubtle
        } else if isCurrent {
            text = \.accentStrong
            marker = \.accentStrong
        } else {
            text = \.textMuted
            marker = \.borderSubtle
        }
        return (text, marker)
    }

    /// The nesting depth of each entry, from the heading levels: an entry is nested under
    /// the nearest earlier entry with a lower level.
    static func depths(levels: [Int]) -> [Int] {
        var stack: [Int] = []
        return levels.map { level in
            while let last = stack.last, last >= level { stack.removeLast() }
            let depth = stack.count
            stack.append(level)
            return depth
        }
    }

    /// The section being read, from the top of each heading in the visible area.
    ///
    /// As on the web, it is the first heading in the zone from `offset` to `activationRatio`
    /// of the visible height; when none is there, the last heading scrolled above `offset`;
    /// when every heading is below the zone, `nil`, which keeps the previous section.
    static func current<Key: Hashable>(positions: [Key: CGFloat], viewportHeight: CGFloat, offset: CGFloat) -> Key? {
        let zoneEnd = max(offset, viewportHeight * activationRatio)
        let inZone = positions.filter { $0.value >= offset && $0.value <= zoneEnd }
        if let first = inZone.min(by: { $0.value < $1.value }) { return first.key }
        let above = positions.filter { $0.value < offset }
        return above.max(by: { $0.value < $1.value })?.key
    }

    /// The visible title and spoken name of the list.
    static func title(_ title: String?) -> String {
        title ?? MonarchTableOfContentsStrings.title
    }

    /// The value read for an entry: "Current section" for the current one, nothing otherwise.
    static func value(isCurrent: Bool, currentLabel: String?) -> String {
        isCurrent ? (currentLabel ?? MonarchTableOfContentsStrings.current) : ""
    }
}
