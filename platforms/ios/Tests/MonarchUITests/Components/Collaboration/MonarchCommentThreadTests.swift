import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

private func comment(_ index: Int, author: String = "Tom Becker", canDelete: Bool = false)
    -> MonarchCommentThreadComment
{
    MonarchCommentThreadComment(
        id: "c\(index)",
        author: author,
        body: "Comment \(index)",
        createdAt: now.addingTimeInterval(TimeInterval(index * 300 - 20_000)),
        canDelete: canDelete
    )
}

private let pair = [comment(0, author: "Priya Raman"), comment(1, canDelete: true)]
private let long = (0..<8).map { comment($0) }

enum MonarchCommentThreadFixtures: ComponentFixtures {
    static let id = "comment-thread"
    static let fixtures = [
        Fixture("open with a quote, resolve, delete and reply") {
            MonarchCommentThread(
                pair,
                quote: "Billed monthly, cancel any time.",
                now: now,
                onResolvedChange: { _ in },
                onDelete: { _ in },
                onReply: { _ in }
            )
        },
        Fixture("folded replies") {
            MonarchCommentThread(long, now: now, onReply: { _ in })
        },
        Fixture("all replies visible") {
            MonarchCommentThread(long, visibleReplies: 10, now: now)
        },
        Fixture("resolved, with the hint") {
            MonarchCommentThread(pair, isResolved: true, now: now, onResolvedChange: { _ in }, onReply: { _ in })
        },
        Fixture("read only, no actions") {
            MonarchCommentThread(pair, now: now)
        },
        Fixture("empty") {
            MonarchCommentThread([], now: now, onReply: { _ in })
        },
        Fixture("long text and custom labels") {
            MonarchCommentThread(
                [
                    MonarchCommentThreadComment(
                        id: "l",
                        author: "Maximiliana Alexandrova-Fitzgerald",
                        body: String(repeating: "A long comment that wraps over several lines. ", count: 6),
                        createdAt: now.addingTimeInterval(-90)
                    )
                ],
                quote: String(repeating: "A long quoted passage from the document. ", count: 4),
                isResolved: true,
                now: now,
                labels: MonarchCommentThreadLabels(reopen: "Open the discussion again", resolved: "Done"),
                onResolvedChange: { _ in }
            )
        },
    ]
}

@Suite("MonarchCommentThread")
struct MonarchCommentThreadTests {
    private typealias Tokens = MonarchCommentThreadTokens

    @Test("Replies fold behind the button, never just one")
    func folding() {
        #expect(Tokens.foldedCount(replies: 7, visibleReplies: 3, isExpanded: false) == 4)
        #expect(Tokens.foldedCount(replies: 4, visibleReplies: 3, isExpanded: false) == 0)
        #expect(Tokens.foldedCount(replies: 5, visibleReplies: 3, isExpanded: false) == 2)
        #expect(Tokens.foldedCount(replies: 7, visibleReplies: 3, isExpanded: true) == 0)
        #expect(Tokens.foldedCount(replies: 0, visibleReplies: 3, isExpanded: false) == 0)
        #expect(Tokens.foldedCount(replies: 3, visibleReplies: 0, isExpanded: false) == 3)
        #expect(Tokens.foldedCount(replies: 3, visibleReplies: -2, isExpanded: false) == 3)
    }

    @Test("The first comment and the latest replies are shown")
    func shown() {
        let result = Tokens.shown(long, folded: 4)
        #expect(result.root?.id == "c0")
        #expect(result.replies.map(\.id) == ["c5", "c6", "c7"])
        let all = Tokens.shown(long, folded: 0)
        #expect(all.replies.count == 7)
        let empty = Tokens.shown([], folded: 0)
        #expect(empty.root == nil && empty.replies.isEmpty)
    }

    @Test("The reply field shows while open, the hint while resolved, nothing without replies")
    func footer() {
        #expect(Tokens.footer(canReply: true, isResolved: false) == .composer)
        #expect(Tokens.footer(canReply: true, isResolved: true) == .resolvedHint)
        #expect(Tokens.footer(canReply: false, isResolved: false) == .none)
        #expect(Tokens.footer(canReply: false, isResolved: true) == .none)
    }

    @Test("Spoken name, state and fold texts")
    func accessibility() {
        let labels = MonarchCommentThreadLabels()
        #expect(Tokens.threadLabel(pair, labels: labels) == "Thread started by Priya Raman")
        #expect(Tokens.threadLabel([], labels: labels) == nil)
        #expect(Tokens.stateValue(isResolved: true, labels: labels) == "Resolved")
        #expect(Tokens.stateValue(isResolved: false, labels: labels) == nil)
        #expect(Tokens.showEarlierTitle(1, labels: labels) == "Show 1 earlier reply")
        #expect(Tokens.showEarlierTitle(4, labels: labels) == "Show 4 earlier replies")
        #expect(Tokens.earlierShownText(1, labels: labels) == "1 earlier reply shown")
        #expect(Tokens.earlierShownText(4, labels: labels) == "4 earlier replies shown")
        let custom = MonarchCommentThreadLabels(thread: { "Fil de \($0)" }, showEarlier: { "+\($0)" })
        #expect(Tokens.threadLabel(pair, labels: custom) == "Fil de Priya Raman")
        #expect(Tokens.showEarlierTitle(4, labels: custom) == "+4")
    }

    @Test("Resolve becomes Reopen, and each announces its own notice")
    func resolve() {
        let labels = MonarchCommentThreadLabels()
        #expect(Tokens.resolveButton(isResolved: false, labels: labels) == ("Resolve", Tokens.resolveSymbol))
        #expect(Tokens.resolveButton(isResolved: true, labels: labels) == ("Reopen", Tokens.reopenSymbol))
        #expect(Tokens.resolvedNotice(true, labels: labels) == "Thread resolved")
        #expect(Tokens.resolvedNotice(false, labels: labels) == "Thread reopened")
        let custom = MonarchCommentThreadLabels(reopen: "Open again", reopenedNotice: "Open")
        #expect(Tokens.resolveButton(isResolved: true, labels: custom).title == "Open again")
        #expect(Tokens.resolvedNotice(false, labels: custom) == "Open")
    }

    @Test("Delete needs the action and the comment's permission")
    func delete() {
        #expect(Tokens.canDelete(comment(1, canDelete: true), hasDeleteAction: true))
        #expect(!Tokens.canDelete(comment(1, canDelete: true), hasDeleteAction: false))
        #expect(!Tokens.canDelete(comment(1, canDelete: false), hasDeleteAction: true))
    }

    @Test("A reply is trimmed, and white space alone is not posted")
    func replyBody() {
        #expect(Tokens.replyBody("  Looks good \n") == "Looks good")
        #expect(Tokens.replyBody(" \n\t") == nil)
        #expect(Tokens.replyBody("") == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchCommentThreadStrings.quote, MonarchCommentThreadStrings.reply,
            MonarchCommentThreadStrings.replyPlaceholder, MonarchCommentThreadStrings.send,
            MonarchCommentThreadStrings.resolve, MonarchCommentThreadStrings.reopen,
            MonarchCommentThreadStrings.resolved, MonarchCommentThreadStrings.resolvedHint,
            MonarchCommentThreadStrings.resolvedNotice, MonarchCommentThreadStrings.reopenedNotice,
            MonarchCommentThreadStrings.sentNotice, MonarchCommentThreadStrings.actionFailed,
            MonarchCommentThreadStrings.delete, MonarchCommentThreadStrings.deleteTitle,
            MonarchCommentThreadStrings.deleteMessage, MonarchCommentThreadStrings.cancel,
            MonarchCommentThreadStrings.thread("A"), MonarchCommentThreadStrings.showEarlier(2),
            MonarchCommentThreadStrings.earlierShown(2),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
    }
}
