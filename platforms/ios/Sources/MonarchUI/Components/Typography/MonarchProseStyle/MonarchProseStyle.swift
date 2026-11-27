import Foundation
import SwiftUI

/// Type styles for long-form content you do not lay out element by element:
/// help articles, release notes, Markdown comments and documents.
///
/// ```swift
/// MonarchProse(MonarchProseStyle(size: .sm)) {
///     Text("Getting started").monarchProseBlock(.heading(2))
///     Text(style.markdown("Read the [overview](https://example.com) first, then run `make`.", in: theme))
///     Text("Create a project").monarchProseBlock(.listItem(marker: "•"))
///     Text("Invite your team").monarchProseBlock(.listItem(marker: "•"))
/// }
/// ```
///
/// SwiftUI draws Markdown inline only (emphasis, links, code spans); it
/// does not lay out headings, lists, quotations or code blocks. Prose is
/// the layer that does: the style sets the base size and the reading
/// measure, ``MonarchProse`` spaces its blocks with the web's rhythm, each
/// block takes its type and colour from ``MonarchProseBlock`` through
/// `monarchProseBlock(_:)`, and ``inline(_:in:)`` styles the links and
/// code of attributed text. A Markdown renderer maps its blocks to these.
///
/// Prose never renders HTML. Every size is relative to the base size and
/// scales with Dynamic Type.
///
/// Accessibility: a heading block has the header trait and its heading
/// level, so the VoiceOver rotor moves between headings. Links are
/// underlined, not told apart by colour alone. Bullets are not read;
/// numbered markers are.
public struct MonarchProseStyle: Sendable, Hashable {
    /// The base size. Headings, code and spacing scale with it.
    public let size: MonarchProseSize
    /// Whether the line length is limited to the reading measure, about
    /// 70 characters.
    public let limitsLineLength: Bool

    /// A prose style.
    ///
    /// - Parameters:
    ///   - size: `sm` for comments, chat and side panels, `md` for
    ///     documents, `lg` for long reads.
    ///   - limitsLineLength: Whether the line length is limited to the
    ///     reading measure. Turn it off when the container already sets the
    ///     width.
    public init(size: MonarchProseSize = .md, limitsLineLength: Bool = true) {
        self.size = size
        self.limitsLineLength = limitsLineLength
    }

    /// Text with the prose styles on its links and code spans.
    ///
    /// Links take the link colour and an underline; code spans take the
    /// look of ``MonarchInlineCodeStyle/subtle``. Emphasis and strong
    /// emphasis are drawn by SwiftUI from the Markdown intents. Other runs
    /// are unchanged.
    ///
    /// - Parameters:
    ///   - text: Attributed text, for example from `AttributedString(markdown:)`.
    ///   - theme: The theme, for the colours.
    /// - Returns: The text, with its links and code spans styled.
    public func inline(_ text: AttributedString, in theme: MonarchTheme) -> AttributedString {
        var result = MonarchInlineCodeStyle.subtle.applied(to: text, in: theme)
        let link = MonarchProseTokens.linkAttributes(in: theme)
        for run in result.runs where run.link != nil {
            result[run.range].mergeAttributes(link)
        }
        return result
    }

    /// A keyboard key inside running text, such as "Enter": the monospaced face on a muted background.
    ///
    /// - Parameters:
    ///   - key: The key's name, shown as it is.
    ///   - theme: The theme.
    /// - Returns: The key as one styled run, to append to other attributed text.
    public func key(_ key: String, in theme: MonarchTheme) -> AttributedString {
        MonarchInlineCodeStyle.subtle.attributedString(key, in: theme)
    }

    /// A highlighted word inside running text: the text colour on the warning background, as the web's mark.
    ///
    /// - Parameters:
    ///   - text: The highlighted text.
    ///   - theme: The theme.
    /// - Returns: The text as one styled run, to append to other attributed text.
    public func highlight(_ text: String, in theme: MonarchTheme) -> AttributedString {
        var result = AttributedString(text)
        result.swiftUI.backgroundColor = theme.colors.warningSubtle
        result.swiftUI.foregroundColor = theme.colors.textDefault
        return result
    }

    /// Text from inline Markdown, with the prose styles on its links and
    /// code spans.
    ///
    /// Only inline Markdown is read: emphasis, links and code spans. White
    /// space and line breaks are kept. Text that is not valid Markdown is
    /// shown as it is.
    ///
    /// - Parameters:
    ///   - markdown: The text of one block, in Markdown.
    ///   - theme: The theme, for the colours.
    /// - Returns: The text without the Markdown marks, styled.
    public func markdown(_ markdown: String, in theme: MonarchTheme) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        let parsed = (try? AttributedString(markdown: markdown, options: options)) ?? AttributedString(markdown)
        return inline(parsed, in: theme)
    }
}

#if DEBUG
    private struct MonarchProsePreviewArticle: View {
        let style: MonarchProseStyle
        @Environment(\.monarchTheme) private var theme

        var body: some View {
            MonarchProse(style) {
                Text("Getting started").monarchProseBlock(.heading(2))
                Text(
                    style.markdown("Read the [overview](https://example.com) first, then run `npm install`.", in: theme)
                )
                Text("Create a project").monarchProseBlock(.listItem(marker: "•"))
                Text("Set up notifications").monarchProseBlock(.listItem(marker: "•"))
                Text("Email").monarchProseBlock(.listItem(marker: "◦", depth: 1))
                Text("Small steps every day add up.").monarchProseBlock(.quote)
                Text("let total = items.reduce(0) { $0 + $1.price }").monarchProseBlock(.code)
                Divider().monarchProseBlock(.rule)
                Text("Options").monarchProseBlock(.heading(3))
                Text("1.").monarchProseBlock(.listItem(marker: "1."))
            }
        }
    }

    #Preview("MonarchProseStyle") {
        MonarchPreviewMatrix {
            ForEach(MonarchProseSize.allCases, id: \.self) { size in
                MonarchProsePreviewArticle(style: MonarchProseStyle(size: size))
            }
        }
    }
#endif
