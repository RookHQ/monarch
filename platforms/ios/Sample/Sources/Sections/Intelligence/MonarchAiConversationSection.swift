import MonarchUI
import SwiftUI

/// A transport for the catalogue: it answers from a script, word by word,
/// and fails when the prompt contains "fail". It does no networking.
private struct ScriptedTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        let prompt = request.messages.last?.text ?? ""
        let attempt = request.trigger == .regenerate ? "Another version: " : ""
        let words = "\(attempt)You asked \"\(prompt)\". This reply is scripted and arrives one word at a time."
            .split(separator: " ")
        return MonarchAiEventStream { continuation in
            let task = Task {
                continuation.yield(.agent(MonarchAiAgentRef(name: "Catalogue assistant")))
                for word in words {
                    try? await Task.sleep(for: .milliseconds(120))
                    if Task.isCancelled { break }
                    continuation.yield(.textDelta(word + " "))
                    if prompt.localizedCaseInsensitiveContains("fail") {
                        continuation.yield(.error(code: "http-503", message: "The scripted service is down."))
                        break
                    }
                }
                continuation.yield(.done())
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

struct MonarchAiConversationSection: CataloguePage {
    static let title = "AI conversation (model)"

    @Environment(\.monarchTheme) private var theme
    @State private var conversation = MonarchAiConversation(transport: ScriptedTransport())
    @State private var draft = "What changed since yesterday?"
    @State private var isOnline = true

    var body: some View {
        CatalogueCaption("Status: \(conversation.status.rawValue)")

        ForEach(conversation.messages) { message in
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                Text(message.isQueued ? "\(message.role.rawValue), queued" : message.role.rawValue)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                Text(message.text)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                if let branch = message.branch {
                    MonarchAiMessageBranch(
                        selection: Binding(
                            get: { branch.index },
                            set: { conversation.branchTo(message.id, index: $0) }
                        ),
                        count: branch.count
                    )
                    .disabled(conversation.isBusy)
                }
            }
        }

        if let error = conversation.error {
            let kind = MonarchAiErrorKind(code: error.code)
            let details = "code: \(error.code)\nmessage: \(error.message)"
            if error.isRetryable {
                MonarchAiErrorMessage(kind, details: details, onRetry: { conversation.retry() })
            } else {
                MonarchAiErrorMessage(kind, details: details)
            }
        }

        MonarchTextField("Prompt", text: $draft, helper: "A prompt that contains \"fail\" ends in an error.")
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("Send", variant: .primary, size: .sm) {
                if conversation.send(draft) { draft = "" }
            }
            MonarchButton("Stop", size: .sm) { conversation.stop() }
                .disabled(!conversation.isBusy)
            MonarchButton("Regenerate", size: .sm) { conversation.regenerate() }
                .disabled(conversation.isBusy || conversation.messages.isEmpty)
        }
        Toggle("Online (messages sent offline are queued)", isOn: $isOnline)
            .monarchTextStyle(.body)
            .onChange(of: isOnline) { _, newValue in
                conversation.setOnline(newValue)
            }
        MonarchButton("Reset", variant: .ghost, size: .sm) { conversation.reset() }
    }
}
