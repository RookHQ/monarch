import MonarchTokens
import MonarchUI
import SwiftUI

private struct TimelineMessage: MonarchVirtualMessageListItem {
    let id: String
    let time: Date
    let authorID: String?
    let text: String
}

/// Recipe: a conversation grouped by day, from MonarchVirtualMessageList,
/// MonarchMessageRow, MonarchReadReceipt and MonarchTypingIndicator.
struct MessageTimelineSection: CataloguePage {
    static let title = "MessageTimeline (recipe)"

    @State private var messages = [
        TimelineMessage(
            id: "m1",
            time: Date.now.addingTimeInterval(-90_000),
            authorID: "ben",
            text: "Can someone review the release notes?"
        ),
        TimelineMessage(
            id: "m2",
            time: Date.now.addingTimeInterval(-89_940),
            authorID: "ben",
            text: "They are in the shared folder."
        ),
        TimelineMessage(
            id: "m3",
            time: Date.now.addingTimeInterval(-1_800),
            authorID: "ana",
            text: "On it this morning."
        ),
        TimelineMessage(
            id: "m4",
            time: Date.now.addingTimeInterval(-300),
            authorID: "ana",
            text: "Done. Two small fixes."
        ),
    ]
    @State private var isTyping = true

    var body: some View {
        Toggle("Ben is typing", isOn: $isTyping)
        MonarchButton("Receive a message", size: .sm) {
            messages.append(
                TimelineMessage(id: UUID().uuidString, time: .now, authorID: "ben", text: "Thanks, looks good.")
            )
        }
        MonarchVirtualMessageList(
            messages,
            firstUnreadID: "m4",
            header: {
                Text("This is the start of #release.")
                    .monarchTextStyle(.bodyStrong)
                    .padding(MonarchTokens.Space.space4)
            },
            footer: {
                VStack(alignment: .trailing, spacing: MonarchTokens.Space.space1) {
                    MonarchReadReceipt([MonarchReadReceiptReader(name: "Ben Okafor")])
                    MonarchTypingIndicator(isTyping ? ["Ben Okafor"] : [])
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, MonarchTokens.Space.space4)
            },
            row: { message, context in
                MonarchMessageRow(
                    MonarchMessageRowAuthor(message.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
                    time: message.time,
                    isGrouped: context.isGrouped
                ) {
                    Text(message.text)
                }
            }
        )
        .frame(height: MonarchTokens.Space.space24 * 5)
    }
}
