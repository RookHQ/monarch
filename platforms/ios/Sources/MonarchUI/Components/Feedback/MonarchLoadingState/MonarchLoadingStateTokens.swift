import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchLoadingState``.
enum MonarchLoadingStateTokens {
    /// The gap between rows and between cards.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the avatar and the lines, and between the lines.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the avatar and the lines, and between the line and the value of a row.
    static let rowGap: CGFloat = MonarchTokens.Space.space3
    /// The height of a skeleton line.
    static let barHeight: CGFloat = MonarchTokens.Space.space3
    /// The height of the skeleton heading of the detail variant.
    static let headingBarHeight: CGFloat = MonarchTokens.Space.space5
    /// The corner radius of a skeleton line.
    static let barRadius: CGFloat = MonarchTokens.Radius.sm
    /// The width of the short value at the end of a row.
    static let valueWidth: CGFloat = MonarchTokens.Space.space16 + MonarchTokens.Space.space12
    /// The least height of a bordered row.
    static let rowMinHeight: CGFloat = MonarchTokens.Space.space10
    /// The padding above and below a row's content.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The diameter of the avatar placeholder before Dynamic Type is applied.
    static let avatarSize: CGFloat = MonarchTokens.Space.space8
    /// The height of a card placeholder before Dynamic Type is applied.
    static let cardHeight: CGFloat = MonarchTokens.Space.space24
    /// The narrowest a card column may be.
    static let cardMinimumWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    /// The gap between the spinner and its label.
    static let spinnerSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding around the spinner.
    static let spinnerPadding: CGFloat = MonarchTokens.Space.space6
    /// The glyph the placeholder lines are made of. Never shown: the system
    /// placeholder draws a bar the width of the text.
    static let placeholderGlyph: Character = "0"
    /// Glyphs in a line that fills the width; a line of p percent gets p
    /// percent of these.
    static let fullLineGlyphs = 48
    /// The fill role of avatar and card placeholders.
    static var blockFill: MonarchColorRole { \.bgMuted }
    /// The foreground role the redacted lines are drawn with.
    static var lineFill: MonarchColorRole { \.textSubtle }
    /// The role of the spinner's label.
    static var label: MonarchColorRole { \.textMuted }

    /// The number of placeholder rows: never negative.
    static func rowCount(_ rows: Int) -> Int {
        max(0, rows)
    }

    /// The widths of the two lines of row `index`, in percent of the row,
    /// varied so the placeholder does not look like a pattern.
    static func lineWidths(_ index: Int) -> (first: Int, second: Int) {
        (55 + (index * 17) % 30, 30 + (index * 11) % 25)
    }

    /// The width of the short value at the end of row `index` in the rows
    /// variant, in percent: half the second line.
    static func metaWidth(_ index: Int) -> Int {
        lineWidths(index).second / 2
    }

    /// The widths of the lines of the detail placeholder, in percent.
    static let detailWidths = [40, 90, 75, 60]

    /// The placeholder text of a line `percent` wide.
    static func placeholder(percent: Int) -> String {
        String(repeating: placeholderGlyph, count: max(1, fullLineGlyphs * percent / 100))
    }
}
