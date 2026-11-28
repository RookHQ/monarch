import Foundation
import SwiftUI

/// What renews or expires next: one row per renewal with its date and day
/// count, amount, status, owner and next step, with filters, totals per
/// currency and a row action.
///
/// ```swift
/// MonarchRenewalsPanel(
///     renewals,
///     filters: [MonarchRenewalFilter(value: "30", label: "30 days"), MonarchRenewalFilter(value: "90", label: "90 days")],
///     filter: $window,
///     totals: [MonarchRenewalTotal(currencyCode: "USD", amount: 48_200, count: 6)],
///     onSelect: { renewal in path.append(renewal.id) },
///     onAction: { renewal in editing = renewal }
/// )
/// ```
///
/// The panel is a ``MonarchSectionCard``: skeleton rows while loading, an
/// empty state, and an error state with a retry. Data and callbacks come in;
/// fetching, filtering and sorting stay in the app. Amounts are
/// ``MonarchMoney`` in their own currency and are never converted or added
/// across currencies. Statuses come from the built-in renewal statuses
/// (``statuses``: open, in_progress, renewed, lost, void), overridden by
/// `registry`. An open renewal due within `urgentWithinDays`, or past, shows
/// a warning icon before its date. The view does not scroll; place it in a
/// `ScrollView` or a `List` row.
///
/// Accessibility: the title is a header. Each renewal is one element read as
/// its name, then the description, "Due soon" when urgent, the date and day
/// count, the amount, the status, the probability, the renewal note, the
/// owner and next step and the outcome; with `onSelect` it is a button. The
/// row action is a separate button named after the renewal ("Update Acme
/// support contract"). Statuses have words and icons, overdue steps have the
/// word "Overdue", so nothing depends on colour.
@_spi(Experimental) public struct MonarchRenewalsPanel<Actions: View>: View {
    private let items: [MonarchRenewalItem]
    private let title: String?
    private let description: String?
    private let filters: [MonarchRenewalFilter]
    private let filter: Binding<String>?
    private let totals: [MonarchRenewalTotal]
    private let unpricedCount: Int
    private let locale: Locale?
    private let now: Date?
    private let calendar: Calendar?
    private let urgentWithinDays: Int
    private let registry: MonarchStatusRegistry?
    private let isLoading: Bool
    private let errorMessage: String?
    private let isRetrying: Bool
    private let labels: MonarchRenewalsPanelLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchRenewalItem) -> Void)?
    private let onAction: (@MainActor (MonarchRenewalItem) -> Void)?
    private let canAct: (@MainActor (MonarchRenewalItem) -> Bool)?
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.locale) private var environmentLocale
    @Environment(\.calendar) private var environmentCalendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.monarchStatusRegistry) private var installedStatuses

    private typealias Tokens = MonarchRenewalsPanelTokens

    /// The built-in renewal statuses: open, in_progress, renewed, lost and
    /// void, with localised labels, tones and icons. Extend or replace them
    /// with the panel's `registry`.
    nonisolated public static var statuses: MonarchStatusRegistry { MonarchRenewalsPanelTokens.statuses }

    /// A renewals panel with header actions.
    ///
    /// - Parameters:
    ///   - items: The renewals in view, already filtered and sorted by the app.
    ///   - title: Replaces the built-in title "Renewals".
    ///   - description: A line under the title, such as when renewals appear and close.
    ///   - filters: Filter options, such as time windows. Shown as a segmented picker, or a menu when they do not fit.
    ///   - filter: The selected filter value. The app filters `items` when it changes.
    ///   - totals: The totals per currency, shown above the list.
    ///   - unpricedCount: How many renewals have no price, shown beside the totals.
    ///   - locale: The locale of amounts and dates. Default: the environment's.
    ///   - now: The reference time for day counts. Default: the current time.
    ///   - calendar: The calendar and time zone of dates and day counts. Default: the environment's.
    ///   - urgentWithinDays: Open renewals this many days away or fewer are marked urgent.
    ///   - registry: Statuses that add to or replace the built-in renewal statuses.
    ///   - isLoading: Shows skeleton rows.
    ///   - errorMessage: Shows the error state with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the error state.
    ///   - onSelect: Opens a renewal. Makes each row a button.
    ///   - onAction: The row action, such as opening an update form. Shows a button on each row.
    ///   - canAct: Hides the row action for some renewals, such as closed ones.
    ///   - actions: Buttons or a menu at the trailing edge of the header.
    public init(
        _ items: [MonarchRenewalItem],
        title: String? = nil,
        description: String? = nil,
        filters: [MonarchRenewalFilter] = [],
        filter: Binding<String>? = nil,
        totals: [MonarchRenewalTotal] = [],
        unpricedCount: Int = 0,
        locale: Locale? = nil,
        now: Date? = nil,
        calendar: Calendar? = nil,
        urgentWithinDays: Int = 14,
        registry: MonarchStatusRegistry? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchRenewalsPanelLabels = MonarchRenewalsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchRenewalItem) -> Void)? = nil,
        onAction: (@MainActor (MonarchRenewalItem) -> Void)? = nil,
        canAct: (@MainActor (MonarchRenewalItem) -> Bool)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.items = items
        self.title = title
        self.description = description
        self.filters = filters
        self.filter = filter
        self.totals = totals
        self.unpricedCount = unpricedCount
        self.locale = locale
        self.now = now
        self.calendar = calendar
        self.urgentWithinDays = urgentWithinDays
        self.registry = registry
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.isRetrying = isRetrying
        self.labels = labels
        self.onRetry = onRetry
        self.onSelect = onSelect
        self.onAction = onAction
        self.canAct = canAct
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        let texts = Tokens.texts(labels)
        MonarchSectionCard(
            title ?? texts.title,
            description: description,
            state: Tokens.cardState(isLoading: isLoading, errorMessage: errorMessage, isEmpty: items.isEmpty),
            emptyMessage: texts.emptyMessage,
            errorMessage: errorMessage,
            isRetrying: isRetrying,
            labels: MonarchSectionCardLabels(
                loading: texts.loading,
                emptyTitle: texts.emptyTitle,
                errorTitle: texts.errorTitle
            ),
            onRetry: onRetry,
            content: { content(texts) },
            actions: { actions }
        )
    }

    private var isCompactWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .compact
        #else
            false
        #endif
    }

    private var resolvedLocale: Locale { locale ?? environmentLocale }

    private var resolvedCalendar: Calendar {
        if let calendar { return calendar }
        var calendar = environmentCalendar
        calendar.timeZone = timeZone
        return calendar
    }

    @ViewBuilder private func content(_ texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            if let filter, !filters.isEmpty {
                filterPicker(filter, texts: texts)
            }
            if Tokens.showsTotals(totals, unpricedCount: unpricedCount) {
                totalsView(texts)
            }
            list(texts)
        }
    }

    private func filterPicker(_ selection: Binding<String>, texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        MonarchSegmentedControl(
            texts.filters,
            selection: selection,
            items: filters.map {
                MonarchSegmentedItem(Tokens.filterLabel($0, locale: resolvedLocale), value: $0.value)
            },
            fillsWidth: false
        )
    }

    private func totalsView(_ texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        // The totals are tiles in one row, as on the web; at the accessibility sizes they wrap one under another.
        let columns = [
            GridItem(
                .adaptive(minimum: dynamicTypeSize.isAccessibilitySize ? 10_000 : 150),
                spacing: Tokens.inlineSpacing,
                alignment: .top
            )
        ]
        return LazyVGrid(columns: columns, alignment: .leading, spacing: Tokens.inlineSpacing) {
            ForEach(totals) { total in
                tile {
                    Text(texts.totalIn(total.currencyCode, total.count))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    MonarchMoney(
                        total.amount,
                        currencyCode: total.currencyCode,
                        locale: resolvedLocale,
                        textStyle: .bodyStrong
                    )
                    .minimumScaleFactor(Tokens.tileScale)
                    .lineLimit(1)
                    if let estimated = total.estimated {
                        Text(
                            texts.estimated(
                                MonarchMoney.formatted(
                                    estimated,
                                    currencyCode: total.currencyCode,
                                    locale: resolvedLocale
                                )
                            )
                        )
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
            if unpricedCount > 0 {
                tile {
                    Text(texts.unpricedCount)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    Text(unpricedCount.formatted(.number.locale(resolvedLocale)))
                        .monarchTextStyle(.bodyStrong)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                    Text(texts.unpricedHint)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(texts.totals)
    }

    private func tile(@ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            content()
        }
        .padding(Tokens.tilePadding)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(theme.colors.bgSurface, in: MonarchShapes.container)
        .overlay(MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func list(_ texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        // The table at regular width, as on the web; on a phone the stacked rows, so no column is cut off.
        if dynamicTypeSize.isAccessibilitySize || isCompactWidth {
            stackedList(texts)
        } else {
            table(texts)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    /// The renewals as the web's table: the item, the renewal date, the amount, the status and the row action.
    private func table(_ texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        let reference = now ?? Date()
        return Grid(
            alignment: .topLeading,
            horizontalSpacing: Tokens.inlineSpacing,
            verticalSpacing: Tokens.inlineSpacing
        ) {
            GridRow {
                heading(texts.columnItem)
                heading(texts.columnRenewal)
                heading(texts.columnAmount).gridColumnAlignment(.trailing)
                heading(texts.columnStatus)
                if onAction != nil { Color.clear.frame(width: 0, height: 0) }
            }
            ForEach(items, id: \.id) { item in
                let row = Tokens.row(
                    item,
                    now: reference,
                    calendar: resolvedCalendar,
                    locale: resolvedLocale,
                    urgentWithinDays: urgentWithinDays,
                    showsOwner: false,
                    texts: texts
                )
                let status = MonarchStatusBadgeTokens.resolve(
                    item.status,
                    in: [Tokens.registry(registry), installedStatuses]
                )
                MonarchDivider().gridCellUnsizedAxes(.horizontal)
                GridRow {
                    tableCell(item, texts: texts, row: row, status: status) {
                        VStack(alignment: .leading, spacing: 0) {
                            name(item)
                            if let description = item.description { caption(description) }
                            if let renewal = row.renewal { caption(renewal) }
                        }
                    }
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .firstTextBaseline, spacing: Tokens.lineSpacing) {
                            if row.isUrgent {
                                MonarchIcon(systemName: Tokens.urgentSymbol, size: .sm)
                                    .foregroundStyle(theme.colors.warningStrong)
                            }
                            Text(row.date)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(row.isUrgent ? theme.colors.warningStrong : theme.colors.textDefault)
                        }
                        if let when = row.when { caption(when) }
                    }
                    .accessibilityHidden(true)
                    amount(item, texts: texts)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                        MonarchStatusBadge(status, size: .sm)
                        if let probability = row.probability { caption(probability) }
                    }
                    .accessibilityHidden(true)
                    if let onAction, canAct?(item) ?? true {
                        MonarchButton(texts.action, size: .xs) { onAction(item) }
                            .accessibilityLabel(texts.actionFor(texts.action, item.name))
                    }
                }
            }
        }
        .fixedSize(horizontal: true, vertical: false)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(texts.list)
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityHidden(true)
    }

    /// The first cell of a row: the item, read as the whole row, and a button when rows open the renewal.
    @ViewBuilder private func tableCell(
        _ item: MonarchRenewalItem,
        texts: MonarchRenewalsPanelTokens.Texts,
        row: MonarchRenewalsPanelTokens.Row,
        status: MonarchStatusDefinition,
        @ViewBuilder content: () -> some View
    ) -> some View {
        let value = Tokens.accessibilityValue(
            item,
            row: row,
            amount: Tokens.amount(item, locale: resolvedLocale, texts: texts),
            status: status.label,
            texts: texts
        )
        if let onSelect {
            Button {
                onSelect(item)
            } label: {
                content().contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(item.name)
            .accessibilityValue(value)
            .accessibilityAddTraits(.isButton)
        } else {
            content()
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(item.name)
                .accessibilityValue(value)
        }
    }

    private func stackedList(_ texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        let showsOwner = Tokens.showsOwner(items)
        let reference = now ?? Date()
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                if index > 0 {
                    MonarchDivider()
                }
                let row = Tokens.row(
                    item,
                    now: reference,
                    calendar: resolvedCalendar,
                    locale: resolvedLocale,
                    urgentWithinDays: urgentWithinDays,
                    showsOwner: showsOwner,
                    texts: texts
                )
                VStack(alignment: .leading, spacing: Tokens.inlineSpacing) {
                    summary(item, row: row, texts: texts)
                    if let onAction, canAct?(item) ?? true {
                        MonarchButton(texts.action, size: .sm) { onAction(item) }
                            .accessibilityLabel(texts.actionFor(texts.action, item.name))
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(texts.list)
    }

    @ViewBuilder private func summary(
        _ item: MonarchRenewalItem,
        row: MonarchRenewalsPanelTokens.Row,
        texts: MonarchRenewalsPanelTokens.Texts
    ) -> some View {
        let status = MonarchStatusBadgeTokens.resolve(item.status, in: [Tokens.registry(registry), installedStatuses])
        let value = Tokens.accessibilityValue(
            item,
            row: row,
            amount: Tokens.amount(item, locale: resolvedLocale, texts: texts),
            status: status.label,
            texts: texts
        )
        if let onSelect {
            Button {
                onSelect(item)
            } label: {
                HStack(spacing: Tokens.inlineSpacing) {
                    rowContent(item, row: row, status: status, texts: texts)
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
            rowContent(item, row: row, status: status, texts: texts)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(item.name)
                .accessibilityValue(value)
        }
    }

    private func rowContent(
        _ item: MonarchRenewalItem,
        row: MonarchRenewalsPanelTokens.Row,
        status: MonarchStatusDefinition,
        texts: MonarchRenewalsPanelTokens.Texts
    ) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.inlineSpacing) {
                    name(item)
                    Spacer(minLength: 0)
                    amount(item, texts: texts)
                }
                VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                    name(item)
                    amount(item, texts: texts)
                }
            }
            if let description = item.description {
                caption(description)
            }
            if let renewal = row.renewal {
                caption(renewal)
            }
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.lineSpacing) {
                    if row.isUrgent {
                        MonarchIcon(systemName: Tokens.urgentSymbol, size: .sm)
                            .foregroundStyle(theme.colors.warningStrong)
                    }
                    Text(row.date)
                        .monarchTextStyle(.label)
                        .foregroundStyle(row.isUrgent ? theme.colors.warningStrong : theme.colors.textDefault)
                }
                if let when = row.when {
                    caption(when)
                }
            }
            MonarchFlowLayout(spacing: Tokens.inlineSpacing, alignment: .center) {
                MonarchStatusBadge(status, size: .sm)
                if let probability = row.probability {
                    caption(probability)
                }
            }
            if let outcome = item.outcome {
                caption(outcome)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
    }

    private func name(_ item: MonarchRenewalItem) -> some View {
        Text(item.name)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors.textDefault)
            .multilineTextAlignment(.leading)
    }

    @ViewBuilder private func amount(_ item: MonarchRenewalItem, texts: MonarchRenewalsPanelTokens.Texts) -> some View {
        VStack(alignment: .trailing, spacing: 0) {
            if let amount = item.amount, let code = item.currencyCode {
                MonarchMoney(amount, currencyCode: code, locale: resolvedLocale, textStyle: .bodyStrong)
            } else {
                Text(texts.unpriced)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let note = item.amountNote {
                caption(note)
            }
        }
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}

extension MonarchRenewalsPanel where Actions == EmptyView {
    /// A renewals panel.
    ///
    /// - Parameters:
    ///   - items: The renewals in view, already filtered and sorted by the app.
    ///   - title: Replaces the built-in title "Renewals".
    ///   - description: A line under the title.
    ///   - filters: Filter options, such as time windows.
    ///   - filter: The selected filter value.
    ///   - totals: The totals per currency, shown above the list.
    ///   - unpricedCount: How many renewals have no price.
    ///   - locale: The locale of amounts and dates. Default: the environment's.
    ///   - now: The reference time for day counts. Default: the current time.
    ///   - calendar: The calendar and time zone of dates and day counts. Default: the environment's.
    ///   - urgentWithinDays: Open renewals this many days away or fewer are marked urgent.
    ///   - registry: Statuses that add to or replace the built-in renewal statuses.
    ///   - isLoading: Shows skeleton rows.
    ///   - errorMessage: Shows the error state with this message.
    ///   - isRetrying: Whether a retry is in progress.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading.
    ///   - onSelect: Opens a renewal. Makes each row a button.
    ///   - onAction: The row action. Shows a button on each row.
    ///   - canAct: Hides the row action for some renewals.
    public init(
        _ items: [MonarchRenewalItem],
        title: String? = nil,
        description: String? = nil,
        filters: [MonarchRenewalFilter] = [],
        filter: Binding<String>? = nil,
        totals: [MonarchRenewalTotal] = [],
        unpricedCount: Int = 0,
        locale: Locale? = nil,
        now: Date? = nil,
        calendar: Calendar? = nil,
        urgentWithinDays: Int = 14,
        registry: MonarchStatusRegistry? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        isRetrying: Bool = false,
        labels: MonarchRenewalsPanelLabels = MonarchRenewalsPanelLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchRenewalItem) -> Void)? = nil,
        onAction: (@MainActor (MonarchRenewalItem) -> Void)? = nil,
        canAct: (@MainActor (MonarchRenewalItem) -> Bool)? = nil
    ) {
        self.init(
            items,
            title: title,
            description: description,
            filters: filters,
            filter: filter,
            totals: totals,
            unpricedCount: unpricedCount,
            locale: locale,
            now: now,
            calendar: calendar,
            urgentWithinDays: urgentWithinDays,
            registry: registry,
            isLoading: isLoading,
            errorMessage: errorMessage,
            isRetrying: isRetrying,
            labels: labels,
            onRetry: onRetry,
            onSelect: onSelect,
            onAction: onAction,
            canAct: canAct,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchRenewalsPanel") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchRenewalsPanel(
                    [
                        MonarchRenewalItem(
                            id: "1",
                            name: "Acme support contract",
                            status: "open",
                            renewalDate: now.addingTimeInterval(86_400 * 6),
                            description: "Support · Acme Inc",
                            amount: 12_000,
                            currencyCode: "USD",
                            amountNote: "per year",
                            autoRenews: false,
                            probability: 60,
                            owner: "Priya",
                            nextAction: "Send a quote",
                            isNextActionOverdue: true
                        ),
                        MonarchRenewalItem(
                            id: "2",
                            name: "example.com",
                            status: "renewed",
                            renewalDate: now.addingTimeInterval(86_400 * 40),
                            autoRenews: true,
                            outcome: "Renewed for two years"
                        ),
                    ],
                    totals: [MonarchRenewalTotal(currencyCode: "USD", amount: 12_000, count: 1)],
                    unpricedCount: 1,
                    now: now,
                    onAction: { _ in }
                )
                MonarchRenewalsPanel([], isLoading: true)
                MonarchRenewalsPanel([])
            }
        }
    }
#endif
