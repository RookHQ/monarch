import SwiftUI

/// The grid layout: plans as columns, features as rows.
struct MonarchPricingTableGrid<Action: View>: View {
    let plans: [MonarchPricingPlan]
    let features: [MonarchPricingFeature]
    let period: MonarchPricingPeriod?
    let isPriceShown: Bool
    let labels: MonarchPricingTableLabels
    let action: (MonarchPricingPlan) -> Action
    /// Narrower columns, so that the whole table fits a phone column.
    var isCompact = false

    @Environment(\.monarchTheme) private var theme

    private var featureMinWidth: CGFloat {
        isCompact
            ? MonarchPricingTableTokens.compactFeatureColumnMinWidth : MonarchPricingTableTokens.featureColumnMinWidth
    }

    private var planMinWidth: CGFloat {
        isCompact ? MonarchPricingTableTokens.compactPlanColumnMinWidth : MonarchPricingTableTokens.planColumnMinWidth
    }

    var body: some View {
        Grid(
            alignment: .topLeading,
            horizontalSpacing: 0,
            verticalSpacing: 0
        ) {
            GridRow {
                Color.clear.gridCellUnsizedAxes([.horizontal, .vertical])
                ForEach(plans) { plan in
                    header(plan)
                }
            }
            ForEach(Array(MonarchPricingTableTokens.groups(features).enumerated()), id: \.offset) { _, group in
                if let title = group.title {
                    MonarchDivider().gridCellUnsizedAxes(.horizontal)
                    // As on the web, a group is a muted band across the whole table.
                    Text(title)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(.horizontal, MonarchPricingTableTokens.headerPadding)
                        .padding(.vertical, MonarchPricingTableTokens.rowSpacing)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.colors[role: \.bgSubtle])
                        .gridCellColumns(plans.count + 1)
                        .accessibilityAddTraits(.isHeader)
                }
                ForEach(group.features) { feature in
                    MonarchDivider().gridCellUnsizedAxes(.horizontal)
                    GridRow(alignment: .firstTextBaseline) {
                        featureLabel(feature)
                        ForEach(plans) { plan in
                            let value = MonarchPricingTableTokens.value(feature, plan: plan.id)
                            MonarchPricingValueMark(value: value)
                                .padding(.horizontal, MonarchPricingTableTokens.headerPadding)
                                .padding(.vertical, MonarchPricingTableTokens.rowSpacing)
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                                .background(plan.isHighlighted ? theme.colors[role: \.accentSubtle] : Color.clear)
                                .accessibilityElement(children: .ignore)
                                .accessibilityLabel(
                                    MonarchPricingTableTokens.cellAccessibilityLabel(
                                        plan: plan.name,
                                        feature: feature.label,
                                        value: value,
                                        labels: labels
                                    )
                                )
                        }
                    }
                }
            }
        }
        // Rows take the height of their tallest cell, so a highlighted column's tint runs unbroken.
        .fixedSize(horizontal: false, vertical: true)
    }

    private func featureLabel(_ feature: MonarchPricingFeature) -> some View {
        VStack(alignment: .leading, spacing: MonarchPricingTableTokens.textSpacing) {
            Text(feature.label)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            if let description = feature.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.leading)
        .padding(.vertical, MonarchPricingTableTokens.rowSpacing)
        .frame(minWidth: featureMinWidth, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func header(_ plan: MonarchPricingPlan) -> some View {
        let price = isPriceShown ? MonarchPricingTableTokens.price(plan, period: period?.id) : nil
        return VStack(alignment: .leading, spacing: MonarchPricingTableTokens.textSpacing) {
            Text(plan.name)
                .monarchTextStyle(isCompact ? .label : .subheading)
                .lineLimit(isCompact ? 1 : nil)
                .minimumScaleFactor(isCompact ? 0.7 : 1)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if let badge = plan.badge {
                MonarchBadge(badge, tone: plan.isHighlighted ? .accent : .neutral, variant: .subtle)
                    .lineLimit(isCompact ? 1 : nil)
                    .minimumScaleFactor(isCompact ? 0.6 : 1)
            }
            if plan.isCurrent {
                MonarchBadge(labels.currentPlan ?? MonarchPricingTableStrings.currentPlan, variant: .outline)
            }
            if let price {
                MonarchFlowLayout(spacing: MonarchPricingTableTokens.textSpacing) {
                    Text(price)
                        .monarchTextStyle(isCompact ? .subheading : .title)
                        .lineLimit(isCompact ? 1 : nil)
                        .minimumScaleFactor(isCompact ? 0.6 : 1)
                        .foregroundStyle(theme.colors.textDefault)
                    if let suffix = MonarchPricingTableTokens.priceSuffix(plan, period: period) {
                        Text(suffix).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
                    }
                }
                .accessibilityElement(children: .combine)
                if let note = MonarchPricingTableTokens.priceNote(plan, period: period) {
                    Text(note).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
                }
            }
            if let description = plan.description {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .lineLimit(isCompact ? 3 : nil)
                    .minimumScaleFactor(isCompact ? 0.75 : 1)
                    .fixedSize(horizontal: false, vertical: true)
            }
            action(plan)
                // On a phone column the button text is a size smaller, so a name such as "Choose Starter" fits.
                .transformEnvironment(\.dynamicTypeSize) { size in
                    if isCompact { size = min(size, .xSmall) }
                }
                .lineLimit(isCompact ? 2 : nil)
                .fixedSize(horizontal: false, vertical: isCompact)
        }
        .multilineTextAlignment(.leading)
        .padding(MonarchPricingTableTokens.headerPadding)
        .frame(minWidth: planMinWidth, maxWidth: .infinity, alignment: .topLeading)
        .frame(maxHeight: .infinity, alignment: .topLeading)
        .background(plan.isHighlighted ? theme.colors[role: \.accentSubtle] : Color.clear)
    }
}
