import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchCommentThread``.
enum MonarchCommentThreadTokens {
    /// The gap between the parts of the thread.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the quote and the resolve button.
    static let topSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding around the top row and the reply field, matching the message rows.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The width of the bar beside the quote.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The gap between the quote bar and its text.
    static let quoteSpacing: CGFloat = MonarchTokens.Space.space2
    /// The role of the quote bar.
    static var quoteBar: MonarchColorRole { \.borderStrong }
    /// The role of the quote text and the resolved hint.
    static var muted: MonarchColorRole { \.textMuted }
    /// The symbol of the resolve button and the resolved badge.
    static let resolveSymbol = "checkmark"
    /// The symbol of the reopen button.
    static let reopenSymbol = "arrow.uturn.backward"
    /// The symbol of the delete menu item.
    static let deleteSymbol = "trash"
    /// Replies kept visible by default.
    static let defaultVisibleReplies = 3

    /// Which action is waiting for the app.
    enum Busy: Equatable {
        /// Posting a reply.
        case reply
        /// Resolving or reopening.
        case resolve
        /// Deleting a comment.
        case delete
    }

    /// What is shown under the comments.
    enum Footer: Equatable {
        /// The reply field.
        case composer
        /// The hint that says to reopen before replying.
        case resolvedHint
        /// Nothing: the app takes no replies.
        case none
    }

    /// The number of replies folded behind "Show N earlier replies". The
    /// first comment and the latest `visibleReplies` replies stay visible;
    /// one folded reply is never worth a button, so nothing folds then.
    static func foldedCount(replies: Int, visibleReplies: Int, isExpanded: Bool) -> Int {
        let keep = max(0, visibleReplies)
        guard !isExpanded, replies > keep + 1 else { return 0 }
        return replies - keep
    }

    /// The comments that are shown, in order: the first, then the replies after the folded ones.
    static func shown(
        _ comments: [MonarchCommentThreadComment],
        folded: Int
    ) -> (root: MonarchCommentThreadComment?, replies: [MonarchCommentThreadComment]) {
        guard let root = comments.first else { return (nil, []) }
        return (root, Array(comments.dropFirst().dropFirst(folded)))
    }

    /// What the footer shows: the reply field while open, the hint while resolved.
    static func footer(canReply: Bool, isResolved: Bool) -> Footer {
        guard canReply else { return .none }
        return isResolved ? .resolvedHint : .composer
    }

    /// The thread's spoken name, from its first author; `nil` while empty.
    static func threadLabel(_ comments: [MonarchCommentThreadComment], labels: MonarchCommentThreadLabels) -> String? {
        guard let author = comments.first?.author else { return nil }
        return labels.thread?(author) ?? MonarchCommentThreadStrings.thread(author)
    }

    /// The spoken state of the thread: "Resolved" or nothing.
    static func stateValue(isResolved: Bool, labels: MonarchCommentThreadLabels) -> String? {
        isResolved ? labels.resolved ?? MonarchCommentThreadStrings.resolved : nil
    }

    /// The title of the fold button.
    static func showEarlierTitle(_ count: Int, labels: MonarchCommentThreadLabels) -> String {
        labels.showEarlier?(count) ?? MonarchCommentThreadStrings.showEarlier(count)
    }

    /// Announced when folded replies are shown.
    static func earlierShownText(_ count: Int, labels: MonarchCommentThreadLabels) -> String {
        labels.earlierShown?(count) ?? MonarchCommentThreadStrings.earlierShown(count)
    }

    /// The title and symbol of the resolve button: Reopen while resolved.
    static func resolveButton(isResolved: Bool, labels: MonarchCommentThreadLabels) -> (title: String, symbol: String) {
        isResolved
            ? (labels.reopen ?? MonarchCommentThreadStrings.reopen, reopenSymbol)
            : (labels.resolve ?? MonarchCommentThreadStrings.resolve, resolveSymbol)
    }

    /// Announced after the thread is resolved (`true`) or reopened.
    static func resolvedNotice(_ isResolved: Bool, labels: MonarchCommentThreadLabels) -> String {
        isResolved
            ? labels.resolvedNotice ?? MonarchCommentThreadStrings.resolvedNotice
            : labels.reopenedNotice ?? MonarchCommentThreadStrings.reopenedNotice
    }

    /// Whether a comment's menu offers Delete.
    static func canDelete(_ comment: MonarchCommentThreadComment, hasDeleteAction: Bool) -> Bool {
        hasDeleteAction && comment.canDelete
    }

    /// The reply text that is posted: trimmed, or `nil` when nothing is left.
    static func replyBody(_ text: String) -> String? {
        let body = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return body.isEmpty ? nil : body
    }
}
