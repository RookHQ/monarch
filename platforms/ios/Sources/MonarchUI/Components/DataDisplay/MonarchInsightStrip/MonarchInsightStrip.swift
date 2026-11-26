import SwiftUI

/// A row of insights for the top of a dashboard. Every insight says whether
/// a rule or a model raised it.
///
/// ```swift
/// MonarchInsightStrip(
///     "Needs attention",
///     insights: [
///         MonarchInsight(
///             id: "renewals",
///             title: "3 domains expire in 30 days",
///             why: "Auto-renew is off for all three.",
///             tone: .warning,
///             origin: .rule
///         )
///     ]
/// )
/// ```
///
/// Insights are ordered most severe first. With `limit`, the rest are behind
/// a "Show more" button. With `onDismiss`, every insight has a dismiss
/// button and the app removes the insight from the list.
///
/// Each insight is a ``MonarchAiInsightCallout``.
///
/// Accessibility: the title is a heading. Each insight's title is read with
/// the tone name first and the origin label after it, then the reason, so
/// neither the tone nor the origin depends on colour. The evidence section,
/// the action and the dismiss button are separate elements with 44-point
/// touch targets.
/// The loading placeholder is one element named "Loading insights".
public struct MonarchInsightStrip: View {
    private let title: String?
    private let insights: [MonarchInsight]
    private let layout: MonarchInsightStripLayout
    private let isLoading: Bool
    private let error: String?
    private let labels: MonarchInsightStripLabels
    private let limit: Int?
    private let sortsBySeverity: Bool
    private let onRetry: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor (String) -> Void)?

    @State private var isExpanded = false
    @Environment(\.monarchTheme) private var theme

    /// An insight strip.
    ///
    /// - Parameters:
    ///   - title: A heading for the strip, already localised.
    ///   - insights: The insights. Rule-based and model insights can be
    ///     mixed; each shows its origin.
    ///   - layout: How the insights are arranged.
    ///   - isLoading: Whether placeholder cards are shown instead of the
    ///     insights.
    ///   - error: A failure message, which replaces the insights.
    ///   - labels: Replacements for the built-in strings.
    ///   - limit: Show at most this many, with a toggle for the rest.
    ///   - sortsBySeverity: Whether the most severe insights come first.
    ///   - onRetry: With `error`, adds a "Try again" button that calls this.
    ///   - onDismiss: Adds a dismiss button to every insight. Receives the
    ///     id of the insight; the app removes it.
    public init(
        _ title: String? = nil,
        insights: [MonarchInsight],
        layout: MonarchInsightStripLayout = .scroll,
        isLoading: Bool = false,
        error: String? = nil,
        labels: MonarchInsightStripLabels = MonarchInsightStripLabels(),
        limit: Int? = nil,
        sortsBySeverity: Bool = true,
        onRetry: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor (String) -> Void)? = nil
    ) {
        self.title = title
        self.insights = insights
        self.layout = layout
        self.isLoading = isLoading
        self.error = error
        self.labels = labels
        self.limit = limit
        self.sortsBySeverity = sortsBySeverity
        self.onRetry = onRetry
        self.onDismiss = onDismiss
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchInsightStripTokens.sectionSpacing) {
            if let title, !title.isEmpty {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
            }
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var content: some View {
        if isLoading {
            arranged(Array(0..<MonarchInsightStripTokens.placeholderCount), id: \.self) { _ in
                placeholder
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(labels.loading ?? MonarchInsightStripStrings.loading)
        } else if let error, !error.isEmpty {
            failure(error)
        } else if insights.isEmpty {
            HStack(alignment: .firstTextBaseline, spacing: MonarchInsightStripTokens.iconSpacing) {
                MonarchIcon(systemName: MonarchInsightStripTokens.emptySymbol, size: .sm)
                    .foregroundStyle(theme.colors.successStrong)
                Text(labels.empty ?? MonarchInsightStripStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
        } else {
            list
        }
    }

    @ViewBuilder private var list: some View {
        let ordered = sortsBySeverity ? MonarchInsightStripTokens.sorted(insights) : insights
        let shownCount = MonarchInsightStripTokens.shownCount(
            count: ordered.count,
            limit: limit,
            isExpanded: isExpanded
        )
        arranged(Array(ordered.prefix(shownCount)), id: \.id) { insight in
            callout(for: insight)
        }
        if MonarchInsightStripTokens.canCollapse(count: ordered.count, limit: limit) {
            Button(
                MonarchInsightStripTokens.toggleTitle(
                    hidden: ordered.count - shownCount,
                    isExpanded: isExpanded,
                    labels: labels
                )
            ) {
                isExpanded.toggle()
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
        }
    }

    /// One insight, as a ``MonarchAiInsightCallout`` with its origin, evidence and action.
    @ViewBuilder private func callout(for insight: MonarchInsight) -> some View {
        let calloutLabels = MonarchInsightStripTokens.calloutLabels(labels)
        let origin = MonarchInsightStripTokens.calloutOrigin(insight.origin)
        if let evidence = insight.evidence, !evidence.isEmpty {
            MonarchAiInsightCallout(
                insight.title,
                why: insight.why,
                origin: origin,
                tone: insight.tone,
                actionTitle: insight.actionTitle,
                labels: calloutLabels,
                action: insight.action,
                onDismiss: dismissAction(for: insight.id)
            ) {
                Text(evidence)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        } else {
            MonarchAiInsightCallout(
                insight.title,
                why: insight.why,
                origin: origin,
                tone: insight.tone,
                actionTitle: insight.actionTitle,
                labels: calloutLabels,
                action: insight.action,
                onDismiss: dismissAction(for: insight.id)
            )
        }
    }

    /// The dismiss action of one insight, or `nil` when insights cannot be
    /// dismissed.
    private func dismissAction(for id: String) -> (@MainActor () -> Void)? {
        guard let onDismiss else { return nil }
        return { onDismiss(id) }
    }

    /// Arranges cards in the chosen layout.
    @ViewBuilder
    private func arranged<Item, ID: Hashable, Card: View>(
        _ items: [Item],
        id: KeyPath<Item, ID>,
        @ViewBuilder card: @escaping (Item) -> Card
    ) -> some View {
        let spacing = MonarchInsightStripTokens.cardSpacing
        switch layout {
        case .scroll where items.count == 1:
            // One card has nothing to scroll to: it takes the full width.
            VStack(alignment: .leading, spacing: spacing) {
                ForEach(items, id: id) { item in card(item) }
            }
        case .scroll:
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: spacing) {
                    ForEach(items, id: id) { item in
                        card(item).containerRelativeFrame(.horizontal) { width, _ in
                            width * MonarchInsightStripTokens.scrollCardWidth
                        }
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
            .scrollIndicators(.hidden)
            .scrollClipDisabled()
            .monarchTrailingFade()
        case .stack:
            VStack(alignment: .leading, spacing: spacing) {
                ForEach(items, id: id) { item in card(item) }
            }
        case .grid:
            LazyVGrid(
                columns: [
                    GridItem(
                        .adaptive(minimum: MonarchInsightStripTokens.gridMinimumWidth),
                        spacing: spacing,
                        alignment: .top
                    )
                ],
                alignment: .leading,
                spacing: spacing
            ) {
                ForEach(items, id: id) { item in card(item) }
            }
        }
    }

    /// A card in the shape of an insight, drawn as a placeholder.
    private var placeholder: some View {
        MonarchCard {
            Text(labels.loading ?? MonarchInsightStripStrings.loading).monarchTextStyle(.bodyStrong)
            Text(labels.empty ?? MonarchInsightStripStrings.empty).monarchTextStyle(.body)
        }
        .redacted(reason: .placeholder)
    }

    @ViewBuilder private func failure(_ message: String) -> some View {
        if let onRetry {
            MonarchInlineErrorRetry(message, retryLabel: labels.retry, onRetry: onRetry)
        } else {
            MonarchInlineNotice(message, tone: .danger, variant: .plain)
        }
    }
}

#if DEBUG
    #Preview("MonarchInsightStrip") {
        let insights = [
            MonarchInsight(
                id: "renewals",
                title: "3 domains expire in 30 days",
                why: "Auto-renew is off for all three.",
                tone: .warning,
                origin: .rule,
                evidence: "example.com, example.org, example.net",
                actionTitle: "Review renewals"
            ) {},
            MonarchInsight(
                id: "churn",
                title: "Renewal at risk",
                why: "Usage fell by half in the last month.",
                tone: .danger,
                origin: .ai
            ),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchInsightStrip("Needs attention", insights: insights, onDismiss: { _ in })
                MonarchInsightStrip(insights: insights, layout: .stack, limit: 1)
                MonarchInsightStrip(insights: [], isLoading: true)
                MonarchInsightStrip(insights: [], error: "The insights could not be loaded.", onRetry: {})
                MonarchInsightStrip(insights: [])
            }
        }
    }
#endif
