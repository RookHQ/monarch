import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchCommentsPanel``.
enum MonarchCommentsPanelTokens {
    /// Padding around the filter picker.
    static let filterPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the avatar and the text of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a row's content.
    static let rowPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the open thread.
    static let threadPadding: CGFloat = MonarchTokens.Space.space4
    /// The lines of a quote shown in the list; the open thread shows all of it.
    static let quoteLines = 2
    /// The rows of the loading placeholder.
    static let placeholderRows = 3
    /// The symbol of the empty state.
    static let emptySymbol = "text.bubble"
    /// The role of the author in a row.
    static var author: MonarchColorRole { \.textDefault }
    /// The role of the quote and the count line in a row.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of a load failure without a retry.
    static var failure: MonarchColorRole { \.dangerStrong }

    /// What the panel shows in place of the list.
    enum Content: Equatable {
        /// The threads are loading.
        case loading
        /// Loading failed, with the app's message.
        case failed(String)
        /// No thread under the filter.
        case empty
        /// The threads under the filter.
        case list
    }

    /// The content: a failure wins, then loading, then empty.
    static func content(
        threads: [MonarchCommentsPanelThread]?,
        errorMessage: String?,
        filter: MonarchCommentsPanelFilter
    )
        -> Content
    {
        if let errorMessage { return .failed(errorMessage) }
        guard let threads else { return .loading }
        return visible(threads, filter: filter).isEmpty ? .empty : .list
    }

    /// The threads listed under a filter, in the given order.
    static func visible(_ threads: [MonarchCommentsPanelThread], filter: MonarchCommentsPanelFilter)
        -> [MonarchCommentsPanelThread]
    {
        threads.filter { $0.isResolved == (filter == .resolved) }
    }

    /// The thread shown in full: the selected one, only while it is listed
    /// under the filter, so a thread resolved elsewhere returns to the list.
    static func shownThread(
        _ threads: [MonarchCommentsPanelThread]?,
        filter: MonarchCommentsPanelFilter,
        selectedID: String?
    ) -> MonarchCommentsPanelThread? {
        guard let selectedID, let threads else { return nil }
        return visible(threads, filter: filter).first { $0.id == selectedID }
    }

    /// The last activity of a thread: `updatedAt`, or the newest comment's time.
    static func lastActivity(_ thread: MonarchCommentsPanelThread) -> Date? {
        thread.updatedAt ?? thread.comments.last?.createdAt
    }

    /// "3 comments".
    static func commentCount(_ count: Int, labels: MonarchCommentsPanelLabels) -> String {
        labels.commentCount?(count) ?? MonarchCommentsPanelStrings.commentCount(count)
    }

    /// The line under a row: "3 comments · 5 minutes ago".
    static func detailLine(
        _ thread: MonarchCommentsPanelThread,
        now: Date,
        locale: Locale,
        calendar: Calendar,
        labels: MonarchCommentsPanelLabels
    ) -> String {
        let count = commentCount(thread.comments.count, labels: labels)
        guard let date = lastActivity(thread) else { return count }
        let time = MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar)
        return "\(count) · \(time)"
    }

    /// What VoiceOver reads for a row: the starter, the quote, the count and the last activity.
    static func rowLabel(
        _ thread: MonarchCommentsPanelThread,
        now: Date,
        locale: Locale,
        calendar: Calendar,
        labels: MonarchCommentsPanelLabels
    ) -> String {
        var parts: [String] = []
        if let author = thread.comments.first?.author, !author.isEmpty { parts.append(author) }
        if let quote = thread.quote, !quote.isEmpty { parts.append(quote) }
        parts.append(commentCount(thread.comments.count, labels: labels))
        if let date = lastActivity(thread) {
            parts.append(MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar))
        }
        return parts.joined(separator: ", ")
    }

    /// The title of the empty state under a filter.
    static func emptyTitle(_ filter: MonarchCommentsPanelFilter, labels: MonarchCommentsPanelLabels) -> String {
        switch filter {
        case .open: labels.emptyOpen ?? MonarchCommentsPanelStrings.emptyOpen
        case .resolved: labels.emptyResolved ?? MonarchCommentsPanelStrings.emptyResolved
        }
    }

    /// The hint of the empty state: only under Open, where a new discussion starts.
    static func emptyMessage(_ filter: MonarchCommentsPanelFilter, labels: MonarchCommentsPanelLabels) -> String? {
        filter == .open ? labels.emptyHint ?? MonarchCommentsPanelStrings.emptyHint : nil
    }

    /// The name of a segment.
    static func filterTitle(_ filter: MonarchCommentsPanelFilter, labels: MonarchCommentsPanelLabels) -> String {
        switch filter {
        case .open: labels.open ?? MonarchCommentsPanelStrings.open
        case .resolved: labels.resolved ?? MonarchCommentsPanelStrings.resolved
        }
    }

    /// Which actions the open thread offers: the panel's action and the thread's permission.
    static func actions(
        _ thread: MonarchCommentsPanelThread,
        hasReply: Bool,
        hasResolve: Bool,
        hasDelete: Bool
    ) -> (reply: Bool, resolve: Bool, delete: Bool) {
        (hasReply && thread.canReply, hasResolve && thread.canResolve, hasDelete)
    }
}
