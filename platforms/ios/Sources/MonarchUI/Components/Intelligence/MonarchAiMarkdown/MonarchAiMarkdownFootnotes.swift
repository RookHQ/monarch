/// Footnote numbers by label, and the definitions in the order they are
/// listed at the end of ``MonarchAiMarkdown``.
struct MonarchAiMarkdownFootnotes: Equatable {
    /// The number of each referenced footnote, by label.
    let numbers: [String: Int]
    /// The listed footnotes: number and the definition's blocks.
    let ordered: [(number: Int, children: [MonarchAiMarkdownBlock])]

    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.numbers == rhs.numbers && lhs.ordered.map(\.number) == rhs.ordered.map(\.number)
            && lhs.ordered.map(\.children) == rhs.ordered.map(\.children)
    }

    /// Footnote labels referenced in a block, in reading order.
    static func references(in block: MonarchAiMarkdownBlock) -> [String] {
        var out: [String] = []
        walk(block, into: &out)
        return out
    }

    private static func walk(_ block: MonarchAiMarkdownBlock, into out: inout [String]) {
        switch block {
        case .heading(_, let content), .paragraph(let content):
            inline(content, into: &out)
        case .table(let table):
            for row in [table.header] + table.rows {
                for cell in row { inline(cell, into: &out) }
            }
        case .quote(let children):
            for child in children { walk(child, into: &out) }
        case .list(_, _, _, let items):
            for item in items {
                for child in item.children { walk(child, into: &out) }
            }
        case .code, .rule, .footnoteDefinition:
            break
        }
    }

    private static func inline(_ nodes: [MonarchAiMarkdownInline], into out: inout [String]) {
        for node in nodes {
            switch node {
            case .footnoteReference(let label):
                out.append(label)
            case .styled(_, let children), .link(_, _, _, let children):
                inline(children, into: &out)
            case .text, .code, .math, .image, .lineBreak:
                break
            }
        }
    }

    /// Numbers footnotes in the order of their first reference, as the web
    /// does. A definition that is never referenced is not listed; a
    /// reference without a definition stays text.
    static func of(_ blocks: [MonarchAiMarkdownBlock]) -> Self {
        var definitions: [String: [MonarchAiMarkdownBlock]] = [:]
        for case .footnoteDefinition(let label, let children) in blocks where definitions[label] == nil {
            definitions[label] = children
        }
        var numbers: [String: Int] = [:]
        var ordered: [(number: Int, children: [MonarchAiMarkdownBlock])] = []
        for block in blocks {
            for label in references(in: block) where numbers[label] == nil {
                guard let children = definitions[label] else { continue }
                let number = numbers.count + 1
                numbers[label] = number
                ordered.append((number, children))
            }
        }
        return Self(numbers: numbers, ordered: ordered)
    }
}
