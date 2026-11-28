import SwiftUI

/// Plans compared feature by feature, with an optional billing period
/// picker. A phone shows one plan per page with its features below; a wide
/// screen shows plans as columns and features as rows.
///
/// ```swift
/// MonarchPricingTable(
///     plans: [
///         MonarchPricingPlan("starter", name: "Starter", prices: ["month": "$0"]),
///         MonarchPricingPlan("team", name: "Team", prices: ["month": "$12", "year": "$120"], badge: "Recommended", isHighlighted: true),
///     ],
///     features: [
///         MonarchPricingFeature("seats", label: "Seats", values: ["starter": "3", "team": "Unlimited"]),
///         MonarchPricingFeature("sso", label: "Single sign-on", values: ["team": true]),
///     ],
///     periods: [MonarchPricingPeriod("month", label: "Monthly", priceSuffix: "/ month"), MonarchPricingPeriod("year", label: "Yearly", priceSuffix: "/ year")]
/// ) { plan in
///     MonarchButton("Choose \(plan.name)", variant: plan.isHighlighted ? .primary : .secondary, fullWidth: true) { choose(plan) }
/// }
/// ```
///
/// Prices are shown exactly as given and never calculated. A plan without a
/// price for the period shows its fallback `price` (such as "Contact us")
/// without the period's suffix and note. The table only presents plans: a
/// purchase inside an app must go through the store's billing (StoreKit)
/// where the store's rules require it.
///
/// Accessibility: the table is a container named "Plan comparison". Plan
/// names and group titles are headings. In the grid each cell is read as
/// the plan, the feature and "Included", "Not included" or the text; on a
/// page each feature is read as its name and value. Checks and dashes are
/// never the only cue. The period and plan pickers are standard segmented
/// pickers.
public struct MonarchPricingTable<Action: View>: View {
    private let plans: [MonarchPricingPlan]
    private let features: [MonarchPricingFeature]
    private let periods: [MonarchPricingPeriod]
    private let period: Binding<String>?
    private let layout: MonarchPricingTableLayout
    private let isPriceShown: Bool
    private let isPeriodPickerShown: Bool
    private let labels: MonarchPricingTableLabels
    private let action: (MonarchPricingPlan) -> Action

    @State private var storedPeriod: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    /// A pricing table with a call to action per plan.
    ///
    /// - Parameters:
    ///   - plans: The plans, in display order. Five at most reads well.
    ///   - features: The feature rows, with a value per plan.
    ///   - periods: The billing periods. With two or more, a picker is shown.
    ///   - period: The selected period id. Without it the table keeps its own, starting with the first.
    ///   - layout: Pages, a grid, or automatic by width and text size.
    ///   - isPriceShown: Whether prices are shown in the plan headers.
    ///   - isPeriodPickerShown: Whether the period picker is shown; turn it off when the screen shows its own.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: The call to action of a plan, normally one button.
    public init(
        plans: [MonarchPricingPlan],
        features: [MonarchPricingFeature],
        periods: [MonarchPricingPeriod] = [],
        period: Binding<String>? = nil,
        layout: MonarchPricingTableLayout = .automatic,
        isPriceShown: Bool = true,
        isPeriodPickerShown: Bool = true,
        labels: MonarchPricingTableLabels = MonarchPricingTableLabels(),
        @ViewBuilder action: @escaping (MonarchPricingPlan) -> Action
    ) {
        self.plans = plans
        self.features = features
        self.periods = periods
        self.period = period
        self.layout = layout
        self.isPriceShown = isPriceShown
        self.isPeriodPickerShown = isPeriodPickerShown
        self.labels = labels
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let selected = MonarchPricingTableTokens.selectedPeriod(
            bound: period?.wrappedValue,
            stored: storedPeriod,
            periods: periods
        )
        let option = periods.first { $0.id == selected }
        VStack(alignment: .leading, spacing: MonarchPricingTableTokens.sectionSpacing) {
            if MonarchPricingTableTokens.showsPeriodPicker(periods: periods.count, isShown: isPeriodPickerShown) {
                periodPicker(selected: selected, option: option)
            }
            switch resolvedLayout {
            case .grid:
                // The columns share the width, as on the web; at the accessibility text sizes they keep a readable width and the table scrolls sideways.
                ViewThatFits(in: .horizontal) {
                    grid(compact: false, period: option)
                    // The whole table in a phone column: it is laid out at the width it is given.
                    grid(compact: true, period: option)
                        .frame(
                            idealWidth: MonarchPricingTableTokens.compactFeatureColumnMinWidth
                                + CGFloat(plans.count) * MonarchPricingTableTokens.compactPlanColumnMinWidth
                        )
                    ScrollView(.horizontal) { grid(compact: false, period: option) }
                        .scrollIndicators(.hidden)
                        .monarchTrailingFade()
                }
            case .paged, .automatic:
                MonarchPricingTablePages(
                    plans: plans,
                    features: features,
                    period: option,
                    isPriceShown: isPriceShown,
                    labels: labels,
                    action: action
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.caption ?? MonarchPricingTableStrings.caption)
    }

    private func grid(compact: Bool, period option: MonarchPricingPeriod?) -> MonarchPricingTableGrid<Action> {
        MonarchPricingTableGrid(
            plans: plans,
            features: features,
            period: option,
            isPriceShown: isPriceShown,
            labels: labels,
            action: action,
            isCompact: compact
        )
    }

    private var resolvedLayout: MonarchPricingTableLayout {
        #if os(iOS)
            let isRegularWidth = horizontalSizeClass == .regular
        #else
            let isRegularWidth = true
        #endif
        return MonarchPricingTableTokens.resolvedLayout(
            layout,
            isRegularWidth: isRegularWidth,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
    }

    private func periodPicker(selected: String?, option: MonarchPricingPeriod?) -> some View {
        let selection = Binding<String>(
            get: { selected ?? "" },
            set: { newValue in
                if let period {
                    period.wrappedValue = newValue
                } else {
                    storedPeriod = newValue
                }
            }
        )
        return VStack(alignment: .center, spacing: MonarchPricingTableTokens.textSpacing) {
            MonarchSegmentedControl(
                labels.billingPeriod ?? MonarchPricingTableStrings.billingPeriod,
                selection: selection,
                items: periods.map { MonarchSegmentedItem($0.label, value: $0.id) },
                size: .sm,
                fillsWidth: false
            )
            .frame(maxWidth: .infinity)
            if let suffix = option?.suffix, !suffix.isEmpty {
                Text(suffix)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }
}

extension MonarchPricingTable where Action == EmptyView {
    /// A pricing table without calls to action.
    ///
    /// - Parameters:
    ///   - plans: The plans, in display order. Five at most reads well.
    ///   - features: The feature rows, with a value per plan.
    ///   - periods: The billing periods. With two or more, a picker is shown.
    ///   - period: The selected period id. Without it the table keeps its own, starting with the first.
    ///   - layout: Pages, a grid, or automatic by width and text size.
    ///   - isPriceShown: Whether prices are shown in the plan headers.
    ///   - isPeriodPickerShown: Whether the period picker is shown.
    ///   - labels: Replacements for the built-in strings.
    public init(
        plans: [MonarchPricingPlan],
        features: [MonarchPricingFeature],
        periods: [MonarchPricingPeriod] = [],
        period: Binding<String>? = nil,
        layout: MonarchPricingTableLayout = .automatic,
        isPriceShown: Bool = true,
        isPeriodPickerShown: Bool = true,
        labels: MonarchPricingTableLabels = MonarchPricingTableLabels()
    ) {
        self.init(
            plans: plans,
            features: features,
            periods: periods,
            period: period,
            layout: layout,
            isPriceShown: isPriceShown,
            isPeriodPickerShown: isPeriodPickerShown,
            labels: labels
        ) { _ in
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchPricingTable") {
        let plans = [
            MonarchPricingPlan("starter", name: "Starter", prices: ["month": "$0"]),
            MonarchPricingPlan(
                "team",
                name: "Team",
                prices: ["month": "$12"],
                badge: "Recommended",
                isHighlighted: true
            ),
            MonarchPricingPlan("enterprise", name: "Enterprise", price: "Contact us"),
        ]
        let features = [
            MonarchPricingFeature(
                "seats",
                label: "Seats",
                values: ["starter": "3", "team": "Unlimited", "enterprise": "Unlimited"]
            ),
            MonarchPricingFeature(
                "sso",
                label: "Single sign-on",
                group: "Security",
                values: ["team": true, "enterprise": true]
            ),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchPricingTable(plans: plans, features: features, layout: .grid)
                MonarchPricingTable(plans: plans, features: features, layout: .paged)
            }
        }
    }
#endif
