import SwiftUI

/// What every block view of ``MonarchAiMarkdown`` needs to render.
struct MonarchAiMarkdownContext {
    let renderer: MonarchAiMarkdownInlineRenderer
    let labels: MonarchAiMarkdownLabels
    let isStreaming: Bool
    let image: (@MainActor (URL, String) -> AnyView)?
    let codeBlock: (@MainActor (String, String, Bool) -> AnyView)?
}

/// A column of blocks spaced with the answer rhythm. Footnote definitions are
/// left out: they are listed at the end.
struct MonarchAiMarkdownBlocks: View {
    let blocks: [MonarchAiMarkdownBlock]
    let context: MonarchAiMarkdownContext
    let depth: Int
    let isTight: Bool
    let isMuted: Bool

    var body: some View {
        let shown = blocks.filter { !$0.isFootnoteDefinition }
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(shown.enumerated()), id: \.offset) { index, block in
                MonarchAiMarkdownBlockView(block: block, context: context, depth: depth, isMuted: isMuted)
                    .padding(
                        .top,
                        MonarchAiMarkdownTokens.gap(
                            before: block,
                            after: index > 0 ? shown[index - 1] : nil,
                            isTight: isTight
                        )
                    )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// One block of ``MonarchAiMarkdown``.
struct MonarchAiMarkdownBlockView: View {
    let block: MonarchAiMarkdownBlock
    let context: MonarchAiMarkdownContext
    let depth: Int
    let isMuted: Bool

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiMarkdownTokens

    var body: some View {
        switch block {
        case .heading(let level, let content):
            Text(context.renderer.attributed(content))
                .monarchTextStyle(Tokens.headingStyle(level: level))
                .foregroundStyle(theme.colors[role: Tokens.textRole(isMuted: isMuted)])
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(MonarchProseTokens.accessibilityLevel(level))
        case .paragraph(let content):
            paragraph(content)
        case .code(let code):
            codeView(code)
        case .quote(let children):
            MonarchAiMarkdownBlocks(blocks: children, context: context, depth: depth, isTight: false, isMuted: true)
                .padding(.leading, Tokens.quoteIndent)
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(theme.colors[role: Tokens.Role.quoteBar])
                        .frame(width: Tokens.quoteBarWidth)
                        .accessibilityHidden(true)
                }
        case .list(let ordered, let start, let isLoose, let items):
            MonarchAiMarkdownListView(
                ordered: ordered,
                start: start,
                isLoose: isLoose,
                items: items,
                context: context,
                depth: depth,
                isMuted: isMuted
            )
        case .table(let table):
            MonarchAiMarkdownTableView(table: table, context: context, isMuted: isMuted)
        case .rule:
            Rectangle()
                .fill(theme.colors[role: Tokens.Role.rule])
                .frame(maxWidth: .infinity)
                .frame(height: Tokens.borderWidth)
                .accessibilityHidden(true)
        case .footnoteDefinition:
            EmptyView()
        }
    }

    @ViewBuilder private func paragraph(_ content: [MonarchAiMarkdownInline]) -> some View {
        if let image = context.image, let picture = Tokens.standaloneImage(content),
            let url = MonarchAiMarkdownLinks.url(picture.source)
        {
            image(url, picture.alt)
        } else {
            Text(context.renderer.attributed(content))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.textRole(isMuted: isMuted)])
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .textSelection(.enabled)
        }
    }

    @ViewBuilder private func codeView(_ code: MonarchAiMarkdownCode) -> some View {
        let isComplete = Tokens.isComplete(code, isStreaming: context.isStreaming)
        if let codeBlock = context.codeBlock {
            codeBlock(code.language, code.code, isComplete)
        } else {
            MonarchCodeBlock(
                code.code,
                language: Tokens.codeLanguage(code),
                title: code.filename,
                isCopyable: isComplete
            )
        }
    }
}
