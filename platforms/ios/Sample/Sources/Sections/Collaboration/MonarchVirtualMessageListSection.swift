import MonarchTokens
import MonarchUI
import SwiftUI

private struct SampleMessage: MonarchVirtualMessageListItem {
    let id: Int
    let time: Date
    let authorID: String?
    let text: String
}

struct MonarchVirtualMessageListSection: CataloguePage {
    static let title = "VirtualMessageList"

    @State private var messages: [SampleMessage] = Self.page(before: 100, count: 30)
    @State private var oldest = 70
    @State private var isLoadingOlder = false
    @State private var isAtBottom = true

    var body: some View {
        Text(isAtBottom ? "At the newest message" : "Reading older messages")
            .monarchTextStyle(.caption)
        HStack {
            MonarchButton("Receive a message", size: .sm) {
                let id = (messages.last?.id ?? 0) + 1
                messages.append(SampleMessage(id: id, time: .now, authorID: "ben", text: "New message \(id)"))
            }
        }
        MonarchVirtualMessageList(
            messages,
            firstUnreadID: 95,
            hasOlder: oldest > 0,
            isLoadingOlder: isLoadingOlder,
            onLoadOlder: loadOlder,
            onAtBottomChange: { isAtBottom = $0 },
            header: {
                Text("This is the start of #design.")
                    .monarchTextStyle(.bodyStrong)
                    .padding(MonarchTokens.Space.space4)
            },
            footer: { MonarchTypingIndicator(["Ben Okafor"]) },
            row: { message, context in
                MonarchMessageRow(
                    MonarchMessageRowAuthor(message.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
                    time: message.time,
                    isGrouped: context.isGrouped,
                    isHighlighted: context.isHighlighted
                ) {
                    Text(message.text)
                }
            }
        )
        .frame(height: MonarchTokens.Space.space24 * 5)
    }

    private func loadOlder() {
        guard !isLoadingOlder, oldest > 0 else { return }
        isLoadingOlder = true
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(500))
            messages.insert(contentsOf: Self.page(before: oldest, count: 20), at: 0)
            oldest -= 20
            isLoadingOlder = false
        }
    }

    private static func page(before id: Int, count: Int) -> [SampleMessage] {
        (max(0, id - count)..<id).map { index in
            SampleMessage(
                id: index,
                time: Date.now.addingTimeInterval(TimeInterval((index - 100) * 1_500)),
                authorID: index % 4 == 0 ? "ben" : "ana",
                text: "Message \(index)"
            )
        }
    }
}
