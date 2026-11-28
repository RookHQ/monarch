import MonarchUI
import SwiftUI

/// The catalogue plays a scripted call: the app side (audio, connection,
/// captions) is simulated so every state can be seen.
struct MonarchAiVoiceModeSection: CataloguePage {
    static let title = "AI voice mode"

    @State private var isPresented = false
    @State private var call = VoiceModeDemoCall()
    @State private var compactState: MonarchAiVoiceModeState = .listening

    var body: some View {
        CatalogueCaption("Full screen, presented with fullScreenCover")
        MonarchButton("Start a voice conversation", systemImage: "waveform", variant: .primary) {
            call.start()
            isPresented = true
        }
        .fullScreenCover(isPresented: $isPresented) {
            MonarchAiVoiceMode(
                state: call.state,
                level: call.level,
                agent: MonarchAiAgentRef(name: "Ada"),
                captions: call.messages,
                isMuted: call.isMuted,
                elapsedSeconds: call.seconds,
                onMuteChange: { call.isMuted = $0 },
                onInterrupt: { call.interrupt() },
                onEnd: {
                    call.end()
                    isPresented = false
                }
            )
        }
        CatalogueCaption("Compact, every state")
        Picker("State", selection: $compactState) {
            ForEach(MonarchAiVoiceModeState.allCases, id: \.self) { state in
                Text(String(describing: state)).tag(state)
            }
        }
        MonarchAiVoiceMode(
            state: compactState,
            level: 0.6,
            agent: MonarchAiAgentRef(name: "Ada"),
            elapsedSeconds: 245,
            size: .compact,
            onMuteChange: { compactState = $0 ? .muted : .listening },
            onInterrupt: { compactState = .listening },
            onEnd: { compactState = .ended },
            onRestart: { compactState = .connecting },
            onRetry: { compactState = .connecting }
        )
    }
}

/// A pretend call: it connects, alternates turns and streams captions.
@MainActor
@Observable
private final class VoiceModeDemoCall {
    var state: MonarchAiVoiceModeState = .connecting
    var level: Double = 0
    var isMuted = false
    var seconds = 0
    var messages: [MonarchAiConversationMessage] = []
    private var task: Task<Void, Never>?

    func start() {
        task?.cancel()
        state = .connecting
        seconds = 0
        messages = []
        task = Task { [weak self] in await self?.run() }
    }

    func interrupt() {
        state = .listening
    }

    func end() {
        task?.cancel()
        state = .ended
    }

    private func run() async {
        try? await Task.sleep(for: .seconds(1))
        let turns: [(MonarchAiRole, String)] = [
            (.user, "What is on my calendar today?"),
            (.assistant, "You have two meetings: design review at ten and a call with Sam at three."),
            (.user, "Move the call with Sam to four."),
            (.assistant, "Done. The call with Sam is now at four."),
        ]
        for (index, turn) in turns.enumerated() where !Task.isCancelled {
            state = turn.0 == .user ? (isMuted ? .muted : .listening) : .speaking
            var words: [String] = []
            for word in turn.1.split(separator: " ") where !Task.isCancelled {
                words.append(String(word))
                level = Double.random(in: 0.2...0.9)
                show(id: "\(index)", role: turn.0, text: words.joined(separator: " "), streaming: true)
                try? await Task.sleep(for: .milliseconds(250))
                seconds += 1
            }
            show(id: "\(index)", role: turn.0, text: turn.1, streaming: false)
            level = 0
            if turn.0 == .user {
                state = .thinking
                try? await Task.sleep(for: .seconds(1))
            }
        }
        if !Task.isCancelled { state = isMuted ? .muted : .listening }
    }

    private func show(id: String, role: MonarchAiRole, text: String, streaming: Bool) {
        let message = MonarchAiConversationMessage(
            id: id,
            role: role,
            parts: [.text(MonarchAiTextPart(text: text, state: streaming ? .streaming : .done))],
            createdAt: MonarchAiConversationMessage.timestamp(.now)
        )
        if let index = messages.firstIndex(where: { $0.id == id }) {
            messages[index] = message
        } else {
            messages.append(message)
        }
    }
}
