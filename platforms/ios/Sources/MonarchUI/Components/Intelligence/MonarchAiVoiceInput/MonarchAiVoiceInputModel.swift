import Foundation
import Observation
import SwiftUI

/// The dictation session behind ``MonarchAiVoiceInput``: it consumes a
/// recogniser's events and turns them into state, a transcript and
/// announcements. Separate from the view so it is tested without one.
@MainActor
@Observable
final class MonarchAiVoiceInputModel {
    /// Where dictation is.
    private(set) var state: MonarchAiVoiceInputState = .idle
    /// The live transcript of this session.
    private(set) var preview = ""
    /// The input level, 0 through 1.
    private(set) var level: Double = 0
    /// Why the last session failed, or `nil`.
    private(set) var failure: MonarchAiSpeechError?

    /// Receives recognised text: guesses with `false`, settled phrases with `true`.
    @ObservationIgnored var onTranscript: (@MainActor (String, Bool) -> Void)?
    /// Receives every state change.
    @ObservationIgnored var onStateChange: (@MainActor (MonarchAiVoiceInputState) -> Void)?
    /// Posts a spoken announcement. Replaced in tests.
    @ObservationIgnored var announce: @MainActor (String) -> Void = {
        AccessibilityNotification.Announcement($0).post()
    }
    /// The strings of the announcements.
    @ObservationIgnored var labels = MonarchAiVoiceInputLabels()
    /// The task that reads the current session's events.
    @ObservationIgnored private(set) var task: Task<Void, Never>?

    @ObservationIgnored private var recognizer: (any MonarchAiSpeechRecognizer)?
    @ObservationIgnored private var committed = ""
    @ObservationIgnored private var session = 0

    /// Whether a session is starting or listening.
    var isActive: Bool { MonarchAiVoiceInputTokens.isActive(state) }

    /// Starts a session, unless one is running.
    func start(with recognizer: any MonarchAiSpeechRecognizer, locale: Locale?, continuous: Bool) {
        guard !isActive else { return }
        session += 1
        let id = session
        self.recognizer = recognizer
        committed = ""
        preview = ""
        failure = nil
        level = 0
        setState(.starting)
        announce(labels.listening ?? MonarchAiVoiceInputStrings.listening)
        let stream = recognizer.start(locale: locale, continuous: continuous)
        task = Task { [weak self] in
            do {
                for try await event in stream {
                    guard let self, self.session == id else { return }
                    self.handle(event)
                }
                self?.finish(nil, session: id)
            } catch {
                self?.finish(error, session: id)
            }
        }
    }

    /// Asks the recogniser to stop; the session ends when its last words are in.
    func stop() {
        guard isActive else { return }
        recognizer?.stop()
    }

    /// Ends the session at once and discards pending words, for example when the view goes away.
    func cancel() {
        session += 1
        task?.cancel()
        task = nil
        recognizer = nil
        level = 0
        if isActive { setState(.idle) }
    }

    /// Applies one event of the current session.
    func handle(_ event: MonarchAiSpeechEvent) {
        switch event {
        case .started:
            setState(.listening)
        case .level(let value):
            level = MonarchAiVoiceInputTokens.clampedLevel(value)
        case .partial(let text):
            let text = text.trimmingCharacters(in: .whitespacesAndNewlines)
            setState(.listening)
            guard !text.isEmpty else { return }
            preview = MonarchAiVoiceInputTokens.preview(committed: committed, interim: text)
            onTranscript?(text, false)
        case .final(let text):
            let text = text.trimmingCharacters(in: .whitespacesAndNewlines)
            setState(.listening)
            guard !text.isEmpty else { return }
            committed = MonarchAiVoiceInputTokens.commit(committed, phrase: text)
            preview = committed
            onTranscript?(text, true)
        }
    }

    /// Ends the session: normally, cancelled, or with a failure.
    func finish(_ error: (any Error)?, session id: Int) {
        guard id == session else { return }
        task = nil
        recognizer = nil
        level = 0
        switch error {
        case nil:
            if isActive {
                setState(.idle)
                announce(labels.stopped ?? MonarchAiVoiceInputStrings.stopped)
            }
        case is CancellationError:
            if isActive { setState(.idle) }
        default:
            let failure = (error as? MonarchAiSpeechError) ?? .failed
            self.failure = failure
            setState(MonarchAiVoiceInputTokens.state(for: failure))
            announce(MonarchAiVoiceInputTokens.message(failure, labels: labels))
        }
    }

    /// The number of the current session, for ``finish(_:session:)``.
    var currentSession: Int { session }

    private func setState(_ next: MonarchAiVoiceInputState) {
        guard state != next else { return }
        state = next
        onStateChange?(next)
    }
}
