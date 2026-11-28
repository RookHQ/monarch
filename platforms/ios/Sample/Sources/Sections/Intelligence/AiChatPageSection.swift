import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A transport that answers every prompt after a short pause.
private struct ChatTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        let prompt = request.messages.last?.text ?? ""
        return MonarchAiEventStream { continuation in
            let task = Task {
                try? await Task.sleep(for: .milliseconds(500))
                continuation.yield(.textDelta("You asked: \(prompt)"))
                continuation.yield(.done())
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

/// A send the conversation refused, so the prompt input gives the text back.
private struct NotSent: Error {}

/// Recipe: the full chat screen, history in a split view sidebar and the
/// conversation in the detail; a navigation stack on iPhone.
struct AiChatPageSection: CataloguePage {
    static let title = "AiChatPage (recipe)"

    @Environment(\.monarchTheme) private var theme

    private let starters = [
        MonarchAiStarterPrompt(id: "sum", title: "Summarize my week", prompt: "Summarize my week."),
        MonarchAiStarterPrompt(id: "plan", title: "Plan a launch", prompt: "Plan a product launch."),
    ]

    @State private var conversation = MonarchAiConversation(transport: ChatTransport())
    @State private var conversations = [
        MonarchAiConversationSummary(id: "c1", title: "Weekly summary", updatedAt: .now),
        MonarchAiConversationSummary(
            id: "c2",
            title: "Release checklist",
            updatedAt: .now.addingTimeInterval(-86_400 * 2),
            isPinned: true
        ),
    ]
    @State private var activeID: String? = "c1"
    @State private var column = NavigationSplitViewColumn.sidebar
    @State private var prompt = ""

    private var title: String { conversations.first { $0.id == activeID }?.title ?? "" }

    var body: some View {
        CatalogueCaption("History beside the chat; a stack on iPhone")
        NavigationSplitView(preferredCompactColumn: $column) {
            MonarchAiConversationList(
                conversations,
                activeID: activeID,
                onNew: {
                    let id = UUID().uuidString
                    conversations.insert(MonarchAiConversationSummary(id: id, title: "", updatedAt: .now), at: 0)
                    open(id)
                },
                onRename: { id, title in rename(id, to: title) },
                onPin: { id, isPinned in
                    if let index = conversations.firstIndex(where: { $0.id == id }) {
                        conversations[index].isPinned = isPinned
                    }
                },
                onDelete: { id in conversations.removeAll { $0.id == id } },
                onSelect: { id in open(id) }
            )
            .navigationTitle("History")
        } detail: {
            detail
        }
        .frame(height: MonarchTokens.Space.space24 * 6)
    }

    private var detail: some View {
        MonarchAiThread(
            conversation.messages,
            status: conversation.status,
            error: conversation.error,
            isOffline: !conversation.isOnline,
            queuedCount: conversation.queued.count,
            isSlow: conversation.isSlow,
            onStop: { conversation.stop() },
            empty: {
                ScrollView {
                    MonarchAiWelcome("How can I help?", agent: conversation.agent) {
                        MonarchAiStarterPrompts(starters, layout: .list) { starter in
                            conversation.send(starter.prompt)
                        }
                        .disabled(conversation.isBusy || !conversation.canSend)
                    }
                    .padding(theme.spacing.space4)
                }
            }
        )
        .safeAreaInset(edge: .bottom) {
            MonarchAiPromptInput(
                "Message the assistant",
                text: $prompt,
                isBusy: conversation.isBusy,
                onStop: { conversation.stop() },
                triggerMenu: { EmptyView() },
                toolbar: { EmptyView() },
                footer: { MonarchAiDisclaimer() },
                onSubmit: { text in
                    guard conversation.send(text) else { throw NotSent() }
                }
            )
            .padding(theme.spacing.space4)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                MonarchAiConversationHeader(
                    title,
                    agent: conversation.agent,
                    onRename: { newTitle in
                        if let activeID { rename(activeID, to: newTitle) }
                    }
                )
            }
        }
    }

    private func open(_ id: String) {
        activeID = id
        conversation.reset()
        column = .detail
    }

    private func rename(_ id: String, to title: String) {
        if let index = conversations.firstIndex(where: { $0.id == id }) { conversations[index].title = title }
    }
}
