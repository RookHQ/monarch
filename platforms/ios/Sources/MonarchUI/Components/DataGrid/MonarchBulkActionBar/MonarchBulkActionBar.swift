import SwiftUI

/// The bar shown while items are selected: the count, the actions that
/// apply to the selection with a More menu for the rest, and Clear selection.
///
/// ```swift
/// List(invoices, selection: $selection) { InvoiceRow(invoice: $0) }
///     .safeAreaInset(edge: .bottom) {
///         MonarchBulkActionBar(
///             count: selection.count,
///             totalCount: invoices.count,
///             actions: [
///                 MonarchBulkAction(id: "archive", title: "Archive", systemImage: "archivebox") { archive() },
///                 MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) {
///                     isConfirming = true
///                 },
///             ]
///         ) {
///             selection.removeAll()
///         }
///     }
/// ```
///
/// The app owns the selection (`List(selection:)` with `EditMode` on iOS)
/// and runs the actions. The bar takes no room while nothing is selected.
/// Put it in `safeAreaInset(edge: .bottom)` to keep it above the list, or
/// inline above a table.
///
/// Accessibility: the bar is a container named "Bulk actions"; the count is
/// read first, then each action as a button. Every change of the count is
/// announced ("3 of 120 selected", then "Selection cleared"), including
/// when the bar first appears.
public struct MonarchBulkActionBar<Accessories: View>: View {
    private let count: Int
    private let totalCount: Int?
    private let actions: [MonarchBulkAction]
    private let maxVisibleActions: Int
    private let isBusy: Bool
    private let busyActionID: String?
    private let labels: MonarchBulkActionBarLabels
    private let onClear: @MainActor () -> Void
    private let accessories: Accessories

    @Environment(\.monarchTheme) private var theme

    /// A bulk action bar with extra controls after the actions.
    ///
    /// - Parameters:
    ///   - count: The number of selected items. The bar is shown while it is above zero.
    ///   - totalCount: The number of items that could be selected, for "3 of 120 selected".
    ///   - actions: The actions. Those after `maxVisibleActions` go into a More menu.
    ///   - maxVisibleActions: How many actions are buttons before the More menu.
    ///   - isBusy: Whether an action is running in the app: every action is blocked.
    ///   - busyActionID: The action that is running: it shows progress and the others are blocked.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClear: Empties the selection.
    ///   - accessories: Extra controls after the actions, such as a property picker.
    public init(
        count: Int,
        totalCount: Int? = nil,
        actions: [MonarchBulkAction] = [],
        maxVisibleActions: Int = 3,
        isBusy: Bool = false,
        busyActionID: String? = nil,
        labels: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
        onClear: @escaping @MainActor () -> Void,
        @ViewBuilder accessories: () -> Accessories
    ) {
        self.count = count
        self.totalCount = totalCount
        self.actions = actions
        self.maxVisibleActions = maxVisibleActions
        self.isBusy = isBusy
        self.busyActionID = busyActionID
        self.labels = labels
        self.onClear = onClear
        self.accessories = accessories()
    }

    /// The content of the view.
    public var body: some View {
        ZStack {
            if MonarchBulkActionBarTokens.isOpen(count: count) {
                bar
            }
        }
        .onChange(of: count) { old, new in
            if let message = MonarchBulkActionBarTokens.announcement(
                from: old,
                to: new,
                total: totalCount,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private var bar: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchBulkActionBarTokens.spacing) {
                countText
                actionButtons
                Spacer(minLength: 0)
                clearButton
            }
            VStack(alignment: .leading, spacing: MonarchBulkActionBarTokens.spacing) {
                HStack(spacing: MonarchBulkActionBarTokens.spacing) {
                    countText
                    Spacer(minLength: 0)
                    clearButton
                }
                MonarchFlowLayout(spacing: MonarchBulkActionBarTokens.spacing) {
                    actionButtons
                }
            }
        }
        .padding(MonarchBulkActionBarTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchElevation(.presented, fill: MonarchBulkActionBarTokens.background, in: MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchBulkActionBarTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.bar ?? MonarchBulkActionBarStrings.bar)
    }

    private var countText: some View {
        Text(MonarchBulkActionBarTokens.countText(count: count, total: totalCount, labels: labels))
            .monarchTextStyle(.label, weight: MonarchBulkActionBarTokens.countWeight)
            .monospacedDigit()
            .foregroundStyle(theme.colors[role: MonarchBulkActionBarTokens.countColor])
    }

    @ViewBuilder private var actionButtons: some View {
        let split = MonarchBulkActionBarTokens.split(actions, maxVisible: maxVisibleActions)
        ForEach(split.visible) { action in
            MonarchButton(
                action.title,
                systemImage: action.systemImage,
                variant: MonarchBulkActionBarTokens.variant(action),
                size: .sm,
                isLoading: busyActionID == action.id,
                action: action.action
            )
            .disabled(MonarchBulkActionBarTokens.isDisabled(action, isBusy: isBusy, busyActionID: busyActionID))
        }
        if !split.overflow.isEmpty {
            Menu {
                ForEach(split.overflow) { action in
                    Button(role: action.isDestructive ? .destructive : nil, action: action.action) {
                        if let systemImage = action.systemImage {
                            Label(action.title, systemImage: systemImage)
                        } else {
                            Text(action.title)
                        }
                    }
                    .disabled(
                        MonarchBulkActionBarTokens.isDisabled(action, isBusy: isBusy, busyActionID: busyActionID)
                    )
                }
            } label: {
                HStack(spacing: MonarchBulkActionBarTokens.spacing) {
                    Text(labels.more ?? MonarchBulkActionBarStrings.more)
                    MonarchIcon(systemName: MonarchBulkActionBarTokens.moreSymbol, size: .sm)
                }
            }
            .menuStyle(.button)
            .buttonStyle(.monarch(.secondary, size: .sm))
            .disabled(MonarchBulkActionBarTokens.isBlocked(isBusy: isBusy, busyActionID: busyActionID))
        }
        accessories
    }

    private var clearButton: some View {
        MonarchButton(
            labels.clear ?? MonarchBulkActionBarStrings.clear,
            systemImage: MonarchBulkActionBarTokens.clearSymbol,
            variant: .ghost,
            size: .sm,
            action: onClear
        )
    }
}

extension MonarchBulkActionBar where Accessories == EmptyView {
    /// A bulk action bar.
    ///
    /// - Parameters:
    ///   - count: The number of selected items. The bar is shown while it is above zero.
    ///   - totalCount: The number of items that could be selected, for "3 of 120 selected".
    ///   - actions: The actions. Those after `maxVisibleActions` go into a More menu.
    ///   - maxVisibleActions: How many actions are buttons before the More menu.
    ///   - isBusy: Whether an action is running in the app: every action is blocked.
    ///   - busyActionID: The action that is running: it shows progress and the others are blocked.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClear: Empties the selection.
    public init(
        count: Int,
        totalCount: Int? = nil,
        actions: [MonarchBulkAction] = [],
        maxVisibleActions: Int = 3,
        isBusy: Bool = false,
        busyActionID: String? = nil,
        labels: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
        onClear: @escaping @MainActor () -> Void
    ) {
        self.init(
            count: count,
            totalCount: totalCount,
            actions: actions,
            maxVisibleActions: maxVisibleActions,
            isBusy: isBusy,
            busyActionID: busyActionID,
            labels: labels,
            onClear: onClear,
            accessories: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchBulkActionBar") {
        MonarchPreviewMatrix {
            VStack {
                MonarchBulkActionBar(
                    count: 3,
                    totalCount: 120,
                    actions: [
                        MonarchBulkAction(id: "archive", title: "Archive", systemImage: "archivebox") {},
                        MonarchBulkAction(id: "tag", title: "Add tag", systemImage: "tag") {},
                        MonarchBulkAction(id: "export", title: "Export") {},
                        MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) {},
                    ],
                    onClear: {}
                )
                MonarchBulkActionBar(
                    count: 1,
                    actions: [MonarchBulkAction(id: "archive", title: "Archive") {}],
                    busyActionID: "archive",
                    onClear: {}
                )
            }
        }
    }
#endif
