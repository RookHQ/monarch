import Foundation

/// Splits text into sentences on the web's boundaries, measured in UTF-16
/// units, so the iOS announcers and streaming text agree with the web's
/// live regions character for character. A sentence ends after `.`, `!`,
/// `?`, `…` or their full-width forms, with any closing quotes or brackets,
/// when whitespace follows.
enum MonarchSentences {
    /// The UTF-16 offsets just after each sentence end in `text`, starting
    /// at `from`. The end of the text is not included unless a sentence
    /// mark and whitespace end there.
    static func boundaries(in text: String, from start: Int = 0) -> [Int] {
        let length = text.utf16.count
        guard start < length else { return [] }
        let begin = String.Index(utf16Offset: max(0, start), in: text)
        // A local pattern: `Regex` is not `Sendable`, so it cannot be a static.
        let sentenceEnd = /[.!?…。！？][”’"')\]]*(?=\s)/
        return text[begin...].matches(of: sentenceEnd).map { $0.range.upperBound.utf16Offset(in: text) }
    }

    /// The complete sentences of `text`, trimmed, leaving out the unfinished
    /// rest after the last boundary.
    static func complete(in text: String) -> [String] {
        var result: [String] = []
        var previous = 0
        for boundary in boundaries(in: text) {
            let lower = String.Index(utf16Offset: previous, in: text)
            let upper = String.Index(utf16Offset: boundary, in: text)
            let sentence = text[lower..<upper].trimmingCharacters(in: .whitespacesAndNewlines)
            if !sentence.isEmpty { result.append(sentence) }
            previous = boundary
        }
        return result
    }
}
