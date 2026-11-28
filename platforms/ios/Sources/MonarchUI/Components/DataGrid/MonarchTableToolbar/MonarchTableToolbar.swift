import SwiftUI

/// The bar above a table or list: the filters, the result count and the
/// app's controls (a view switcher, a column menu, actions). While rows are
/// selected the filters give way to a ``MonarchBulkActionBar`` with the count,
/// the bulk actions and Clear selection.
///
/// ```swift
/// NavigationStack {
///     List(filtered, selection: $selection) { ContactRow(contact: $0) }
///         .searchable(text: $query, prompt: "Search contacts")
///         .safeAreaInset(edge: .top) {
///             MonarchTableToolbar(
///                 resultCount: filtered.count,
///                 selectedCount: selection.count,
///                 totalCount: contacts.count,
///                 bulkActions: [MonarchBulkAction(id: "archive", title: "Archive") { archive() }],
///                 onClearSelection: { selection.removeAll() },
///                 filters: { FilterChips(filters: $filters) },
///                 controls: {
///                     Picker("View", selection: $mode) { … }.pickerStyle(.segmented)
///                     Menu("Columns") { … }
///                 }
///             )
///             .padding(.horizontal)
///         }
/// }
/// ```
///
/// Search is the system's: the app adds `searchable(text:placement:prompt:)`
/// to the list, which puts the field in the navigation bar. The view
/// switcher is a segmented `Picker` and the column menu a `Menu` of `Toggle`
/// items; both go in `controls`.
///
/// Accessibility: the bar is a container named "Table controls". A change of
/// the result count is announced ("12 results"). The bulk selection bar
/// announces its own count.
public struct MonarchTableToolbar<Filters: View, Controls: View>: View {
    private let resultCount: Int?
    private let selectedCount: Int
    private let totalCount: Int?
    private let bulkActions: [MonarchBulkAction]
    private let labels: MonarchTableToolbarLabels
    private let onClearSelection: (@MainActor () -> Void)?
    private let filters: Filters
    private let controls: Controls

    @Environment(\.monarchTheme) private var theme

    /// A table toolbar with filters.
    ///
    /// - Parameters:
    ///   - resultCount: Rows that match the search and filters, shown as "N results" and announced.
    ///   - selectedCount: Selected rows. While it is above zero the bulk selection bar replaces the filters.
    ///   - totalCount: Rows that could be selected, for "3 of 120 selected".
    ///   - bulkActions: Actions on the selected rows.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClearSelection: Clears the selection. Without it there is no bulk selection bar.
    ///   - filters: Filter controls, such as filter chips or a filter button.
    ///   - controls: Trailing controls: a view switcher, a column menu, actions.
    public init(
        resultCount: Int? = nil,
        selectedCount: Int = 0,
        totalCount: Int? = nil,
        bulkActions: [MonarchBulkAction] = [],
        labels: MonarchTableToolbarLabels = MonarchTableToolbarLabels(),
        onClearSelection: (@MainActor () -> Void)? = nil,
        @ViewBuilder filters: () -> Filters,
        @ViewBuilder controls: () -> Controls
    ) {
        self.resultCount = resultCount
        self.selectedCount = selectedCount
        self.totalCount = totalCount
        self.bulkActions = bulkActions
        self.labels = labels
        self.onClearSelection = onClearSelection
        self.filters = filters()
        self.controls = controls()
    }

    private var isSelecting: Bool {
        MonarchTableToolbarTokens.isSelecting(selectedCount: selectedCount, canClear: onClearSelection != nil)
    }

    /// The content of the view.
    public var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchTableToolbarTokens.spacing) {
                start
                Spacer(minLength: MonarchTableToolbarTokens.spacing)
                end
            }
            VStack(alignment: .leading, spacing: MonarchTableToolbarTokens.lineSpacing) {
                start
                end
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.toolbar ?? MonarchTableToolbarStrings.toolbar)
        .onChange(of: resultCount) { old, new in
            if let message = MonarchTableToolbarTokens.announcement(from: old, to: new, labels: labels) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    @ViewBuilder private var start: some View {
        if isSelecting, let onClearSelection {
            MonarchBulkActionBar(
                count: selectedCount,
                totalCount: totalCount,
                actions: bulkActions,
                onClear: onClearSelection
            )
        } else if Filters.self != EmptyView.self {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: MonarchTableToolbarTokens.spacing) {
                    filters
                }
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        }
    }

    private var end: some View {
        // In one row while the count and the controls fit; otherwise the count first and the controls wrapping below.
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchTableToolbarTokens.spacing) {
                count
                Spacer(minLength: 0)
                controls
            }
            VStack(alignment: .leading, spacing: MonarchTableToolbarTokens.lineSpacing) {
                count
                MonarchFlowLayout(spacing: MonarchTableToolbarTokens.spacing) {
                    controls
                }
            }
        }
    }

    @ViewBuilder private var count: some View {
        if let resultCount {
            Text(MonarchTableToolbarTokens.resultsText(resultCount, labels: labels))
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchTableToolbarTokens.countColor])
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

extension MonarchTableToolbar where Filters == EmptyView {
    /// A table toolbar without filters.
    ///
    /// - Parameters:
    ///   - resultCount: Rows that match the search, shown as "N results" and announced.
    ///   - selectedCount: Selected rows. While it is above zero the bulk selection bar is shown.
    ///   - totalCount: Rows that could be selected, for "3 of 120 selected".
    ///   - bulkActions: Actions on the selected rows.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClearSelection: Clears the selection. Without it there is no bulk selection bar.
    ///   - controls: Trailing controls: a view switcher, a column menu, actions.
    public init(
        resultCount: Int? = nil,
        selectedCount: Int = 0,
        totalCount: Int? = nil,
        bulkActions: [MonarchBulkAction] = [],
        labels: MonarchTableToolbarLabels = MonarchTableToolbarLabels(),
        onClearSelection: (@MainActor () -> Void)? = nil,
        @ViewBuilder controls: () -> Controls
    ) {
        self.init(
            resultCount: resultCount,
            selectedCount: selectedCount,
            totalCount: totalCount,
            bulkActions: bulkActions,
            labels: labels,
            onClearSelection: onClearSelection,
            filters: { EmptyView() },
            controls: controls
        )
    }
}

#if DEBUG
    #Preview("MonarchTableToolbar") {
        MonarchPreviewMatrix {
            VStack {
                MonarchTableToolbar(
                    resultCount: 1204,
                    filters: {
                        MonarchTag("Status: Open")
                    },
                    controls: {
                        Picker("View", selection: .constant("table")) {
                            Text("Table").tag("table")
                            Text("Board").tag("board")
                        }
                        .pickerStyle(.segmented)
                        .fixedSize()
                    }
                )
                MonarchTableToolbar(
                    resultCount: 12,
                    selectedCount: 3,
                    totalCount: 12,
                    bulkActions: [MonarchBulkAction(id: "archive", title: "Archive") {}],
                    onClearSelection: {},
                    controls: {
                        MonarchButton("New", systemImage: "plus", variant: .primary, size: .sm) {}
                    }
                )
            }
        }
    }
#endif
