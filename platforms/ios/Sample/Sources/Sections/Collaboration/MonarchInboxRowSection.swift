import MonarchUI
import SwiftUI

struct MonarchInboxRowSection: CataloguePage {
    static let title = "Inbox row"

    private struct Conversation: Identifiable {
        let id: Int
        let title: String
        let preview: String
        let minutesAgo: Double
        var unread: Int
    }

    @State private var conversations = [
        Conversation(
            id: 1,
            title: "Ana Silva",
            preview: "Can you look at the invoice before Friday?",
            minutesAgo: 5,
            unread: 3
        ),
        Conversation(id: 2, title: "Ben Okafor", preview: "Thanks, that fixed it.", minutesAgo: 120, unread: 0),
        Conversation(
            id: 3,
            title: "Design team",
            preview: "New file shared: onboarding v3",
            minutesAgo: 4_400,
            unread: 1
        ),
    ]
    @State private var selection: Int?

    var body: some View {
        CatalogueCaption("Swipe right to mark read or unread, left to archive")
        List(selection: $selection) {
            ForEach(conversations) { conversation in
                MonarchInboxRow(
                    conversation.title,
                    preview: conversation.preview,
                    timestamp: Date().addingTimeInterval(-conversation.minutesAgo * 60),
                    systemImage: conversation.id == 1 ? "envelope" : nil,
                    systemImageLabel: conversation.id == 1 ? "via email" : nil,
                    unreadCount: conversation.unread,
                    swipeActions: [
                        .toggleRead { toggleRead(conversation.id) },
                        .archive { conversations.removeAll { $0.id == conversation.id } },
                    ]
                ) {
                    MonarchBadge(
                        conversation.id == 1 ? "Open" : "Solved",
                        tone: conversation.id == 1 ? .info : .success
                    )
                }
                .tag(conversation.id)
            }
        }
        .listStyle(.plain)
        .frame(minHeight: 320)
    }

    private func toggleRead(_ id: Int) {
        guard let index = conversations.firstIndex(where: { $0.id == id }) else { return }
        conversations[index].unread = conversations[index].unread > 0 ? 0 : 1
    }
}
