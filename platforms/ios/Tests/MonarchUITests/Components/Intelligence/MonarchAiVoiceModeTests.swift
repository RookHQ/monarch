import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private func voiceMessage(
    _ id: String,
    _ role: MonarchAiRole,
    _ text: String,
    streaming: Bool = false,
    agent: MonarchAiAgentRef? = nil
) -> MonarchAiConversationMessage {
    MonarchAiConversationMessage(
        id: id,
        role: role,
        parts: [.text(MonarchAiTextPart(text: text, state: streaming ? .streaming : .done))],
        agent: agent,
        createdAt: "2025-03-10T12:00:00.000Z"
    )
}

private let voiceCaptions = [
    voiceMessage("1", .user, "What is on my calendar today?"),
    voiceMessage("2", .assistant, "You have two meetings: design review at ten and a call with Sam at three."),
    voiceMessage("3", .user, "Move the call to", streaming: true),
]

enum MonarchAiVoiceModeFixtures: ComponentFixtures {
    static let id = "ai-voice-mode"
    static let fixtures = [
        Fixture("every state, compact") {
            VStack {
                ForEach(MonarchAiVoiceModeState.allCases, id: \.self) { state in
                    MonarchAiVoiceMode(
                        state: state,
                        level: 0.7,
                        agent: MonarchAiAgentRef(name: "Ada"),
                        elapsedSeconds: 245,
                        size: .compact,
                        onMuteChange: { _ in },
                        onInterrupt: {},
                        onEnd: {},
                        onRestart: {},
                        onRetry: {}
                    )
                }
            }
        },
        Fixture("full size with captions") {
            MonarchAiVoiceMode(
                state: .speaking,
                level: 0.5,
                agent: MonarchAiAgentRef(name: "Ada"),
                captions: voiceCaptions,
                elapsedSeconds: 3729,
                onMuteChange: { _ in },
                onInterrupt: {},
                onEnd: {}
            )
            .frame(height: 700)
        },
        Fixture("muted while thinking, captions hidden, no handlers") {
            MonarchAiVoiceMode(
                state: .thinking,
                captions: voiceCaptions,
                captionsVisible: .constant(false),
                isMuted: true,
                size: .compact
            )
        },
        Fixture("empty captions and a long error") {
            VStack {
                MonarchAiVoiceMode(state: .listening, size: .compact, onEnd: {})
                MonarchAiVoiceMode(
                    state: .error,
                    error: "The connection to the voice service was lost and could not be restored after three tries.",
                    size: .compact,
                    onRetry: {}
                )
            }
        },
    ]
}

@Suite("MonarchAiVoiceMode")
struct MonarchAiVoiceModeTests {
    typealias Tokens = MonarchAiVoiceModeTokens

    @Test("Only ended and error are not live")
    func live() {
        let live = MonarchAiVoiceModeState.allCases.filter(Tokens.isLive)
        #expect(Set(live) == [.connecting, .listening, .thinking, .speaking, .muted, .reconnecting])
    }

    @Test("Muted follows the app's value, the muted state by default, and never after the call")
    func muted() {
        #expect(Tokens.isMuted(.muted, isMuted: nil))
        #expect(!Tokens.isMuted(.listening, isMuted: nil))
        #expect(Tokens.isMuted(.speaking, isMuted: true))
        #expect(!Tokens.isMuted(.muted, isMuted: false))
        #expect(!Tokens.isMuted(.ended, isMuted: true))
        #expect(!Tokens.isMuted(.error, isMuted: true))
    }

    @Test("Interrupt shows only while the assistant thinks or speaks, with a handler")
    func interrupt() {
        #expect(Tokens.canInterrupt(.thinking, hasHandler: true))
        #expect(Tokens.canInterrupt(.speaking, hasHandler: true))
        #expect(!Tokens.canInterrupt(.speaking, hasHandler: false))
        #expect(!Tokens.canInterrupt(.listening, hasHandler: true))
        #expect(!Tokens.canInterrupt(.muted, hasHandler: true))
    }

    @Test("The level drives the visual only while someone talks, limited, and not with Reduce Motion")
    func level() {
        #expect(Tokens.shownLevel(0.6, state: .listening, reduceMotion: false) == 0.6)
        #expect(Tokens.shownLevel(3, state: .speaking, reduceMotion: false) == 1)
        #expect(Tokens.shownLevel(-1, state: .speaking, reduceMotion: false) == 0)
        #expect(Tokens.shownLevel(.nan, state: .speaking, reduceMotion: false) == 0)
        #expect(Tokens.shownLevel(0.6, state: .thinking, reduceMotion: false) == 0)
        #expect(Tokens.shownLevel(0.6, state: .speaking, reduceMotion: true) == 0.5)
    }

    @Test("Each state has a glyph, so the visual matches the written state")
    func glyphs() {
        #expect(Tokens.glyph(.error) == .error)
        #expect(Tokens.glyph(.muted) == .muted)
        #expect(Tokens.glyph(.listening) == .bars)
        #expect(Tokens.glyph(.speaking) == .bars)
        #expect(Tokens.glyph(.connecting) == .dots)
        #expect(Tokens.glyph(.thinking) == .dots)
        #expect(Tokens.glyph(.reconnecting) == .dots)
        #expect(Tokens.glyph(.ended) == .rest)
    }

    @Test("Visual colours: AI roles while live, danger on error, neutral when muted or ended")
    func colours() {
        #expect(Tokens.visualRoles(.speaking).orb == \MonarchColorTokens.aiSolid)
        #expect(Tokens.visualRoles(.speaking).glyph == \MonarchColorTokens.aiOnSolid)
        #expect(Tokens.visualRoles(.error).orb == \MonarchColorTokens.dangerSolid)
        #expect(Tokens.visualRoles(.muted).glyph == \MonarchColorTokens.textMuted)
        #expect(Tokens.visualRoles(.ended) == Tokens.visualRoles(.muted))
    }

    @Test("The halo and the bars grow with the level and never vanish")
    func geometry() {
        #expect(Tokens.haloScale(level: 0) < Tokens.haloScale(level: 1))
        let quiet = Tokens.barHeights(level: 0)
        let loud = Tokens.barHeights(level: 1)
        #expect(quiet.count == Tokens.barCount)
        #expect(quiet.allSatisfy { $0 > 0 })
        #expect(zip(quiet, loud).allSatisfy { $0 < $1 })
        #expect(loud.max() == 1)
        #expect(loud == loud.reversed())
    }

    @Test("State changes are announced, except the first state and turns into listening or speaking")
    func announcements() {
        let labels = MonarchAiVoiceModeLabels()
        #expect(Tokens.stateAnnouncement(from: nil, to: .connecting, errorText: "", labels: labels) == nil)
        #expect(Tokens.stateAnnouncement(from: .connecting, to: .listening, errorText: "", labels: labels) == nil)
        #expect(Tokens.stateAnnouncement(from: .listening, to: .speaking, errorText: "", labels: labels) == nil)
        #expect(Tokens.stateAnnouncement(from: .speaking, to: .thinking, errorText: "", labels: labels) == "Thinking…")
        #expect(Tokens.stateAnnouncement(from: .thinking, to: .thinking, errorText: "", labels: labels) == nil)
        #expect(
            Tokens.stateAnnouncement(from: .speaking, to: .error, errorText: "Lost the line.", labels: labels)
                == "Something went wrong: Lost the line."
        )
        #expect(
            Tokens.stateAnnouncement(from: .speaking, to: .ended, errorText: "", labels: labels) == "Conversation ended"
        )
    }

    @Test("Durations are clocks with minutes, or hours past an hour")
    func durations() {
        let locale = Locale(identifier: "en_US")
        #expect(Tokens.duration(0, locale: locale) == "0:00")
        #expect(Tokens.duration(245, locale: locale) == "4:05")
        #expect(Tokens.duration(3729, locale: locale) == "1:02:09")
        #expect(Tokens.duration(-5, locale: locale) == "0:00")
        let labels = MonarchAiVoiceModeLabels()
        #expect(Tokens.elapsedLabel("4:05", labels: labels) == "Call time: 4:05")
        #expect(Tokens.lasted("4:05", labels: labels) == "Lasted 4:05")
    }

    @Test("Captions come from user and assistant messages; streaming ones are interim")
    func captions() {
        let messages =
            voiceCaptions + [
                voiceMessage("4", .system, "You are a helpful assistant."),
                voiceMessage("5", .assistant, "   "),
                voiceMessage("6", .assistant, "Done.", agent: MonarchAiAgentRef(name: "Planner")),
            ]
        let lines = Tokens.captions(messages, agentName: "Ada", labels: MonarchAiVoiceModeLabels())
        #expect(lines.map(\.id) == ["1", "2", "3", "6"])
        #expect(lines.map(\.speaker) == ["You", "Ada", "You", "Planner"])
        #expect(lines.map(\.isFinal) == [true, true, false, true])
        #expect(lines[0].accessibilityLabel == "You: What is on my calendar today?")
        #expect(lines[0].isUser && !lines[1].isUser)
    }

    @Test("Each final caption is announced once")
    func captionAnnouncements() {
        let lines = Tokens.captions(voiceCaptions, agentName: "Ada", labels: MonarchAiVoiceModeLabels())
        #expect(Tokens.unannounced(lines, announced: []).map(\.id) == ["1", "2"])
        #expect(Tokens.unannounced(lines, announced: ["1"]).map(\.id) == ["2"])
        // The interim caption becomes final: now it is due.
        let settled = Tokens.captions(
            [voiceMessage("3", .user, "Move the call to four.")],
            agentName: "Ada",
            labels: MonarchAiVoiceModeLabels()
        )
        #expect(Tokens.unannounced(settled, announced: ["1", "2"]).map(\.id) == ["3"])
    }

    @Test("Names, the surface label and the state text, with overrides")
    func names() {
        let builtIn = MonarchAiVoiceModeLabels()
        #expect(Tokens.agentName(nil, labels: builtIn) == "Assistant")
        #expect(Tokens.agentName(MonarchAiAgentRef(name: ""), labels: builtIn) == "Assistant")
        #expect(Tokens.agentName(MonarchAiAgentRef(name: "Ada"), labels: builtIn) == "Ada")
        #expect(Tokens.surfaceLabel(agentName: "Ada", labels: builtIn) == "Voice conversation with Ada")
        let custom = MonarchAiVoiceModeLabels(
            label: { "Gespräch mit \($0)" },
            assistant: "Assistent",
            you: "Du",
            states: [.muted: "Stumm"]
        )
        #expect(Tokens.surfaceLabel(agentName: "Ada", labels: custom) == "Gespräch mit Ada")
        #expect(Tokens.agentName(nil, labels: custom) == "Assistent")
        #expect(Tokens.stateText(.muted, labels: custom) == "Stumm")
        #expect(Tokens.stateText(.speaking, labels: custom) == "Speaking…")
        #expect(
            Tokens.captions([voiceMessage("1", .user, "Hi")], agentName: "A", labels: custom).first?.speaker == "Du"
        )
    }

    @Test("Every built-in string resolves")
    func strings() {
        let all =
            MonarchAiVoiceModeState.allCases.map(MonarchAiVoiceModeStrings.state) + [
                MonarchAiVoiceModeStrings.assistant, MonarchAiVoiceModeStrings.you,
                MonarchAiVoiceModeStrings.microphoneMuted, MonarchAiVoiceModeStrings.mute,
                MonarchAiVoiceModeStrings.interrupt, MonarchAiVoiceModeStrings.captions,
                MonarchAiVoiceModeStrings.end, MonarchAiVoiceModeStrings.restart,
                MonarchAiVoiceModeStrings.retry, MonarchAiVoiceModeStrings.captionsEmpty,
                MonarchAiVoiceModeStrings.elapsed, MonarchAiVoiceModeStrings.errorFallback,
                MonarchAiVoiceModeStrings.label("Ada"), MonarchAiVoiceModeStrings.lasted("1:00"),
            ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(Set(MonarchAiVoiceModeState.allCases.map(MonarchAiVoiceModeStrings.state)).count == 8)
    }

    #if os(iOS)
        @Test("Compact controls keep 44-point touch targets")
        @MainActor
        func touchTargets() {
            let size = Host.layout(
                MonarchAiVoiceMode(state: .ended, size: .compact, onRestart: {})
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
