import SwiftUI

/// A model-written summary of a record or a page on a card: the summary as
/// Markdown, when it was written, its sources and a Refresh action, with
/// generating, stale and error states, always marked as an AI summary.
///
/// ```swift
/// MonarchAiSummaryCard(
///     "Account summary",
///     summary: account.summary,
///     status: account.isStale ? .stale : .ready,
///     generatedAt: account.summarizedAt,
///     sources: account.summarySources,
///     onRefresh: { Task { await account.summarize() } }
/// )
/// ```
///
/// A product pattern, experimental until a Rook app adopts it, built only
/// from Monarch parts: ``MonarchCard``, ``MonarchAiGeneratedBadge`` with the
/// label "AI summary", ``MonarchAiMarkdown`` for the untrusted summary,
/// ``MonarchBanner`` for the stale and error notices, redacted text for the
/// placeholder, ``MonarchAiSourceList`` for the sources and ``MonarchButton``
/// for Refresh. The age is relative to `now` (the current time by default)
/// and is not updated live: render the card again with a new `now`, for
/// example from a `TimelineView`.
///
/// Accessibility: the title is a header after the "AI summary" label. While
/// a summary is being written Refresh shows a spinner, and the start and the
/// end are announced ("Generating summary…", "Summary updated."); nothing is
/// announced when the card first appears. The age is read as the full date
/// and time. The placeholder lines are hidden from VoiceOver.
@_spi(Experimental) public struct MonarchAiSummaryCard<Actions: View>: View {
    private let title: String
    private let summary: String
    private let status: MonarchAiSummaryCardStatus
    private let isRefreshing: Bool
    private let generatedAt: Date?
    private let now: Date?
    private let sources: [MonarchAiSourcePart]
    private let errorMessage: String?
    private let labels: MonarchAiSummaryCardLabels
    private let onRefresh: (@MainActor () -> Void)?
    private let onOpenSource: (@MainActor (MonarchAiSourcePart) -> Void)?
    private let onOpenLink: (@MainActor (URL) -> Void)?
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    private typealias Tokens = MonarchAiSummaryCardTokens

    /// A summary card with extra actions in the footer.
    ///
    /// - Parameters:
    ///   - title: What is summarised, for example "Account summary". A header.
    ///   - summary: The summary as Markdown, treated as untrusted model output.
    ///     Empty while there is none.
    ///   - status: Ready, generating, stale or error.
    ///   - isRefreshing: Whether a refresh is running: Refresh shows a spinner.
    ///   - generatedAt: When the summary was written, shown as "Generated 3 hours ago".
    ///   - now: The reference time for the age. The current time when `nil`.
    ///   - sources: The documents or records the summary was written from.
    ///   - errorMessage: A detail shown under the error title when `status` is `error`.
    ///   - labels: Replaces the built-in strings.
    ///   - onRefresh: Adds Refresh, which asks for a new summary.
    ///   - onOpenSource: Opens a source, for example in a preview sheet.
    ///     Without it a source with a safe URL opens through the system.
    ///   - onOpenLink: Opens a link in the summary in the app.
    ///   - actions: More buttons in the footer, after the age.
    public init(
        _ title: String,
        summary: String,
        status: MonarchAiSummaryCardStatus = .ready,
        isRefreshing: Bool = false,
        generatedAt: Date? = nil,
        now: Date? = nil,
        sources: [MonarchAiSourcePart] = [],
        errorMessage: String? = nil,
        labels: MonarchAiSummaryCardLabels = MonarchAiSummaryCardLabels(),
        onRefresh: (@MainActor () -> Void)? = nil,
        onOpenSource: (@MainActor (MonarchAiSourcePart) -> Void)? = nil,
        onOpenLink: (@MainActor (URL) -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.summary = summary
        self.status = status
        self.isRefreshing = isRefreshing
        self.generatedAt = generatedAt
        self.now = now
        self.sources = sources
        self.errorMessage = errorMessage
        self.labels = labels
        self.onRefresh = onRefresh
        self.onOpenSource = onOpenSource
        self.onOpenLink = onOpenLink
        self.actions = actions()
    }

    private var openSource: (@MainActor (MonarchAiSourceListItem) -> Void)? {
        guard let onOpenSource else { return nil }
        return { item in onOpenSource(item.source) }
    }

    private var isBusy: Bool {
        Tokens.isBusy(status: status, isRefreshing: isRefreshing)
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.gap) {
                header
                notice
                summaryBody
                if !sources.isEmpty {
                    MonarchAiSourceList(
                        sources.map { MonarchAiSourceListItem($0) },
                        labels: MonarchAiSourceListLabels(heading: labels.sources),
                        onOpen: openSource
                    )
                }
                footer
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .onChange(of: isBusy) { wasBusy, isBusy in
            if let text = Tokens.announcement(wasBusy: wasBusy, isBusy: isBusy, status: status, labels: labels) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .top, spacing: Tokens.headerGap) {
                titleBlock
                refreshButton
            }
            VStack(alignment: .leading, spacing: Tokens.headerGap) {
                titleBlock
                refreshButton
            }
        }
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: Tokens.titleGap) {
            MonarchAiGeneratedBadge(size: .sm, label: labels.aiSummary)
            Text(title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: Tokens.titleRole])
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var refreshButton: some View {
        if let onRefresh {
            MonarchButton(
                labels.refresh,
                systemImage: Tokens.refreshSymbol,
                variant: .ghost,
                size: .sm,
                isLoading: isBusy,
                loadingLabel: labels.generating,
                action: onRefresh
            )
            .fixedSize()
        }
    }

    @ViewBuilder private var notice: some View {
        switch status {
        case .stale:
            MonarchBanner(message: labels.stale, tone: .warning)
        case .error:
            let banner = Tokens.errorBanner(detail: errorMessage, labels: labels)
            MonarchBanner(banner.title, message: banner.message, tone: .danger)
        case .ready, .generating:
            EmptyView()
        }
    }

    @ViewBuilder private var summaryBody: some View {
        switch Tokens.body(summary: summary, isBusy: isBusy, status: status) {
        case .summary:
            MonarchAiMarkdown(summary, onOpenLink: onOpenLink)
        case .placeholder:
            VStack(alignment: .leading, spacing: Tokens.headerGap) {
                Text(labels.generating)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                Text(Tokens.placeholderText(labels.generating))
                    .monarchTextStyle(.body)
                    .lineLimit(Tokens.placeholderLines)
                    .redacted(reason: .placeholder)
                    .accessibilityHidden(true)
            }
        case .nothing:
            EmptyView()
        case .empty:
            Text(labels.empty)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.mutedRole])
        }
    }

    @ViewBuilder private var footer: some View {
        let hasActions = Actions.self != EmptyView.self
        if generatedAt != nil || hasActions {
            MonarchFlowLayout(spacing: Tokens.headerGap, rowSpacing: Tokens.titleGap) {
                if let generatedAt {
                    Text(Tokens.generatedLine(generatedAt, now: now ?? Date(), locale: locale, labels: labels))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                        .accessibilityLabel(
                            Tokens.generatedSpoken(generatedAt, locale: locale, timeZone: timeZone, labels: labels)
                        )
                }
                actions
            }
        }
    }
}

extension MonarchAiSummaryCard where Actions == EmptyView {
    /// A summary card.
    ///
    /// - Parameters:
    ///   - title: What is summarised, for example "Account summary". A header.
    ///   - summary: The summary as Markdown, treated as untrusted model output.
    ///     Empty while there is none.
    ///   - status: Ready, generating, stale or error.
    ///   - isRefreshing: Whether a refresh is running: Refresh shows a spinner.
    ///   - generatedAt: When the summary was written, shown as "Generated 3 hours ago".
    ///   - now: The reference time for the age. The current time when `nil`.
    ///   - sources: The documents or records the summary was written from.
    ///   - errorMessage: A detail shown under the error title when `status` is `error`.
    ///   - labels: Replaces the built-in strings.
    ///   - onRefresh: Adds Refresh, which asks for a new summary.
    ///   - onOpenSource: Opens a source, for example in a preview sheet.
    ///   - onOpenLink: Opens a link in the summary in the app.
    public init(
        _ title: String,
        summary: String,
        status: MonarchAiSummaryCardStatus = .ready,
        isRefreshing: Bool = false,
        generatedAt: Date? = nil,
        now: Date? = nil,
        sources: [MonarchAiSourcePart] = [],
        errorMessage: String? = nil,
        labels: MonarchAiSummaryCardLabels = MonarchAiSummaryCardLabels(),
        onRefresh: (@MainActor () -> Void)? = nil,
        onOpenSource: (@MainActor (MonarchAiSourcePart) -> Void)? = nil,
        onOpenLink: (@MainActor (URL) -> Void)? = nil
    ) {
        self.init(
            title,
            summary: summary,
            status: status,
            isRefreshing: isRefreshing,
            generatedAt: generatedAt,
            now: now,
            sources: sources,
            errorMessage: errorMessage,
            labels: labels,
            onRefresh: onRefresh,
            onOpenSource: onOpenSource,
            onOpenLink: onOpenLink,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiSummaryCard") {
        let now = Date()
        MonarchPreviewMatrix {
            MonarchAiSummaryCard(
                "Account summary",
                summary:
                    "Renewal is **in 30 days**. Usage grew 12% this quarter.\n\n- Two open tickets\n- One overdue invoice",
                generatedAt: now.addingTimeInterval(-3 * 3_600),
                now: now,
                sources: [
                    MonarchAiSourcePart(id: "s1", title: "Q3 usage report", url: "https://example.com/q3", kind: .doc)
                ],
                onRefresh: {}
            )
            MonarchAiSummaryCard("Deal summary", summary: "", status: .generating, onRefresh: {})
            MonarchAiSummaryCard("Ticket summary", summary: "", status: .error, errorMessage: "The model timed out.")
        }
    }
#endif
