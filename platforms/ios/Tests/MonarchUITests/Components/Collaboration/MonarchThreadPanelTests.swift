import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    calendar.locale = Locale(identifier: "en_US")
    return calendar
}()

private struct Reply: MonarchVirtualMessageListItem {
    let id: Int
    let time: Date
    let authorID: String?
    let text: String
}

private let replies: [Reply] = (0..<6).map { index in
    Reply(
        id: index,
        time: now.addingTimeInterval(TimeInterval(index * 60 - (index < 2 ? 90_000 : 600))),
        authorID: index.isMultiple(of: 3) ? "ana" : "ben",
        text: "Reply \(index)"
    )
}

private let doNothing: @MainActor () -> Void = {}
private let followNothing: @MainActor (Bool) async throws -> Void = { _ in }
private let seeNothing: @MainActor (Int) -> Void = { _ in }

/// Hosts the panel in a navigation stack with a fixed locale and calendar.
@MainActor private func panel(
    _ items: [Reply] = replies,
    replyCount: Int? = nil,
    status: MonarchThreadPanelStatus = .ready,
    isFollowing: Bool = false,
    notice: String? = nil,
    labels: MonarchThreadPanelLabels = MonarchThreadPanelLabels(),
    hasActions: Bool = true
) -> some View {
    NavigationStack {
        MonarchThreadPanel(
            items,
            replyCount: replyCount,
            status: status,
            isFollowing: isFollowing,
            notice: notice,
            now: now,
            labels: labels,
            onClose: hasActions ? doNothing : nil,
            onRetry: hasActions ? doNothing : nil,
            onFollowingChange: hasActions ? followNothing : nil,
            onRepliesSeen: hasActions ? seeNothing : nil,
            root: {
                MonarchMessageRow(MonarchMessageRowAuthor("Lee Park"), time: now.addingTimeInterval(-100_000), now: now)
                {
                    Text("Who can review the release notes?")
                }
            },
            typing: { MonarchTypingIndicator(["Ben Okafor"]) },
            composer: { MonarchMessageComposer("Reply in thread", text: .constant("")) { _ in } },
            headerActions: { Button("More", systemImage: "ellipsis") {} },
            reply: { reply, context in
                MonarchMessageRow(
                    MonarchMessageRowAuthor(reply.authorID == "ana" ? "Ana Lopez" : "Ben Okafor"),
                    time: reply.time,
                    now: now,
                    isGrouped: context.isGrouped
                ) {
                    Text(reply.text)
                }
            }
        )
    }
    .environment(\.locale, Locale(identifier: "en_US"))
    .environment(\.calendar, calendar)
    .frame(height: 560)
}

enum MonarchThreadPanelFixtures: ComponentFixtures {
    static let id = "thread-panel"
    static let fixtures = [
        Fixture("ready, following, with typing and composer") { panel(isFollowing: true) },
        Fixture("not following, more replies than loaded, with a notice") {
            panel(replyCount: 640, notice: "Showing the first 6 replies.")
        },
        Fixture("no replies") { panel([]) },
        Fixture("loading") { panel(status: .loading) },
        Fixture("error with retry") { panel(status: .error) },
        Fixture("error without retry") { panel(status: .error, hasActions: false) },
        Fixture("deleted with close") { panel(status: .deleted) },
        Fixture("deleted without close") { panel(status: .deleted, hasActions: false) },
        Fixture("custom labels, no actions") {
            panel(
                labels: MonarchThreadPanelLabels(
                    title: "Replies to Lee",
                    replyCount: { "\($0) answers" },
                    list: MonarchVirtualMessageListLabels(today: "Now")
                ),
                hasActions: false
            )
        },
        Fixture("without typing or toolbar buttons") {
            NavigationStack {
                MonarchThreadPanel(
                    replies,
                    now: now,
                    root: { EmptyView() },
                    composer: { EmptyView() },
                    reply: { reply, _ in Text(reply.text) }
                )
            }
            .environment(\.calendar, calendar)
            .frame(height: 360)
        },
    ]
}

@Suite("MonarchThreadPanel")
struct MonarchThreadPanelTests {
    private typealias Tokens = MonarchThreadPanelTokens

    @Test("The divider counts the app's number, or the loaded replies")
    func count() {
        #expect(Tokens.count(replyCount: nil, loaded: 4) == 4)
        #expect(Tokens.count(replyCount: 640, loaded: 4) == 640)
        #expect(Tokens.count(replyCount: -3, loaded: 4) == 0)
        let labels = MonarchThreadPanelLabels()
        #expect(Tokens.replyCountText(0, labels: labels) == "0 replies")
        #expect(Tokens.replyCountText(1, labels: labels) == "1 reply")
        #expect(Tokens.replyCountText(640, labels: labels) == "640 replies")
        #expect(
            Tokens.replyCountText(2, labels: MonarchThreadPanelLabels(replyCount: { "\($0) answers" })) == "2 answers"
        )
    }

    @Test("The replies are named Replies unless the app names the list")
    func listLabels() {
        #expect(Tokens.listLabels(MonarchThreadPanelLabels()).list == "Replies")
        #expect(Tokens.listLabels(MonarchThreadPanelLabels(replies: "Answers")).list == "Answers")
        let named = MonarchThreadPanelLabels(replies: "Answers", list: MonarchVirtualMessageListLabels(list: "Thread"))
        #expect(Tokens.listLabels(named).list == "Thread")
        let partial = MonarchThreadPanelLabels(list: MonarchVirtualMessageListLabels(today: "Now"))
        #expect(Tokens.listLabels(partial).list == "Replies")
        #expect(Tokens.listLabels(partial).today == "Now")
    }

    @Test("Follow reads Following while following")
    func follow() {
        let labels = MonarchThreadPanelLabels()
        #expect(Tokens.followButton(isFollowing: false, labels: labels) == ("Follow", Tokens.followSymbol))
        #expect(Tokens.followButton(isFollowing: true, labels: labels) == ("Following", Tokens.followingSymbol))
        let custom = MonarchThreadPanelLabels(following: "Subscribed")
        #expect(Tokens.followButton(isFollowing: true, labels: custom).title == "Subscribed")
    }

    @Test("Follow shows only while ready; the typing indicator and the reply box show in every state (web parity)")
    func visibility() {
        for status in MonarchThreadPanelStatus.allCases {
            #expect(Tokens.showsFollow(status: status, hasAction: true) == (status == .ready))
            #expect(!Tokens.showsFollow(status: status, hasAction: false))
        }
    }

    @Test("Following, unfollowing and a failure each announce their notice")
    func notices() {
        let labels = MonarchThreadPanelLabels()
        #expect(
            Tokens.followNotice(isFollowing: true, didFail: false, labels: labels) == "You’ll be notified about replies"
        )
        #expect(
            Tokens.followNotice(isFollowing: false, didFail: false, labels: labels)
                == "You won’t be notified about replies"
        )
        #expect(
            Tokens.followNotice(isFollowing: true, didFail: true, labels: labels)
                == "Thread notifications didn’t change. Try again."
        )
        let custom = MonarchThreadPanelLabels(followedNotice: "On", followFailed: "Failed")
        #expect(Tokens.followNotice(isFollowing: true, didFail: false, labels: custom) == "On")
        #expect(Tokens.followNotice(isFollowing: false, didFail: true, labels: custom) == "Failed")
    }

    @Test("The seen timer runs only while ready, wanted and active, and restarts with the count")
    func seen() {
        let armed = Tokens.seen(count: 3, status: .ready, isActive: true, hasAction: true)
        #expect(armed.isArmed && armed.count == 3)
        #expect(!Tokens.seen(count: 3, status: .loading, isActive: true, hasAction: true).isArmed)
        #expect(!Tokens.seen(count: 3, status: .deleted, isActive: true, hasAction: true).isArmed)
        #expect(!Tokens.seen(count: 3, status: .ready, isActive: false, hasAction: true).isArmed)
        #expect(!Tokens.seen(count: 3, status: .ready, isActive: true, hasAction: false).isArmed)
        #expect(armed != Tokens.seen(count: 4, status: .ready, isActive: true, hasAction: true))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchThreadPanelStrings.title, MonarchThreadPanelStrings.close, MonarchThreadPanelStrings.follow,
            MonarchThreadPanelStrings.following, MonarchThreadPanelStrings.followedNotice,
            MonarchThreadPanelStrings.unfollowedNotice, MonarchThreadPanelStrings.followFailed,
            MonarchThreadPanelStrings.replies, MonarchThreadPanelStrings.loading, MonarchThreadPanelStrings.error,
            MonarchThreadPanelStrings.deleted, MonarchThreadPanelStrings.replyCount(1),
            MonarchThreadPanelStrings.replyCount(2),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchThreadPanelStrings.title == "Thread")
        #expect(MonarchThreadPanelStrings.error == "This thread couldn’t be loaded.")
    }
}
