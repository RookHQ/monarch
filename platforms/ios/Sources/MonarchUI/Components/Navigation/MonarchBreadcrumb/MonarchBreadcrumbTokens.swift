import MonarchTokens
import SwiftUI

/// One place in a breadcrumb trail: a level, or the menu of collapsed levels.
enum MonarchBreadcrumbSegment: Equatable {
    /// The level at this index of the items.
    case item(Int)
    /// The levels at these indices of the items, collapsed into a menu.
    case collapsed([Int])
}

/// How one level is drawn.
enum MonarchBreadcrumbLevelKind: Equatable {
    /// A link to an ancestor.
    case link
    /// An ancestor without a screen, shown as text.
    case text
    /// The last level, the screen the person is on.
    case current
}

/// Component tokens for ``MonarchBreadcrumb``.
enum MonarchBreadcrumbTokens {
    /// The type role of the trail.
    static let textStyle: MonarchTextStyle = .label
    /// The gap between a level and a separator.
    static let spacing: CGFloat = MonarchTokens.Space.space1
    /// How far the touch area of a linked level reaches past its text, so
    /// short levels stay easy to hit without spacing the trail unevenly.
    static let hitOutset: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol between two levels. A `forward` symbol, so it follows the reading direction.
    static let separatorSymbol = "chevron.forward"
    /// The SF Symbol of the collapsed-levels button.
    static let moreSymbol = "ellipsis"
    /// The fewest levels shown when the trail collapses: the first, the menu and the last.
    static let minimumVisible = 3
    /// The number of levels shown before the middle ones collapse.
    static let defaultMaxVisible = 4
    /// The weight of the current level.
    static let currentWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The colour of a separator.
    static var separatorColor: MonarchColorRole { \.textSubtle }

    /// The places of the trail. When there are more items than `maxVisible`, the first item
    /// stays, the middle ones collapse into a menu and the last `maxVisible - 2` stay.
    static func segments(count: Int, maxVisible: Int) -> [MonarchBreadcrumbSegment] {
        let limit = max(maxVisible, minimumVisible)
        guard count > limit else { return (0..<max(count, 0)).map { .item($0) } }
        let tailStart = count - (limit - 2)
        return [.item(0), .collapsed(Array(1..<tailStart))] + (tailStart..<count).map { .item($0) }
    }

    /// How the level at `index` is drawn: the last is current, the others link when they can.
    static func kind(index: Int, count: Int, isLinked: Bool) -> MonarchBreadcrumbLevelKind {
        if index == count - 1 { return .current }
        return isLinked ? .link : .text
    }

    /// The colour of a level: muted for a link or a plain level, the default
    /// text colour for the current one.
    static func textColor(_ kind: MonarchBreadcrumbLevelKind) -> MonarchColorRole {
        kind == .current ? \.textDefault : \.textMuted
    }

    /// The spoken name of the collapsed-levels button, with the count formatted for the locale.
    static func moreLabel(count: Int, labels: MonarchBreadcrumbLabels, locale: Locale = .current) -> String {
        let formatted = count.formatted(.number.locale(locale))
        return labels.more?(formatted) ?? MonarchBreadcrumbStrings.more(formatted)
    }

    /// The spoken name of the trail.
    static func label(_ labels: MonarchBreadcrumbLabels) -> String {
        labels.label ?? MonarchBreadcrumbStrings.label
    }

    /// The value read for a level: "Current page" for the last, nothing for the others.
    static func value(_ kind: MonarchBreadcrumbLevelKind, labels: MonarchBreadcrumbLabels) -> String {
        kind == .current ? (labels.current ?? MonarchBreadcrumbStrings.current) : ""
    }
}
