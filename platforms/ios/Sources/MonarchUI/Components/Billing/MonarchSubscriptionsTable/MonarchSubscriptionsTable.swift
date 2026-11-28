import Foundation
import SwiftUI

/// Recurring agreements: name and plan, customer, status, price per billing
/// period, the normalised monthly amount and the next renewal. Rows on a
/// phone, a table at regular width.
///
/// ```swift
/// MonarchSubscriptionsTable(
///     [
///         MonarchSubscription(
///             "sub_1", name: "Acme Pro", plan: "Team plan", quantity: 3, customer: "Acme Inc",
///             status: "active", amount: 49, currencyCode: "USD", renewalDate: renewsOn
///         )
///     ],
///     onSelect: { subscription in path.append(subscription.id) }
/// )
/// ```
///
/// Each amount stays in its own currency; nothing is converted or summed.
/// Sorting and filtering stay in the app. Statuses are looked up in the
/// built-in subscription statuses (``statuses``: draft, trial, active,
/// past_due, paused, cancelled, expired), overridden by `registry`, then in
/// those installed with `monarchStatusRegistry(_:)`. At compact width and at
/// the accessibility text sizes each subscription is a block: name and
/// price, the plan line, the customer and renewal, and the status. At
/// regular width it is a row of columns when the columns fit, and blocks
/// otherwise. The view does not scroll; place it
/// in a `ScrollView`.
///
/// Accessibility: the table is a container named "Subscriptions". Each
/// subscription is one element read as its name, plan, customer, status,
/// price ("$49.00 per month"), monthly figure and renewal, in both layouts;
/// with `onSelect` it is a button. "Not converted" has a hint that explains
/// it. Statuses have words and icons, so they never depend on colour.
public struct MonarchSubscriptionsTable: View {
    private let subscriptions: [MonarchSubscription]
    private let registry: MonarchStatusRegistry?
    private let locale: Locale?
    private let isCustomerShown: Bool
    private let isMonthlyShown: Bool?
    private let labels: MonarchSubscriptionsTableLabels
    private let onSelect: (@MainActor (MonarchSubscription) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale
    @Environment(\.timeZone) private var timeZone
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.monarchStatusRegistry) private var installedStatuses

    private typealias Tokens = MonarchSubscriptionsTableTokens

    /// The built-in subscription statuses: draft, trial, active, past_due,
    /// paused, cancelled and expired, with localised labels, tones and icons.
    /// Extend or replace them with the table's `registry`.
    nonisolated public static var statuses: MonarchStatusRegistry { MonarchSubscriptionsTableTokens.statuses }

    /// A table of subscriptions.
    ///
    /// - Parameters:
    ///   - subscriptions: The rows, in display order.
    ///   - registry: Statuses that add to or replace the built-in subscription statuses.
    ///   - locale: The locale of amounts and dates. Default: the environment's.
    ///   - isCustomerShown: Whether the customer is shown. Turn it off on a single customer's screen.
    ///   - isMonthlyShown: Whether the monthly column is shown. Default: when any row has a monthly figure.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Opens a subscription. Makes each row a button.
    public init(
        _ subscriptions: [MonarchSubscription],
        registry: MonarchStatusRegistry? = nil,
        locale: Locale? = nil,
        isCustomerShown: Bool = true,
        isMonthlyShown: Bool? = nil,
        labels: MonarchSubscriptionsTableLabels = MonarchSubscriptionsTableLabels(),
        onSelect: (@MainActor (MonarchSubscription) -> Void)? = nil
    ) {
        self.subscriptions = subscriptions
        self.registry = registry
        self.locale = locale
        self.isCustomerShown = isCustomerShown
        self.isMonthlyShown = isMonthlyShown
        self.labels = labels
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let texts = Tokens.texts(labels)
        if subscriptions.isEmpty {
            MonarchEmptyState(
                texts.emptyTitle,
                message: texts.emptyMessage,
                systemImage: Tokens.emptySymbol,
                size: .sm
            )
        } else {
            Group {
                if Tokens.usesStackedRows(
                    isRegularWidth: horizontalSizeClass == .regular,
                    isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
                ) {
                    if dynamicTypeSize.isAccessibilitySize {
                        stacked(texts: texts)
                    } else {
                        // At phone width the web's table keeps three columns (the subscription, its status and
                        // its price), with the customer and renewal under the name; stacked blocks are for the
                        // accessibility text sizes.
                        compactTable(texts: texts)
                    }
                } else {
                    // A regular size class does not promise room for six
                    // columns (an iPad in split view, a narrow container).
                    // Squeezed, the cells wrap a few letters per line, so the
                    // full table is used only when its columns fit at their ideal
                    // width; otherwise it is the same three-column table as at
                    // compact width.
                    ViewThatFits(in: .horizontal) {
                        table(texts: texts)
                        compactTable(texts: texts)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(texts.caption)
        }
    }

    private var resolvedLocale: Locale { locale ?? environmentLocale }
    private var statusRegistry: MonarchStatusRegistry { Tokens.registry(registry) }
    private var showsMonthly: Bool { Tokens.showsMonthly(isMonthlyShown, subscriptions: subscriptions) }

    private func rowLabel(_ subscription: MonarchSubscription, texts: MonarchSubscriptionsTableTokens.Texts) -> String {
        let status = MonarchStatusBadgeTokens.resolve(subscription.status, in: [statusRegistry, installedStatuses])
        return Tokens.rowLabel(
            subscription,
            statusLabel: status.label,
            texts: texts,
            locale: resolvedLocale,
            timeZone: timeZone,
            showsCustomer: isCustomerShown,
            showsMonthly: showsMonthly
        )
    }

    // MARK: Rows

    private func stacked(texts: MonarchSubscriptionsTableTokens.Texts) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(subscriptions.enumerated()), id: \.element.id) { index, subscription in
                if index > 0 {
                    MonarchDivider()
                }
                selectable(subscription, texts: texts) {
                    stackedRow(subscription, texts: texts)
                }
            }
        }
    }

    private func stackedRow(_ subscription: MonarchSubscription, texts: MonarchSubscriptionsTableTokens.Texts)
        -> some View
    {
        let renewal = Tokens.renewal(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone)
        return HStack(spacing: Tokens.gap) {
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: Tokens.gap) {
                        name(subscription, isLink: false)
                        Spacer(minLength: 0)
                        price(subscription, texts: texts).fixedSize()
                    }
                    VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                        name(subscription, isLink: false)
                        price(subscription, texts: texts)
                    }
                }
                caption(Tokens.meta(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone))
                caption(Tokens.secondaryLine(subscription.customer, renewal: renewal, showsCustomer: isCustomerShown))
                MonarchStatusBadge(subscription.status, registry: statusRegistry, showsIcon: false, size: .sm)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if onSelect != nil {
                MonarchIcon(systemName: Tokens.disclosureSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    /// The row as one element, and a button when rows open subscriptions.
    @ViewBuilder private func selectable(
        _ subscription: MonarchSubscription,
        texts: MonarchSubscriptionsTableTokens.Texts,
        @ViewBuilder content: () -> some View
    ) -> some View {
        let label = rowLabel(subscription, texts: texts)
        let hint = Tokens.rowHint(subscription, texts: texts, showsMonthly: showsMonthly)
        let row = content()
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        if let onSelect {
            Button {
                onSelect(subscription)
            } label: {
                row
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityHint(hint ?? "")
            .accessibilityAddTraits(.isButton)
        } else {
            row
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityHint(hint ?? "")
        }
    }

    // MARK: Table

    private func compactTable(texts: MonarchSubscriptionsTableTokens.Texts) -> some View {
        Grid(alignment: .leading, horizontalSpacing: Tokens.columnSpacing, verticalSpacing: Tokens.rowSpacing) {
            GridRow {
                heading(texts.subscription)
                heading(texts.status)
                heading(texts.price).gridColumnAlignment(.trailing)
            }
            ForEach(subscriptions) { subscription in
                let renewal = Tokens.renewal(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone)
                MonarchDivider()
                GridRow(alignment: .center) {
                    selectable(subscription, texts: texts) {
                        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                            name(subscription, isLink: onSelect != nil)
                            caption(Tokens.meta(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone))
                            caption(
                                Tokens.secondaryLine(
                                    subscription.customer,
                                    renewal: renewal,
                                    showsCustomer: isCustomerShown
                                )
                            )
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    MonarchStatusBadge(subscription.status, registry: statusRegistry, showsIcon: false, size: .sm)
                        .fixedSize()
                        .accessibilityHidden(true)
                    VStack(alignment: .trailing, spacing: 0) {
                        MonarchMoney(
                            subscription.amount,
                            currencyCode: subscription.currencyCode,
                            locale: resolvedLocale
                        )
                        .foregroundStyle(theme.colors.textDefault)
                        caption(texts.priceSuffix(Tokens.period(subscription, texts: texts)))
                    }
                    .fixedSize()
                    .accessibilityHidden(true)
                }
            }
        }
    }

    private func table(texts: MonarchSubscriptionsTableTokens.Texts) -> some View {
        Grid(alignment: .leading, horizontalSpacing: Tokens.columnSpacing, verticalSpacing: Tokens.rowSpacing) {
            GridRow {
                heading(texts.subscription)
                if isCustomerShown {
                    heading(texts.customer)
                }
                heading(texts.status)
                heading(texts.price).gridColumnAlignment(.trailing)
                if showsMonthly {
                    heading(texts.monthly).gridColumnAlignment(.trailing)
                }
                heading(texts.renewal)
            }
            ForEach(subscriptions) { subscription in
                MonarchDivider()
                GridRow(alignment: .firstTextBaseline) {
                    selectable(subscription, texts: texts) {
                        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                            name(subscription, isLink: onSelect != nil)
                            caption(Tokens.meta(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    if isCustomerShown {
                        cellText(subscription.customer ?? texts.noValue)
                            .accessibilityHidden(true)
                    }
                    MonarchStatusBadge(subscription.status, registry: statusRegistry, showsIcon: false, size: .sm)
                        .fixedSize()
                        .accessibilityHidden(true)
                    price(subscription, texts: texts)
                        .fixedSize()
                        .accessibilityHidden(true)
                    if showsMonthly {
                        monthlyCell(subscription, texts: texts)
                            .fixedSize()
                            .accessibilityHidden(true)
                    }
                    cellText(Tokens.renewal(subscription, texts: texts, locale: resolvedLocale, timeZone: timeZone))
                        .accessibilityHidden(true)
                }
            }
        }
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityHidden(true)
    }

    @ViewBuilder private func monthlyCell(
        _ subscription: MonarchSubscription,
        texts: MonarchSubscriptionsTableTokens.Texts
    ) -> some View {
        switch Tokens.monthly(subscription) {
        case .amount(let amount, let code):
            MonarchMoney(amount, currencyCode: code, locale: resolvedLocale)
                .foregroundStyle(theme.colors.textDefault)
        case .notConverted:
            caption(texts.notConverted)
        case .none:
            MonarchMoney(nil, currencyCode: subscription.currencyCode)
        }
    }

    // MARK: Parts

    private func name(_ subscription: MonarchSubscription, isLink: Bool) -> some View {
        Text(subscription.name)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors[role: Tokens.nameRole(isSelectable: isLink)])
            .fixedSize(horizontal: false, vertical: true)
    }

    private func price(_ subscription: MonarchSubscription, texts: MonarchSubscriptionsTableTokens.Texts) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.textSpacing) {
            MonarchMoney(subscription.amount, currencyCode: subscription.currencyCode, locale: resolvedLocale)
                .foregroundStyle(theme.colors.textDefault)
            Text(texts.priceSuffix(Tokens.period(subscription, texts: texts)))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .fixedSize(horizontal: false, vertical: true)
    }

    private func cellText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
    }
}

#if DEBUG
    #Preview("MonarchSubscriptionsTable") {
        let rows = [
            MonarchSubscription(
                "1",
                name: "Acme Pro",
                plan: "Team plan",
                quantity: 3,
                customer: "Acme Inc",
                status: "active",
                amount: 49,
                currencyCode: "USD",
                monthly: .amount(49, currencyCode: "USD"),
                renewalDate: Date(timeIntervalSince1970: 1_775_952_000)
            ),
            MonarchSubscription(
                "2",
                name: "Globex Support",
                customer: "Globex",
                status: "past_due",
                amount: 1_200,
                currencyCode: "EUR",
                interval: .year,
                monthly: .notConverted
            ),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSubscriptionsTable(rows, onSelect: { _ in })
                MonarchSubscriptionsTable([])
            }
        }
    }
#endif
