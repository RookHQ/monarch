import SwiftUI

/// Requests waiting on the viewer, with Approve, Send back (with a reason)
/// and Review on each, swipe actions, optional search and bulk approval.
///
/// ```swift
/// NavigationStack {
///     MonarchApprovalQueue(
///         requests,
///         isSearchable: true,
///         onApprove: { request in try await api.approve(request.id) },
///         onReject: { request, reason in try await api.sendBack(request.id, reason: reason) },
///         onReview: { request in path.append(request.id) }
///     )
///     .navigationTitle("Approvals")
/// }
/// ```
///
/// A standard `List`: swiping a request trailing approves it (a full swipe
/// approves at once) or sends it back, and swiping leading reviews it; the
/// same actions are buttons on every row. The app performs each decision
/// and removes decided requests from `items`. While an `async` handler
/// runs, the row's Approve shows progress; if it throws, the row says so.
/// Sending back opens a dialog that asks for a reason (unless
/// `rejectReason` is `.none`) and stays open until the app is done.
/// With `onApproveMany`, rows get checkboxes and a bulk action bar appears
/// at the bottom while requests are selected. Search uses `searchable`, so
/// place the queue in a `NavigationStack`.
///
/// Accessibility: the list is named "Requests waiting for you". Each
/// request is one element read as the requester, title, absolute time,
/// Past due, badges and summary; its buttons say which request they act on
/// ("Approve request from Sam Lee"). Decisions are announced. Overdue is a
/// badge with a word, not only a colour.
@_spi(Experimental) public struct MonarchApprovalQueue<Footer: View>: View {
    private let items: [MonarchApprovalQueueItem]
    private let rejectReason: MonarchApprovalQueueRejectReason
    private let isSearchable: Bool
    private let limit: Int?
    private let now: Date?
    private let labels: MonarchApprovalQueueLabels
    private let onApprove: (@MainActor (MonarchApprovalQueueItem) async throws -> Void)?
    private let onReject: (@MainActor (MonarchApprovalQueueItem, String?) async throws -> Void)?
    private let onReview: (@MainActor (MonarchApprovalQueueItem) -> Void)?
    private let onApproveMany: (@MainActor ([MonarchApprovalQueueItem]) async throws -> Void)?
    private let footer: Footer

    @State private var query = ""
    @State private var selection: Set<String> = []
    @State private var busyIDs: Set<String> = []
    @State private var failedIDs: Set<String> = []
    @State private var rejecting: MonarchApprovalQueueItem?
    @State private var confirmingReject: MonarchApprovalQueueItem?
    @State private var isConfirmingBulk = false
    @State private var isBulkBusy = false
    @State private var isBulkFailed = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.locale) private var locale

    /// An approval queue with content under the list.
    ///
    /// - Parameters:
    ///   - items: The requests, in the order to show them. Remove a request once it is decided.
    ///   - rejectReason: Whether sending back asks for a reason, and whether it is required.
    ///   - isSearchable: Whether the list has a search field that filters by requester and title.
    ///   - limit: Shows at most this many requests, for a dashboard card; put "See all" in `footer`.
    ///   - now: The reference time for relative times. The current time, refreshed every minute, when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onApprove: Approves one request. Adds Approve; the row shows progress until it returns.
    ///   - onReject: Sends one request back with the trimmed reason, or `nil`. Adds Send back.
    ///   - onReview: Opens the full request. Adds Review.
    ///   - onApproveMany: Approves the selected requests after a confirmation. Adds checkboxes.
    ///   - footer: Content after the requests, such as a "See all requests" link.
    public init(
        _ items: [MonarchApprovalQueueItem],
        rejectReason: MonarchApprovalQueueRejectReason = .required,
        isSearchable: Bool = false,
        limit: Int? = nil,
        now: Date? = nil,
        labels: MonarchApprovalQueueLabels = MonarchApprovalQueueLabels(),
        onApprove: (@MainActor (MonarchApprovalQueueItem) async throws -> Void)? = nil,
        onReject: (@MainActor (MonarchApprovalQueueItem, String?) async throws -> Void)? = nil,
        onReview: (@MainActor (MonarchApprovalQueueItem) -> Void)? = nil,
        onApproveMany: (@MainActor ([MonarchApprovalQueueItem]) async throws -> Void)? = nil,
        @ViewBuilder footer: () -> Footer
    ) {
        self.items = items
        self.rejectReason = rejectReason
        self.isSearchable = isSearchable
        self.limit = limit
        self.now = now
        self.labels = labels
        self.onApprove = onApprove
        self.onReject = onReject
        self.onReview = onReview
        self.onApproveMany = onApproveMany
        self.footer = footer()
    }

    private typealias Tokens = MonarchApprovalQueueTokens

    private var isSelectable: Bool { onApproveMany != nil }

    private var zonedCalendar: Calendar {
        var zoned = calendar
        zoned.timeZone = timeZone
        return zoned
    }

    /// The content of the view.
    public var body: some View {
        if isSearchable {
            content.monarchSearchable(text: $query, prompt: Text(labels.search))
        } else {
            content
        }
    }

    private var content: some View {
        let chosen = Tokens.chosen(items, selection: selection)
        return Group {
            if let now {
                list(now: now)
            } else {
                MonarchRelativeTimeClock(dates: items.map(\.submittedAt)) { current in
                    list(now: current)
                }
            }
        }
        .sheet(item: $rejecting) { item in
            if let onReject {
                MonarchApprovalQueueRejectSheet(
                    item: item,
                    requirement: rejectReason,
                    labels: labels,
                    onReject: onReject,
                    onDone: { finishReject(item) }
                )
                .presentationDetents([.medium, .large])
            }
        }
        .alert(
            confirmingReject.map { labels.rejectTitle($0.requester.name) } ?? "",
            isPresented: Binding(get: { confirmingReject != nil }, set: { if !$0 { confirmingReject = nil } }),
            presenting: confirmingReject
        ) { item in
            Button(labels.reject, role: .destructive) { rejectWithoutReason(item) }
            Button(labels.cancel, role: .cancel) {}
        } message: { _ in
            Text(labels.rejectDescription)
        }
        .alert(labels.bulkTitle(chosen.count), isPresented: $isConfirmingBulk) {
            Button(labels.bulkConfirm) { approveMany(chosen) }
            Button(labels.cancel, role: .cancel) {}
        } message: {
            Text(labels.bulkDescription)
        }
    }

    private func list(now: Date) -> some View {
        let visible = Tokens.visible(items, query: query, limit: limit)
        return List {
            Group {
                if isSelectable && !items.isEmpty {
                    selectAllRow(visible)
                }
                if items.isEmpty {
                    MonarchEmptyState(
                        labels.emptyTitle,
                        message: labels.emptyDescription,
                        kind: .success,
                        size: .sm
                    )
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
                } else if visible.isEmpty {
                    MonarchEmptyState(
                        labels.noMatchesTitle,
                        kind: .filtered,
                        size: .sm,
                        onClearFilters: { query = "" }
                    )
                    .frame(maxWidth: .infinity)
                    .listRowSeparator(.hidden)
                } else {
                    ForEach(visible) { item in
                        row(item, now: now)
                    }
                }
                footer
            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
        .accessibilityLabel(labels.label)
    }

    private func selectAllRow(_ visible: [MonarchApprovalQueueItem]) -> some View {
        let decidable = visible.filter(\.canDecide)
        let bindings = decidable.map { item in
            Binding(
                get: { selection.contains(item.id) },
                set: { selection = Tokens.toggled(selection, id: item.id, isOn: $0) }
            )
        }
        let chosen = Tokens.chosen(items, selection: selection)
        return VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            // Select all, the count and Approve selected sit in one row, as on the web.
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Tokens.actionSpacing) {
                    selectAllToggle(bindings, isEmpty: decidable.isEmpty)
                    selectionSummary(chosen)
                }
                VStack(alignment: .leading, spacing: Tokens.actionSpacing) {
                    selectAllToggle(bindings, isEmpty: decidable.isEmpty)
                    HStack(spacing: Tokens.actionSpacing) { selectionSummary(chosen) }
                }
            }
            if isBulkFailed {
                failure
            }
        }
    }

    private func selectAllToggle(_ bindings: [Binding<Bool>], isEmpty: Bool) -> some View {
        Toggle(labels.selectAll, sources: bindings, isOn: \.self)
            .toggleStyle(.monarchCheckbox)
            .disabled(isEmpty)
            .fixedSize()
    }

    @ViewBuilder private func selectionSummary(_ chosen: [MonarchApprovalQueueItem]) -> some View {
        Text(MonarchBulkActionBarStrings.selected(chosen.count, total: nil))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        Spacer(minLength: 0)
        MonarchButton(
            labels.approveSelected,
            systemImage: Tokens.approveSymbol,
            variant: .success,
            size: .sm,
            isLoading: isBulkBusy
        ) {
            isConfirmingBulk = true
        }
        .disabled(chosen.isEmpty)
    }

    private func row(_ item: MonarchApprovalQueueItem, now: Date) -> some View {
        let calendar = zonedCalendar
        let isBusy = busyIDs.contains(item.id)
        let isFailed = failedIDs.contains(item.id)
        let name = item.requester.name
        let canApprove = onApprove != nil && item.canDecide
        let canReject = onReject != nil && item.canDecide
        return HStack(alignment: .top, spacing: Tokens.columnSpacing) {
            if isSelectable {
                Toggle(
                    labels.selectItem(name),
                    isOn: Binding(
                        get: { selection.contains(item.id) },
                        set: { selection = Tokens.toggled(selection, id: item.id, isOn: $0) }
                    )
                )
                .labelsHidden()
                .toggleStyle(.monarchCheckbox)
                .disabled(!item.canDecide)
            }
            MonarchAvatar(
                url: item.requester.avatarURL,
                name: name,
                seed: item.requester.seed,
                size: .sm,
                isDecorative: true
            )
            VStack(alignment: .leading, spacing: Tokens.actionSpacing) {
                summary(item, now: now, calendar: calendar, isFailed: isFailed)
                if onReview != nil || canApprove || canReject {
                    MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                        if let onReview {
                            MonarchButton(labels.review, systemImage: Tokens.reviewSymbol, variant: .ghost, size: .sm) {
                                onReview(item)
                            }
                            .accessibilityLabel(labels.reviewItem(name))
                        }
                        if canReject {
                            MonarchButton(labels.reject, systemImage: Tokens.rejectSymbol, size: .sm) {
                                startReject(item)
                            }
                            .disabled(isBusy)
                            .accessibilityLabel(labels.rejectItem(name))
                        }
                        if canApprove {
                            MonarchButton(
                                labels.approve,
                                systemImage: Tokens.approveSymbol,
                                variant: .success,
                                size: .sm,
                                isLoading: isBusy
                            ) {
                                approve(item)
                            }
                            .accessibilityLabel(labels.approveItem(name))
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, Tokens.rowVerticalPadding)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            if canApprove && !isBusy {
                Button(labels.approve, systemImage: Tokens.approveSymbol) { approve(item) }
                    .tint(theme.colors[role: Tokens.approveTint])
            }
            if canReject && !isBusy {
                Button(labels.reject, systemImage: Tokens.rejectSymbol) { startReject(item) }
                    .tint(theme.colors[role: Tokens.rejectTint])
            }
        }
        .swipeActions(edge: .leading) {
            if let onReview {
                Button(labels.review, systemImage: Tokens.reviewSymbol) { onReview(item) }
                    .tint(theme.colors[role: Tokens.reviewTint])
            }
        }
    }

    private func summary(_ item: MonarchApprovalQueueItem, now: Date, calendar: Calendar, isFailed: Bool) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            Text(item.requester.name)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            if let detail = item.requesterDetail, !detail.isEmpty {
                caption(detail)
            }
            MonarchFlowLayout(spacing: Tokens.lineSpacing) {
                Text(item.title)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                if let submittedAt = item.submittedAt {
                    caption(Tokens.metaDot)
                    caption(Tokens.relativeTime(submittedAt, now: now, calendar: calendar, locale: locale))
                }
                if item.isOverdue {
                    MonarchBadge(labels.overdue, tone: .danger)
                }
                ForEach(Array(item.badges.enumerated()), id: \.offset) { _, badge in
                    MonarchBadge(badge.label, tone: badge.tone, systemImage: badge.systemImage)
                }
            }
            if let summary = item.summary, !summary.isEmpty {
                caption(summary)
            }
            if isFailed {
                failure
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Tokens.accessibilityLabel(
                item,
                time: item.submittedAt.map { Tokens.absoluteTime($0, calendar: calendar, locale: locale) },
                isFailed: isFailed,
                labels: labels
            )
        )
    }

    private var failure: some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.lineSpacing) {
            MonarchIcon(systemName: Tokens.failedSymbol, size: .sm)
            Text(labels.failed).monarchTextStyle(.caption)
        }
        .foregroundStyle(theme.colors.dangerStrong)
        .accessibilityElement(children: .combine)
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private func approve(_ item: MonarchApprovalQueueItem) {
        guard let onApprove, !busyIDs.contains(item.id) else { return }
        busyIDs.insert(item.id)
        failedIDs.remove(item.id)
        Task {
            do {
                try await onApprove(item)
                selection.remove(item.id)
                AccessibilityNotification.Announcement(labels.approved(item.requester.name)).post()
            } catch {
                failedIDs.insert(item.id)
                AccessibilityNotification.Announcement(labels.failed).post()
            }
            busyIDs.remove(item.id)
        }
    }

    private func startReject(_ item: MonarchApprovalQueueItem) {
        failedIDs.remove(item.id)
        if rejectReason == .none {
            confirmingReject = item
        } else {
            rejecting = item
        }
    }

    private func finishReject(_ item: MonarchApprovalQueueItem) {
        selection.remove(item.id)
        AccessibilityNotification.Announcement(labels.rejected(item.requester.name)).post()
    }

    private func rejectWithoutReason(_ item: MonarchApprovalQueueItem) {
        guard let onReject else { return }
        busyIDs.insert(item.id)
        Task {
            do {
                try await onReject(item, nil)
                finishReject(item)
            } catch {
                failedIDs.insert(item.id)
                AccessibilityNotification.Announcement(labels.failed).post()
            }
            busyIDs.remove(item.id)
        }
    }

    private func approveMany(_ chosen: [MonarchApprovalQueueItem]) {
        guard let onApproveMany, !chosen.isEmpty else { return }
        isBulkBusy = true
        isBulkFailed = false
        Task {
            do {
                try await onApproveMany(chosen)
                selection = []
                AccessibilityNotification.Announcement(labels.bulkApproved(chosen.count)).post()
            } catch {
                isBulkFailed = true
                AccessibilityNotification.Announcement(labels.failed).post()
            }
            isBulkBusy = false
        }
    }
}

extension MonarchApprovalQueue where Footer == EmptyView {
    /// An approval queue.
    ///
    /// - Parameters:
    ///   - items: The requests, in the order to show them. Remove a request once it is decided.
    ///   - rejectReason: Whether sending back asks for a reason, and whether it is required.
    ///   - isSearchable: Whether the list has a search field that filters by requester and title.
    ///   - limit: Shows at most this many requests.
    ///   - now: The reference time for relative times. The current time, refreshed every minute, when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onApprove: Approves one request. Adds Approve; the row shows progress until it returns.
    ///   - onReject: Sends one request back with the trimmed reason, or `nil`. Adds Send back.
    ///   - onReview: Opens the full request. Adds Review.
    ///   - onApproveMany: Approves the selected requests after a confirmation. Adds checkboxes.
    public init(
        _ items: [MonarchApprovalQueueItem],
        rejectReason: MonarchApprovalQueueRejectReason = .required,
        isSearchable: Bool = false,
        limit: Int? = nil,
        now: Date? = nil,
        labels: MonarchApprovalQueueLabels = MonarchApprovalQueueLabels(),
        onApprove: (@MainActor (MonarchApprovalQueueItem) async throws -> Void)? = nil,
        onReject: (@MainActor (MonarchApprovalQueueItem, String?) async throws -> Void)? = nil,
        onReview: (@MainActor (MonarchApprovalQueueItem) -> Void)? = nil,
        onApproveMany: (@MainActor ([MonarchApprovalQueueItem]) async throws -> Void)? = nil
    ) {
        self.init(
            items,
            rejectReason: rejectReason,
            isSearchable: isSearchable,
            limit: limit,
            now: now,
            labels: labels,
            onApprove: onApprove,
            onReject: onReject,
            onReview: onReview,
            onApproveMany: onApproveMany,
            footer: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchApprovalQueue") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            MonarchApprovalQueue(
                [
                    MonarchApprovalQueueItem(
                        id: "1",
                        requester: MonarchApprovalPerson("Alex Morgan"),
                        requesterDetail: "Design",
                        title: "Expense claim",
                        summary: "3 receipts, travel",
                        submittedAt: now.addingTimeInterval(-86_400 * 2),
                        isOverdue: true
                    ),
                    MonarchApprovalQueueItem(
                        id: "2",
                        requester: MonarchApprovalPerson("Sam Lee"),
                        title: "Leave, 2 days",
                        submittedAt: now.addingTimeInterval(-3_600)
                    ),
                ],
                now: now,
                onApprove: { _ in },
                onReject: { _, _ in },
                onReview: { _ in },
                onApproveMany: { _ in }
            )
            .frame(height: 480)
        }
    }
#endif
