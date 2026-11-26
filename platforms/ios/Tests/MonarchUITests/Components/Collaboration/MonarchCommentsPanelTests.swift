import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
private let locale = Locale(identifier: "en_US")
private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    calendar.locale = Locale(identifier: "en_US")
    return calendar
}()

private func comment(_ id: String, _ author: String, minutesAgo: Int, canDelete: Bool = false)
    -> MonarchCommentThreadComment
{
    MonarchCommentThreadComment(
        id: id,
        author: author,
        body: "Comment \(id)",
        createdAt: now.addingTimeInterval(TimeInterval(-minutesAgo * 60)),
        canDelete: canDelete
    )
}

private let open = MonarchCommentsPanelThread(
    id: "t1",
    comments: [
        comment("c1", "Priya Raman", minutesAgo: 50),
        comment("c2", "Tom Becker", minutesAgo: 20, canDelete: true),
    ],
    quote: "Billed monthly, cancel any time."
)
private let general = MonarchCommentsPanelThread(
    id: "t2",
    comments: [comment("c3", "Lee Park", minutesAgo: 5)],
    canResolve: false,
    canReply: false
)
private let done = MonarchCommentsPanelThread(
    id: "t3",
    comments: [comment("c4", "Ana Lopez", minutesAgo: 600)],
    quote: "Prices include tax.",
    updatedAt: now.addingTimeInterval(-120),
    isResolved: true
)
private let threads = [open, general, done]

private let doNothing: @MainActor () -> Void = {}
private let postNothing: @MainActor (String, String) async throws -> Void = { _, _ in }
private let resolveNothing: @MainActor (String, Bool) async throws -> Void = { _, _ in }

/// Hosts the panel in a navigation stack with a fixed locale and calendar.
@MainActor private func panel(
    _ threads: [MonarchCommentsPanelThread]?,
    filter: MonarchCommentsPanelFilter = .open,
    selected: String? = nil,
    errorMessage: String? = nil,
    labels: MonarchCommentsPanelLabels = MonarchCommentsPanelLabels(),
    hasRetry: Bool = false,
    hasActions: Bool = true
) -> some View {
    NavigationStack {
        MonarchCommentsPanel(
            threads,
            filter: .constant(filter),
            selectedThreadID: .constant(selected),
            errorMessage: errorMessage,
            now: now,
            labels: labels,
            onRetry: hasRetry ? doNothing : nil,
            onClose: hasActions ? doNothing : nil,
            onReply: hasActions ? postNothing : nil,
            onResolvedChange: hasActions ? resolveNothing : nil,
            onDeleteComment: hasActions ? postNothing : nil
        )
    }
    .environment(\.locale, locale)
    .environment(\.calendar, calendar)
    .frame(height: 480)
}

enum MonarchCommentsPanelFixtures: ComponentFixtures {
    static let id = "comments-panel"
    static let fixtures = [
        Fixture("open threads, with close and actions") { panel(threads) },
        Fixture("resolved threads") { panel(threads, filter: .resolved) },
        Fixture("an open thread pushed") { panel(threads, selected: "t1") },
        Fixture("a resolved thread pushed") { panel(threads, filter: .resolved, selected: "t3") },
        Fixture("a thread without reply or resolve") { panel(threads, selected: "t2") },
        Fixture("read only, no actions") { panel(threads, hasActions: false) },
        Fixture("loading") { panel(nil) },
        Fixture("error with retry") { panel(threads, errorMessage: "Comments couldn't be loaded.", hasRetry: true) },
        Fixture("error without retry") { panel(nil, errorMessage: "Comments couldn't be loaded.") },
        Fixture("empty open") { panel([done]) },
        Fixture("empty resolved") { panel([open], filter: .resolved) },
        Fixture("long text and custom labels") {
            panel(
                [
                    MonarchCommentsPanelThread(
                        id: "long",
                        comments: [
                            comment("l1", "Maximiliana Alexandrova-Fitzgerald", minutesAgo: 3)
                        ],
                        quote: String(repeating: "A long quoted passage from the document. ", count: 6)
                    ),
                    MonarchCommentsPanelThread(id: "none", comments: []),
                ],
                labels: MonarchCommentsPanelLabels(
                    title: "Discussion",
                    open: "Unresolved",
                    commentCount: { "\($0) notes" }
                )
            )
        },
    ]
}

@Suite("MonarchCommentsPanel")
struct MonarchCommentsPanelTests {
    private typealias Tokens = MonarchCommentsPanelTokens

    @Test("Threads are listed by filter, in the given order")
    func visible() {
        #expect(Tokens.visible(threads, filter: .open).map(\.id) == ["t1", "t2"])
        #expect(Tokens.visible(threads, filter: .resolved).map(\.id) == ["t3"])
        #expect(Tokens.visible([], filter: .open).isEmpty)
    }

    @Test("A failure wins, then loading, then empty")
    func content() {
        #expect(Tokens.content(threads: nil, errorMessage: "No", filter: .open) == .failed("No"))
        #expect(Tokens.content(threads: threads, errorMessage: "No", filter: .open) == .failed("No"))
        #expect(Tokens.content(threads: nil, errorMessage: nil, filter: .open) == .loading)
        #expect(Tokens.content(threads: [done], errorMessage: nil, filter: .open) == .empty)
        #expect(Tokens.content(threads: [done], errorMessage: nil, filter: .resolved) == .list)
        #expect(Tokens.content(threads: [], errorMessage: nil, filter: .resolved) == .empty)
    }

    @Test("The selected thread is shown only while it is listed under the filter")
    func shownThread() {
        #expect(Tokens.shownThread(threads, filter: .open, selectedID: "t1")?.id == "t1")
        #expect(Tokens.shownThread(threads, filter: .resolved, selectedID: "t1") == nil)
        #expect(Tokens.shownThread(threads, filter: .resolved, selectedID: "t3")?.id == "t3")
        #expect(Tokens.shownThread(threads, filter: .open, selectedID: nil) == nil)
        #expect(Tokens.shownThread(nil, filter: .open, selectedID: "t1") == nil)
        #expect(Tokens.shownThread(threads, filter: .open, selectedID: "missing") == nil)
    }

    @Test("The last activity is updatedAt, or the newest comment")
    func lastActivity() {
        #expect(Tokens.lastActivity(open) == now.addingTimeInterval(-1_200))
        #expect(Tokens.lastActivity(done) == now.addingTimeInterval(-120))
        #expect(Tokens.lastActivity(MonarchCommentsPanelThread(id: "e", comments: [])) == nil)
    }

    @Test("Rows show and read the count and the last activity")
    func rows() {
        let labels = MonarchCommentsPanelLabels()
        #expect(
            Tokens.detailLine(open, now: now, locale: locale, calendar: calendar, labels: labels)
                == "2 comments · 20 minutes ago"
        )
        #expect(
            Tokens.rowLabel(open, now: now, locale: locale, calendar: calendar, labels: labels)
                == "Priya Raman, Billed monthly, cancel any time., 2 comments, 20 minutes ago"
        )
        #expect(
            Tokens.rowLabel(general, now: now, locale: locale, calendar: calendar, labels: labels)
                == "Lee Park, 1 comment, 5 minutes ago"
        )
        let empty = MonarchCommentsPanelThread(id: "e", comments: [])
        #expect(Tokens.detailLine(empty, now: now, locale: locale, calendar: calendar, labels: labels) == "0 comments")
        #expect(Tokens.rowLabel(empty, now: now, locale: locale, calendar: calendar, labels: labels) == "0 comments")
        let custom = MonarchCommentsPanelLabels(commentCount: { "\($0) notes" })
        #expect(Tokens.commentCount(2, labels: custom) == "2 notes")
    }

    @Test("Empty titles per filter, with the hint only under Open")
    func empty() {
        let labels = MonarchCommentsPanelLabels()
        #expect(Tokens.emptyTitle(.open, labels: labels) == "No open comments")
        #expect(Tokens.emptyTitle(.resolved, labels: labels) == "No resolved comments")
        #expect(Tokens.emptyMessage(.open, labels: labels) == "Select text to start a discussion.")
        #expect(Tokens.emptyMessage(.resolved, labels: labels) == nil)
        let custom = MonarchCommentsPanelLabels(emptyResolved: "Nothing settled", emptyHint: "Long-press text")
        #expect(Tokens.emptyTitle(.resolved, labels: custom) == "Nothing settled")
        #expect(Tokens.emptyMessage(.open, labels: custom) == "Long-press text")
    }

    @Test("Segment titles and overrides")
    func filterTitles() {
        let labels = MonarchCommentsPanelLabels()
        #expect(Tokens.filterTitle(.open, labels: labels) == "Open")
        #expect(Tokens.filterTitle(.resolved, labels: labels) == "Resolved")
        #expect(Tokens.filterTitle(.open, labels: MonarchCommentsPanelLabels(open: "Active")) == "Active")
    }

    @Test("The open thread offers an action only with the panel's action and the thread's permission")
    func actions() {
        let all = Tokens.actions(open, hasReply: true, hasResolve: true, hasDelete: true)
        #expect(all.reply && all.resolve && all.delete)
        let locked = Tokens.actions(general, hasReply: true, hasResolve: true, hasDelete: true)
        #expect(!locked.reply && !locked.resolve && locked.delete)
        let none = Tokens.actions(open, hasReply: false, hasResolve: false, hasDelete: false)
        #expect(!none.reply && !none.resolve && !none.delete)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchCommentsPanelStrings.title, MonarchCommentsPanelStrings.close,
            MonarchCommentsPanelStrings.filter, MonarchCommentsPanelStrings.open,
            MonarchCommentsPanelStrings.resolved, MonarchCommentsPanelStrings.loading,
            MonarchCommentsPanelStrings.emptyOpen, MonarchCommentsPanelStrings.emptyResolved,
            MonarchCommentsPanelStrings.emptyHint, MonarchCommentsPanelStrings.commentCount(1),
            MonarchCommentsPanelStrings.commentCount(3),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchCommentsPanelStrings.commentCount(1) == "1 comment")
        #expect(MonarchCommentsPanelStrings.commentCount(3) == "3 comments")
    }
}
