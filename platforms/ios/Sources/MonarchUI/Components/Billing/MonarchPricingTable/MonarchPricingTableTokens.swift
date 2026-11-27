import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPricingTable``.
enum MonarchPricingTableTokens {
    /// The gap between the pickers and the plans.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between grid columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between grid rows and between features.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between lines inside a header cell or a feature.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between a feature's mark and its text.
    static let markSpacing: CGFloat = MonarchTokens.Space.space2
    /// The inset of a page, so the plan card's border and shadow are not clipped.
    static let pageInset: CGFloat = MonarchTokens.Space.space1
    /// Padding inside a grid header cell.
    static let headerPadding: CGFloat = MonarchTokens.Space.space2
    /// The least width of the feature column in the grid. Wide enough to read; on a phone the table scrolls sideways, as one table, like the web.
    static let featureColumnMinWidth: CGFloat = MonarchTokens.Space.space24
    /// The least width of a plan column in the grid.
    static let planColumnMinWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space4
    /// The least widths when the whole grid has to fit a phone column instead of scrolling.
    static let compactFeatureColumnMinWidth: CGFloat = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2)
    static let compactPlanColumnMinWidth: CGFloat = MonarchTokens.Space.space24
    /// The width of the border of a highlighted grid header. Wider than a
    /// hairline, so the emphasis is not colour alone.
    static let highlightBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of an included feature.
    static let includedSymbol = "checkmark"
    /// The SF Symbol of a feature that is not included.
    static let notIncludedSymbol = "minus"

    /// One group of rows: its heading, or `nil` for rows without a group.
    struct Group: Equatable {
        let title: String?
        let features: [MonarchPricingFeature]
    }

    /// The price a plan shows for a period: the period's price, else the fallback.
    static func price(_ plan: MonarchPricingPlan, period: String?) -> String? {
        if let period, let price = plan.prices[period] { return price }
        return plan.price
    }

    /// Whether the plan's price comes from the period, so the period's
    /// suffix and note apply. A fallback such as "Contact us" gets neither.
    static func isPeriodic(_ plan: MonarchPricingPlan, period: String?) -> Bool {
        guard let period else { return false }
        return plan.prices[period] != nil
    }

    /// The text after the price: the plan's own suffix, else the period's
    /// when the price comes from the period.
    static func priceSuffix(_ plan: MonarchPricingPlan, period: MonarchPricingPeriod?) -> String? {
        if let suffix = plan.priceSuffix { return suffix }
        guard let period, isPeriodic(plan, period: period.id) else { return nil }
        return period.priceSuffix
    }

    /// The line under the price: the period's note, only for a period price.
    static func priceNote(_ plan: MonarchPricingPlan, period: MonarchPricingPeriod?) -> String? {
        guard let period, isPeriodic(plan, period: period.id) else { return nil }
        return period.priceNote
    }

    /// The feature rows grouped by `group`, keeping first-seen order.
    static func groups(_ features: [MonarchPricingFeature]) -> [Group] {
        var order: [String?] = []
        var rows: [String?: [MonarchPricingFeature]] = [:]
        for feature in features {
            if rows[feature.group] == nil { order.append(feature.group) }
            rows[feature.group, default: []].append(feature)
        }
        return order.map { Group(title: $0, features: rows[$0] ?? []) }
    }

    /// A feature's value for a plan; a missing plan is not included.
    static func value(_ feature: MonarchPricingFeature, plan: String) -> MonarchPricingFeatureValue {
        feature.values[plan] ?? .notIncluded
    }

    /// The layout to draw. Automatic is the grid only at regular width and
    /// below the accessibility text sizes.
    static func resolvedLayout(
        _ layout: MonarchPricingTableLayout,
        isRegularWidth: Bool,
        isAccessibilitySize: Bool
    ) -> MonarchPricingTableLayout {
        guard layout == .automatic else { return layout }
        return isRegularWidth && !isAccessibilitySize ? .grid : .paged
    }

    /// Whether the billing period picker is shown: with two or more periods,
    /// unless the app shows its own.
    static func showsPeriodPicker(periods: Int, isShown: Bool) -> Bool {
        isShown && periods > 1
    }

    /// The period that is selected: the app's, the remembered one, or the first.
    static func selectedPeriod(bound: String?, stored: String?, periods: [MonarchPricingPeriod]) -> String? {
        bound ?? stored ?? periods.first?.id
    }

    /// The words for a value.
    static func valueText(_ value: MonarchPricingFeatureValue, labels: MonarchPricingTableLabels) -> String {
        switch value {
        case .included: labels.included ?? MonarchPricingTableStrings.included
        case .notIncluded: labels.notIncluded ?? MonarchPricingTableStrings.notIncluded
        case .text(let text): text
        }
    }

    /// What VoiceOver reads for a grid cell: the plan, the feature and the value.
    static func cellAccessibilityLabel(
        plan: String,
        feature: String,
        value: MonarchPricingFeatureValue,
        labels: MonarchPricingTableLabels
    ) -> String {
        "\(plan), \(feature): \(valueText(value, labels: labels))"
    }

    /// What VoiceOver reads for a feature on a plan's page: the feature,
    /// the value and the description.
    static func featureAccessibilityLabel(
        _ feature: MonarchPricingFeature,
        value: MonarchPricingFeatureValue,
        labels: MonarchPricingTableLabels
    ) -> String {
        var parts = ["\(feature.label): \(valueText(value, labels: labels))"]
        if let description = feature.description, !description.isEmpty { parts.append(description) }
        return parts.joined(separator: ", ")
    }

    /// The mark of a value, or `nil` for text.
    static func symbol(_ value: MonarchPricingFeatureValue) -> String? {
        switch value {
        case .included: includedSymbol
        case .notIncluded: notIncludedSymbol
        case .text: nil
        }
    }

    /// The colour of a value's mark.
    static func markRole(_ value: MonarchPricingFeatureValue) -> MonarchColorRole {
        value == .notIncluded ? \.textSubtle : \.accentStrong
    }

    /// The colour of a feature's name on a plan's page: muted when not included.
    static func labelRole(_ value: MonarchPricingFeatureValue) -> MonarchColorRole {
        value == .notIncluded ? \.textMuted : \.textDefault
    }
}
