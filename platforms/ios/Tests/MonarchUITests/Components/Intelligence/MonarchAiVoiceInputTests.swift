import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A recogniser whose events the test sends by hand.
@MainActor
final class VoiceInputTestRecognizer: MonarchAiSpeechRecognizer {
    var available = true
    private(set) var continuation: AsyncThrowingStream<MonarchAiSpeechEvent, any Error>.Continuation?
    private(set) var startCount = 0
    private(set) var stopCount = 0
    private(set) var lastContinuous: Bool?
    private(set) var lastLocale: Locale?

    func isAvailable(for locale: Locale?) -> Bool { available }

    func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error> {
        startCount += 1
        lastContinuous = continuous
        lastLocale = locale
        let (stream, continuation) = AsyncThrowingStream<MonarchAiSpeechEvent, any Error>.makeStream()
        self.continuation = continuation
        return stream
    }

    func stop() {
        stopCount += 1
    }
}

enum MonarchAiVoiceInputFixtures: ComponentFixtures {
    static let id = "ai-voice-input"
    static let fixtures = [
        Fixture("toggle and push to talk, every size") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    HStack {
                        MonarchAiVoiceInput(size: size, recognizer: VoiceInputTestRecognizer()) { _, _ in }
                        MonarchAiVoiceInput(mode: .pushToTalk, size: size, recognizer: VoiceInputTestRecognizer()) {
                            _,
                            _ in
                        }
                    }
                }
            }
        },
        Fixture("disabled") {
            MonarchAiVoiceInput(recognizer: VoiceInputTestRecognizer()) { _, _ in }.disabled(true)
        },
        Fixture("unavailable draws nothing") {
            let recognizer = VoiceInputTestRecognizer()
            recognizer.available = false
            return VStack {
                Text("Prompt")
                MonarchAiVoiceInput(recognizer: recognizer) { _, _ in }
            }
        },
        Fixture("translated labels") {
            MonarchAiVoiceInput(
                mode: .pushToTalk,
                recognizer: VoiceInputTestRecognizer(),
                labels: MonarchAiVoiceInputLabels(holdToTalk: "Gedrückt halten, um zu sprechen")
            ) { _, _ in }
        },
    ]
}

@Suite("MonarchAiVoiceInput")
@MainActor
struct MonarchAiVoiceInputTests {
    typealias Tokens = MonarchAiVoiceInputTokens

    private func model() -> (MonarchAiVoiceInputModel, Recorder) {
        let model = MonarchAiVoiceInputModel()
        let recorder = Recorder()
        model.announce = { recorder.announcements.append($0) }
        model.onTranscript = { recorder.transcripts.append(($0, $1)) }
        model.onStateChange = { recorder.states.append($0) }
        return (model, recorder)
    }

    @MainActor
    final class Recorder {
        var announcements: [String] = []
        var transcripts: [(String, Bool)] = []
        var states: [MonarchAiVoiceInputState] = []
    }

    @Test("Starting announces listening and moves to starting, then listening when the microphone opens")
    func start() {
        let (model, recorder) = model()
        let recognizer = VoiceInputTestRecognizer()
        model.start(with: recognizer, locale: Locale(identifier: "de-DE"), continuous: true)
        #expect(model.state == .starting)
        #expect(recognizer.startCount == 1)
        #expect(recognizer.lastContinuous == true)
        #expect(recognizer.lastLocale == Locale(identifier: "de-DE"))
        #expect(recorder.announcements == ["Listening…"])
        model.handle(.started)
        #expect(model.state == .listening)
        #expect(recorder.states == [.starting, .listening])
        // A second start while active does nothing.
        model.start(with: recognizer, locale: nil, continuous: true)
        #expect(recognizer.startCount == 1)
    }

    @Test("Guesses update the preview without committing; settled phrases are passed once")
    func transcript() {
        let (model, recorder) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        model.handle(.partial("book a"))
        #expect(model.preview == "book a")
        model.handle(.partial(" book a table "))
        #expect(model.preview == "book a table")
        model.handle(.final("Book a table."))
        #expect(model.preview == "Book a table.")
        model.handle(.partial("for two"))
        #expect(model.preview == "Book a table. for two")
        model.handle(.final("For two."))
        #expect(model.preview == "Book a table. For two.")
        model.handle(.partial("   "))
        #expect(recorder.transcripts.map(\.0) == ["book a", "book a table", "Book a table.", "for two", "For two."])
        #expect(recorder.transcripts.map(\.1) == [false, false, true, false, true])
    }

    @Test("Levels are limited to 0 through 1")
    func levels() {
        let (model, _) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        model.handle(.level(0.4))
        #expect(model.level == 0.4)
        model.handle(.level(7))
        #expect(model.level == 1)
        model.handle(.level(-1))
        #expect(model.level == 0)
        model.handle(.level(.nan))
        #expect(model.level == 0)
    }

    @Test("Stopping asks the recogniser; the end of the stream returns to idle and announces it")
    func stopAndFinish() {
        let (model, recorder) = model()
        let recognizer = VoiceInputTestRecognizer()
        model.start(with: recognizer, locale: nil, continuous: false)
        model.handle(.started)
        model.stop()
        #expect(recognizer.stopCount == 1)
        model.finish(nil, session: model.currentSession)
        #expect(model.state == .idle)
        #expect(model.level == 0)
        #expect(recorder.announcements.last == "Stopped listening.")
        // Stopping while idle does nothing.
        model.stop()
        #expect(recognizer.stopCount == 1)
    }

    @Test("A refusal leads to denied with guidance; other failures to error", arguments: MonarchAiSpeechError.allCases)
    func failures(error: MonarchAiSpeechError) {
        let (model, recorder) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        model.finish(error, session: model.currentSession)
        #expect(model.failure == error)
        #expect(model.state == (error == .denied ? .denied : .error))
        #expect(recorder.announcements.last == Tokens.message(error, labels: MonarchAiVoiceInputLabels()))
    }

    @Test("An unknown error counts as a failure; a cancellation does not")
    func otherErrors() {
        let (model, recorder) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        model.finish(URLError(.notConnectedToInternet), session: model.currentSession)
        #expect(model.failure == .failed)
        #expect(model.state == .error)

        let (cancelled, cancelledRecorder) = self.model()
        cancelled.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        cancelled.finish(CancellationError(), session: cancelled.currentSession)
        #expect(cancelled.state == .idle)
        #expect(cancelled.failure == nil)
        #expect(cancelledRecorder.announcements == ["Listening…"])
        _ = recorder
    }

    @Test("A new session clears the last failure and the preview")
    func restart() {
        let (model, _) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        model.handle(.final("Hello"))
        model.finish(MonarchAiSpeechError.noSpeech, session: model.currentSession)
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        #expect(model.failure == nil)
        #expect(model.preview.isEmpty)
        #expect(model.state == .starting)
    }

    @Test("Cancelling ends the session at once and ignores its late end")
    func cancel() {
        let (model, recorder) = model()
        model.start(with: VoiceInputTestRecognizer(), locale: nil, continuous: true)
        let session = model.currentSession
        model.cancel()
        #expect(model.state == .idle)
        model.finish(MonarchAiSpeechError.failed, session: session)
        #expect(model.failure == nil)
        #expect(recorder.announcements == ["Listening…"])
    }

    @Test("Events from the recogniser's stream reach the model")
    func stream() async {
        let (model, recorder) = model()
        let recognizer = VoiceInputTestRecognizer()
        model.start(with: recognizer, locale: nil, continuous: true)
        recognizer.continuation?.yield(.started)
        recognizer.continuation?.yield(.final("Hello"))
        recognizer.continuation?.finish()
        await model.task?.value
        #expect(recorder.transcripts.map(\.0) == ["Hello"])
        #expect(model.state == .idle)
        #expect(recorder.states == [.starting, .listening, .idle])
    }

    @Test("A failure thrown by the stream shows its message")
    func streamFailure() async {
        let (model, _) = model()
        let recognizer = VoiceInputTestRecognizer()
        model.start(with: recognizer, locale: nil, continuous: true)
        recognizer.continuation?.finish(throwing: MonarchAiSpeechError.denied)
        await model.task?.value
        #expect(model.state == .denied)
        #expect(model.failure == .denied)
    }

    @Test("Status text shows only while starting or listening")
    func status() {
        let labels = MonarchAiVoiceInputLabels()
        #expect(Tokens.statusText(.starting, labels: labels) == "Starting microphone…")
        #expect(Tokens.statusText(.listening, labels: labels) == "Listening…")
        #expect(Tokens.statusText(.idle, labels: labels) == nil)
        #expect(Tokens.statusText(.denied, labels: labels) == nil)
        #expect(Tokens.statusText(.error, labels: labels) == nil)
        #expect(Tokens.isActive(.starting) && Tokens.isActive(.listening))
        #expect(!Tokens.isActive(.idle) && !Tokens.isActive(.denied) && !Tokens.isActive(.error))
    }

    @Test("Every failure has its own message, tone and an icon through the notice")
    func messages() {
        let labels = MonarchAiVoiceInputLabels()
        let messages = MonarchAiSpeechError.allCases.map { Tokens.message($0, labels: labels) }
        #expect(Set(messages).count == messages.count)
        #expect(messages.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(Tokens.tone(.denied) == .warning)
        #expect(Tokens.tone(.failed) == .danger)
        #expect(Tokens.tone(.noSpeech) == .info)
        #expect(Tokens.offersSettings(.denied))
        #expect(!Tokens.offersSettings(.failed))
    }

    @Test("Button titles, the transcript label and overrides")
    func labels() {
        let builtIn = MonarchAiVoiceInputLabels()
        #expect(Tokens.buttonTitle(mode: .toggle, labels: builtIn) == "Dictate")
        #expect(Tokens.buttonTitle(mode: .pushToTalk, labels: builtIn) == "Hold to talk")
        #expect(Tokens.transcriptLabel("book a table", labels: builtIn) == "Transcript: book a table")
        let custom = MonarchAiVoiceInputLabels(
            dictate: "Diktieren",
            listening: "Hört zu …",
            denied: "Kein Zugriff",
            transcript: "Text"
        )
        #expect(Tokens.buttonTitle(mode: .toggle, labels: custom) == "Diktieren")
        #expect(Tokens.statusText(.listening, labels: custom) == "Hört zu …")
        #expect(Tokens.message(.denied, labels: custom) == "Kein Zugriff")
        #expect(Tokens.transcriptLabel("x", labels: custom) == "Text: x")
    }

    @Test("Every built-in string resolves")
    func strings() {
        let all = [
            MonarchAiVoiceInputStrings.dictate, MonarchAiVoiceInputStrings.holdToTalk,
            MonarchAiVoiceInputStrings.holdHint, MonarchAiVoiceInputStrings.starting,
            MonarchAiVoiceInputStrings.listening, MonarchAiVoiceInputStrings.stopped,
            MonarchAiVoiceInputStrings.denied, MonarchAiVoiceInputStrings.noSpeech,
            MonarchAiVoiceInputStrings.noMicrophone, MonarchAiVoiceInputStrings.unavailable,
            MonarchAiVoiceInputStrings.error, MonarchAiVoiceInputStrings.transcript,
            MonarchAiVoiceInputStrings.openSettings,
        ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
    }

    @Test("Speech framework errors map to failures; cancellations are not failures")
    func systemErrors() {
        func error(_ code: Int) -> NSError { NSError(domain: "kAFAssistantErrorDomain", code: code) }
        #expect(MonarchAiSystemSpeechRecognizer.speechError(error(1110)) == .noSpeech)
        #expect(MonarchAiSystemSpeechRecognizer.speechError(error(216)) == nil)
        #expect(MonarchAiSystemSpeechRecognizer.speechError(error(1700)) == .denied)
        #expect(MonarchAiSystemSpeechRecognizer.speechError(error(1101)) == .unavailable)
        #expect(MonarchAiSystemSpeechRecognizer.speechError(error(9999)) == .failed)
        #expect(MonarchAiSystemSpeechRecognizer.speechError(URLError(.badURL)) == .failed)
    }

    #if os(iOS)
        @Test("Both buttons keep a 44-point touch target at the small size")
        func touchTarget() {
            for mode in MonarchAiVoiceInputMode.allCases {
                let size = Host.layout(
                    MonarchAiVoiceInput(mode: mode, size: .sm, recognizer: VoiceInputTestRecognizer()) { _, _ in }
                        .fixedSize()
                )
                #expect(size.height >= MonarchAccessibility.minimumHitTarget)
            }
        }
    #endif
}
