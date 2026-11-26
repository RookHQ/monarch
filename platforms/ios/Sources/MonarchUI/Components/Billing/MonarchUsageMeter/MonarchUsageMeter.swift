import SwiftUI

/// Usage against limits: for each metered item, what was used, the limit and
/// a meter that changes near the limit and over it.
///
/// ```swift
/// MonarchUsageMeter(
///     "Usage",
///     items: [
///         MonarchUsageMeterItem("Requests", used: 8_200, limit: 10_000),
///         MonarchUsageMeterItem("Storage", used: 12, limit: 50, unit: "GB"),
///         MonarchUsageMeterItem("Seats", used: 4),
///     ],
///     period: "This month",
///     resetsAt: resetDate
/// )
/// ```
///
/// An item without a limit shows the count alone. Numbers and the reset date
/// are written in the locale and time zone of the environment.
///
/// Accessibility: the title is a heading. Each item is one element whose label
/// is the item's label and whose value is the figures, the level and the
/// percentage ("8,200 of 10,000, Near the limit, 82% used"). Near and over the
/// limit are written and have an icon, so they do not depend on colour. When
/// an item reaches the warning share or goes over its limit while the view is on screen, VoiceOver announces
/// that item once, for the whole meter.
public struct MonarchUsageMeter: View {
    private let title: String?
    private let items: [MonarchUsageMeterItem]
    private let period: String?
    private let resetsAt: Date?
    private let warningAt: Double
    private let isLoading: Bool
    private let error: String?
    private let isRetrying: Bool
    private let labels: MonarchUsageMeterLabels
    private let onRetry: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    /// A usage meter.
    ///
    /// - Parameters:
    ///   - title: The heading, already localised.
    ///   - items: The metered items, in display order.
    ///   - period: The period measured, such as "This month".
    ///   - resetsAt: When the quota resets.
    ///   - warningAt: The fraction of the limit at which an item is near the limit.
    ///   - isLoading: Whether the figures are being loaded. Shows placeholder rows, as many as `items`, or three, instead of the items.
    ///   - error: An error message, already localised. Shown instead of the items.
    ///   - isRetrying: Whether a retry is running, so the retry button waits.
    ///   - labels: Replaces the built-in strings.
    ///   - onRetry: Adds a retry button under the error.
    public init(
        _ title: String? = nil,
        items: [MonarchUsageMeterItem],
        period: String? = nil,
        resetsAt: Date? = nil,
        warningAt: Double = 0.8,
        isLoading: Bool = false,
        error: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchUsageMeterLabels = MonarchUsageMeterLabels(),
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.items = items
        self.period = period
        self.resetsAt = resetsAt
        self.warningAt = warningAt
        self.isLoading = isLoading
        self.error = error
        self.isRetrying = isRetrying
        self.labels = labels
        self.onRetry = onRetry
    }

    /// The content of the view.
    public var body: some View {
        let levels = items.map { MonarchUsageMeterTokens.level(used: $0.used, limit: $0.limit, warningAt: warningAt) }
        VStack(alignment: .leading, spacing: MonarchUsageMeterTokens.sectionSpacing) {
            header
            if let error {
                failure(error)
            } else if isLoading {
                loading
            } else {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    MonarchUsageMeterRow(item: item, level: levels[index], labels: labels)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .onChange(of: Dictionary(zip(items.map(\.id), levels), uniquingKeysWith: { first, _ in first })) { old, _ in
            guard error == nil, !isLoading else { return }
            // Only the items that just reached the warning share or went over the limit are read, once for the meter.
            let crossed = MonarchUsageMeterTokens.crossings(previous: old, items: items, levels: levels)
            let text = MonarchUsageMeterTokens.announcement(
                items: crossed.items,
                levels: crossed.levels,
                labels: labels
            )
            if !text.isEmpty {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    @ViewBuilder private var header: some View {
        let subtitle = MonarchUsageMeterTokens.subtitle(
            period: period,
            resetsAt: resetsAt,
            locale: locale,
            timeZone: timeZone,
            labels: labels
        )
        if title != nil || subtitle != nil {
            VStack(alignment: .leading, spacing: MonarchUsageMeterTokens.textSpacing) {
                if let title {
                    Text(title)
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                }
                if let subtitle {
                    Text(subtitle)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
    }

    /// Redacted rows in the shape of the real ones, read as one "Loading usage".
    private var loading: some View {
        let placeholders = MonarchUsageMeterTokens.placeholderItems(count: items.count)
        return VStack(alignment: .leading, spacing: MonarchUsageMeterTokens.sectionSpacing) {
            ForEach(placeholders) { item in
                MonarchUsageMeterRow(item: item, level: .normal, labels: labels)
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading)
    }

    @ViewBuilder private func failure(_ message: String) -> some View {
        if let onRetry {
            MonarchInlineErrorRetry(message, isRetrying: isRetrying, retryLabel: labels.retry, onRetry: onRetry)
        } else {
            MonarchInlineNotice(message, tone: .danger, variant: .plain)
        }
    }
}

#if DEBUG
    #Preview("MonarchUsageMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchUsageMeter(
                    "Usage",
                    items: [
                        MonarchUsageMeterItem("Requests", used: 4_200, limit: 10_000),
                        MonarchUsageMeterItem("Storage", used: 42, limit: 50, unit: "GB"),
                        MonarchUsageMeterItem(
                            "Minutes",
                            used: 130,
                            limit: 100,
                            description: "Extra minutes are billed."
                        ),
                        MonarchUsageMeterItem("Seats", used: 4),
                    ],
                    period: "This month",
                    resetsAt: Date(timeIntervalSince1970: 0)
                )
                MonarchUsageMeter("Usage", items: [], isLoading: true)
                MonarchUsageMeter("Usage", items: [], error: "Usage could not be loaded.") {}
            }
        }
    }
#endif
