import Foundation
import SwiftUI

/// Turns inline tokens into an `AttributedString`, never into markup:
/// links get a `link` attribute only when
/// ``MonarchAiMarkdownLinks/isSafeLink(_:allowsRelative:)`` allows them and
/// keep their text otherwise; links do not nest; images become their alt
/// text; footnote references become raised numbers.
struct MonarchAiMarkdownInlineRenderer {
    let theme: MonarchTheme
    let footnoteNumbers: [String: Int]
    let showsLinkHosts: Bool
    let allowsRelativeLinks: Bool
    let linkHost: @Sendable (String) -> String
    let footnoteFont: Font?
    let footnoteRaise: CGFloat
    /// The fonts of strong and emphasised text for a custom font family; `nil` keeps the presentation intent.
    var strongFont: Font? = nil
    var emphasisFont: Font? = nil

    /// The attributed text of inline tokens, with the inline code and link styles applied.
    func attributed(_ nodes: [MonarchAiMarkdownInline]) -> AttributedString {
        let text = append(nodes, inLink: false)
        return MonarchInlineCodeStyle.subtle.applied(to: text, in: theme)
    }

    private func append(_ nodes: [MonarchAiMarkdownInline], inLink: Bool) -> AttributedString {
        var result = AttributedString()
        for node in nodes {
            switch node {
            case .text(let value) where value == "\u{258D}":
                // The streaming caret, in the AI colour as on the web.
                var caret = AttributedString(value)
                caret.foregroundColor = theme.colors.aiStrong
                result += caret
            case .text(let value):
                // A soft line break inside a paragraph is a space, as in a browser.
                result += AttributedString(value.replacingOccurrences(of: "\n", with: " "))
            case .code(let value), .math(let value):
                var code = AttributedString(value)
                code.inlinePresentationIntent = .code
                result += code
            case .styled(let kind, let children):
                result += styled(append(children, inLink: inLink), kind: kind)
            case .lineBreak:
                result += AttributedString("\n")
            case .image(_, let alt, _):
                result += AttributedString(alt)
            case .link(let href, _, _, let children):
                result += link(href: href, children: children, inLink: inLink)
            case .footnoteReference(let label):
                result += footnote(label)
            }
        }
        return result
    }

    private func styled(_ text: AttributedString, kind: MonarchAiMarkdownEmphasis) -> AttributedString {
        var result = text
        let intent: InlinePresentationIntent =
            switch kind {
            case .emphasis: .emphasized
            case .strong: .stronglyEmphasized
            case .strikethrough: .strikethrough
            }
        let ranges = result.runs.map(\.range)
        for range in ranges {
            let current = result[range].inlinePresentationIntent ?? []
            result[range].inlinePresentationIntent = current.union(intent)
        }
        if kind == .strong, let strongFont {
            result.swiftUI.font = strongFont
        } else if kind == .emphasis, let emphasisFont {
            result.swiftUI.font = emphasisFont
        }
        if kind == .strikethrough {
            result.swiftUI.strikethroughStyle = .single
            result.swiftUI.foregroundColor = theme.colors[role: MonarchAiMarkdownTokens.Role.deleted]
        }
        return result
    }

    private func link(href: String, children: [MonarchAiMarkdownInline], inLink: Bool) -> AttributedString {
        guard !inLink, MonarchAiMarkdownLinks.isSafeLink(href, allowsRelative: allowsRelativeLinks),
            let url = MonarchAiMarkdownLinks.url(href)
        else {
            return append(children, inLink: true)
        }
        var text = append(children, inLink: true)
        text.link = url
        text.mergeAttributes(MonarchProseTokens.linkAttributes(in: theme))
        if showsLinkHosts,
            let host = MonarchAiMarkdownLinks.shownHost(href: href, linkText: MonarchAiMarkdownText.inline(children))
        {
            var suffix = AttributedString(" " + linkHost(host))
            suffix.swiftUI.foregroundColor = theme.colors[role: MonarchAiMarkdownTokens.Role.linkHost]
            text += suffix
        }
        return text
    }

    private func footnote(_ label: String) -> AttributedString {
        guard let number = footnoteNumbers[label] else { return AttributedString("[^\(label)]") }
        var text = AttributedString("\(number)")
        text.swiftUI.baselineOffset = footnoteRaise
        if let footnoteFont { text.swiftUI.font = footnoteFont }
        text.swiftUI.foregroundColor = theme.colors.textLink
        return text
    }
}
