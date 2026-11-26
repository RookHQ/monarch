import SwiftUI

/// How much AI the person has used against their quotas: one meter per
/// scope (this turn, this conversation, this month) in tokens or credits.
///
/// ```swift
/// MonarchAiUsageMeter(scopes: [
///     MonarchAiUsageScope("This turn", used: 1_850),
///     MonarchAiUsageScope("Conversation", used: 42_000, limit: 50_000),
///     MonarchAiUsageScope("This month", used: 920, limit: 1_000, unit: .credits),
/// ])
/// ```
///
/// A preset of ``MonarchUsageMeter``: it names the units, counts scopes by
/// position, and keeps the meter's warning near the limit and over it. A
/// scope without a limit shows the amount alone.
///
/// Accessibility: a group named by `title`, or by "AI usage" when there is
/// no title. Each scope is one element read as its label with the figures,
/// the level and the percentage as its value. Near and over the limit are
/// written with an icon, not shown by colour alone, and announced when a
/// scope reaches them while the view is on screen.
public struct MonarchAiUsageMeter: View {
    private let title: String?
    private let scopes: [MonarchAiUsageScope]
    private let period: String?
    private let resetsAt: Date?
    private let warningAt: Double
    private let isLoading: Bool
    private let error: String?
    private let labels: MonarchAiUsageMeterLabels
    private let onRetry: (@MainActor () -> Void)?

    /// An AI usage meter.
    ///
    /// - Parameters:
    ///   - title: A visible heading, already localised.
    ///   - scopes: The quotas, in display order.
    ///   - period: The period measured, such as "November".
    ///   - resetsAt: When the quotas reset.
    ///   - warningAt: The share of a limit, 0 to 1, at which a scope is near the limit.
    ///   - isLoading: Whether the figures are being loaded.
    ///   - error: An error message, already localised, shown instead of the scopes.
    ///   - labels: Replaces the built-in strings.
    ///   - onRetry: Adds a retry button under the error.
    public init(
        _ title: String? = nil,
        scopes: [MonarchAiUsageScope],
        period: String? = nil,
        resetsAt: Date? = nil,
        warningAt: Double = 0.8,
        isLoading: Bool = false,
        error: String? = nil,
        labels: MonarchAiUsageMeterLabels = MonarchAiUsageMeterLabels(),
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.scopes = scopes
        self.period = period
        self.resetsAt = resetsAt
        self.warningAt = warningAt
        self.isLoading = isLoading
        self.error = error
        self.labels = labels
        self.onRetry = onRetry
    }

    /// The usage meter's strings: the web's status line ("This month: 87% of the limit used", nothing below the
    /// warning level) unless the app replaced it.
    private var meterLabels: MonarchUsageMeterLabels {
        var meter = labels.meter
        if meter.statusLine == nil {
            meter.statusLine = { label, isNearOrOver, isOver, percent in
                guard isNearOrOver else { return "" }
                return MonarchAiUsageMeterStrings.limitUsed(label: label, percent: percent, isOver: isOver)
            }
        }
        return meter
    }

    /// The content of the view.
    public var body: some View {
        MonarchUsageMeter(
            title,
            items: MonarchAiUsageMeterTokens.items(scopes: scopes, labels: labels),
            period: period,
            resetsAt: resetsAt,
            warningAt: warningAt,
            isLoading: isLoading,
            error: error,
            labels: meterLabels,
            onRetry: onRetry
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title ?? labels.label)
    }
}

#if DEBUG
    #Preview("MonarchAiUsageMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiUsageMeter(
                    scopes: [
                        MonarchAiUsageScope("This turn", used: 1_850),
                        MonarchAiUsageScope("Conversation", used: 42_000, limit: 50_000),
                        MonarchAiUsageScope("This month", used: 1_000, limit: 1_000, unit: .credits),
                    ]
                )
                MonarchAiUsageMeter("AI usage", scopes: [], isLoading: true)
            }
        }
    }
#endif
