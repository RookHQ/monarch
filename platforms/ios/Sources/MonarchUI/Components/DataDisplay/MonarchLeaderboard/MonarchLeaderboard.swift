import SwiftUI

/// A ranked list for dashboards (top agents, best campaign steps, busiest
/// teams): rank, avatar, name, value, an optional change and a bar relative
/// to the leader.
///
/// ```swift
/// MonarchLeaderboard(
///     [
///         MonarchLeaderboardEntry("Ana Lima", value: 42, change: 0.12),
///         MonarchLeaderboardEntry("Sam Ortiz", value: 38, isCurrent: true),
///     ],
///     label: "Tickets solved this month",
///     valueLabel: "tickets solved",
///     sort: .descending
/// )
/// ```
///
/// Equal values share a rank and the next rank skips (1, 1, 3). The viewer's
/// own row is tinted and tagged "(you)" in words. The rows are a plain stack,
/// so the board fits inside a ``MonarchWidget``, a `List` row or a scroll
/// view; it does not scroll on its own.
///
/// Accessibility: each row is one element read as "Rank 1: Ana Lima, (you),
/// 42 tickets solved, up 12%, better". Avatars and bars are hidden, because
/// the name and the number carry the data. With `onSelect` each row is a
/// button with a 44-point touch target. Placeholder rows are one element
/// named "Loading ranking".
public struct MonarchLeaderboard: View {
    private let entries: [MonarchLeaderboardEntry]
    private let label: String?
    private let valueLabel: String?
    private let sort: MonarchBarListSort
    private let format: MonarchMetricFormat
    private let isCompact: Bool
    private let changeComparison: String?
    private let changeGoodDirection: MonarchDeltaIndicatorGoodDirection
    private let showsAvatars: Bool
    private let showsBars: Bool
    private let highlightsTop: Bool
    private let limit: Int?
    private let total: Int?
    private let isLoading: Bool
    private let loadingRows: Int
    private let errorMessage: String?
    private let isRetrying: Bool
    private let labels: MonarchLeaderboardLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onShowMore: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchLeaderboardEntry) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var rankWidth = MonarchLeaderboardTokens.rankWidth

    /// A leaderboard.
    ///
    /// - Parameters:
    ///   - entries: The entries, best first, or in any order with `sort`.
    ///   - label: Names the ranking for VoiceOver, such as "Tickets solved this month".
    ///   - valueLabel: What the value counts, read after it, such as "open tickets".
    ///   - sort: `.descending` ranks the largest value first, `.ascending` the
    ///     smallest (response times), `.none` keeps the given order.
    ///   - format: How a value without `display` is written.
    ///   - isCompact: Whether values are written short, such as 1.2K.
    ///   - changeComparison: The comparison period read after a change, such as "vs last month".
    ///   - changeGoodDirection: Which direction of change is an improvement.
    ///   - showsAvatars: Whether each row shows an avatar.
    ///   - showsBars: Whether each row shows a bar relative to the largest value.
    ///   - highlightsTop: Whether the top three ranks get a filled marker.
    ///   - limit: Shows at most this many rows.
    ///   - total: How many entries exist, when `entries` is already a top N. Adds "N more".
    ///   - isLoading: Shows placeholder rows.
    ///   - loadingRows: How many placeholder rows.
    ///   - errorMessage: Replaces the rows with this failure, already localised.
    ///   - isRetrying: Whether the retry is running.
    ///   - labels: Replacements for the built-in strings.
    ///   - onShowMore: Makes "N more" a button that opens the full ranking.
    ///   - onSelect: Makes each row a button that opens the entry.
    ///   - onRetry: With `errorMessage`, adds a retry button that runs this.
    public init(
        _ entries: [MonarchLeaderboardEntry],
        label: String? = nil,
        valueLabel: String? = nil,
        sort: MonarchBarListSort = .none,
        format: MonarchMetricFormat = .number,
        isCompact: Bool = false,
        changeComparison: String? = nil,
        changeGoodDirection: MonarchDeltaIndicatorGoodDirection = .up,
        showsAvatars: Bool = true,
        showsBars: Bool = true,
        highlightsTop: Bool = false,
        limit: Int? = nil,
        total: Int? = nil,
        isLoading: Bool = false,
        loadingRows: Int = 5,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchLeaderboardLabels = MonarchLeaderboardLabels(),
        onShowMore: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchLeaderboardEntry) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.entries = entries
        self.label = label
        self.valueLabel = valueLabel
        self.sort = sort
        self.format = format
        self.isCompact = isCompact
        self.changeComparison = changeComparison
        self.changeGoodDirection = changeGoodDirection
        self.showsAvatars = showsAvatars
        self.showsBars = showsBars
        self.highlightsTop = highlightsTop
        self.limit = limit
        self.total = total
        self.isLoading = isLoading
        self.loadingRows = loadingRows
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.labels = labels
        self.onRetry = onRetry
        self.onShowMore = onShowMore
        self.onSelect = onSelect
    }

    private typealias Tokens = MonarchLeaderboardTokens

    /// The content of the view.
    public var body: some View {
        let ranked = Tokens.rank(entries, sort: sort)
        let shown = Tokens.shown(ranked, limit: limit)
        Group {
            switch Tokens.phase(isLoading: isLoading, errorMessage: errorMessage, shownCount: shown.count) {
            case .loading:
                placeholder
            case .failed:
                failure(errorMessage ?? "")
            case .empty:
                MonarchEmptyState(labels.empty ?? MonarchLeaderboardStrings.empty, size: .sm)
            case .ready:
                rows(shown, hidden: Tokens.hidden(shown: shown.count, entries: entries.count, total: total))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label ?? "")
    }

    // MARK: States

    private var placeholder: some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(0..<max(1, loadingRows), id: \.self) { index in
                row(
                    Tokens.Ranked(
                        entry: MonarchLeaderboardEntry(Tokens.placeholderName, value: 0, id: "\(index)"),
                        rank: index + 1
                    ),
                    share: 0,
                    valueText: Tokens.placeholderName
                )
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchLeaderboardStrings.loading)
    }

    @ViewBuilder private func failure(_ message: String) -> some View {
        if let onRetry {
            MonarchInlineErrorRetry(message, isRetrying: isRetrying, retryLabel: labels.retry, onRetry: onRetry)
        } else {
            Label {
                Text(message).monarchTextStyle(.body)
            } icon: {
                MonarchIcon(systemName: "exclamationmark.triangle", size: .sm)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityElement(children: .combine)
        }
    }

    // MARK: Rows

    private func rows(_ shown: [Tokens.Ranked], hidden: Int) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(shown) { ranked in
                let valueText = Tokens.valueText(ranked.entry, format: format, isCompact: isCompact, locale: locale)
                let spoken = Tokens.rowLabel(
                    ranked,
                    valueText: valueText,
                    valueLabel: valueLabel,
                    changeLabel: Tokens.changeLabel(
                        ranked.entry,
                        comparison: changeComparison,
                        goodDirection: changeGoodDirection,
                        locale: locale
                    ),
                    labels: labels
                )
                selectable(ranked.entry, label: spoken) {
                    row(ranked, share: Tokens.share(ranked.entry.value, among: shown), valueText: valueText)
                }
            }
            if hidden > 0 {
                more(Tokens.moreText(hidden, labels: labels))
            }
        }
    }

    @ViewBuilder private func selectable(
        _ entry: MonarchLeaderboardEntry,
        label: String,
        @ViewBuilder content: () -> some View
    ) -> some View {
        if let onSelect {
            Button {
                onSelect(entry)
            } label: {
                content()
                    .contentShape(Rectangle())
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityAddTraits(.isButton)
        } else {
            content()
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        }
    }

    private func row(_ ranked: Tokens.Ranked, share: Double, valueText: String) -> some View {
        let entry = ranked.entry
        let isStacked = dynamicTypeSize.isAccessibilitySize
        return HStack(alignment: .top, spacing: Tokens.columnSpacing) {
            rankMarker(ranked.rank)
            if showsAvatars && !isStacked {
                MonarchAvatar(
                    url: entry.avatarURL,
                    name: entry.name,
                    seed: entry.seed,
                    shape: entry.avatarShape,
                    size: .sm,
                    isDecorative: true
                )
            }
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                nameLine(entry)
                if let secondary = entry.secondary, !secondary.isEmpty {
                    Text(secondary)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.secondaryText])
                }
                if isStacked {
                    figures(entry, valueText: valueText, alignment: .leading)
                }
                if showsBars {
                    MonarchLeaderboardBar(fraction: share)
                        .accessibilityHidden(true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !isStacked {
                figures(entry, valueText: valueText, alignment: .trailing)
            }
        }
        .padding(Tokens.rowPadding)
        .background {
            if entry.isCurrent {
                MonarchShapes.container.fill(theme.colors[role: Tokens.currentFill])
                MonarchShapes.container.strokeBorder(
                    theme.colors[role: Tokens.currentBorder],
                    lineWidth: MonarchShapes.borderWidth
                )
            }
        }
    }

    private func rankMarker(_ rank: Int) -> some View {
        let marker = Tokens.rankMarker(rank: rank, highlightsTop: highlightsTop)
        return Text(rank, format: .number.locale(locale))
            .monarchTextStyle(.label)
            .monospacedDigit()
            .foregroundStyle(theme.colors[role: marker.text])
            .frame(minWidth: rankWidth, minHeight: rankWidth)
            .background {
                if let fill = marker.fill {
                    Circle().fill(theme.colors[role: fill])
                }
            }
    }

    @ViewBuilder private func nameLine(_ entry: MonarchLeaderboardEntry) -> some View {
        let badge = entry.badge.flatMap { $0.isEmpty ? nil : $0 }
        // As on the web, the name, "(you)" and the badge share one line, wrapping when it is narrow.
        MonarchFlowLayout(spacing: Tokens.lineSpacing) {
            Text(entry.name)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors[role: Tokens.primaryText])
                .fixedSize(horizontal: false, vertical: true)
            if entry.isCurrent {
                Text(labels.you ?? MonarchLeaderboardStrings.you)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryText])
            }
            if let badge {
                MonarchBadge(badge, tone: entry.badgeTone)
            }
        }
    }

    private func figures(
        _ entry: MonarchLeaderboardEntry,
        valueText: String,
        alignment: HorizontalAlignment
    ) -> some View {
        VStack(alignment: alignment, spacing: Tokens.lineSpacing) {
            Text(valueText)
                .monarchTextStyle(.bodyStrong)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: Tokens.primaryText])
            if let change = entry.change {
                MonarchDeltaIndicator(change, comparison: nil, goodDirection: changeGoodDirection, size: .sm)
            }
        }
        .fixedSize()
    }

    @ViewBuilder private func more(_ text: String) -> some View {
        if let onShowMore {
            Button(text, action: onShowMore)
                .buttonStyle(.monarchLink(.inline))
                .monarchTextStyle(.caption)
        } else {
            Text(text)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.secondaryText])
        }
    }
}

#if DEBUG
    #Preview("MonarchLeaderboard") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchLeaderboard(
                    [
                        MonarchLeaderboardEntry("Ana Lima", value: 42, change: 0.12, badge: "3 breached"),
                        MonarchLeaderboardEntry("Sam Ortiz", value: 42, secondary: "Tier 2", isCurrent: true),
                        MonarchLeaderboardEntry("Kai Chen", value: 17, change: -0.05),
                    ],
                    label: "Tickets solved",
                    valueLabel: "tickets solved",
                    sort: .descending,
                    highlightsTop: true,
                    total: 9
                )
                MonarchLeaderboard([], isLoading: true, loadingRows: 2)
                MonarchLeaderboard([])
                MonarchLeaderboard([], errorMessage: "Couldn’t load the ranking.", onRetry: {})
            }
        }
    }
#endif

/// The bar under a row: the first data colour on the muted track, as the web draws it.
private struct MonarchLeaderboardBar: View {
    let fraction: Double
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(theme.colors.bgMuted)
                Capsule()
                    .fill(theme.colors.data1)
                    .frame(width: proxy.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: 6)
        .accessibilityHidden(true)
    }
}
