import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiVoiceInput``.
enum MonarchAiVoiceInputTokens {
    /// The gap between the button, the level meter and the status.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the controls, the transcript and the notice.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the button.
    static let microphoneSymbol = "mic"
    /// The SF Symbol of the button while listening.
    static let listeningSymbol = "mic.fill"
    /// The number of segments of the level meter, as on the web.
    static let meterSegments = 5
    /// The type role of the status and the transcript.
    static let statusStyle: MonarchTextStyle = .caption
    /// The type role of the transcript.
    static let transcriptStyle: MonarchTextStyle = .body

    /// Whether the state is a listening session: starting or listening.
    static func isActive(_ state: MonarchAiVoiceInputState) -> Bool {
        state == .starting || state == .listening
    }

    /// The title of the button for a mode.
    static func buttonTitle(mode: MonarchAiVoiceInputMode, labels: MonarchAiVoiceInputLabels) -> String {
        switch mode {
        case .toggle: labels.dictate ?? MonarchAiVoiceInputStrings.dictate
        case .pushToTalk: labels.holdToTalk ?? MonarchAiVoiceInputStrings.holdToTalk
        }
    }

    /// The visible status beside the button, or `nil` when there is none.
    static func statusText(_ state: MonarchAiVoiceInputState, labels: MonarchAiVoiceInputLabels) -> String? {
        switch state {
        case .starting: labels.starting ?? MonarchAiVoiceInputStrings.starting
        case .listening: labels.listening ?? MonarchAiVoiceInputStrings.listening
        case .idle, .denied, .error: nil
        }
    }

    /// The state a failure leads to.
    static func state(for error: MonarchAiSpeechError) -> MonarchAiVoiceInputState {
        error == .denied ? .denied : .error
    }

    /// The message shown for a failure.
    static func message(_ error: MonarchAiSpeechError, labels: MonarchAiVoiceInputLabels) -> String {
        switch error {
        case .denied: labels.denied ?? MonarchAiVoiceInputStrings.denied
        case .noSpeech: labels.noSpeech ?? MonarchAiVoiceInputStrings.noSpeech
        case .noMicrophone: labels.noMicrophone ?? MonarchAiVoiceInputStrings.noMicrophone
        case .unavailable: labels.unavailable ?? MonarchAiVoiceInputStrings.unavailable
        case .failed: labels.error ?? MonarchAiVoiceInputStrings.error
        }
    }

    /// The tone of the notice for a failure. The notice also has an icon and
    /// the tone's spoken name, so the tone is not colour alone.
    static func tone(_ error: MonarchAiSpeechError) -> MonarchTone {
        switch error {
        case .noSpeech: .info
        case .denied, .noMicrophone, .unavailable: .warning
        case .failed: .danger
        }
    }

    /// Whether the notice offers to open Settings: only when access is off.
    static func offersSettings(_ error: MonarchAiSpeechError) -> Bool {
        error == .denied
    }

    /// The live transcript: the settled words, then the current guess.
    static func preview(committed: String, interim: String) -> String {
        let interim = interim.trimmingCharacters(in: .whitespacesAndNewlines)
        if committed.isEmpty { return interim }
        if interim.isEmpty { return committed }
        return "\(committed) \(interim)"
    }

    /// The settled words with a new phrase added.
    static func commit(_ committed: String, phrase: String) -> String {
        preview(committed: committed, interim: phrase)
    }

    /// What VoiceOver reads for the transcript: "Transcript: book a table".
    static func transcriptLabel(_ text: String, labels: MonarchAiVoiceInputLabels) -> String {
        "\(labels.transcript ?? MonarchAiVoiceInputStrings.transcript): \(text)"
    }

    /// The meter level, limited to 0 through 1. Not-a-number gives 0.
    static func clampedLevel(_ level: Double) -> Double {
        level.isNaN ? 0 : min(max(level, 0), 1)
    }
}
