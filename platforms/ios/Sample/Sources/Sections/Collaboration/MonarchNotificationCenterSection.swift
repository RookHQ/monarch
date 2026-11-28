import MonarchUI
import SwiftUI

struct MonarchNotificationCenterSection: CataloguePage {
    static let title = "Notification center"

    @State private var showsCenter = false
    @State private var items = [
        MonarchNotificationCenterItem(
            id: "n1",
            title: "Ana Lopez mentioned you in Launch checklist",
            message: "Can you confirm the legal sign-off date?",
            time: Date().addingTimeInterval(-1_080),
            actorName: "Ana Lopez"
        ),
        MonarchNotificationCenterItem(
            id: "n2",
            title: "Ben Okafor assigned you Update pricing page",
            time: Date().addingTimeInterval(-10_500),
            actorName: "Ben Okafor"
        ),
        MonarchNotificationCenterItem(
            id: "n3",
            title: "Chloé Martin replied to your comment",
            message: "Agreed, let's ship the shorter version.",
            time: Date().addingTimeInterval(-86_400),
            isRead: true,
            actorName: "Chloé Martin"
        ),
        MonarchNotificationCenterItem(
            id: "n4",
            title: "Your weekly export is ready",
            time: Date().addingTimeInterval(-86_400 * 3),
            isRead: true,
            systemImage: "arrow.down.doc",
            systemImageLabel: "Export"
        ),
    ]

    private var unread: Int { items.filter { !$0.isRead }.count }

    var body: some View {
        CatalogueCaption("Open from the bell; swipe right to mark one read")
        MonarchNotificationsBell(unreadCount: unread) { showsCenter = true }
            .sheet(isPresented: $showsCenter) {
                NavigationStack {
                    MonarchNotificationCenter(
                        items,
                        hasMore: true,
                        onMarkRead: markRead,
                        onMarkAllRead: { items = items.map(read) },
                        onLoadMore: {},
                        onSelect: { _ in }
                    ) {
                        Button("Notification settings") {}
                            .buttonStyle(.monarchLink(.standalone))
                    }
                }
            }
        CatalogueCaption("Inline, pushed in a navigation stack")
        NavigationStack {
            MonarchNotificationCenter(items, showsDoneButton: false, onMarkRead: markRead)
        }
        .frame(minHeight: 480)
    }

    private func markRead(_ id: String) {
        items = items.map { $0.id == id ? read($0) : $0 }
    }

    private func read(_ item: MonarchNotificationCenterItem) -> MonarchNotificationCenterItem {
        MonarchNotificationCenterItem(
            id: item.id,
            title: item.title,
            message: item.message,
            time: item.time,
            isRead: true,
            actorName: item.actorName,
            actorAvatarURL: item.actorAvatarURL,
            systemImage: item.systemImage,
            systemImageLabel: item.systemImageLabel
        )
    }
}
