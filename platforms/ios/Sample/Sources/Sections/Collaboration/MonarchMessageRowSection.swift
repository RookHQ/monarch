import MonarchUI
import SwiftUI

struct MonarchMessageRowSection: CataloguePage {
    static let title = "Message row"

    @State private var reactions = [
        MonarchReactionBarItem("👍", count: 3, isReacted: true, label: "thumbs up", reactedBy: "You, Ben and Chloé"),
        MonarchReactionBarItem("🎉", count: 1, label: "party popper"),
    ]
    @State private var failed = true
    @State private var isDeleted = false

    var body: some View {
        CatalogueCaption("Touch and hold a message for its actions")
        VStack(spacing: 0) {
            MonarchMessageRow(
                MonarchMessageRowAuthor("Ana Lopez"),
                time: Date().addingTimeInterval(-480),
                reactions: reactions,
                replyCount: 4,
                isDeleted: isDeleted,
                onReactionToggle: toggle,
                onOpenThread: {}
            ) {
                Text("The release notes are ready for review.")
            } actions: {
                Button("Reply", systemImage: "arrowshape.turn.up.left") {}
                Button("Delete", systemImage: "trash", role: .destructive) { isDeleted = true }
            }
            MonarchMessageRow(
                MonarchMessageRowAuthor("Ana Lopez"),
                time: Date().addingTimeInterval(-420),
                isGrouped: true,
                isEdited: true,
                onShowEdits: {}
            ) {
                Text("Comments by Thursday, please.")
            }
            MonarchMessageRow(
                MonarchMessageRowAuthor("Ben Okafor"),
                time: Date(),
                status: failed ? .failed : .sent,
                onRetry: { failed = false },
                onDiscard: { failed = false }
            ) {
                Text("Shipping it now.")
            }
            MonarchMessageRow(
                MonarchMessageRowAuthor("Chloé Martin"),
                time: Date(),
                variant: .internal,
                tags: ["Internal"]
            ) {
                Text("The customer is on the enterprise plan.")
            }
            MonarchMessageRow(MonarchMessageRowAuthor("System"), time: Date(), variant: .system) {
                Text("Ana Lopez joined the channel")
            }
        }
        CatalogueCaption("Pending")
        MonarchMessageRow(MonarchMessageRowAuthor("Ben Okafor"), time: Date(), status: .pending) {
            Text("On its way")
        }
    }

    private func toggle(_ emoji: String, _ isOn: Bool) {
        if let index = reactions.firstIndex(where: { $0.emoji == emoji }) {
            let item = reactions[index]
            reactions[index] = MonarchReactionBarItem(
                item.emoji,
                count: item.count + (isOn ? 1 : -1),
                isReacted: isOn,
                label: item.label
            )
            reactions.removeAll { $0.count <= 0 }
        } else if isOn {
            reactions.append(MonarchReactionBarItem(emoji, count: 1, isReacted: true))
        }
    }
}
