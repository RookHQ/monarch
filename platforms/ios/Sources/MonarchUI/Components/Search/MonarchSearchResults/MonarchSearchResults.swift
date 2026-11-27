import SwiftUI

/// Search results in labelled groups with counts, highlighted matches, and
/// loading, empty and error states.
///
/// ```swift
/// MonarchSearchResults(
///     groups: [
///         MonarchSearchResultGroup("Documents", id: "docs", results: documents, total: 24),
///         MonarchSearchResultGroup("People", id: "people", results: people),
///     ],
///     query: query,
///     isLoading: isSearching,
///     onShowAll: { showAll($0) }
/// ) { result, _ in
///     open(result)
/// }
/// .searchable(text: $query)
/// ```
///
/// The results are a plain `List` with a `Section` per group, so the list
/// scrolls and works with `.searchable`. Each row is a
/// ``MonarchSearchResultItem`` with the query's words highlighted. A group
/// without a title shows its rows without a heading, for results without
/// groups. Groups with no results are left out.
///
/// States, as on the web: with `isFailed` an error state with Try again;
/// with no results and `isLoading`, skeleton rows; with no results, an empty
/// state; with results and `isLoading`, the results stay and the summary
/// reads "Searching…".
///
/// Accessibility: the summary ("12 results for “travel”") is shown above the
/// results and announced when it changes, as the web's live region; the
/// error title is announced too. Each heading is read with its count, such
/// as "Documents, 24 results", and has the header trait. Results are
/// buttons that read the title, path and snippet. Skeleton rows are hidden
/// from VoiceOver.
public struct MonarchSearchResults: View {
    private let groups: [MonarchSearchResultGroup]
    private let query: String
    private let isLoading: Bool
    private let isFailed: Bool
    private let errorMessage: String?
    private let loadingRows: Int
    private let labels: MonarchSearchResultsLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onShowAll: (@MainActor (MonarchSearchResultGroup) -> Void)?
    private let onSelect: @MainActor (MonarchSearchResult, MonarchSearchResultGroup) -> Void

    @Environment(\.monarchTheme) private var theme

    /// Search results.
    ///
    /// - Parameters:
    ///   - groups: The results in groups. Pass one group with a `nil` title
    ///     for results without groups.
    ///   - query: What the person searched for. Its words are highlighted and
    ///     named in the summary.
    ///   - isLoading: Whether a search is running.
    ///   - isFailed: Whether the search failed. It replaces the results with an error state.
    ///   - errorMessage: Why the search failed, shown under the error title.
    ///   - loadingRows: How many skeleton rows to show while loading with no results.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Runs the search again. Adds Try again to the error state.
    ///   - onShowAll: Shows every match of a group. Adds a Show all button
    ///     to groups that have more matches than they show.
    ///   - onSelect: Opens a result.
    public init(
        groups: [MonarchSearchResultGroup],
        query: String = "",
        isLoading: Bool = false,
        isFailed: Bool = false,
        errorMessage: String? = nil,
        loadingRows: Int = 4,
        labels: MonarchSearchResultsLabels = MonarchSearchResultsLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onShowAll: (@MainActor (MonarchSearchResultGroup) -> Void)? = nil,
        onSelect: @escaping @MainActor (MonarchSearchResult, MonarchSearchResultGroup) -> Void
    ) {
        self.groups = groups
        self.query = query
        self.isLoading = isLoading
        self.isFailed = isFailed
        self.errorMessage = errorMessage
        self.loadingRows = max(0, loadingRows)
        self.labels = labels
        self.onRetry = onRetry
        self.onShowAll = onShowAll
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let visible = MonarchSearchResultsTokens.visibleGroups(groups)
        let status = MonarchSearchResultsTokens.status(
            isFailed: isFailed,
            resultCount: visible.reduce(0) { $0 + $1.results.count },
            isLoading: isLoading
        )
        let summary = MonarchSearchResultsTokens.summary(
            status: status,
            isLoading: isLoading,
            total: MonarchSearchResultsTokens.total(of: visible),
            query: query,
            labels: labels
        )
        VStack(alignment: .leading, spacing: MonarchSearchResultsTokens.spacing) {
            if status == .ready {
                HStack(spacing: MonarchSearchResultsTokens.headerSpacing) {
                    Text(summary)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textMuted)
                    if isLoading {
                        ProgressView()
                            .controlSize(.small)
                            .accessibilityHidden(true)
                    }
                }
                .padding(.horizontal, MonarchSearchResultsTokens.rowInset)
            }
            content(status: status, groups: visible)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .onChange(of: summary) { _, newValue in
            if !newValue.isEmpty {
                AccessibilityNotification.Announcement(newValue).post()
            }
        }
        .onChange(of: status) { _, newValue in
            if newValue == .error {
                AccessibilityNotification.Announcement(MonarchSearchResultsTokens.errorTitle(labels: labels)).post()
            }
        }
    }

    @ViewBuilder
    private func content(status: MonarchSearchResultsTokens.Status, groups: [MonarchSearchResultGroup]) -> some View {
        switch status {
        case .error:
            MonarchErrorState(
                MonarchSearchResultsTokens.errorTitle(labels: labels),
                message: errorMessage,
                size: .sm,
                retryLabel: labels.retry,
                onRetry: onRetry
            )
        case .empty:
            MonarchEmptyState(
                MonarchSearchResultsTokens.emptyTitle(query: query, labels: labels),
                message: MonarchSearchResultsTokens.emptyMessage(labels: labels),
                systemImage: "magnifyingglass",
                kind: .filtered,
                size: .sm
            )
        case .loading:
            VStack(alignment: .leading, spacing: 0) {
                ForEach(0..<loadingRows, id: \.self) { _ in
                    MonarchSearchResultItem(
                        MonarchSearchResultsTokens.skeletonTitle,
                        snippet: MonarchSearchResultsTokens.skeletonSnippet
                    )
                }
            }
            .padding(.horizontal, MonarchSearchResultsTokens.rowInset)
            .redacted(reason: .placeholder)
            .accessibilityHidden(true)
        case .ready:
            list(groups)
        }
    }

    private func list(_ groups: [MonarchSearchResultGroup]) -> some View {
        let terms = MonarchSearchResultItem.terms(from: query)
        return List {
            ForEach(groups) { group in
                Section {
                    ForEach(group.results) { result in
                        row(result, group: group, terms: terms)
                    }
                    if MonarchSearchResultsTokens.showsShowAll(group, hasHandler: onShowAll != nil) {
                        MonarchButton(
                            MonarchSearchResultsTokens.showAllTitle(group, labels: labels),
                            variant: .ghost,
                            size: .sm
                        ) {
                            onShowAll?(group)
                        }
                        .listRowSeparator(.hidden)
                        .monarchRowsOnCanvas()
                    }
                } header: {
                    if group.title != nil {
                        header(group)
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    private func row(_ result: MonarchSearchResult, group: MonarchSearchResultGroup, terms: [String]) -> some View {
        MonarchSearchResultItem(
            result.title,
            path: result.path,
            snippet: result.snippet,
            terms: terms,
            titleRanges: result.titleRanges,
            snippetRanges: result.snippetRanges,
            snippetHasMarkTags: result.snippetHasMarkTags,
            systemImage: result.systemImage,
            fileName: result.fileName,
            tone: result.tone,
            action: { onSelect(result, group) },
            meta: {
                if let meta = result.meta {
                    if result.showsMetaAsBadge {
                        MonarchBadge(meta)
                    } else {
                        Text(meta)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
        )
        .listRowInsets(
            EdgeInsets(
                top: 0,
                leading: MonarchSearchResultsTokens.rowInset,
                bottom: 0,
                trailing: MonarchSearchResultsTokens.rowInset
            )
        )
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }

    private func header(_ group: MonarchSearchResultGroup) -> some View {
        VStack(alignment: .leading, spacing: MonarchSearchResultsTokens.headerSpacing) {
            headerLine(group)
            MonarchDivider()
        }
        .textCase(nil)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchSearchResultsTokens.headerAccessibilityLabel(group, labels: labels))
        .accessibilityAddTraits(.isHeader)
    }

    private func headerLine(_ group: MonarchSearchResultGroup) -> some View {
        HStack(spacing: MonarchSearchResultsTokens.headerSpacing) {
            if let systemImage = group.systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let title = group.title {
                Text(title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
            }
            Text(MonarchSearchResultsTokens.countText(group))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .padding(.horizontal, MonarchSearchResultsTokens.countPadding)
                .background(theme.colors.bgMuted, in: Capsule())
        }
    }
}

#if DEBUG
    #Preview("MonarchSearchResults") {
        MonarchPreviewMatrix {
            MonarchSearchResults(
                groups: [
                    MonarchSearchResultGroup(
                        "Documents",
                        id: "docs",
                        results: [
                            MonarchSearchResult(
                                "Travel policy",
                                id: "1",
                                path: ["Handbook", "Finance"],
                                snippet: "Book travel at least two weeks ahead.",
                                fileName: "travel-policy.pdf",
                                meta: "PDF"
                            )
                        ],
                        total: 12
                    )
                ],
                query: "travel",
                onShowAll: { _ in },
                onSelect: { _, _ in }
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 5)
        }
    }
#endif
