import Foundation
import SwiftUI

/// What unused items cost to keep: a headline with the count and renewal
/// cost per currency, the reasons they count as unused, the decisions made
/// so far, what letting them lapse would save, and the top items.
///
/// ```swift
/// MonarchSavingsPanel(
///     unusedCount: 9,
///     costs: [MonarchSavingsAmount(1_200, currencyCode: "USD")],
///     unpricedCount: 2,
///     categories: [MonarchSavingsCategory(id: "parked", count: 5, label: "Parked")],
///     items: unusedItems,
///     onItemSelect: { item in path.append(item.id) }
/// )
/// ```
///
/// The panel is a ``MonarchSectionCard``: skeleton rows while loading, an
/// empty state when nothing is unused, and an error state with a retry.
/// Only real prices are added up, each currency on its own; unpriced items
/// are counted, never estimated. Categories and decisions are
/// ``MonarchStatusBadge``s looked up in `registry`, then in the registries
/// installed with `monarchStatusRegistry(_:)`. The view does not scroll.
///
/// Accessibility: the title is a header. The headline is one sentence.
/// Each category is one element read as its label and count, a button with
/// `onCategorySelect`. The decision line starts with "Decisions". Each item
/// is one element read as its name, then category, date, amount and
/// decision; a button with `onItemSelect`. Badges have words and icons.
@_spi(Experimental) public struct MonarchSavingsPanel<Actions: View>: View {
    private let unusedCount: Int
    private let costs: [MonarchSavingsAmount]
    private let unpricedCount: Int
    private let categories: [MonarchSavingsCategory]
    private let decisions: [MonarchSavingsDecisionCount]
    private let potentialSavings: [MonarchSavingsAmount]
    private let items: [MonarchSavingsItem]
    private let maxItems: Int
    private let registry: MonarchStatusRegistry?
    private let title: String?
    private let description: String?
    private let locale: Locale?
    private let calendar: Calendar?
    private let isLoading: Bool
    private let errorMessage: String?
    private let isRetrying: Bool
    private let labels: MonarchSavingsPanelLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onCategorySelect: (@MainActor (MonarchSavingsCategory) -> Void)?
    private let onItemSelect: (@MainActor (MonarchSavingsItem) -> Void)?
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale
    @Environment(\.calendar) private var environmentCalendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.monarchStatusRegistry) private var installedStatuses

    private typealias Tokens = MonarchSavingsPanelTokens
    private typealias Strings = MonarchSavingsPanelStrings

    /// A savings panel with header actions.
    ///
    /// - Parameters:
    ///   - unusedCount: How many items are unused. Zero shows the empty state.
    ///   - costs: The renewal cost of the unused items, one amount per currency.
    ///   - unpricedCount: Unused items without a price; counted, never estimated.
    ///   - categories: The unused items grouped by reason.
    ///   - decisions: How many items have each decision.
    ///   - potentialSavings: What letting the marked items lapse would save, one amount per currency.
    ///   - items: The unused items to list, most urgent first.
    ///   - maxItems: Lists at most this many items, then "and <n> more".
    ///   - registry: Statuses for the category and decision badges.
    ///   - title: Replaces the built-in title "Savings".
    ///   - description: A line under the title.
    ///   - locale: The locale of amounts and dates. Default: the environment's.
    ///   - calendar: The calendar and time zone of dates. Default: the environment's.
    ///   - isLoading: Shows skeleton rows.
    ///   - errorMessage: Shows the error state with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the error state.
    ///   - onCategorySelect: Opens the items of a category. Makes each category a button.
    ///   - onItemSelect: Opens an item. Makes each item a button.
    ///   - actions: Buttons or a menu at the trailing edge of the header, such as Review all.
    public init(
        unusedCount: Int,
        costs: [MonarchSavingsAmount] = [],
        unpricedCount: Int = 0,
        categories: [MonarchSavingsCategory] = [],
        decisions: [MonarchSavingsDecisionCount] = [],
        potentialSavings: [MonarchSavingsAmount] = [],
        items: [MonarchSavingsItem] = [],
        maxItems: Int = 6,
        registry: MonarchStatusRegistry? = nil,
        title: String? = nil,
        description: String? = nil,
        locale: Locale? = nil,
        calendar: Calendar? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchSavingsPanelLabels = MonarchSavingsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onCategorySelect: (@MainActor (MonarchSavingsCategory) -> Void)? = nil,
        onItemSelect: (@MainActor (MonarchSavingsItem) -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.unusedCount = unusedCount
        self.costs = costs
        self.unpricedCount = unpricedCount
        self.categories = categories
        self.decisions = decisions
        self.potentialSavings = potentialSavings
        self.items = items
        self.maxItems = maxItems
        self.registry = registry
        self.title = title
        self.description = description
        self.locale = locale
        self.calendar = calendar
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.labels = labels
        self.onRetry = onRetry
        self.onCategorySelect = onCategorySelect
        self.onItemSelect = onItemSelect
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        MonarchSectionCard(
            title ?? labels.title ?? Strings.title,
            description: description,
            state: Tokens.cardState(isLoading: isLoading, errorMessage: errorMessage, unusedCount: unusedCount),
            emptyMessage: labels.emptyMessage ?? Strings.emptyMessage,
            errorMessage: errorMessage,
            isRetrying: isRetrying,
            labels: MonarchSectionCardLabels(
                loading: labels.loading ?? Strings.loading,
                emptyTitle: labels.emptyTitle ?? Strings.emptyTitle,
                errorTitle: labels.errorTitle ?? Strings.errorTitle
            ),
            onRetry: onRetry,
            content: { content },
            actions: { actions }
        )
    }

    private var resolvedLocale: Locale { locale ?? environmentLocale }

    private var resolvedCalendar: Calendar {
        if let calendar { return calendar }
        var calendar = environmentCalendar
        calendar.timeZone = timeZone
        return calendar
    }

    private var content: some View {
        let cost = Tokens.join(costs, locale: resolvedLocale)
        let shownCategories = Tokens.shownCategories(categories)
        let list = Tokens.shownItems(items, max: maxItems)
        return VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            Text(
                labels.headline?(unusedCount, cost, unpricedCount)
                    ?? Strings.headline(unusedCount, cost: cost, unpriced: unpricedCount)
            )
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
            if !shownCategories.isEmpty {
                MonarchFlowLayout(spacing: Tokens.inlineSpacing) {
                    ForEach(shownCategories) { category in
                        categoryChip(category)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.categories ?? Strings.categories)
            }
            if let line = Tokens.decisionsLine(decisions, locale: resolvedLocale) {
                Text(line)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityLabel("\(labels.decisions ?? Strings.decisions): \(line)")
            }
            if let saving = Tokens.join(potentialSavings, locale: resolvedLocale) {
                Text(labels.potentialSavings?(saving) ?? Strings.potentialSavings(saving))
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if !list.shown.isEmpty {
                // The rows sit in one bordered box, as on the web.
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(list.shown.enumerated()), id: \.element.id) { index, item in
                        if index > 0 {
                            MonarchDivider()
                        }
                        itemRow(item)
                            .padding(.horizontal, Tokens.rowSpacing)
                            .padding(.vertical, Tokens.rowSpacing)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .clipShape(MonarchShapes.control)
                .overlay(
                    MonarchShapes.control.strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth)
                )
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.items ?? Strings.items)
            }
            if list.hidden > 0 {
                Text(labels.more?(list.hidden) ?? Strings.more(list.hidden))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    private func statusLabel(_ key: String) -> String {
        MonarchStatusBadgeTokens.resolve(key, in: [registry, installedStatuses].compactMap { $0 }).label
    }

    @ViewBuilder private func categoryChip(_ category: MonarchSavingsCategory) -> some View {
        let chip = HStack(spacing: Tokens.lineSpacing) {
            MonarchStatusBadge(category.id, registry: registry, label: category.label, size: .sm)
            Text(category.count.formatted(.number.locale(resolvedLocale)))
                .monarchTextStyle(.label)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textDefault)
        }
        let spoken =
            "\(category.label ?? statusLabel(category.id)), \(category.count.formatted(.number.locale(resolvedLocale)))"
        if let onCategorySelect {
            Button {
                onCategorySelect(category)
            } label: {
                chip.monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken)
            .accessibilityAddTraits(.isButton)
        } else {
            chip
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spoken)
        }
    }

    @ViewBuilder private func itemRow(_ item: MonarchSavingsItem) -> some View {
        let unpriced = labels.unpriced ?? Strings.unpriced
        let value = Tokens.itemValue(
            item,
            category: item.category.map(statusLabel),
            decision: item.decision.map(statusLabel),
            locale: resolvedLocale,
            calendar: resolvedCalendar,
            unpriced: unpriced
        )
        if let onItemSelect {
            Button {
                onItemSelect(item)
            } label: {
                HStack(spacing: Tokens.inlineSpacing) {
                    itemContent(item, unpriced: unpriced)
                    MonarchIcon(systemName: Tokens.disclosureSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(item.name)
            .accessibilityValue(value)
            .accessibilityAddTraits(.isButton)
        } else {
            itemContent(item, unpriced: unpriced)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(item.name)
                .accessibilityValue(value)
        }
    }

    private func itemContent(_ item: MonarchSavingsItem, unpriced: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            Text(item.name)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
                .multilineTextAlignment(.leading)
            MonarchFlowLayout(spacing: Tokens.inlineSpacing, alignment: .center) {
                if let category = item.category {
                    MonarchStatusBadge(category, registry: registry, variant: .outline, size: .sm)
                }
                if let date = item.date {
                    Text(Tokens.date(date, locale: resolvedLocale, calendar: resolvedCalendar))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let money = item.amount {
                    MonarchMoney(
                        money.amount,
                        currencyCode: money.currencyCode,
                        locale: resolvedLocale,
                        textStyle: .label
                    )
                } else if item.isUnpriced {
                    Text(unpriced)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let decision = item.decision {
                    MonarchStatusBadge(decision, registry: registry, size: .sm)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
    }
}

extension MonarchSavingsPanel where Actions == EmptyView {
    /// A savings panel.
    ///
    /// - Parameters:
    ///   - unusedCount: How many items are unused. Zero shows the empty state.
    ///   - costs: The renewal cost of the unused items, one amount per currency.
    ///   - unpricedCount: Unused items without a price.
    ///   - categories: The unused items grouped by reason.
    ///   - decisions: How many items have each decision.
    ///   - potentialSavings: What letting the marked items lapse would save.
    ///   - items: The unused items to list, most urgent first.
    ///   - maxItems: Lists at most this many items.
    ///   - registry: Statuses for the category and decision badges.
    ///   - title: Replaces the built-in title "Savings".
    ///   - description: A line under the title.
    ///   - locale: The locale of amounts and dates.
    ///   - calendar: The calendar and time zone of dates.
    ///   - isLoading: Shows skeleton rows.
    ///   - errorMessage: Shows the error state with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading.
    ///   - onCategorySelect: Opens the items of a category.
    ///   - onItemSelect: Opens an item.
    public init(
        unusedCount: Int,
        costs: [MonarchSavingsAmount] = [],
        unpricedCount: Int = 0,
        categories: [MonarchSavingsCategory] = [],
        decisions: [MonarchSavingsDecisionCount] = [],
        potentialSavings: [MonarchSavingsAmount] = [],
        items: [MonarchSavingsItem] = [],
        maxItems: Int = 6,
        registry: MonarchStatusRegistry? = nil,
        title: String? = nil,
        description: String? = nil,
        locale: Locale? = nil,
        calendar: Calendar? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchSavingsPanelLabels = MonarchSavingsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onCategorySelect: (@MainActor (MonarchSavingsCategory) -> Void)? = nil,
        onItemSelect: (@MainActor (MonarchSavingsItem) -> Void)? = nil
    ) {
        self.init(
            unusedCount: unusedCount,
            costs: costs,
            unpricedCount: unpricedCount,
            categories: categories,
            decisions: decisions,
            potentialSavings: potentialSavings,
            items: items,
            maxItems: maxItems,
            registry: registry,
            title: title,
            description: description,
            locale: locale,
            calendar: calendar,
            isLoading: isLoading,
            errorMessage: errorMessage,
            isRetrying: isRetrying,
            labels: labels,
            onRetry: onRetry,
            onCategorySelect: onCategorySelect,
            onItemSelect: onItemSelect,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchSavingsPanel") {
        MonarchPreviewMatrix {
            VStack {
                MonarchSavingsPanel(
                    unusedCount: 9,
                    costs: [MonarchSavingsAmount(1_200, currencyCode: "USD")],
                    unpricedCount: 2,
                    categories: [MonarchSavingsCategory(id: "warning", count: 5, label: "Parked")],
                    decisions: [MonarchSavingsDecisionCount(id: "keep", label: "Keep", count: 3)],
                    potentialSavings: [MonarchSavingsAmount(400, currencyCode: "USD")],
                    items: [
                        MonarchSavingsItem(
                            id: "1",
                            name: "old-brand.com",
                            amount: MonarchSavingsAmount(40, currencyCode: "USD")
                        )
                    ]
                )
                MonarchSavingsPanel(unusedCount: 0)
                MonarchSavingsPanel(unusedCount: 0, isLoading: true)
            }
        }
    }
#endif
