import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiVoiceMode``.
enum MonarchAiVoiceModeTokens {
    // MARK: - Sizes

    /// The diameter of the central orb, before Dynamic Type scaling.
    static func orbSize(_ size: MonarchAiVoiceModeSize) -> CGFloat {
        switch size {
        case .md: MonarchTokens.Space.space24
        case .compact: MonarchTokens.Space.space16
        }
    }

    /// How much larger than the orb the halo grows at full level.
    static let haloGrowth: Double = 0.5
    /// The halo's size relative to the orb at rest.
    static let haloRest: Double = 1.2
    /// The number of level bars in the orb, as on the web.
    static let barCount = 5
    /// The bar heights relative to the tallest, from the leading bar to the trailing one.
    static let barProfile: [Double] = [0.5, 0.8, 1, 0.8, 0.5]
    /// The smallest bar height, as a share of the full height, so bars never vanish.
    static let barFloor: Double = 0.25
    /// The share of the orb's diameter the bars span, in both directions.
    static let barArea: Double = 0.5

    /// The padding around the surface.
    static func padding(_ size: MonarchAiVoiceModeSize) -> CGFloat {
        switch size {
        case .md: MonarchTokens.Space.space6
        case .compact: MonarchTokens.Space.space3
        }
    }

    /// The gap between the stage, the captions and the controls.
    static func spacing(_ size: MonarchAiVoiceModeSize) -> CGFloat {
        switch size {
        case .md: MonarchTokens.Space.space6
        case .compact: MonarchTokens.Space.space3
        }
    }

    /// The gap between lines of text in the summary and the captions.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the controls.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space2
    /// The tallest the captions log grows before it scrolls, before Dynamic Type scaling.
    static func captionsHeight(_ size: MonarchAiVoiceModeSize) -> CGFloat {
        switch size {
        case .md: MonarchTokens.Space.space24 * 3
        case .compact: MonarchTokens.Space.space24 * 1.75
        }
    }

    /// The side of the agent's avatar, before Dynamic Type scaling.
    static let avatarSize: CGFloat = MonarchTokens.Space.space6

    /// The button size of the controls.
    static func buttonSize(_ size: MonarchAiVoiceModeSize) -> MonarchButtonSize {
        switch size {
        case .md: .md
        case .compact: .sm
        }
    }

    /// The type role of the state text.
    static func stateStyle(_ size: MonarchAiVoiceModeSize) -> MonarchTextStyle {
        size == .md ? .heading : .bodyStrong
    }

    // MARK: - States

    /// Whether the call is live: not ended and not failed.
    static func isLive(_ state: MonarchAiVoiceModeState) -> Bool {
        state != .ended && state != .error
    }

    /// Whether the microphone is muted: the app's value, or the muted state. Never while not live.
    static func isMuted(_ state: MonarchAiVoiceModeState, isMuted: Bool?) -> Bool {
        isLive(state) && (isMuted ?? (state == .muted))
    }

    /// Whether someone is talking, so the level drives the visual.
    static func isVoiced(_ state: MonarchAiVoiceModeState) -> Bool {
        state == .listening || state == .speaking
    }

    /// Whether the call is waiting: connecting, thinking or reconnecting.
    static func isBusy(_ state: MonarchAiVoiceModeState) -> Bool {
        state == .connecting || state == .thinking || state == .reconnecting
    }

    /// Whether Interrupt is offered: while the assistant thinks or speaks, and only with a handler.
    static func canInterrupt(_ state: MonarchAiVoiceModeState, hasHandler: Bool) -> Bool {
        hasHandler && (state == .thinking || state == .speaking)
    }

    /// The level the visual follows: 0 through 1 while someone talks, 0
    /// otherwise and with Reduce Motion.
    static func shownLevel(_ level: Double, state: MonarchAiVoiceModeState, reduceMotion: Bool) -> Double {
        guard isVoiced(state), level.isFinite else { return 0 }
        // With Reduce Motion the bars hold the reading at rest instead of dancing: still bars, not a row of dots.
        return reduceMotion ? 0.5 : min(max(level, 0), 1)
    }

    /// What the orb shows.
    enum Glyph: Equatable {
        case error
        case muted
        case bars
        case dots
        case rest
    }

    /// The glyph in the orb for a state.
    static func glyph(_ state: MonarchAiVoiceModeState) -> Glyph {
        if state == .error { return .error }
        if state == .muted { return .muted }
        if isVoiced(state) { return .bars }
        if isBusy(state) { return .dots }
        return .rest
    }

    /// The colour roles of the visual: halo, orb and glyph.
    struct VisualRoles: Equatable {
        let halo: MonarchColorRole
        let orb: MonarchColorRole
        let glyph: MonarchColorRole
        /// The outline of the orb.
        let ring: MonarchColorRole
    }

    /// The colours of the visual for a state.
    static func visualRoles(_ state: MonarchAiVoiceModeState) -> VisualRoles {
        switch state {
        case .error: VisualRoles(halo: \.dangerSubtle, orb: \.dangerSolid, glyph: \.dangerOnSolid, ring: \.dangerSolid)
        case .muted, .ended: VisualRoles(halo: \.bgSubtle, orb: \.bgEmphasis, glyph: \.textMuted, ring: \.bgEmphasis)
        // As the web: speaking is a filled AI orb with a halo; every other live state is an outlined, tinted orb.
        case .speaking: VisualRoles(halo: \.aiSubtle, orb: \.aiSolid, glyph: \.aiOnSolid, ring: \.aiSolid)
        default: VisualRoles(halo: \.bgSurface, orb: \.aiSubtle, glyph: \.aiStrong, ring: \.aiBorder)
        }
    }

    /// The halo's diameter relative to the orb.
    static func haloScale(level: Double) -> Double {
        haloRest + level * haloGrowth
    }

    /// The bar heights as shares of the bar area, from the level.
    static func barHeights(level: Double) -> [Double] {
        barProfile.map { $0 * (barFloor + (1 - barFloor) * level) }
    }

    // MARK: - Text

    /// The assistant's name: the agent's, or the built-in "Assistant".
    static func agentName(_ agent: MonarchAiAgentRef?, labels: MonarchAiVoiceModeLabels) -> String {
        if let name = agent?.name, !name.isEmpty { return name }
        return labels.assistant ?? MonarchAiVoiceModeStrings.assistant
    }

    /// The spoken name of the surface.
    static func surfaceLabel(agentName: String, labels: MonarchAiVoiceModeLabels) -> String {
        labels.label?(agentName) ?? MonarchAiVoiceModeStrings.label(agentName)
    }

    /// The visible state text.
    static func stateText(_ state: MonarchAiVoiceModeState, labels: MonarchAiVoiceModeLabels) -> String {
        labels.states[state] ?? MonarchAiVoiceModeStrings.state(state)
    }

    /// What is announced when the state changes, or `nil`. The first state is
    /// not announced, and flips into listening or speaking stay silent: the
    /// audio itself says whose turn it is.
    static func stateAnnouncement(
        from old: MonarchAiVoiceModeState?,
        to new: MonarchAiVoiceModeState,
        errorText: String,
        labels: MonarchAiVoiceModeLabels
    ) -> String? {
        guard let old, old != new, !isVoiced(new) else { return nil }
        let text = stateText(new, labels: labels)
        return new == .error ? "\(text): \(errorText)" : text
    }

    /// The running time as a clock with the locale's digits: `4:05`, or `1:02:09` past an hour.
    static func duration(_ seconds: Int, locale: Locale) -> String {
        let total = max(0, seconds)
        let pattern: Duration.TimeFormatStyle.Pattern = total >= 3600 ? .hourMinuteSecond : .minuteSecond
        return Duration.seconds(total).formatted(.time(pattern: pattern).locale(locale))
    }

    /// What VoiceOver reads for the running time: "Call time: 4:05".
    static func elapsedLabel(_ duration: String, labels: MonarchAiVoiceModeLabels) -> String {
        "\(labels.elapsed ?? MonarchAiVoiceModeStrings.elapsed): \(duration)"
    }

    /// The duration line of an ended call: "Lasted 4:05".
    static func lasted(_ duration: String, labels: MonarchAiVoiceModeLabels) -> String {
        labels.lasted?(duration) ?? MonarchAiVoiceModeStrings.lasted(duration)
    }

    // MARK: - Captions

    /// One line of captions, from a conversation message.
    struct Caption: Equatable, Identifiable {
        let id: String
        let isUser: Bool
        let speaker: String
        let text: String
        let isFinal: Bool

        /// What VoiceOver reads: "You: Book a table for two".
        var accessibilityLabel: String { "\(speaker): \(text)" }
    }

    /// The captions of the person's and the assistant's messages, oldest
    /// first. A message is final once none of its text is streaming. Other
    /// roles and empty messages are left out.
    static func captions(
        _ messages: [MonarchAiConversationMessage],
        agentName: String,
        labels: MonarchAiVoiceModeLabels
    ) -> [Caption] {
        let you = labels.you ?? MonarchAiVoiceModeStrings.you
        return messages.compactMap { message in
            guard message.role == .user || message.role == .assistant else { return nil }
            let text = message.text.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return nil }
            let isUser = message.role == .user
            let speaker = isUser ? you : (message.agent?.name ?? agentName)
            return Caption(id: message.id, isUser: isUser, speaker: speaker, text: text, isFinal: !message.isStreaming)
        }
    }

    /// The final captions that have not been announced yet, oldest first.
    static func unannounced(_ captions: [Caption], announced: Set<String>) -> [Caption] {
        captions.filter { $0.isFinal && !announced.contains($0.id) }
    }
}
