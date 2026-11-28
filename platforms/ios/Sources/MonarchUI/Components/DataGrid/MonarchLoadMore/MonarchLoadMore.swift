import SwiftUI

/// The footer of a paginated list: a Load more button with loading, failure
/// and retry, an end-of-list line, an optional count, and announcements.
///
/// ```swift
/// List {
///     ForEach(model.items) { ItemRow(item: $0) }
///     MonarchLoadMore(
///         hasMore: model.nextCursor != nil,
///         isLoading: model.isLoading,
///         isFailed: model.failed,
///         loadedCount: model.items.count,
///         autoLoads: true,
///         onLoadMore: { model.loadNextPage() }
///     )
/// }
/// ```
///
/// The app keeps the cursor and fetches pages; the footer only shows where
/// the list stands. With `autoLoads` the next page loads when the footer
/// scrolls into view; the button stays for VoiceOver, Switch Control and
/// when auto-loading pauses after a failure.
///
/// Accessibility: the button is a standard ``MonarchButton`` that reads
/// "Loading…" as its value while busy. A failure is a
/// ``MonarchInlineErrorRetry`` row. New items ("20 more items loaded."),
/// a failure and the end of the list are announced; the count and the end
/// text are static text.
public struct MonarchLoadMore: View {
    private let hasMore: Bool
    private let isLoading: Bool
    private let isFailed: Bool
    private let loadedCount: Int?
    private let totalCount: Int?
    private let showsCount: Bool?
    private let showsEnd: Bool
    private let autoLoads: Bool
    private let labels: MonarchLoadMoreLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onLoadMore: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var isVisible = false

    /// A load-more footer.
    ///
    /// - Parameters:
    ///   - hasMore: Whether there is another page.
    ///   - isLoading: Whether a page is being fetched.
    ///   - isFailed: Whether the last fetch failed. Shows the failure and a
    ///     retry button, and pauses auto-loading.
    ///   - loadedCount: The items on screen. Increases are announced.
    ///   - totalCount: The total, when the server knows it.
    ///   - showsCount: Whether the count is shown. By default when
    ///     `loadedCount` is given.
    ///   - showsEnd: Whether "That's everything" is shown and announced at the end.
    ///   - autoLoads: Whether the next page loads when the footer appears.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Runs when retry is activated. By default `onLoadMore`.
    ///   - onLoadMore: Loads the next page.
    public init(
        hasMore: Bool,
        isLoading: Bool = false,
        isFailed: Bool = false,
        loadedCount: Int? = nil,
        totalCount: Int? = nil,
        showsCount: Bool? = nil,
        showsEnd: Bool = true,
        autoLoads: Bool = false,
        labels: MonarchLoadMoreLabels = MonarchLoadMoreLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onLoadMore: @escaping @MainActor () -> Void
    ) {
        self.hasMore = hasMore
        self.isLoading = isLoading
        self.isFailed = isFailed
        self.loadedCount = loadedCount
        self.totalCount = totalCount
        self.showsCount = showsCount
        self.showsEnd = showsEnd
        self.autoLoads = autoLoads
        self.labels = labels
        self.onRetry = onRetry
        self.onLoadMore = onLoadMore
    }

    private var snapshot: MonarchLoadMoreTokens.Snapshot {
        MonarchLoadMoreTokens.Snapshot(loadedCount: loadedCount, isFailed: isFailed, hasMore: hasMore)
    }

    /// The content of the view.
    public var body: some View {
        let phase = MonarchLoadMoreTokens.phase(hasMore: hasMore, isLoading: isLoading, isFailed: isFailed)
        // As on the web, the count and the button share a row when they fit; otherwise the button goes under.
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchLoadMoreTokens.spacing) { parts(phase) }
            VStack(spacing: MonarchLoadMoreTokens.spacing) { parts(phase) }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, MonarchLoadMoreTokens.verticalPadding)
        .onAppear {
            isVisible = true
            autoLoadIfNeeded()
        }
        .onDisappear { isVisible = false }
        .onChange(of: snapshot) { old, new in
            let message = MonarchLoadMoreTokens.announcement(from: old, to: new, showsEnd: showsEnd, labels: labels)
            if let message {
                AccessibilityNotification.Announcement(message).post()
            }
        }
        .onChange(of: isLoading) { autoLoadIfNeeded() }
    }

    private func mutedText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchLoadMoreTokens.muted])
            .multilineTextAlignment(.center)
    }

    private func autoLoadIfNeeded() {
        if MonarchLoadMoreTokens.shouldAutoLoad(
            autoLoads: autoLoads,
            isVisible: isVisible,
            hasMore: hasMore,
            isLoading: isLoading,
            isFailed: isFailed
        ) {
            onLoadMore()
        }
    }

    @ViewBuilder private func parts(_ phase: MonarchLoadMoreTokens.Phase) -> some View {
        if let count = MonarchLoadMoreTokens.countText(
            loadedCount: loadedCount,
            totalCount: totalCount,
            showsCount: showsCount,
            labels: labels
        ) {
            mutedText(count)
        }
        switch phase {
        case .failed:
            MonarchInlineErrorRetry(
                labels.failed ?? MonarchLoadMoreStrings.failed,
                isRetrying: isLoading,
                retryLabel: labels.retry ?? MonarchLoadMoreStrings.retry,
                onRetry: onRetry ?? onLoadMore
            )
        case .idle, .loading:
            let loadingTitle = labels.loading ?? MonarchLoadMoreStrings.loading
            MonarchButton(
                phase == .loading ? loadingTitle : (labels.loadMore ?? MonarchLoadMoreStrings.loadMore),
                size: .sm,
                isLoading: phase == .loading,
                loadingLabel: loadingTitle,
                action: onLoadMore
            )
        case .done:
            if showsEnd {
                mutedText(labels.end ?? MonarchLoadMoreStrings.end)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchLoadMore") {
        MonarchPreviewMatrix {
            VStack {
                MonarchLoadMore(hasMore: true, loadedCount: 20, totalCount: 120, onLoadMore: {})
                MonarchLoadMore(hasMore: true, isLoading: true, loadedCount: 40, onLoadMore: {})
                MonarchLoadMore(hasMore: true, isFailed: true, onLoadMore: {})
                MonarchLoadMore(hasMore: false, loadedCount: 120, totalCount: 120, onLoadMore: {})
            }
        }
    }
#endif
