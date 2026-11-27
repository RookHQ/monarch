import Foundation

/// Keeps what ``MonarchAiStreamingText`` reuses between updates: the last
/// parse, so a growing text is parsed incrementally, the spoken text of
/// finished blocks, and the announcer. A reference, so the view can update
/// it while rendering.
@MainActor
final class MonarchAiStreamingTextModel {
    private var state: MonarchAiMarkdownParseState?
    private var spokenCache: [(block: MonarchAiMarkdownBlock, text: String)] = []
    /// What has been announced so far.
    var announcer = MonarchAiStreamingTextAnnouncer()

    /// The blocks of `text`, reusing the previous parse where the text only grew.
    func display(_ text: String, isStreaming: Bool) -> MonarchAiStreamingTextTokens.Display {
        let next = MonarchAiMarkdownParser.parseIncremental(text, previous: state)
        state = next
        return MonarchAiStreamingTextTokens.display(next, isStreaming: isStreaming)
    }

    /// The spoken text of the shown blocks and the length of the finished
    /// part. Finished blocks are worked out once and cached by position.
    func spoken(
        _ display: MonarchAiStreamingTextTokens.Display,
        isStreaming: Bool,
        codeBlock: String,
        table: String
    ) -> (text: String, done: Int) {
        var texts: [String] = []
        for (index, block) in display.finished.enumerated() {
            if index < spokenCache.count, spokenCache[index].block == block {
                texts.append(spokenCache[index].text)
                continue
            }
            let text = MonarchAiStreamingTextTokens.spoken(block, codeBlock: codeBlock, table: table)
            if index < spokenCache.count {
                spokenCache[index] = (block, text)
                spokenCache.removeSubrange((index + 1)...)
            } else {
                spokenCache.append((block, text))
            }
            texts.append(text)
        }
        if spokenCache.count > display.finished.count {
            spokenCache.removeSubrange(display.finished.count...)
        }
        let tail = display.tail.map { MonarchAiStreamingTextTokens.spoken($0, codeBlock: codeBlock, table: table) }
        return MonarchAiStreamingTextTokens.combine(texts + tail, isStreaming: isStreaming)
    }
}
