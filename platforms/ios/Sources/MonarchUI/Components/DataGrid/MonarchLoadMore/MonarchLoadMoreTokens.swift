import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchLoadMore``.
enum MonarchLoadMoreTokens {
    /// The gap between the count, the message and the button.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the footer.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The role of the count and the end text.
    static var muted: MonarchColorRole { \.textMuted }

    /// What the footer shows.
    enum Phase: Equatable {
        /// The button, ready.
        case idle
        /// The button, busy.
        case loading
        /// The failure message with a retry button.
        case failed
        /// Everything is loaded.
        case done
    }

    /// The facts that announcements compare between two draws.
    struct Snapshot: Equatable {
        /// Items on screen.
        let loadedCount: Int?
        /// Whether the last page failed.
        let isFailed: Bool
        /// Whether there is another page.
        let hasMore: Bool
    }

    /// The phase: a failure wins, then loading, then whether there is more.
    static func phase(hasMore: Bool, isLoading: Bool, isFailed: Bool) -> Phase {
        if isFailed { return .failed }
        if isLoading { return .loading }
        return hasMore ? .idle : .done
    }

    /// The count text, or `nil` when no count is shown: shown by default
    /// when the loaded count is known.
    static func countText(
        loadedCount: Int?,
        totalCount: Int?,
        showsCount: Bool?,
        labels: MonarchLoadMoreLabels
    ) -> String? {
        guard let loadedCount, showsCount ?? true else { return nil }
        return labels.count?(loadedCount, totalCount) ?? MonarchLoadMoreStrings.count(loadedCount, total: totalCount)
    }

    /// What is announced between two draws: new items, a failure, the end
    /// of the list, joined in that order; `nil` when nothing changed.
    static func announcement(
        from old: Snapshot,
        to new: Snapshot,
        showsEnd: Bool,
        labels: MonarchLoadMoreLabels
    ) -> String? {
        var parts: [String] = []
        if let before = old.loadedCount, let after = new.loadedCount, after > before {
            let added = after - before
            parts.append(labels.loaded?(added) ?? MonarchLoadMoreStrings.loaded(added))
        }
        if new.isFailed && !old.isFailed {
            parts.append(labels.failed ?? MonarchLoadMoreStrings.failed)
        }
        if !new.hasMore && old.hasMore && showsEnd {
            parts.append(labels.end ?? MonarchLoadMoreStrings.end)
        }
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }

    /// Whether to load the next page without a tap: only when asked, while
    /// the footer is on screen, with more to load, and not while loading or
    /// after a failure.
    static func shouldAutoLoad(autoLoads: Bool, isVisible: Bool, hasMore: Bool, isLoading: Bool, isFailed: Bool)
        -> Bool
    {
        autoLoads && isVisible && hasMore && !isLoading && !isFailed
    }
}
