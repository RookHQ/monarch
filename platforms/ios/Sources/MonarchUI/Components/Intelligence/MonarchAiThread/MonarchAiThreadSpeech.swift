import Foundation

/// The spoken text of the newest message for ``MonarchAiThread``'s
/// announcer: the Markdown parsed incrementally with
/// ``MonarchAiStreamingText``'s model, so a growing reply is not parsed from
/// the start on every token, and read as speech with code blocks and tables
/// named. A reference, so the view can keep it between updates.
@MainActor
final class MonarchAiThreadSpeech {
    private var messageID: String?
    private var model = MonarchAiStreamingTextModel()

    /// The spoken text of a message. A different message starts a new parse.
    func spoken(_ message: MonarchAiConversationMessage, codeBlock: String, table: String) -> String {
        if message.id != messageID {
            messageID = message.id
            model = MonarchAiStreamingTextModel()
        }
        let isStreaming = message.isStreaming
        let display = model.display(message.text, isStreaming: isStreaming)
        return model.spoken(display, isStreaming: isStreaming, codeBlock: codeBlock, table: table).text
    }
}
