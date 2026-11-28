import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchProseStyle``.
///
/// Lengths inside prose are in em, multiples of the base size, as on the
/// web, so that the size scales the whole document together.
enum MonarchProseTokens {
    /// The size of `body` text that Dynamic Type scales, used as the
    /// reference for the scale factor.
    static let referenceSize: CGFloat = MonarchTokens.FontSize.base
    /// The widest a block of prose gets with the reading measure, in base
    /// sizes: the web's reading width, `--mn-layout-reading-max`, is 42rem.
    static let readingMeasure: CGFloat = 42
    /// The heading sizes, level 1 to 6, in em.
    static let headingScales: [CGFloat] = [1.875, 1.5, 1.25, 1.125, 1, 1]
    /// The size of code and captions, in em.
    static let smallScale: CGFloat = 0.875
    /// The space between two blocks, in em.
    static let blockSpacing: CGFloat = 1.25
    /// The space before a heading, in em.
    static let headingSpacing: CGFloat = 1.75
    /// The space between a heading and the block under it, in em.
    static let afterHeadingSpacing: CGFloat = 0.5
    /// The space between two items of a list, in em.
    static let listItemSpacing: CGFloat = 0.375
    /// The space around a thematic break, in em.
    static let ruleSpacing: CGFloat = 2
    /// The width of the marker gutter of a list, and the indent of each
    /// level of nesting, in em.
    static let listIndent: CGFloat = 1.5
    /// The indent of a quotation after its bar, in em.
    static let quoteIndent: CGFloat = 1
    /// The width of the bar before a quotation.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The padding above and below the code of a code block.
    static let codeVerticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The padding before and after the code of a code block.
    static let codeHorizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The corner radius of a code block.
    static let codeCornerRadius: CGFloat = MonarchTokens.Radius.md

    /// The shape of a code block.
    static var codeShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: codeCornerRadius, style: .continuous)
    }

    // MARK: Type

    /// The base size in points, before Dynamic Type, for a prose size.
    static func baseSize(_ size: MonarchProseSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.FontSize.sm
        case .md: MonarchTokens.FontSize.base
        case .lg: MonarchTokens.FontSize.lg
        }
    }

    /// The heading level, clamped to 1 to 6.
    static func clampedLevel(_ level: Int) -> Int {
        min(max(level, 1), headingScales.count)
    }

    /// The size of a block in em.
    static func scale(of block: MonarchProseBlock) -> CGFloat {
        switch block {
        case .heading(let level): headingScales[clampedLevel(level) - 1]
        case .code, .caption: smallScale
        case .paragraph, .listItem, .quote, .rule: 1
        }
    }

    /// The line height of a block, as a multiple of its size.
    static func lineHeight(of block: MonarchProseBlock, size: MonarchProseSize) -> CGFloat {
        switch block {
        case .heading(let level):
            clampedLevel(level) <= 2 ? MonarchTokens.LineHeight.tight : MonarchTokens.LineHeight.snug
        case .code:
            MonarchTokens.LineHeight.normal
        case .paragraph, .listItem, .quote, .caption, .rule:
            size == .sm ? MonarchTokens.LineHeight.normal : MonarchTokens.LineHeight.relaxed
        }
    }

    /// The tracking of a block, in em.
    static func tracking(of block: MonarchProseBlock) -> CGFloat {
        switch block {
        case .heading(let level):
            clampedLevel(level) == 1 ? MonarchTokens.LetterSpacing.tight : MonarchTokens.LetterSpacing.snug
        case .paragraph, .listItem, .quote, .code, .caption, .rule:
            MonarchTokens.LetterSpacing.normal
        }
    }

    /// The token weight of a block.
    static func weight(of block: MonarchProseBlock) -> Int {
        if case .heading = block { return MonarchTokens.FontWeight.semibold }
        return MonarchTokens.FontWeight.regular
    }

    /// Whether a block is set in the monospaced face.
    static func isMonospaced(_ block: MonarchProseBlock) -> Bool {
        block == .code
    }

    /// The font size of a block in points: the base size of the prose
    /// size, scaled by Dynamic Type (`scaledReference` is
    /// ``referenceSize`` after scaling), times the size of the block in em.
    static func fontSize(of block: MonarchProseBlock, size: MonarchProseSize, scaledReference: CGFloat) -> CGFloat {
        em(size: size, scaledReference: scaledReference) * scale(of: block)
    }

    /// One em of the prose in points, after Dynamic Type.
    static func em(size: MonarchProseSize, scaledReference: CGFloat) -> CGFloat {
        baseSize(size) * scaledReference / referenceSize
    }

    /// The extra space between lines of a block, from its line height.
    static func lineSpacing(of block: MonarchProseBlock, size: MonarchProseSize, fontSize: CGFloat) -> CGFloat {
        max(0, (lineHeight(of: block, size: size) - MonarchTextStyleSpec.systemLineHeight) * fontSize)
    }

    /// The width of the reading measure in points, after Dynamic Type.
    static func measureWidth(size: MonarchProseSize, scaledReference: CGFloat) -> CGFloat {
        readingMeasure * em(size: size, scaledReference: scaledReference)
    }

    // MARK: Colour

    /// The colour role of the text of a block: muted for quotations,
    /// captions and level 6 headings, the default text colour otherwise.
    static func foreground(of block: MonarchProseBlock) -> MonarchColorRole {
        switch block {
        case .quote, .caption: \.textMuted
        case .heading(let level): clampedLevel(level) == 6 ? \.textMuted : \.textDefault
        case .paragraph, .listItem, .code: \.textDefault
        case .rule: \.borderDefault
        }
    }

    /// The colour role of a list marker.
    static var markerForeground: MonarchColorRole { \.textMuted }
    /// The colour role of the bar before a quotation.
    static var quoteBar: MonarchColorRole { \.borderStrong }
    /// The colour role of a code block's panel.
    static var codeBackground: MonarchColorRole { \.bgSubtle }
    /// The colour role of a code block's border.
    static var codeBorder: MonarchColorRole { \.borderSubtle }
    /// The colour role of a link.
    static var link: MonarchColorRole { \.textLink }

    // MARK: Spacing

    /// The space between two blocks in em: less under a heading, more
    /// before a heading and around a thematic break, little between the
    /// items of a list.
    static func spacing(after previous: MonarchProseBlock, before next: MonarchProseBlock) -> CGFloat {
        if previous == .rule || next == .rule { return ruleSpacing }
        if case .heading = next { return headingSpacing }
        if case .heading = previous { return afterHeadingSpacing }
        if case .listItem = previous, case .listItem = next { return listItemSpacing }
        return blockSpacing
    }

    // MARK: Accessibility

    /// The VoiceOver heading level for a heading level.
    static func accessibilityLevel(_ level: Int) -> AccessibilityHeadingLevel {
        switch clampedLevel(level) {
        case 1: .h1
        case 2: .h2
        case 3: .h3
        case 4: .h4
        case 5: .h5
        default: .h6
        }
    }

    /// Whether a list marker is read: a number or letter ("3.", "b)") is,
    /// a bullet is not.
    static func isMarkerSpoken(_ marker: String) -> Bool {
        marker.contains { $0.isLetter || $0.isNumber }
    }

    // MARK: Inline

    /// The attributes of a link inside prose: the link colour and an
    /// underline, so a link is not told apart by colour alone.
    static func linkAttributes(in theme: MonarchTheme) -> AttributeContainer {
        var container = AttributeContainer()
        container.swiftUI.foregroundColor = theme.colors[role: link]
        container.swiftUI.underlineStyle = .single
        return container
    }
}
