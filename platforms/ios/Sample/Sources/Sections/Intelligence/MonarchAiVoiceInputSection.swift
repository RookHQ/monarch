@_spi(Experimental) import MonarchUI
import SwiftUI

/// The catalogue app has no microphone or speech recognition usage strings,
/// so the page listens with a scripted recogniser. An app with those keys in
/// its Info.plist uses the default `MonarchAiSystemSpeechRecognizer`.
struct MonarchAiVoiceInputSection: CataloguePage {
    static let title = "AI voice input"

    @State private var prompt = ""
    @State private var state = "idle"
    @State private var scripted = VoiceInputDemoRecognizer(failure: nil)
    @State private var refused = VoiceInputDemoRecognizer(failure: .denied)

    var body: some View {
        CatalogueCaption("Press to dictate (scripted speech)")
        MonarchAiVoiceInput(
            recognizer: scripted,
            onStateChange: { state = String(describing: $0) },
            onTranscript: { text, isFinal in
                if isFinal { prompt += (prompt.isEmpty ? "" : " ") + text }
            }
        )
        Text("State: \(state)").monarchTextStyle(.caption)
        TextField("Prompt", text: $prompt, axis: .vertical)
            .textFieldStyle(.roundedBorder)
        CatalogueCaption("Hold to talk")
        MonarchAiVoiceInput(mode: .pushToTalk, size: .sm, recognizer: scripted) { text, isFinal in
            if isFinal { prompt += (prompt.isEmpty ? "" : " ") + text }
        }
        CatalogueCaption("Access turned off")
        MonarchAiVoiceInput(recognizer: refused) { _, _ in }
    }
}

/// Speaks a fixed sentence word by word with a moving level, or fails.
@MainActor
private final class VoiceInputDemoRecognizer: MonarchAiSpeechRecognizer {
    private let failure: MonarchAiSpeechError?
    private var stopped = false

    init(failure: MonarchAiSpeechError?) {
        self.failure = failure
    }

    func isAvailable(for locale: Locale?) -> Bool { true }

    func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error> {
        stopped = false
        let failure = failure
        let words = ["Summarise", "the", "meeting", "notes", "from", "Tuesday"]
        return AsyncThrowingStream { continuation in
            let task = Task { @MainActor [weak self] in
                try? await Task.sleep(for: .milliseconds(400))
                if let failure {
                    continuation.finish(throwing: failure)
                    return
                }
                continuation.yield(.started)
                var heard: [String] = []
                for word in words {
                    guard let self, !self.stopped, !Task.isCancelled else { break }
                    heard.append(word)
                    continuation.yield(.level(Double.random(in: 0.2...0.9)))
                    continuation.yield(.partial(heard.joined(separator: " ")))
                    try? await Task.sleep(for: .milliseconds(350))
                }
                if !heard.isEmpty { continuation.yield(.final(heard.joined(separator: " "))) }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func stop() {
        stopped = true
    }
}
