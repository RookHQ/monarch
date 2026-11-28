import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiThreadSection: CataloguePage {
    static let title = "AiThread"

    @State private var messages: [MonarchAiConversationMessage] = Self.history(count: 6)
    @State private var status: MonarchAiConversationStatus = .idle
    @State private var prompt = ""
    @State private var isOffline = false
    @State private var isSlow = false
    @State private var earlier = 3
    @State private var isLoadingEarlier = false
    @State private var reply: Task<Void, Never>?

    var body: some View {
        CatalogueCaption("A conversation above the prompt input")
        Toggle("Offline", isOn: $isOffline).monarchTextStyle(.body)
        Toggle("Slow reply", isOn: $isSlow).monarchTextStyle(.body)
        MonarchAiThread(
            messages,
            status: status,
            isOffline: isOffline,
            isSlow: isSlow,
            hasEarlier: earlier > 0,
            isLoadingEarlier: isLoadingEarlier,
            onStop: stop,
            onLoadEarlier: loadEarlier
        )
        .frame(height: MonarchTokens.Space.space24 * 5)
        MonarchAiPromptInput(
            "Message the assistant",
            text: $prompt,
            isBusy: status == .submitted || status == .streaming,
            onStop: stop,
            onSubmit: { send($0) }
        )
    }

    private func send(_ text: String) {
        messages.append(
            MonarchAiConversationMessage(
                id: UUID().uuidString,
                role: .user,
                parts: [.text(MonarchAiTextPart(text: text))],
                createdAt: MonarchAiConversationMessage.timestamp(.now)
            )
        )
        status = .submitted
        reply = Task { @MainActor in
            try? await Task.sleep(for: .seconds(1))
            let id = UUID().uuidString
            let words = "Here is a short answer. It arrives a few words at a time, as a model streams it.".split(
                separator: " "
            )
            var written = ""
            status = .streaming
            for word in words {
                guard !Task.isCancelled else { return }
                written += (written.isEmpty ? "" : " ") + word
                let message = MonarchAiConversationMessage(
                    id: id,
                    role: .assistant,
                    parts: [.text(MonarchAiTextPart(text: written, state: .streaming))],
                    createdAt: MonarchAiConversationMessage.timestamp(.now)
                )
                if messages.last?.id == id { messages[messages.count - 1] = message } else { messages.append(message) }
                try? await Task.sleep(for: .milliseconds(150))
            }
            if let index = messages.indices.last {
                messages[index].parts = [.text(MonarchAiTextPart(text: written))]
            }
            status = .idle
        }
    }

    private func stop() {
        reply?.cancel()
        if let index = messages.indices.last, messages[index].role == .assistant {
            messages[index].parts = [.text(MonarchAiTextPart(text: messages[index].text))]
        }
        status = .stopped
    }

    private func loadEarlier() {
        guard !isLoadingEarlier, earlier > 0 else { return }
        isLoadingEarlier = true
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(500))
            let older = Self.history(count: 4, before: earlier)
            messages.insert(contentsOf: older, at: 0)
            earlier -= 1
            isLoadingEarlier = false
        }
    }

    private static func history(count: Int, before page: Int = 0) -> [MonarchAiConversationMessage] {
        (0..<count).map { index in
            let age = TimeInterval((page * count + count - index) * 3_600 * 9)
            return MonarchAiConversationMessage(
                id: "p\(page)-\(index)",
                role: index % 2 == 0 ? .user : .assistant,
                parts: [
                    .text(
                        MonarchAiTextPart(
                            text: index % 2 == 0 ? "Question \(page)-\(index)?" : "Answer \(page)-\(index)."
                        )
                    )
                ],
                createdAt: MonarchAiConversationMessage.timestamp(Date.now.addingTimeInterval(-age))
            )
        }
    }
}
