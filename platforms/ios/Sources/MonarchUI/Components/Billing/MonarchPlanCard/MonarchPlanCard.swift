import SwiftUI

/// One plan in a plan picker: its name, price, who it is for, what it
/// includes and a call to action.
///
/// ```swift
/// MonarchPlanCard(
///     "Team",
///     price: "$12",
///     period: "per seat / month",
///     features: ["Unlimited projects", MonarchPlanCardFeature("Single sign-on", isIncluded: false)],
///     badge: "Recommended",
///     isHighlighted: true
/// ) {
///     MonarchButton("Choose Team", variant: .primary, fullWidth: true) { choose() }
/// }
/// ```
///
/// The card shows prices exactly as given and never formats or invents them.
/// It only presents the plan: a purchase inside an app must go through the
/// store's billing (StoreKit) where the store's rules require it.
///
/// Accessibility: the name is a heading. The price and its period are read as
/// one element. Each feature is one element; a feature that is not included is
/// read with "Not included:" before it and shows a dash, so the difference
/// does not depend on colour. The action stays a separate element.
public struct MonarchPlanCard<Action: View>: View {
    private let name: String
    private let price: String?
    private let period: String?
    private let priceNote: String?
    private let description: String?
    private let featuresLabel: String?
    private let features: [MonarchPlanCardFeature]
    private let badge: String?
    private let footer: String?
    private let isHighlighted: Bool
    private let isCurrent: Bool
    private let currentPlanLabel: String?
    private let notIncludedLabel: String?
    private let action: Action

    @Environment(\.monarchTheme) private var theme

    /// A plan card with a call to action.
    ///
    /// - Parameters:
    ///   - name: The plan name, already localised.
    ///   - price: The price as the product writes it, such as "$12" or "Contact us".
    ///   - period: Text after the price, such as "per seat / month".
    ///   - priceNote: A line under the price, such as "Billed yearly".
    ///   - description: One or two sentences on who the plan is for.
    ///   - featuresLabel: A heading above the features, such as "Everything in Starter, plus".
    ///   - features: What the plan includes and, with `isIncluded: false`, what it does not.
    ///   - badge: A short label beside the name, such as "Recommended".
    ///   - footer: Small print under the features.
    ///   - isHighlighted: Whether the card is emphasised as the recommended plan.
    ///   - isCurrent: Whether this is the customer's current plan.
    ///   - currentPlanLabel: Replaces the built-in badge text "Current plan".
    ///   - notIncludedLabel: Replaces the built-in spoken prefix "Not included:".
    ///   - action: The call to action, normally one button.
    public init(
        _ name: String,
        price: String? = nil,
        period: String? = nil,
        priceNote: String? = nil,
        description: String? = nil,
        featuresLabel: String? = nil,
        features: [MonarchPlanCardFeature] = [],
        badge: String? = nil,
        footer: String? = nil,
        isHighlighted: Bool = false,
        isCurrent: Bool = false,
        currentPlanLabel: String? = nil,
        notIncludedLabel: String? = nil,
        @ViewBuilder action: () -> Action
    ) {
        self.name = name
        self.price = price
        self.period = period
        self.priceNote = priceNote
        self.description = description
        self.featuresLabel = featuresLabel
        self.features = features
        self.badge = badge
        self.footer = footer
        self.isHighlighted = isHighlighted
        self.isCurrent = isCurrent
        self.currentPlanLabel = currentPlanLabel
        self.notIncludedLabel = notIncludedLabel
        self.action = action()
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard(elevation: MonarchPlanCardTokens.elevation(isHighlighted: isHighlighted)) {
            VStack(alignment: .leading, spacing: MonarchPlanCardTokens.sectionSpacing) {
                header
                pricing
                if let description {
                    Text(description)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                action
                featureList
                if let footer {
                    Text(footer)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .overlay {
            if isHighlighted {
                MonarchShapes.container.strokeBorder(
                    theme.colors[role: MonarchPlanCardTokens.highlightBorder],
                    lineWidth: MonarchPlanCardTokens.highlightBorderWidth
                )
                .allowsHitTesting(false)
            }
        }
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchPlanCardTokens.inlineSpacing) {
                title
                Spacer(minLength: 0)
                badges
            }
            VStack(alignment: .leading, spacing: MonarchPlanCardTokens.inlineSpacing) {
                title
                badges
            }
        }
    }

    private var title: some View {
        Text(name)
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors.textDefault)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var badges: some View {
        if let badge {
            MonarchBadge(
                badge,
                tone: isHighlighted ? .accent : .neutral,
                variant: MonarchPlanCardTokens.badgeVariant(isHighlighted: isHighlighted)
            )
        }
        if isCurrent {
            MonarchBadge(currentPlanLabel ?? MonarchPlanCardStrings.currentPlan, variant: .outline)
        }
    }

    @ViewBuilder private var pricing: some View {
        if price != nil || period != nil || priceNote != nil {
            VStack(alignment: .leading, spacing: MonarchPlanCardTokens.textSpacing) {
                if price != nil || period != nil {
                    ViewThatFits(in: .horizontal) {
                        HStack(alignment: .firstTextBaseline, spacing: MonarchPlanCardTokens.inlineSpacing) {
                            priceTexts
                        }
                        VStack(alignment: .leading, spacing: MonarchPlanCardTokens.textSpacing) { priceTexts }
                    }
                    .accessibilityElement(children: .combine)
                }
                if let priceNote {
                    Text(priceNote)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
    }

    @ViewBuilder private var priceTexts: some View {
        if let price {
            Text(price)
                .monarchTextStyle(.title)
                .foregroundStyle(theme.colors.textDefault)
        }
        if let period {
            Text(period)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    @ViewBuilder private var featureList: some View {
        if !features.isEmpty {
            MonarchDivider()
            VStack(alignment: .leading, spacing: MonarchPlanCardTokens.featureSpacing) {
                if let featuresLabel {
                    Text(featuresLabel)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                }
                ForEach(Array(features.enumerated()), id: \.offset) { _, feature in
                    MonarchPlanCardFeatureRow(
                        feature: feature,
                        notIncludedLabel: notIncludedLabel ?? MonarchPlanCardStrings.notIncluded
                    )
                }
            }
        }
    }
}

extension MonarchPlanCard where Action == EmptyView {
    /// A plan card without a call to action.
    ///
    /// - Parameters:
    ///   - name: The plan name, already localised.
    ///   - price: The price as the product writes it, such as "$12" or "Contact us".
    ///   - period: Text after the price, such as "per seat / month".
    ///   - priceNote: A line under the price, such as "Billed yearly".
    ///   - description: One or two sentences on who the plan is for.
    ///   - featuresLabel: A heading above the features, such as "Everything in Starter, plus".
    ///   - features: What the plan includes and, with `isIncluded: false`, what it does not.
    ///   - badge: A short label beside the name, such as "Recommended".
    ///   - footer: Small print under the features.
    ///   - isHighlighted: Whether the card is emphasised as the recommended plan.
    ///   - isCurrent: Whether this is the customer's current plan.
    ///   - currentPlanLabel: Replaces the built-in badge text "Current plan".
    ///   - notIncludedLabel: Replaces the built-in spoken prefix "Not included:".
    public init(
        _ name: String,
        price: String? = nil,
        period: String? = nil,
        priceNote: String? = nil,
        description: String? = nil,
        featuresLabel: String? = nil,
        features: [MonarchPlanCardFeature] = [],
        badge: String? = nil,
        footer: String? = nil,
        isHighlighted: Bool = false,
        isCurrent: Bool = false,
        currentPlanLabel: String? = nil,
        notIncludedLabel: String? = nil
    ) {
        self.init(
            name,
            price: price,
            period: period,
            priceNote: priceNote,
            description: description,
            featuresLabel: featuresLabel,
            features: features,
            badge: badge,
            footer: footer,
            isHighlighted: isHighlighted,
            isCurrent: isCurrent,
            currentPlanLabel: currentPlanLabel,
            notIncludedLabel: notIncludedLabel
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchPlanCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchPlanCard(
                    "Team",
                    price: "$12",
                    period: "per seat / month",
                    priceNote: "Billed yearly",
                    description: "For teams that ship together.",
                    featuresLabel: "Everything in Starter, plus",
                    features: [
                        "Unlimited projects",
                        MonarchPlanCardFeature("Seats", hint: "up to 10"),
                        MonarchPlanCardFeature("Single sign-on", isIncluded: false),
                    ],
                    badge: "Recommended",
                    footer: "Cancel at any time.",
                    isHighlighted: true
                ) {
                    MonarchButton("Choose Team", variant: .primary, fullWidth: true) {}
                }
                MonarchPlanCard("Starter", price: "$0", isCurrent: true)
            }
        }
    }
#endif
