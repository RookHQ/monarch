/// The plain text of parsed Markdown, for alt text, link host checks and
/// spoken table labels.
enum MonarchAiMarkdownText {
    /// Plain text of inline tokens; images give their alt text and footnote references nothing.
    static func inline(_ nodes: [MonarchAiMarkdownInline]) -> String {
        var result = ""
        for node in nodes {
            switch node {
            case .text(let value), .code(let value), .math(let value):
                result += value
            case .lineBreak:
                result += "\n"
            case .image(_, let alt, _):
                result += alt
            case .footnoteReference:
                break
            case .styled(_, let children), .link(_, _, _, let children):
                result += inline(children)
            }
        }
        return result
    }

    /// Plain text of a block, paragraphs separated by blank lines.
    static func block(_ block: MonarchAiMarkdownBlock) -> String {
        switch block {
        case .heading(_, let content), .paragraph(let content):
            return inline(content)
        case .code(let code):
            return code.code
        case .table(let table):
            return ([table.header] + table.rows).map { row in row.map(inline).joined(separator: " ") }
                .joined(separator: "\n")
        case .quote(let children), .footnoteDefinition(_, let children):
            return children.map(Self.block).joined(separator: "\n\n")
        case .list(_, _, _, let items):
            return items.map { item in item.children.map(Self.block).joined(separator: "\n\n") }
                .joined(separator: "\n\n")
        case .rule:
            return ""
        }
    }
}
