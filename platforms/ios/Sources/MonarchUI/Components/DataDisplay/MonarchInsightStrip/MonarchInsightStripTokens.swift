import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchInsightStrip``.
enum MonarchInsightStripTokens {
    /// The gap between the title, the list and the toggle.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between two cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between an icon and the text after it.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The part of the width one card takes in the scrolling row, so the
    /// next card shows at the edge.
    static let scrollCardWidth: CGFloat = 0.86
    /// The least width of a column in the grid.
    static let gridMinimumWidth: CGFloat = MonarchTokens.Breakpoint.sm / 2
    /// The number of placeholder cards while loading.
    static let placeholderCount = 3
    /// The SF Symbol of the empty state.
    static let emptySymbol = "checkmark.circle.fill"

    /// The origin in the form ``MonarchAiInsightCallout`` takes.
    static func calloutOrigin(_ origin: MonarchInsightOrigin) -> MonarchAiInsightOrigin {
        switch origin {
        case .rule: .rule
        case .ai: .ai
        }
    }

    /// The strip's replacements, in the form ``MonarchAiInsightCallout`` takes.
    static func calloutLabels(_ labels: MonarchInsightStripLabels) -> MonarchAiInsightCalloutLabels {
        MonarchAiInsightCalloutLabels(
            ai: labels.aiOrigin,
            rule: labels.ruleOrigin,
            evidence: labels.evidence,
            dismiss: labels.dismiss
        )
    }

    /// The place of a tone in the order: the most severe first.
    static func rank(_ tone: MonarchTone) -> Int {
        switch tone {
        case .danger: 0
        case .warning: 1
        case .info: 2
        case .success: 3
        case .ai: 4
        case .accent: 5
        case .neutral: 6
        }
    }

    /// The insights with the most severe first, keeping the given order
    /// within one tone.
    static func sorted(_ insights: [MonarchInsight]) -> [MonarchInsight] {
        insights.enumerated()
            .sorted { first, second in
                let ranks = (rank(first.element.tone), rank(second.element.tone))
                return ranks.0 == ranks.1 ? first.offset < second.offset : ranks.0 < ranks.1
            }
            .map(\.element)
    }

    /// Whether a limit hides some insights, so the toggle is offered.
    static func canCollapse(count: Int, limit: Int?) -> Bool {
        guard let limit, limit > 0 else { return false }
        return count > limit
    }

    /// How many insights are shown.
    static func shownCount(count: Int, limit: Int?, isExpanded: Bool) -> Int {
        canCollapse(count: count, limit: limit) && !isExpanded ? limit ?? count : count
    }

    /// The title of the toggle.
    static func toggleTitle(hidden: Int, isExpanded: Bool, labels: MonarchInsightStripLabels) -> String {
        if isExpanded {
            labels.showLess ?? MonarchInsightStripStrings.showLess
        } else {
            labels.showMore?(hidden) ?? MonarchInsightStripStrings.showMore(hidden)
        }
    }
}
