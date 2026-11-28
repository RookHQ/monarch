import Foundation
import MonarchTokens

/// Component tokens and logic for ``MonarchAiSourcePreview``.
enum MonarchAiSourcePreviewTokens {
    /// The gap between the header, the quote and the paragraphs.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between paragraphs.
    static let paragraphSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the meta line, and inside the quote block.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the glyph and the meta line.
    static let glyphSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The width of the bar before the quote block.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the bar and the quote.
    static let quoteBarSpacing: CGFloat = MonarchTokens.Space.space3

    /// The fill of the highlighted passage.
    static var markBackground: MonarchColorRole { \.aiSubtle }
    /// The underline of the highlighted passage, so it does not depend on the fill alone.
    static var markUnderline: MonarchColorRole { \.aiCitation }
    /// The bar before the quote block.
    static var quoteBar: MonarchColorRole { \.aiBorder }
    /// The colour of the meta line and the quote caption.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The fill of the sheet.
    static var background: MonarchColorRole { \.bgSurface }

    /// One paragraph of the content, split around the highlighted passage.
    struct Paragraph: Hashable, Identifiable {
        /// The position of the paragraph, from 0.
        let index: Int
        /// The text before the passage, or the whole paragraph.
        let before: String
        /// The highlighted passage, empty when the paragraph has none.
        let mark: String
        /// The text after the passage.
        let after: String

        /// The id of the paragraph.
        var id: Int { index }
        /// Whether the paragraph holds the highlighted passage.
        var isMarked: Bool { !mark.isEmpty }
    }

    /// Where `quote` first appears in `text`, ignoring case and differences
    /// in whitespace; `nil` when it does not.
    static func quoteRange(in text: String, quote: String) -> Range<String.Index>? {
        let words = quote.split(whereSeparator: \.isWhitespace).map(String.init)
        guard !words.isEmpty else { return nil }
        let pattern = words.map(NSRegularExpression.escapedPattern(for:)).joined(separator: "\\s+")
        return text.range(of: pattern, options: [.regularExpression, .caseInsensitive])
    }

    /// The content as paragraphs, split on blank lines and with empty ones
    /// dropped, with the first match of `quote` marked in the paragraphs it covers.
    static func paragraphs(_ text: String, quote: String?) -> [Paragraph] {
        let range = quote.flatMap { quoteRange(in: text, quote: $0) }
        var bounds: [Range<String.Index>] = []
        var start = text.startIndex
        while let gap = text.range(of: "\\n\\s*\\n", options: .regularExpression, range: start..<text.endIndex) {
            bounds.append(start..<gap.lowerBound)
            start = gap.upperBound
        }
        bounds.append(start..<text.endIndex)
        var result: [Paragraph] = []
        for bound in bounds where !text[bound].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let index = result.count
            guard let range, range.lowerBound < bound.upperBound, range.upperBound > bound.lowerBound else {
                result.append(Paragraph(index: index, before: String(text[bound]), mark: "", after: ""))
                continue
            }
            let from = max(range.lowerBound, bound.lowerBound)
            let to = min(range.upperBound, bound.upperBound)
            result.append(
                Paragraph(
                    index: index,
                    before: String(text[bound.lowerBound..<from]),
                    mark: String(text[from..<to]),
                    after: String(text[to..<bound.upperBound])
                )
            )
        }
        return result
    }

    /// Whether the quote is shown in its own block: there is a quote, and
    /// either no content or content that does not contain it.
    static func showsQuoteBlock(quote: String?, content: String?) -> Bool {
        guard let quote, !quote.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        guard let content else { return true }
        return quoteRange(in: content, quote: quote) == nil
    }

    /// Whether there is nothing to preview: no content and no quote.
    static func isEmpty(quote: String?, content: String?) -> Bool {
        (content?.isEmpty ?? true) && (quote?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
    }
}
