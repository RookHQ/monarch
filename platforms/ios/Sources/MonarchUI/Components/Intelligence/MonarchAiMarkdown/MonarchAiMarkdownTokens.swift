import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMarkdown``: the web's
/// rhythm for answers (compact headings, small gaps), the colour roles of
/// the parts that are not running text, and the decisions the views make.
enum MonarchAiMarkdownTokens {
    /// Space between blocks.
    static let blockGap: CGFloat = MonarchTokens.Space.space3
    /// Space above a heading that follows another block.
    static let headingGap: CGFloat = MonarchTokens.Space.space4
    /// Space under a heading.
    static let afterHeadingGap: CGFloat = MonarchTokens.Space.space2
    /// Space between the items and the blocks of a tight list.
    static let tightGap: CGFloat = MonarchTokens.Space.space1
    /// The narrowest list marker column.
    static let markerWidth: CGFloat = MonarchTokens.Space.space5
    /// Space between a list marker and the item.
    static let markerGap: CGFloat = MonarchTokens.Space.space1
    /// The width of the quotation bar.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// Space between the quotation bar and the quoted text.
    static let quoteIndent: CGFloat = MonarchTokens.Space.space3
    /// The horizontal padding of a table cell.
    static let cellHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The vertical padding of a table cell.
    static let cellVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// How many times `space24` a table column may grow before its text wraps.
    static let cellWidthFactor: CGFloat = 2.5
    /// A table column never gets wider than this at the default text size; its text wraps.
    static let maxCellWidth: CGFloat = MonarchTokens.Space.space24 * cellWidthFactor
    /// The corner radius of a table.
    static let tableRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of table borders and rules.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The size of a footnote number, relative to the text.
    static let footnoteScale: CGFloat = 0.75
    /// How far a footnote number is raised, relative to the text size.
    static let footnoteRaise: CGFloat = 0.33
    /// The reference size of the body text that the scaled lengths follow.
    static let bodySize: CGFloat = MonarchTokens.FontSize.base
    /// The SF Symbol of a checked task.
    static let checkedTaskSymbol = "checkmark.square.fill"
    /// The SF Symbol of an open task.
    static let openTaskSymbol = "square"

    /// The colour roles of the parts that are not running text.
    enum Role {
        static var text: MonarchColorRole { \.textDefault }
        static var mutedText: MonarchColorRole { \.textMuted }
        static var quoteBar: MonarchColorRole { \.borderDefault }
        static var marker: MonarchColorRole { \.textMuted }
        static var tableBorder: MonarchColorRole { \.borderSubtle }
        static var tableHeader: MonarchColorRole { \.bgMuted }
        static var rule: MonarchColorRole { \.borderSubtle }
        static var linkHost: MonarchColorRole { \.textMuted }
        static var deleted: MonarchColorRole { \.textMuted }
        static var checkedTask: MonarchColorRole { \.textDisabled }
        static var openTask: MonarchColorRole { \.borderStrong }
    }

    /// The type role of a heading by its Markdown level: `subheading` for
    /// `#`, `bodyStrong` from `##` on, so a model's `#` never shouts.
    static func headingStyle(level: Int) -> MonarchTextStyle {
        level <= 1 ? .subheading : .bodyStrong
    }

    /// The text colour of running text, muted inside a quotation.
    static func textRole(isMuted: Bool) -> MonarchColorRole {
        isMuted ? Role.mutedText : Role.text
    }

    /// The space above a block after the previous one, in a tight list or not.
    static func gap(
        before block: MonarchAiMarkdownBlock,
        after previous: MonarchAiMarkdownBlock?,
        isTight: Bool
    ) -> CGFloat {
        guard let previous else { return 0 }
        if block.isHeading { return headingGap }
        if previous.isHeading { return afterHeadingGap }
        return isTight ? tightGap : blockGap
    }

    /// The space between the items of a list.
    static func itemGap(isLoose: Bool) -> CGFloat {
        isLoose ? blockGap : tightGap
    }

    /// The bullet of an unnumbered item: a disc, and a ring for nested lists.
    static func bullet(depth: Int) -> String {
        depth.isMultiple(of: 2) ? "\u{2022}" : "\u{25E6}"
    }

    /// The visible number of an ordered item.
    static func number(start: Int, index: Int) -> String {
        "\(start + index)."
    }

    /// The source and alt text of a paragraph that is a single image the
    /// app may render: an `http` or `https` source with alt text.
    static func standaloneImage(_ content: [MonarchAiMarkdownInline]) -> (source: String, alt: String)? {
        let visible = content.filter { node in
            if case .text(let value) = node { return !value.allSatisfy(\.isWhitespace) }
            return true
        }
        guard visible.count == 1, case .image(let source, let alt, _) = visible[0],
            MonarchAiMarkdownLinks.isSafeImage(source),
            !alt.allSatisfy(\.isWhitespace)
        else { return nil }
        return (source, alt)
    }

    /// Whether a code block is complete: closed, or the text has stopped streaming.
    static func isComplete(_ code: MonarchAiMarkdownCode, isStreaming: Bool) -> Bool {
        code.isClosed || !isStreaming
    }

    /// The language passed to ``MonarchCodeBlock``: `nil` when the fence named none.
    static func codeLanguage(_ code: MonarchAiMarkdownCode) -> String? {
        code.language.isEmpty ? nil : code.language
    }

    /// The frame alignment of a table column.
    static func alignment(_ alignment: MonarchAiMarkdownAlignment?) -> Alignment {
        switch alignment {
        case .center: .top
        case .trailing: .topTrailing
        case .leading, nil: .topLeading
        }
    }

    /// The text alignment of a table column.
    static func textAlignment(_ alignment: MonarchAiMarkdownAlignment?) -> TextAlignment {
        switch alignment {
        case .center: .center
        case .trailing: .trailing
        case .leading, nil: .leading
        }
    }

    /// The font of a footnote number for a body text size, in the theme's family.
    /// The font of strong and emphasised runs for a body text size: the theme's semibold face, or its regular face in
    /// italic. `nil` for the system font, where the presentation intent already draws them.
    static func inlineFont(theme: MonarchTheme, bodySize: CGFloat, strong: Bool) -> Font? {
        guard let family = theme.typography.fontFamily else {
            // The system font: named explicitly, so that an outer font does not take the weight and slant away.
            return strong ? .system(size: bodySize, weight: .semibold) : .system(size: bodySize).italic()
        }
        if strong {
            if let face = theme.typography.face(for: .semibold) { return .custom(face, fixedSize: bodySize) }
            return .custom(family, fixedSize: bodySize).weight(.semibold)
        }
        return .custom(family, fixedSize: bodySize).italic()
    }

    static func footnoteFont(family: String?, bodySize: CGFloat) -> Font {
        let size = bodySize * footnoteScale
        if let family { return .custom(family, fixedSize: size) }
        return .system(size: size)
    }
}
