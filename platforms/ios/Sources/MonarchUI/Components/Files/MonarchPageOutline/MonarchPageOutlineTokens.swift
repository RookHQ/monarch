import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchPageOutline``.
enum MonarchPageOutlineTokens {
    /// The gap between the title, the list and the stats.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The indent of each nesting level, before Dynamic Type.
    static let indent: CGFloat = MonarchTokens.Space.space4
    /// The gap between the current-section bar and the text.
    static let barSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the current-section bar.
    static let barWidth: CGFloat = MonarchTokens.Space.space0_5
    /// Vertical padding of a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space1
    /// The deepest heading level listed by default.
    /// The rail down the whole list, and how far the rows sit in from it.
    static let railWidth: CGFloat = 1
    static let railInset: CGFloat = MonarchTokens.Space.space3
    static let defaultMaxLevel = 3

    /// The headings that are listed: up to `maxLevel` and not blank.
    static func visible(_ headings: [MonarchPageOutlineHeading], maxLevel: Int) -> [MonarchPageOutlineHeading] {
        headings.filter { $0.level <= maxLevel && !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    /// The nesting depth of each heading, from 0: a heading is nested under the
    /// nearest earlier heading with a lower level, so skipped levels do not add
    /// extra indent.
    static func depths(_ headings: [MonarchPageOutlineHeading]) -> [Int] {
        var stack: [Int] = []
        var depths: [Int] = []
        for heading in headings {
            while let last = stack.last, last >= heading.level { stack.removeLast() }
            depths.append(stack.count)
            stack.append(heading.level)
        }
        return depths
    }

    /// The colour of a row's text.
    static func textRole(isCurrent: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.textDisabled }
        return isCurrent ? \.accentStrong : \.textMuted
    }

    /// The colour of a row's leading bar: accent for the current section, a subtle line otherwise.
    static func barRole(isCurrent: Bool) -> MonarchColorRole {
        isCurrent ? \.accentSolid : \.borderSubtle
    }

    /// The type style of a row: the current section is stronger, so it is not shown by colour alone.
    static func textStyle(isCurrent: Bool) -> MonarchTextStyle {
        .body
    }

    /// What VoiceOver reads after a heading.
    static func accessibilityValue(isCurrent: Bool, currentLabel: String) -> String {
        isCurrent ? currentLabel : ""
    }
}
