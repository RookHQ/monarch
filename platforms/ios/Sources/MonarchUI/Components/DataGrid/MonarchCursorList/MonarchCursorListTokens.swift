import SwiftUI

/// Component tokens and pure logic for ``MonarchCursorList``.
enum MonarchCursorListTokens {
    /// The number of placeholder rows while the first page loads.
    static let loadingRows = 3

    /// What the list shows.
    enum Phase: Equatable {
        /// The first page failed: the error state with retry.
        case failed
        /// The first page is loading: placeholder rows.
        case loading
        /// Nothing was loaded and there is nothing more: the empty state.
        case empty
        /// The items and the footer.
        case ready
    }

    /// The phase, as the web's `data-state`: with no items a failure wins,
    /// then loading, then empty when there is no further page.
    static func phase(count: Int, hasMore: Bool, isLoading: Bool, isFailed: Bool) -> Phase {
        guard count == 0 else { return .ready }
        if isFailed { return .failed }
        if isLoading { return .loading }
        return hasMore ? .ready : .empty
    }

    /// The index of the item that takes VoiceOver focus after a page the
    /// footer asked for has arrived: the first new item. `nil` while the page
    /// is loading, when nothing was asked for, or when nothing new arrived.
    static func focusTarget(pending: Int?, count: Int, isLoading: Bool) -> Int? {
        guard let pending, !isLoading, count > pending, pending >= 0 else { return nil }
        return pending
    }

    /// The footer labels: the list's retry title fills in a footer retry title that is not set.
    static func footerLabels(_ labels: MonarchCursorListLabels) -> MonarchLoadMoreLabels {
        var footer = labels.footer
        if footer.retry == nil { footer.retry = labels.retry }
        return footer
    }
}
