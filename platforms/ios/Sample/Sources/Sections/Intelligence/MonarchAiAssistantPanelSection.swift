import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A transport that answers every prompt after a short pause, a few words at a time.
private struct SampleTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        let prompt = request.messages.last?.text ?? ""
        return MonarchAiEventStream { continuation in
            let task = Task {
                try? await Task.sleep(for: .milliseconds(600))
                for word in ["You", " asked:", " \(prompt)"] {
                    continuation.yield(.textDelta(word))
                    try? await Task.sleep(for: .milliseconds(200))
                }
                continuation.yield(.done())
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

struct MonarchAiAssistantPanelSection: CataloguePage {
    static let title = "AiAssistantPanel"

    @State private var conversation = MonarchAiConversation(transport: SampleTransport())
    @State private var showsAssistant = false
    @State private var draft = ""
    @State private var context = [
        MonarchAiContextItem(id: "page", kind: .page, label: "Quarterly plan", tokens: 1_200),
        MonarchAiContextItem(id: "file", kind: .file, label: "contract.pdf", tokens: 8_400),
    ]
    @State private var isPaneOpen = true

    private let starters = [
        MonarchAiStarterPrompt(id: "sum", title: "Summarize this page", prompt: "Summarize this page."),
        MonarchAiStarterPrompt(id: "week", title: "What changed this week?", prompt: "What changed this week?"),
    ]
    private let agent = MonarchAiAgentRef(name: "Dossier", role: "Workspace assistant")

    var body: some View {
        CatalogueCaption("A sheet on iPhone, an inspector on iPad")
        Button("Ask the assistant") { showsAssistant = true }
            .buttonStyle(.monarch(.primary))
            .monarchAiAssistantPanel(isPresented: $showsAssistant) {
                panel(placement: .automatic, isPresented: $showsAssistant)
            }

        CatalogueCaption("A pane placed by the app")
        if isPaneOpen {
            panel(placement: .inline, isPresented: $isPaneOpen)
                .frame(height: MonarchTokens.Space.space24 * 5)
        } else {
            Button("Show the pane") { isPaneOpen = true }
                .buttonStyle(.monarch(.secondary))
        }
    }

    private func panel(placement: MonarchAiAssistantPanelPlacement, isPresented: Binding<Bool>) -> some View {
        MonarchAiAssistantPanel(
            "Assistant",
            conversation: conversation,
            isPresented: isPresented,
            prompt: $draft,
            placement: placement,
            agent: agent,
            context: context,
            starters: starters,
            onRemoveContext: { id in context.removeAll { $0.id == id } },
            headerActions: {
                Button("New conversation", systemImage: "square.and.pencil") { conversation.reset() }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.monarch(.ghost, size: .sm))
            },
            toolbar: {},
            footer: { MonarchAiDisclaimer() }
        )
    }
}
