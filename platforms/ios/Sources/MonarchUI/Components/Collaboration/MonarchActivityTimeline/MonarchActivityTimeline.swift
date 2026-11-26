import SwiftUI

/// A vertical feed of events (actor, verb, object, time and a marker)
/// grouped by day, with expandable details and Load more. For record
/// histories, audit trails in context and project activity.
///
/// ```swift
/// MonarchActivityTimeline(
///     [
///         MonarchActivityTimelineEvent(
///             id: "1",
///             actor: MonarchActivityTimelineActor("Ana Ruiz"),
///             verb: "moved",
///             object: "Acme renewal",
///             time: movedAt,
///             tags: ["Negotiation"],
///             details: "Stage: Proposal → Negotiation"
///         )
///     ],
///     marker: .avatar,
///     hasMore: true,
///     onLoadMore: { loadNextPage() }
/// )
/// ```
///
/// Days are headed Today, Yesterday or the date, in the environment's
/// calendar and time zone. Each event shows its time relative to `now`,
/// which defaults to the current time and is redrawn while the times are on screen.
/// Automation and system actors get an "Automated" tag, so people and rules
/// are told apart. Filters belong above the timeline; the app filters
/// `events`.
///
/// Accessibility: day headings are headers. Each event is one element read
/// as its sentence, its absolute time, the automated tag, the tags and the
/// body; the marker is decorative. Details are a standard disclosure group
/// after the event. Loading is one element read as "Loading activity".
public struct MonarchActivityTimeline: View {
    private let events: [MonarchActivityTimelineEvent]
    private let isGroupedByDay: Bool
    private let marker: MonarchActivityTimelineMarker
    private let density: MonarchActivityTimelineDensity
    private let expandedIDs: Binding<Set<String>>?
    private let isLoading: Bool
    private let isFailed: Bool
    private let hasMore: Bool
    private let isLoadingMore: Bool
    private let isLoadMoreFailed: Bool
    private let now: Date?
    private let labels: MonarchActivityTimelineLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onLoadMore: (@MainActor () -> Void)?

    @State private var localExpanded: Set<String> = []
    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.locale) private var locale

    /// An activity timeline.
    ///
    /// - Parameters:
    ///   - events: The events, newest first or oldest first; the order is kept.
    ///   - isGroupedByDay: Whether events are grouped under Today, Yesterday and dates.
    ///   - marker: A dot in the event's tone, or the actor's avatar.
    ///   - density: Regular, or compact for side panels.
    ///   - expandedIDs: The ids of events whose details are open. Kept inside when `nil`.
    ///   - isLoading: Whether the first page is loading. Shows placeholder rows.
    ///   - isFailed: Whether loading failed. Shows the error, with Try again when `onRetry` is given.
    ///   - hasMore: Whether another page exists. Shows Load more when `onLoadMore` is given.
    ///   - isLoadingMore: Whether the next page is loading.
    ///   - isLoadMoreFailed: Whether the next page failed to load.
    ///   - now: The reference time for relative times and day headings. The current time when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading after a failure.
    ///   - onLoadMore: Loads the next page.
    public init(
        _ events: [MonarchActivityTimelineEvent],
        isGroupedByDay: Bool = true,
        marker: MonarchActivityTimelineMarker = .dot,
        density: MonarchActivityTimelineDensity = .regular,
        expandedIDs: Binding<Set<String>>? = nil,
        isLoading: Bool = false,
        isFailed: Bool = false,
        hasMore: Bool = false,
        isLoadingMore: Bool = false,
        isLoadMoreFailed: Bool = false,
        now: Date? = nil,
        labels: MonarchActivityTimelineLabels = MonarchActivityTimelineLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onLoadMore: (@MainActor () -> Void)? = nil
    ) {
        self.events = events
        self.isGroupedByDay = isGroupedByDay
        self.marker = marker
        self.density = density
        self.expandedIDs = expandedIDs
        self.isLoading = isLoading
        self.isFailed = isFailed
        self.hasMore = hasMore
        self.isLoadingMore = isLoadingMore
        self.isLoadMoreFailed = isLoadMoreFailed
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onLoadMore = onLoadMore
    }

    private var zonedCalendar: Calendar {
        var zoned = calendar
        zoned.timeZone = timeZone
        return zoned
    }

    private var expanded: Binding<Set<String>> { expandedIDs ?? $localExpanded }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchActivityTimelineTokens.groupSpacing) {
            if isLoading {
                loadingRows
            } else if isFailed {
                errorView
            } else if events.isEmpty {
                MonarchEmptyState(
                    labels.empty ?? MonarchActivityTimelineStrings.empty,
                    systemImage: MonarchActivityTimelineTokens.emptySymbol,
                    size: .sm
                )
                .frame(maxWidth: .infinity)
            } else if let now {
                feed(now: now)
            } else {
                TimelineView(MonarchRelativeTimeSchedule(dates: events.map(\.time))) { context in
                    feed(now: context.date)
                }
            }
            if hasMore, !isLoading, !isFailed, let onLoadMore {
                MonarchLoadMore(
                    hasMore: true,
                    isLoading: isLoadingMore,
                    isFailed: isLoadMoreFailed,
                    showsEnd: false,
                    onLoadMore: onLoadMore
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private func feed(now: Date) -> some View {
        let calendar = zonedCalendar
        if isGroupedByDay {
            ForEach(MonarchActivityTimelineTokens.groups(events, calendar: calendar)) { group in
                VStack(alignment: .leading, spacing: MonarchActivityTimelineTokens.headingSpacing) {
                    Text(
                        MonarchActivityTimelineTokens.dayLabel(
                            group.day,
                            now: now,
                            calendar: calendar,
                            locale: locale,
                            labels: labels
                        )
                    )
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityAddTraits(.isHeader)
                    rows(group.events, now: now)
                }
                .accessibilityElement(children: .contain)
            }
        } else {
            rows(events, now: now)
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.list ?? MonarchActivityTimelineStrings.list)
        }
    }

    private func rows(_ events: [MonarchActivityTimelineEvent], now: Date) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(events.enumerated()), id: \.element.id) { index, event in
                MonarchActivityTimelineRow(
                    event: event,
                    now: now,
                    calendar: zonedCalendar,
                    marker: marker,
                    density: density,
                    isLast: index == events.count - 1,
                    labels: labels,
                    isExpanded: Binding(
                        get: { expanded.wrappedValue.contains(event.id) },
                        set: { isOpen in
                            expanded.wrappedValue = MonarchActivityTimelineTokens.toggled(
                                expanded.wrappedValue,
                                id: event.id,
                                isExpanded: isOpen
                            )
                        }
                    )
                )
            }
        }
    }

    private var loadingRows: some View {
        let placeholder = MonarchActivityTimelineEvent(
            id: "placeholder",
            actor: MonarchActivityTimelineActor("Placeholder name"),
            verb: "changed the status of",
            object: "a placeholder record",
            time: .distantPast
        )
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(0..<MonarchActivityTimelineTokens.loadingRowCount, id: \.self) { index in
                MonarchActivityTimelineRow(
                    event: placeholder,
                    now: .distantPast,
                    calendar: zonedCalendar,
                    marker: .dot,
                    density: density,
                    isLast: index == MonarchActivityTimelineTokens.loadingRowCount - 1,
                    labels: labels,
                    isExpanded: .constant(false)
                )
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchActivityTimelineStrings.loading)
        .accessibilityAddTraits(.updatesFrequently)
    }

    @ViewBuilder private var errorView: some View {
        let message = labels.error ?? MonarchActivityTimelineStrings.error
        if let onRetry {
            MonarchInlineErrorRetry(message, onRetry: onRetry)
        } else {
            HStack(alignment: .firstTextBaseline, spacing: MonarchActivityTimelineTokens.lineSpacing) {
                MonarchIcon(systemName: MonarchActivityTimelineTokens.errorSymbol, size: .sm)
                Text(message).monarchTextStyle(.body)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityElement(children: .combine)
        }
    }
}

#if DEBUG
    #Preview("MonarchActivityTimeline") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchActivityTimeline(
                    [
                        MonarchActivityTimelineEvent(
                            id: "1",
                            actor: MonarchActivityTimelineActor("Ana Ruiz"),
                            verb: "moved",
                            object: "Acme renewal",
                            time: now.addingTimeInterval(-3_600),
                            tone: .info,
                            tags: ["Negotiation"],
                            details: "Stage: Proposal → Negotiation"
                        ),
                        MonarchActivityTimelineEvent(
                            id: "2",
                            actor: MonarchActivityTimelineActor("Follow-up rule", kind: .automation),
                            verb: "sent a reminder to",
                            object: "Ben Okafor",
                            time: now.addingTimeInterval(-90_000),
                            systemImage: "bolt"
                        ),
                    ],
                    marker: .avatar,
                    now: now
                )
                MonarchActivityTimeline([], now: now)
                MonarchActivityTimeline([], isLoading: true)
            }
        }
    }
#endif
