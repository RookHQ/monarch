import SwiftUI

/// The plans of a product on one screen: a header, one billing period picker, a ``MonarchPlanCard`` per plan, a
/// note and an optional ``MonarchPricingTable`` comparing the plans feature by feature.
///
/// ```swift
/// MonarchPricingSection(
///     "Plans that grow with your team",
///     eyebrow: "Pricing",
///     plans: plans,
///     periods: periods,
///     comparison: features,
///     note: "Prices shown before taxes."
/// ) { plan in
///     MonarchButton("Choose \(plan.name)", variant: plan.isHighlighted ? .primary : .secondary, fullWidth: true) {
///         choose(plan)
///     }
/// }
/// ```
///
/// Prices are shown exactly as given. In an app, a purchase must go through the store's billing (StoreKit) where
/// its rules require it; the section only presents the plans.
///
/// Accessibility: the title is a heading and each plan name a heading. The period picker is a standard segmented
/// control named "Billing period"; changing it updates every card and the comparison. The comparison table is
/// named by its title.
public struct MonarchPricingSection<Action: View>: View {
    private let title: String
    private let eyebrow: String?
    private let description: String?
    private let alignment: MonarchMarketingAlignment
    private let plans: [MonarchPricingSectionPlan]
    private let periods: [MonarchPricingPeriod]
    private let period: Binding<String>?
    private let comparison: [MonarchPricingFeature]
    private let comparisonTitle: String?
    private let note: String?
    private let billingPeriodLabel: String?
    private let action: (MonarchPricingSectionPlan) -> Action

    @State private var storedPeriod: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    /// A pricing section with a call to action per plan.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Centred (the default) or leading header text.
    ///   - plans: The plans in display order.
    ///   - periods: Billing periods. With two or more a segmented picker is shown.
    ///   - period: The selected period id. Without it the section keeps its own, starting with the first.
    ///   - comparison: Feature rows for a comparison table under the cards. Empty for none.
    ///   - comparisonTitle: Replaces the built-in comparison title "Compare plans".
    ///   - note: Small print under the cards, such as "Prices shown before taxes."
    ///   - billingPeriodLabel: Replaces the built-in picker name "Billing period".
    ///   - action: The call to action of a plan, normally one full-width button.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .center,
        plans: [MonarchPricingSectionPlan],
        periods: [MonarchPricingPeriod] = [],
        period: Binding<String>? = nil,
        comparison: [MonarchPricingFeature] = [],
        comparisonTitle: String? = nil,
        note: String? = nil,
        billingPeriodLabel: String? = nil,
        @ViewBuilder action: @escaping (MonarchPricingSectionPlan) -> Action
    ) {
        self.title = title
        self.eyebrow = eyebrow
        self.description = description
        self.alignment = alignment
        self.plans = plans
        self.periods = periods
        self.period = period
        self.comparison = comparison
        self.comparisonTitle = comparisonTitle
        self.note = note
        self.billingPeriodLabel = billingPeriodLabel
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let selected = MonarchPricingSectionTokens.selectedPeriod(
            bound: period?.wrappedValue,
            stored: storedPeriod,
            periods: periods
        )
        let option = periods.first { $0.id == selected }
        VStack(alignment: .leading, spacing: MonarchPricingSectionTokens.spacing) {
            MonarchMarketingSectionHeader(title, eyebrow: eyebrow, description: description, alignment: alignment)
            if MonarchPricingSectionTokens.showsPicker(periods: periods.count) {
                MonarchSegmentedControl(
                    billingPeriodLabel ?? MonarchPricingSectionStrings.billingPeriod,
                    selection: selection(selected),
                    items: periods.map { MonarchSegmentedItem($0.label, value: $0.id) },
                    size: .sm,
                    fillsWidth: false
                )
                .frame(maxWidth: .infinity, alignment: MonarchMarketingSectionTokens.frame(alignment))
            }
            cards(period: option)
            if let note {
                Text(note)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchPricingSectionTokens.noteColor])
                    .multilineTextAlignment(MonarchMarketingSectionTokens.text(alignment))
                    .frame(maxWidth: .infinity, alignment: MonarchMarketingSectionTokens.frame(alignment))
            }
            if !comparison.isEmpty {
                VStack(alignment: .leading, spacing: MonarchPricingSectionTokens.comparisonTitleSpacing) {
                    Text(resolvedComparisonTitle)
                        .monarchTextStyle(.heading)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                    MonarchPricingTable(
                        plans: plans.map(\.tablePlan),
                        features: comparison,
                        periods: periods,
                        period: selection(selected),
                        layout: .grid,
                        isPeriodPickerShown: false,
                        labels: MonarchPricingTableLabels(caption: resolvedComparisonTitle)
                    )
                }
                .padding(.top, MonarchPricingSectionTokens.comparisonSpacing)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private var resolvedComparisonTitle: String {
        comparisonTitle ?? MonarchPricingSectionStrings.comparisonTitle
    }

    @ViewBuilder private func cards(period option: MonarchPricingPeriod?) -> some View {
        if MonarchPricingSectionTokens.placesCardsInRow(
            isRegularWidth: isRegularWidth,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize,
            plans: plans.count
        ) {
            HStack(alignment: .top, spacing: MonarchPricingSectionTokens.cardSpacing) {
                ForEach(plans) { plan in card(plan, period: option) }
            }
        } else {
            VStack(spacing: MonarchPricingSectionTokens.cardSpacing) {
                ForEach(plans) { plan in card(plan, period: option) }
            }
        }
    }

    private func card(_ plan: MonarchPricingSectionPlan, period option: MonarchPricingPeriod?) -> some View {
        let tablePlan = plan.tablePlan
        return MonarchPlanCard(
            plan.name,
            price: MonarchPricingTableTokens.price(tablePlan, period: option?.id),
            period: MonarchPricingTableTokens.priceSuffix(tablePlan, period: option),
            priceNote: MonarchPricingTableTokens.priceNote(tablePlan, period: option),
            description: plan.description,
            featuresLabel: plan.featuresLabel,
            features: plan.features,
            badge: plan.badge,
            isHighlighted: plan.isHighlighted,
            isCurrent: plan.isCurrent
        ) {
            action(plan)
        }
    }

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            false
        #endif
    }

    private func selection(_ selected: String?) -> Binding<String> {
        Binding<String>(
            get: { selected ?? "" },
            set: { newValue in
                if let period {
                    period.wrappedValue = newValue
                } else {
                    storedPeriod = newValue
                }
            }
        )
    }
}

extension MonarchPricingSection where Action == EmptyView {
    /// A pricing section without calls to action.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Centred (the default) or leading header text.
    ///   - plans: The plans in display order.
    ///   - periods: Billing periods. With two or more a segmented picker is shown.
    ///   - period: The selected period id. Without it the section keeps its own, starting with the first.
    ///   - comparison: Feature rows for a comparison table under the cards. Empty for none.
    ///   - comparisonTitle: Replaces the built-in comparison title "Compare plans".
    ///   - note: Small print under the cards.
    ///   - billingPeriodLabel: Replaces the built-in picker name "Billing period".
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .center,
        plans: [MonarchPricingSectionPlan],
        periods: [MonarchPricingPeriod] = [],
        period: Binding<String>? = nil,
        comparison: [MonarchPricingFeature] = [],
        comparisonTitle: String? = nil,
        note: String? = nil,
        billingPeriodLabel: String? = nil
    ) {
        self.init(
            title,
            eyebrow: eyebrow,
            description: description,
            alignment: alignment,
            plans: plans,
            periods: periods,
            period: period,
            comparison: comparison,
            comparisonTitle: comparisonTitle,
            note: note,
            billingPeriodLabel: billingPeriodLabel
        ) { _ in
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchPricingSection") {
        MonarchPreviewMatrix {
            MonarchPricingSection(
                "Plans that grow with your team",
                eyebrow: "Pricing",
                plans: [
                    MonarchPricingSectionPlan(
                        "starter",
                        name: "Starter",
                        description: "For individuals getting organised.",
                        prices: ["monthly": "₹A", "yearly": "₹B"],
                        features: ["Up to 3 projects", MonarchPlanCardFeature("Single sign-on", isIncluded: false)]
                    ),
                    MonarchPricingSectionPlan(
                        "team",
                        name: "Team",
                        prices: ["monthly": "₹C", "yearly": "₹D"],
                        badge: "Recommended",
                        isHighlighted: true
                    ),
                ],
                periods: [
                    MonarchPricingPeriod("monthly", label: "Monthly", priceSuffix: "/ month"),
                    MonarchPricingPeriod("yearly", label: "Yearly", priceSuffix: "/ year", priceNote: "Billed yearly"),
                ],
                note: "Prices shown before taxes."
            ) { plan in
                MonarchButton(
                    "Choose \(plan.name)",
                    variant: plan.isHighlighted ? .primary : .secondary,
                    fullWidth: true
                ) {}
            }
        }
    }
#endif
