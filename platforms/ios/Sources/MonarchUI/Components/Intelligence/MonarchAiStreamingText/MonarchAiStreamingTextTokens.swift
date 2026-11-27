import Foundation
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiStreamingText``: which
/// blocks are finished, the caret, and the spoken text. Isolated to the
/// main actor, like the Markdown parser it calls.
@MainActor
enum MonarchAiStreamingTextTokens {
    /// The caret drawn after the last character while text arrives: a left
    /// one-quarter block, as thin as a text caret.
    static let caret = "\u{258D}"

    /// The blocks to draw: the finished ones, kept exactly as parsed so their
    /// views keep their identity, and the tail, the last block completed for
    /// display while streaming.
    struct Display: Equatable {
        /// Every block before the last. They do not change while the text grows.
        let finished: [MonarchAiMarkdownBlock]
        /// The last block, completed while streaming; may parse to several blocks or none.
        let tail: [MonarchAiMarkdownBlock]

        /// Every block, finished first.
        var all: [MonarchAiMarkdownBlock] { finished + tail }
    }

    /// Splits a parse into finished blocks and the tail. While streaming the
    /// last block's source is completed with
    /// ``MonarchAiStreamingTextCompletion/complete(_:)`` and parsed on its
    /// own; a code block needs no completing.
    static func display(_ state: MonarchAiMarkdownParseState, isStreaming: Bool) -> Display {
        guard let last = state.blocks.last, let offset = state.offsets.last else {
            return Display(finished: [], tail: [])
        }
        let finished = Array(state.blocks.dropLast())
        if case .code = last { return Display(finished: finished, tail: [last]) }
        guard isStreaming else { return Display(finished: finished, tail: [last]) }
        let start = String.Index(utf16Offset: min(offset, state.text.utf16.count), in: state.text)
        let source = String(state.text[start...])
        return Display(
            finished: finished,
            tail: MonarchAiMarkdownParser.parse(MonarchAiStreamingTextCompletion.complete(source))
        )
    }

    /// The tail with the caret after its last character, or `nil` when the
    /// last shown block ends in something that is not text (code, a table,
    /// a rule), which shows its own progress.
    static func withCaret(_ blocks: [MonarchAiMarkdownBlock]) -> [MonarchAiMarkdownBlock]? {
        guard let index = blocks.lastIndex(where: { !$0.isFootnoteDefinition }),
            let block = withCaret(blocks[index])
        else { return nil }
        var result = blocks
        result[index] = block
        return result
    }

    private static func withCaret(_ block: MonarchAiMarkdownBlock) -> MonarchAiMarkdownBlock? {
        switch block {
        case .paragraph(let content):
            return .paragraph(content + [.text(caret)])
        case .heading(let level, let content):
            return .heading(level: level, content: content + [.text(caret)])
        case .quote(let children):
            return withCaret(children).map { .quote($0) }
        case .list(let ordered, let start, let isLoose, let items):
            guard let last = items.last, let children = withCaret(last.children) else { return nil }
            let item = MonarchAiMarkdownListItem(checked: last.checked, children: children)
            return .list(ordered: ordered, start: start, isLoose: isLoose, items: Array(items.dropLast()) + [item])
        case .code, .table, .rule, .footnoteDefinition:
            return nil
        }
    }

    /// What VoiceOver hears for a block: its text, with a code block or a
    /// table named instead of read out, and nothing for a footnote
    /// definition, which is read where it is listed.
    static func spoken(_ block: MonarchAiMarkdownBlock, codeBlock: String, table: String) -> String {
        switch block {
        case .code:
            return codeBlock
        case .table:
            return table
        case .footnoteDefinition, .rule:
            return ""
        case .quote(let children):
            return children.map { spoken($0, codeBlock: codeBlock, table: table) }.filter { !$0.isEmpty }
                .joined(separator: "\n\n")
        case .list(_, _, _, let items):
            return items.map { item in
                item.children.map { spoken($0, codeBlock: codeBlock, table: table) }.filter { !$0.isEmpty }
                    .joined(separator: "\n\n")
            }
            .filter { !$0.isEmpty }
            .joined(separator: "\n\n")
        case .heading, .paragraph:
            return MonarchAiMarkdownText.block(block).trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    /// The spoken text of the shown blocks and the length of its finished
    /// part, in UTF-16 units. While streaming the last block is not finished.
    static func spoken(
        _ blocks: [MonarchAiMarkdownBlock],
        isStreaming: Bool,
        codeBlock: String,
        table: String
    ) -> (text: String, done: Int) {
        combine(blocks.map { spoken($0, codeBlock: codeBlock, table: table) }, isStreaming: isStreaming)
    }

    /// Joins the spoken text of each shown block, blank lines between them,
    /// and measures the finished part in UTF-16 units. While streaming the
    /// last block is not finished.
    static func combine(_ texts: [String], isStreaming: Bool) -> (text: String, done: Int) {
        let finishedCount = isStreaming ? max(0, texts.count - 1) : texts.count
        let done = texts.prefix(finishedCount).filter { !$0.isEmpty }.joined(separator: "\n\n")
        let rest = texts.dropFirst(finishedCount).filter { !$0.isEmpty }.joined(separator: "\n\n")
        let text = !done.isEmpty && !rest.isEmpty ? done + "\n\n" + rest : (done.isEmpty ? rest : done)
        return (text, done.utf16.count)
    }
}
