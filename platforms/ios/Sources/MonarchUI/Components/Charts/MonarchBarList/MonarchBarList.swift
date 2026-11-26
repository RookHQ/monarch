import SwiftUI

/// A ranked list with a bar behind each row: top pages, top referrers,
/// rivals by score.
///
/// ```swift
/// MonarchBarList(
///     "Top pages",
///     items: [
///         MonarchBarListItem("/pricing", value: 4_210),
///         MonarchBarListItem("/docs", value: 2_980),
///     ],
///     isRanked: true
/// )
/// ```
///
/// Labels and values are text; the bars only repeat the values. Values are
/// written in the locale of the environment unless `formatValue` is given.
///
/// Accessibility: the list is a container with the label you give it. Each
/// row is one element: its label and description, then its value, or "No
/// value" when there is none. The bars and rank numbers are hidden. With
/// `onSelect` each row is a button with a touch target at least 44 points high.
public struct MonarchBarList: View {
    private let label: String?
    private let items: [MonarchBarListItem]
    private let sort: MonarchBarListSort
    private let maximum: Double?
    private let limit: Int?
    private let isRanked: Bool
    private let isLoading: Bool
    private let loadingRows: Int
    private let error: String?
    private let isRetrying: Bool
    private let missingLabel: String?
    private let emptyLabel: String?
    private let loadingLabel: String?
    private let overflowLabel: String?
    private let retryLabel: String?
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchBarListItem) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A bar list.
    ///
    /// - Parameters:
    ///   - label: Names the list for VoiceOver, such as "Top pages". Already localised.
    ///   - items: The rows.
    ///   - sort: The row order. Rows without a value go last.
    ///   - maximum: The value of a full bar. The largest value when `nil`.
    ///   - limit: Shows only the first rows, with a "Top 5 of 12" footer.
    ///   - isRanked: Whether the rows are numbered.
    ///   - isLoading: Whether the rows are being loaded. Shows placeholder rows instead.
    ///   - loadingRows: How many placeholder rows to show while loading.
    ///   - error: A message, already localised, shown instead of the rows when they could not be loaded.
    ///   - isRetrying: Whether a retry is running, so the retry button waits.
    ///   - missingLabel: Replaces the built-in spoken text "No value".
    ///   - emptyLabel: Replaces the built-in text "No data yet".
    ///   - loadingLabel: Replaces the built-in text "Loading".
    ///   - overflowLabel: Replaces the built-in footer "Top 5 of 12".
    ///   - retryLabel: Replaces the built-in title "Try again" of the retry button.
    ///   - formatValue: Writes a value. The default is a number in the current locale.
    ///   - onRetry: Adds a retry button to the error. Call it with the label `onRetry:`.
    ///   - onSelect: Makes every row a button and runs when one is activated.
    public init(
        _ label: String? = nil,
        items: [MonarchBarListItem],
        sort: MonarchBarListSort = .descending,
        maximum: Double? = nil,
        limit: Int? = nil,
        isRanked: Bool = false,
        isLoading: Bool = false,
        loadingRows: Int = 5,
        error: String? = nil,
        isRetrying: Bool = false,
        missingLabel: String? = nil,
        emptyLabel: String? = nil,
        loadingLabel: String? = nil,
        overflowLabel: String? = nil,
        retryLabel: String? = nil,
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchBarListItem) -> Void)? = nil
    ) {
        self.label = label
        self.items = items
        self.sort = sort
        self.maximum = maximum
        self.limit = limit
        self.isRanked = isRanked
        self.isLoading = isLoading
        self.loadingRows = loadingRows
        self.error = error
        self.isRetrying = isRetrying
        self.missingLabel = missingLabel
        self.emptyLabel = emptyLabel
        self.loadingLabel = loadingLabel
        self.overflowLabel = overflowLabel
        self.retryLabel = retryLabel
        self.formatValue = formatValue
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label ?? "")
    }

    @ViewBuilder private var content: some View {
        if let error = MonarchFieldChrome.activeError(error) {
            failure(error)
        } else if isLoading {
            placeholder
        } else if items.isEmpty {
            message(emptyLabel ?? MonarchBarListStrings.empty)
        } else {
            rows
        }
    }

    /// Redacted rows in the shape of the real ones, read as one "Loading".
    private var placeholder: some View {
        let layout = MonarchBarListTokens.layout(
            items: MonarchBarListTokens.placeholderItems(count: loadingRows),
            sort: .descending,
            maximum: nil,
            limit: nil
        )
        return VStack(alignment: .leading, spacing: MonarchBarListTokens.rowSpacing) {
            ForEach(layout.rows) { row in
                MonarchBarListRow(
                    row: row,
                    valueText: MonarchBarListTokens.placeholderName,
                    accessibilityValue: "",
                    isRanked: isRanked,
                    onSelect: nil
                )
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(loadingLabel ?? MonarchBarListStrings.loading)
    }

    @ViewBuilder private func failure(_ message: String) -> some View {
        if let onRetry {
            MonarchInlineErrorRetry(message, isRetrying: isRetrying, retryLabel: retryLabel, onRetry: onRetry)
        } else {
            MonarchInlineNotice(message, tone: .danger, variant: .plain)
        }
    }

    private var rows: some View {
        let layout = MonarchBarListTokens.layout(items: items, sort: sort, maximum: maximum, limit: limit)
        let missing = missingLabel ?? MonarchBarListStrings.missing
        return VStack(alignment: .leading, spacing: MonarchBarListTokens.rowSpacing) {
            ForEach(layout.rows) { row in
                let value = MonarchBarListTokens.valueText(for: row.item, locale: locale, formatValue: formatValue)
                MonarchBarListRow(
                    row: row,
                    valueText: value,
                    accessibilityValue: value ?? missing,
                    isRanked: isRanked,
                    onSelect: onSelect
                )
            }
            if layout.rows.count < layout.total {
                message(
                    overflowLabel
                        ?? MonarchBarListStrings.overflow(
                            shown: layout.rows.count.formatted(.number.locale(locale)),
                            total: layout.total.formatted(.number.locale(locale))
                        )
                )
            }
        }
    }

    private func message(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}

#if DEBUG
    #Preview("MonarchBarList") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchBarList(
                    "Top pages",
                    items: [
                        MonarchBarListItem(
                            "/pricing",
                            value: 4_210,
                            description: "Campaign: spring",
                            systemImage: "tag"
                        ),
                        MonarchBarListItem("/docs", value: 2_980, isHighlighted: true),
                        MonarchBarListItem("/blog", value: nil),
                        MonarchBarListItem("/about", value: 310),
                    ],
                    limit: 3,
                    isRanked: true
                )
                MonarchBarList("Empty", items: [])
                MonarchBarList("Loading", items: [], isLoading: true)
                MonarchBarList("Failed", items: [], error: "The pages could not be loaded.", onRetry: {})
            }
        }
    }
#endif
