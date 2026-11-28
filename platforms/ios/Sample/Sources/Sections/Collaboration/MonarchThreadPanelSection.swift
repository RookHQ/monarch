@_spi(Experimental) import MonarchUI
import SwiftUI

private struct SampleReply: MonarchVirtualMessageListItem {
    let id: String
    let time: Date
    let authorID: String?
    let text: String
}

struct MonarchThreadPanelSection: CataloguePage {
    static let title = "ThreadPanel"

    @State private var replies = [
        SampleReply(id: "r1", time: Date.now.addingTimeInterval(-1_800), authorID: "ben", text: "I can take it."),
        SampleReply(id: "r2", time: Date.now.addingTimeInterval(-1_740), authorID: "ben", text: "Done by noon."),
        SampleReply(id: "r3", time: Date.now.addingTimeInterval(-600), authorID: "ana", text: "Thanks, both."),
    ]
    @State private var isFollowing = false
    @State private var failsNext = false
    @State private var draft = ""
    @State private var seen = 0
    @State private var status = MonarchThreadPanelStatus.ready
    @State private var isSheetOpen = false

    var body: some View {
        Toggle("Fail the next follow change", isOn: $failsNext)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchThreadPanelStatus.ready)
            Text("Loading").tag(MonarchThreadPanelStatus.loading)
            Text("Error").tag(MonarchThreadPanelStatus.error)
            Text("Deleted").tag(MonarchThreadPanelStatus.deleted)
        }
        .pickerStyle(.segmented)
        Text("Replies seen: \(seen)")
            .monarchTextStyle(.caption)

        CatalogueCaption("Pushed, as on a phone")
        NavigationLink("Open the thread") { panel(onClose: nil) }

        CatalogueCaption("In a sheet, with Close")
        MonarchButton("Open in a sheet") { isSheetOpen = true }
            .sheet(isPresented: $isSheetOpen) {
                NavigationStack { panel(onClose: { isSheetOpen = false }) }
            }
    }

    private func panel(onClose: (@MainActor () -> Void)?) -> some View {
        MonarchThreadPanel(
            replies,
            status: status,
            isFollowing: isFollowing,
            onClose: onClose,
            onRetry: { status = .ready },
            onFollowingChange: { value in
                try await Task.sleep(for: .milliseconds(600))
                if failsNext {
                    failsNext = false
                    throw CancellationError()
                }
                isFollowing = value
            },
            onRepliesSeen: { seen = $0 },
            root: {
                MonarchMessageRow(MonarchMessageRowAuthor("Lee Park"), time: Date.now.addingTimeInterval(-3_600)) {
                    Text("Who can review the release notes?")
                }
            },
            typing: { MonarchTypingIndicator(replies.count > 3 ? [] : ["Ana Lopez"]) },
            composer: {
                MonarchMessageComposer("Reply in thread", text: $draft, showsEmojiButton: false) { message in
                    replies.append(
                        SampleReply(id: UUID().uuidString, time: .now, authorID: "you", text: message.text)
                    )
                }
            },
            headerActions: { EmptyView() },
            reply: { reply, context in
                MonarchMessageRow(
                    MonarchMessageRowAuthor(name(reply.authorID)),
                    time: reply.time,
                    isGrouped: context.isGrouped
                ) {
                    Text(reply.text)
                }
            }
        )
    }

    private func name(_ id: String?) -> String {
        switch id {
        case "ben": "Ben Okafor"
        case "ana": "Ana Lopez"
        default: "You"
        }
    }
}
