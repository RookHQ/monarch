import SwiftUI

/// A list loaded page by page from a cursor: the items, a
/// ``MonarchLoadMore`` footer with a count, and the first-load, empty and
/// error states.
///
/// ```swift
/// MonarchCursorList(
///     model.requests,
///     label: "Requests",
///     hasMore: model.nextCursor != nil,
///     isLoading: model.isLoading,
///     isFailed: model.failed,
///     totalCount: model.total,
///     onLoadMore: { model.loadNextPage() },
///     row: { request in RequestRow(request) }
/// )
/// ```
///
/// The app keeps the cursor and fetches pages; the list only shows where it
/// stands. It is a plain `List`, so it scrolls and fills the space it is
/// given: use it as the content of a screen or a tab, not inside another
/// scroll view. With no items yet, a failure shows a ``MonarchErrorState``
/// with retry, loading shows a ``MonarchLoadingState``, and no further page
/// shows a ``MonarchEmptyState``.
///
/// Accessibility: the list is named by `label`. The footer announces new
/// items, a failure and the end. When a page asked for from the footer
/// arrives, VoiceOver focus moves to its first item, so the reader continues
/// where the new page starts.
public struct MonarchCursorList<Item: Identifiable, Row: View>: View {
    private let items: [Item]
    private let label: String
    private let hasMore: Bool
    private let isLoading: Bool
    private let isFailed: Bool
    private let totalCount: Int?
    private let autoLoads: Bool
    private let showsEnd: Bool
    private let isDivided: Bool
    private let focusesNewItems: Bool
    private let loadingVariant: MonarchLoadingStateVariant
    private let labels: MonarchCursorListLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onLoadMore: @MainActor () -> Void
    private let row: (Item) -> Row

    @State private var pendingFocus: Int?
    @AccessibilityFocusState private var focusedItem: Item.ID?

    /// A cursor list.
    ///
    /// - Parameters:
    ///   - items: Every item loaded so far, in order.
    ///   - label: Names the list for VoiceOver, already localised.
    ///   - hasMore: Whether there is another page.
    ///   - isLoading: Whether a page is being fetched. With no items yet, shows the loading state.
    ///   - isFailed: Whether the last fetch failed. With no items yet, shows the error state.
    ///   - totalCount: The total, when the server knows it ("Showing 20 of 140 items").
    ///   - autoLoads: Whether the next page loads when the footer scrolls into view.
    ///   - showsEnd: Whether "That's everything" is shown after the last page.
    ///   - isDivided: Whether rows are separated by lines.
    ///   - focusesNewItems: Whether VoiceOver focus moves to the first item of a page asked for from the footer.
    ///   - loadingVariant: The shape of the first-page placeholder.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Repeats the failed request. By default `onLoadMore`.
    ///   - onLoadMore: Loads the next page.
    ///   - row: Draws one item. Put a button or a navigation link inside when the row opens something.
    public init(
        _ items: [Item],
        label: String,
        hasMore: Bool,
        isLoading: Bool = false,
        isFailed: Bool = false,
        totalCount: Int? = nil,
        autoLoads: Bool = false,
        showsEnd: Bool = true,
        isDivided: Bool = true,
        focusesNewItems: Bool = true,
        loadingVariant: MonarchLoadingStateVariant = .list,
        labels: MonarchCursorListLabels = MonarchCursorListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onLoadMore: @escaping @MainActor () -> Void,
        @ViewBuilder row: @escaping (Item) -> Row
    ) {
        self.items = items
        self.label = label
        self.hasMore = hasMore
        self.isLoading = isLoading
        self.isFailed = isFailed
        self.totalCount = totalCount
        self.autoLoads = autoLoads
        self.showsEnd = showsEnd
        self.isDivided = isDivided
        self.focusesNewItems = focusesNewItems
        self.loadingVariant = loadingVariant
        self.labels = labels
        self.onRetry = onRetry
        self.onLoadMore = onLoadMore
        self.row = row
    }

    private typealias Tokens = MonarchCursorListTokens

    /// The content of the view.
    public var body: some View {
        switch Tokens.phase(count: items.count, hasMore: hasMore, isLoading: isLoading, isFailed: isFailed) {
        case .failed:
            MonarchErrorState(
                labels.errorTitle ?? MonarchCursorListStrings.errorTitle,
                size: .sm,
                isRetrying: isLoading,
                retryLabel: labels.retry,
                onRetry: onRetry ?? onLoadMore
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .loading:
            MonarchLoadingState(
                labels.loading ?? MonarchCursorListStrings.loading,
                variant: loadingVariant,
                rows: Tokens.loadingRows
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchCursorListStrings.emptyTitle,
                message: labels.emptyMessage ?? MonarchCursorListStrings.emptyMessage,
                size: .sm
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .ready:
            list
        }
    }

    private var list: some View {
        List {
            Group {
                ForEach(items) { item in
                    row(item)
                        .accessibilityFocused($focusedItem, equals: item.id)
                        .listRowSeparator(isDivided ? .automatic : .hidden)
                }
                MonarchLoadMore(
                    hasMore: hasMore,
                    isLoading: isLoading,
                    isFailed: isFailed,
                    loadedCount: items.count,
                    totalCount: totalCount,
                    showsEnd: showsEnd,
                    autoLoads: autoLoads,
                    labels: Tokens.footerLabels(labels),
                    onRetry: { request(onRetry ?? onLoadMore) },
                    onLoadMore: { request(onLoadMore) }
                )
                .listRowSeparator(.hidden)
            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .accessibilityLabel(label)
        .onChange(of: items.count) { moveFocusIfReady() }
        .onChange(of: isLoading) { moveFocusIfReady() }
    }

    private func request(_ action: @MainActor () -> Void) {
        pendingFocus = focusesNewItems ? items.count : nil
        action()
    }

    private func moveFocusIfReady() {
        guard let target = Tokens.focusTarget(pending: pendingFocus, count: items.count, isLoading: isLoading)
        else {
            if !isLoading { pendingFocus = nil }
            return
        }
        pendingFocus = nil
        focusedItem = items[target].id
    }
}

#if DEBUG
    private struct MonarchCursorListPreviewItem: Identifiable {
        let id: Int
    }

    #Preview("MonarchCursorList") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCursorList(
                    (1...3).map(MonarchCursorListPreviewItem.init),
                    label: "Requests",
                    hasMore: true,
                    totalCount: 9,
                    onLoadMore: {},
                    row: { Text("Request \($0.id)") }
                )
                MonarchCursorList(
                    [MonarchCursorListPreviewItem](),
                    label: "Requests",
                    hasMore: false,
                    onLoadMore: {},
                    row: { Text("Request \($0.id)") }
                )
            }
        }
    }
#endif
