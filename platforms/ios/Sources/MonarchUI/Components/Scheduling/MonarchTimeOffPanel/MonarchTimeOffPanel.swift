import MonarchTokens
import SwiftUI

/// Lists time off (holidays, sickness, closures) and adds or removes it: an
/// Include past checkbox, an add form in a sheet (who, type, all day or
/// timed, dates, reason) and a destructive confirmation to remove. Data in,
/// callbacks out; the app saves.
///
/// ```swift
/// MonarchTimeOffPanel(
///     entries: timeOff,
///     timeZone: workspace.timeZone,
///     people: staff.map { MonarchTimeOffOption($0.name, value: $0.id) },
///     allowsClosure: true,
///     onAdd: { draft in try await api.addTimeOff(draft) },
///     onRemove: { entry in try await api.removeTimeOff(entry.id) }
/// )
/// ```
///
/// The panel is a column of rows, so it sits in a scroll view or a form
/// section with other content. Dates are shown and entered in `timeZone`;
/// all-day entries hold the first moment of their first and last day there.
/// While `onAdd` runs the form shows progress and cannot be dismissed; if it
/// throws, the form stays open with the error's description. Removing asks
/// first with the system confirmation dialog; while `onRemove` runs the
/// row's button shows progress, and a failure is shown above the list.
///
/// Accessibility: the title is a header. Each entry is one element read as
/// "Ana Lima, Annual leave, Pending, Mon, Nov 9, 2026 (all day) · Conference",
/// with nothing conveyed by badge colour alone; its remove button is named
/// "Remove time off for Ana Lima, …" and is also a VoiceOver action of the
/// entry. Failures are announced.
@_spi(Experimental) public struct MonarchTimeOffPanel: View {
    private let entries: [MonarchTimeOffEntry]
    private let timeZone: TimeZone?
    private let now: Date
    private let people: [MonarchTimeOffOption]
    private let allowsClosure: Bool
    private let types: [MonarchTimeOffOption]
    private let showsPastBinding: Binding<Bool>?
    private let isLoading: Bool
    private let errorMessage: String?
    private let notice: String?
    private let labels: MonarchTimeOffPanelLabels
    private let canRemove: ((MonarchTimeOffEntry) -> Bool)?
    private let onRetry: (@MainActor () -> Void)?
    private let onAdd: (@MainActor (MonarchTimeOffDraft) async throws -> Void)?
    private let onRemove: (@MainActor (MonarchTimeOffEntry) async throws -> Void)?

    @State private var internalShowsPast = false
    @State private var isAdding = false
    @State private var toRemove: MonarchTimeOffEntry?
    @State private var removingID: String?
    @State private var removeFailure: String?
    @AccessibilityFocusState private var focus: Tokens.FocusTarget?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var environmentCalendar
    @Environment(\.locale) private var locale

    /// A time-off panel.
    ///
    /// - Parameters:
    ///   - entries: The periods to list, in display order.
    ///   - timeZone: The time zone entries are shown and entered in. `nil` is the device's zone
    ///     (`TimeZone.autoupdatingCurrent`), read each time.
    ///   - now: Now, to mark and hide past entries. Pass it to keep the list stable.
    ///   - people: The people the add form offers. One person hides Who.
    ///   - allowsClosure: Whether Who offers Everyone, a closure for everyone.
    ///   - types: The kinds of time off the add form offers, such as leave types.
    ///   - showsPast: Whether past entries are listed. `nil` keeps it inside the panel, off at first.
    ///   - isLoading: Shows placeholder rows instead of the list.
    ///   - errorMessage: Shows a danger banner instead of the list.
    ///   - notice: A notice above the list, such as "2 appointments fall in this period".
    ///   - labels: Replacements for the built-in strings.
    ///   - canRemove: Whether an entry can be removed. By default every entry, when `onRemove` is set.
    ///   - onRetry: Adds Try again to the error banner.
    ///   - onAdd: Adds time off from the form; shows the add button. Throw to keep the form open with the error.
    ///   - onRemove: Removes an entry after a confirmation. Throw to show the error.
    public init(
        entries: [MonarchTimeOffEntry],
        timeZone: TimeZone? = nil,
        now: Date = Date(),
        people: [MonarchTimeOffOption] = [],
        allowsClosure: Bool = false,
        types: [MonarchTimeOffOption] = [],
        showsPast: Binding<Bool>? = nil,
        isLoading: Bool = false,
        errorMessage: String? = nil,
        notice: String? = nil,
        labels: MonarchTimeOffPanelLabels = MonarchTimeOffPanelLabels(),
        canRemove: ((MonarchTimeOffEntry) -> Bool)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onAdd: (@MainActor (MonarchTimeOffDraft) async throws -> Void)? = nil,
        onRemove: (@MainActor (MonarchTimeOffEntry) async throws -> Void)? = nil
    ) {
        self.entries = entries
        self.timeZone = timeZone
        self.now = now
        self.people = people
        self.allowsClosure = allowsClosure
        self.types = types
        self.showsPastBinding = showsPast
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.notice = notice
        self.labels = labels
        self.canRemove = canRemove
        self.onRetry = onRetry
        self.onAdd = onAdd
        self.onRemove = onRemove
    }

    private typealias Tokens = MonarchTimeOffPanelTokens

    /// The zone entries are shown in: `timeZone`, else the device's, read each time.
    var resolvedTimeZone: TimeZone { timeZone ?? .autoupdatingCurrent }

    private var showsPast: Binding<Bool> { showsPastBinding ?? $internalShowsPast }

    /// The calendar in the panel's time zone.
    private var calendar: Calendar {
        var calendar = environmentCalendar
        calendar.timeZone = resolvedTimeZone
        return calendar
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(theme.spacing.space4)
            if let notice, !notice.isEmpty {
                MonarchBanner(message: notice, tone: .info)
                    .padding([.horizontal, .bottom], theme.spacing.space4)
            }
            MonarchDivider()
            HStack {
                Spacer(minLength: 0)
                Toggle(labels.includePast ?? MonarchTimeOffPanelStrings.includePast, isOn: showsPast)
                    .toggleStyle(.monarchCheckbox)
                    .fixedSize()
            }
            .padding(.horizontal, theme.spacing.space4)
            .padding(.vertical, theme.spacing.space2)
            MonarchDivider()
            if let removeFailure {
                MonarchBanner(message: removeFailure, tone: .danger)
                    .padding(theme.spacing.space4)
            }
            list
                .padding(.horizontal, theme.spacing.space4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .clipShape(MonarchShapes.container)
        .overlay(MonarchShapes.container.stroke(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
        .sheet(isPresented: $isAdding) {
            if let onAdd {
                MonarchTimeOffAddForm(
                    people: people,
                    allowsClosure: allowsClosure,
                    types: types,
                    now: now,
                    timeZone: resolvedTimeZone,
                    labels: labels,
                    onAdd: onAdd
                )
            }
        }
        .confirmationDialog(
            labels.removeTitle ?? MonarchTimeOffPanelStrings.removeTitle,
            isPresented: Binding(get: { toRemove != nil }, set: { if !$0 { toRemove = nil } }),
            titleVisibility: .visible,
            presenting: toRemove
        ) { entry in
            Button(labels.removeConfirm ?? MonarchTimeOffPanelStrings.removeConfirm, role: .destructive) {
                remove(entry)
            }
            Button(labels.cancel ?? MonarchTimeOffPanelStrings.cancel, role: .cancel) {}
        } message: { entry in
            let who = Tokens.who(entry, labels: labels)
            Text(Tokens.removeMessage(who: who, when: when(entry), labels: labels))
        }
        .onChange(of: removeFailure) { _, failure in
            if let failure { AccessibilityNotification.Announcement(failure).post() }
        }
    }

    // MARK: Parts

    private var header: some View {
        HStack(alignment: .top, spacing: Tokens.rowSpacing) {
            heading
            Spacer(minLength: 0)
            addButton
        }
    }

    private var heading: some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            Text(labels.title ?? MonarchTimeOffPanelStrings.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: Tokens.textRole])
                .accessibilityAddTraits(.isHeader)
                .accessibilityFocused($focus, equals: .heading)
            Text(labels.message ?? MonarchTimeOffPanelStrings.message)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder private var addButton: some View {
        if onAdd != nil {
            MonarchButton(
                labels.add ?? MonarchTimeOffPanelStrings.add,
                systemImage: Tokens.addSymbol,
                variant: .primary,
                size: .sm
            ) {
                isAdding = true
            }
        }
    }

    @ViewBuilder private var list: some View {
        let visible = Tokens.visible(entries, showsPast: showsPast.wrappedValue, now: now, calendar: calendar)
        if isLoading {
            MonarchLoadingState(
                labels.loading ?? MonarchTimeOffPanelStrings.loading,
                variant: .rows,
                rows: Tokens.loadingRows
            )
        } else if let errorMessage {
            MonarchBanner(
                message: errorMessage,
                tone: .danger,
                actionTitle: onRetry == nil ? nil : labels.retry ?? MonarchTimeOffPanelStrings.retry,
                action: onRetry
            )
        } else if visible.isEmpty {
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchTimeOffPanelStrings.emptyTitle,
                message: labels.emptyMessage ?? MonarchTimeOffPanelStrings.emptyMessage,
                size: .sm
            )
            .frame(maxWidth: .infinity)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(visible.enumerated()), id: \.element.id) { index, entry in
                    if index > 0 { MonarchDivider() }
                    row(entry)
                }
            }
        }
    }

    private func when(_ entry: MonarchTimeOffEntry) -> String {
        Tokens.when(entry, labels: labels, locale: locale, calendar: calendar)
    }

    private func row(_ entry: MonarchTimeOffEntry) -> some View {
        let who = Tokens.who(entry, labels: labels)
        let when = when(entry)
        let badges = Tokens.badges(entry, isPast: Tokens.isPast(entry, now: now, calendar: calendar), labels: labels)
        let detail = Tokens.detail(when, reason: entry.reason)
        let removable = Tokens.isRemovable(entry, hasRemove: onRemove != nil, canRemove: canRemove)
        let removeLabel = Tokens.removeLabel(who: who, when: when, labels: labels)
        return HStack(alignment: .top, spacing: Tokens.rowSpacing) {
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                MonarchFlowLayout(spacing: Tokens.lineSpacing, rowSpacing: Tokens.lineSpacing) {
                    if !who.isEmpty {
                        Text(who)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors[role: Tokens.textRole])
                    }
                    ForEach(badges, id: \.self) { badge in
                        MonarchBadge(badge.text, tone: badge.tone, variant: badge.variant)
                    }
                }
                Text(detail)
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.summary(who: who, badges: badges, detail: detail))
            .accessibilityFocused($focus, equals: .row(entry.id))
            .accessibilityActions {
                if removable, removingID != entry.id {
                    Button(removeLabel) { toRemove = entry }
                }
            }
            if removable {
                removeButton(entry, label: removeLabel)
            }
        }
        .padding(.vertical, Tokens.rowPadding)
    }

    @ViewBuilder private func removeButton(_ entry: MonarchTimeOffEntry, label: String) -> some View {
        if removingID == entry.id {
            ProgressView()
                .monarchMinimumHitTarget()
                .accessibilityLabel(label)
        } else {
            Button {
                toRemove = entry
            } label: {
                MonarchIcon(systemName: Tokens.removeSymbol, label: label, size: .sm)
                    .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .disabled(removingID != nil)
        }
    }

    private func remove(_ entry: MonarchTimeOffEntry) {
        guard let onRemove else { return }
        removeFailure = nil
        removingID = entry.id
        // Decided before the app removes the entry: the row that takes its place, else the heading.
        let next = Tokens.focusAfterRemoval(
            removing: entry.id,
            visibleIDs: Tokens.visible(entries, showsPast: showsPast.wrappedValue, now: now, calendar: calendar).map(
                \.id
            )
        )
        Task {
            do {
                try await onRemove(entry)
                focus = next
            } catch {
                removeFailure = Tokens.failure(error, labels: labels)
            }
            removingID = nil
        }
    }
}

#if DEBUG
    #Preview("MonarchTimeOffPanel") {
        let now = Date()
        MonarchPreviewMatrix {
            MonarchTimeOffPanel(
                entries: [
                    MonarchTimeOffEntry(
                        id: "1",
                        personID: "ana",
                        personName: "Ana Lima",
                        isAllDay: true,
                        start: now,
                        end: Calendar.current.date(byAdding: .day, value: 4, to: now) ?? now,
                        type: "Annual leave",
                        reason: "Family visit",
                        status: "Pending"
                    ),
                    MonarchTimeOffEntry(id: "2", personID: nil, isAllDay: true, start: now, end: now),
                ],
                now: now,
                people: [MonarchTimeOffOption("Ana Lima", value: "ana")],
                allowsClosure: true,
                onAdd: { _ in },
                onRemove: { _ in }
            )
            MonarchTimeOffPanel(entries: [], isLoading: true)
        }
    }
#endif
