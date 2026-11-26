import SwiftUI

/// The paged layout: one plan per page, as a plan card with its features below.
struct MonarchPricingTablePages<Action: View>: View {
    let plans: [MonarchPricingPlan]
    let features: [MonarchPricingFeature]
    let period: MonarchPricingPeriod?
    let isPriceShown: Bool
    let labels: MonarchPricingTableLabels
    let action: (MonarchPricingPlan) -> Action

    @State private var visiblePlan: String?
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchPricingTableTokens.sectionSpacing) {
            if plans.count > 1 {
                MonarchSegmentedControl(
                    labels.plan ?? MonarchPricingTableStrings.plan,
                    selection: planSelection,
                    items: plans.map { MonarchSegmentedItem($0.name, value: Optional($0.id)) }
                )
            }
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: 0) {
                    ForEach(plans) { plan in
                        page(plan)
                            .padding(MonarchPricingTableTokens.pageInset)
                            .containerRelativeFrame(.horizontal)
                            .id(plan.id)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.paging)
            .scrollPosition(id: $visiblePlan)
            .scrollIndicators(.hidden)
        }
    }

    /// The picker shows the first plan until the pages report one.
    private var planSelection: Binding<String?> {
        Binding(
            get: { visiblePlan ?? plans.first?.id },
            set: { visiblePlan = $0 }
        )
    }

    private func page(_ plan: MonarchPricingPlan) -> some View {
        let price = isPriceShown ? MonarchPricingTableTokens.price(plan, period: period?.id) : nil
        return VStack(alignment: .leading, spacing: MonarchPricingTableTokens.sectionSpacing) {
            MonarchPlanCard(
                plan.name,
                price: price,
                period: price == nil ? nil : MonarchPricingTableTokens.priceSuffix(plan, period: period),
                priceNote: price == nil ? nil : MonarchPricingTableTokens.priceNote(plan, period: period),
                description: plan.description,
                badge: plan.badge,
                isHighlighted: plan.isHighlighted,
                isCurrent: plan.isCurrent,
                currentPlanLabel: labels.currentPlan
            ) {
                action(plan)
            }
            featureList(plan)
        }
    }

    private func featureList(_ plan: MonarchPricingPlan) -> some View {
        VStack(alignment: .leading, spacing: MonarchPricingTableTokens.rowSpacing) {
            ForEach(Array(MonarchPricingTableTokens.groups(features).enumerated()), id: \.offset) { _, group in
                if let title = group.title {
                    Text(title)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                }
                ForEach(group.features) { feature in
                    row(feature, value: MonarchPricingTableTokens.value(feature, plan: plan.id))
                }
            }
        }
    }

    private func row(_ feature: MonarchPricingFeature, value: MonarchPricingFeatureValue) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchPricingTableTokens.markSpacing) {
            MonarchIcon(
                systemName: MonarchPricingTableTokens.symbol(value) ?? MonarchPricingTableTokens.includedSymbol,
                size: .sm
            )
            .foregroundStyle(theme.colors[role: MonarchPricingTableTokens.markRole(value)])
            VStack(alignment: .leading, spacing: MonarchPricingTableTokens.textSpacing) {
                Text(feature.label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchPricingTableTokens.labelRole(value)])
                if let description = feature.description, !description.isEmpty {
                    Text(description).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
                }
            }
            .multilineTextAlignment(.leading)
            if case .text(let text) = value {
                Spacer(minLength: MonarchPricingTableTokens.markSpacing)
                Text(text)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .multilineTextAlignment(.trailing)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchPricingTableTokens.featureAccessibilityLabel(feature, value: value, labels: labels))
    }
}
