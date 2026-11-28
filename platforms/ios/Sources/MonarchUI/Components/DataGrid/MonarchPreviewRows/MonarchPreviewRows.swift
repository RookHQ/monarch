import SwiftUI

/// What an import will do with each row of a file: the row number, the
/// result (ready, warning, error or duplicate) as a status badge, the chosen
/// values and the problems, with filter tabs that count each result.
///
/// ```swift
/// MonarchPreviewRows(
///     "Preview of contacts.csv",
///     rows: [
///         MonarchPreviewRow(row: 1, values: ["name": "Ada Lovelace", "email": "ada@example.com"]),
///         MonarchPreviewRow(
///             row: 2,
///             values: ["name": "Alan Turing", "email": "alan@"],
///             errors: [MonarchPreviewRowIssue("Not a valid address", field: "Email")]
///         ),
///     ],
///     columns: [
///         MonarchPreviewRowsColumn("Name", id: "name"),
///         MonarchPreviewRowsColumn("Email", id: "email"),
///     ]
/// )
/// ```
///
/// Each row is a card, the native form of a table row on a phone. Put the
/// view in a `ScrollView`; the rows are lazy, and long files are shown a
/// page at a time with a load more control.
///
/// Accessibility: the rows are a container named by `caption`. Each row is
/// one element whose label is its title ("Row 4") and whose value reads the
/// result, every column with its value ("Email: Empty" for a dash), each
/// issue after its kind, and the note. The result is a badge with an icon
/// and a name as well as a colour; issues have an icon of their kind.
public struct MonarchPreviewRows: View {
    private let caption: String
    private let rows: [MonarchPreviewRow]
    private let columns: [MonarchPreviewRowsColumn]
    private let filterBinding: Binding<MonarchPreviewRowsFilter>?
    private let isFilterable: Bool
    private let pageSize: Int
    private let labels: MonarchPreviewRowsLabels

    @State private var innerFilter = MonarchPreviewRowsFilter.all
    @State private var pages = 1
    @Environment(\.monarchTheme) private var theme

    /// An import preview.
    ///
    /// - Parameters:
    ///   - caption: The spoken name of the rows, for example "Preview of contacts.csv".
    ///   - rows: The rows of the file, in file order.
    ///   - columns: The data columns to show in each row, in order.
    ///   - filter: The chosen filter, when the app owns it. `nil` keeps it inside the view.
    ///   - isFilterable: Whether filter tabs with counts are shown when some rows are not ready.
    ///   - pageSize: Rows shown before the load more control. `0` shows every row.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ caption: String,
        rows: [MonarchPreviewRow],
        columns: [MonarchPreviewRowsColumn],
        filter: Binding<MonarchPreviewRowsFilter>? = nil,
        isFilterable: Bool = true,
        pageSize: Int = 50,
        labels: MonarchPreviewRowsLabels = MonarchPreviewRowsLabels()
    ) {
        self.caption = caption
        self.rows = rows
        self.columns = columns
        self.filterBinding = filter
        self.isFilterable = isFilterable
        self.pageSize = pageSize
        self.labels = labels
    }

    private var filter: Binding<MonarchPreviewRowsFilter> {
        filterBinding ?? $innerFilter
    }

    /// The content of the view.
    public var body: some View {
        let counts = MonarchPreviewRowsTokens.counts(rows)
        let shown = MonarchPreviewRowsTokens.filtered(rows, by: filter.wrappedValue)
        let visible = MonarchPreviewRowsTokens.visibleCount(total: shown.count, pageSize: pageSize, pages: pages)
        VStack(alignment: .leading, spacing: MonarchPreviewRowsTokens.spacing) {
            if MonarchPreviewRowsTokens.showsFilter(
                isFilterable: isFilterable,
                counts: counts,
                filter: filter.wrappedValue
            ) {
                MonarchTabs(
                    labels.filter ?? MonarchPreviewRowsStrings.filter,
                    selection: filter,
                    items: MonarchPreviewRowsTokens.options(rows: rows, filter: filter.wrappedValue, labels: labels)
                        .map {
                            MonarchTabsItem(
                                $0.title,
                                value: $0.filter,
                                count: $0.count.formatted(),
                                countTone: $0.countTone
                            )
                        }
                )
                .monarchTrailingFade()
            }
            if rows.isEmpty {
                MonarchEmptyState(
                    labels.emptyTitle ?? MonarchPreviewRowsStrings.emptyTitle,
                    message: labels.emptyDescription ?? MonarchPreviewRowsStrings.emptyDescription,
                    systemImage: "tablecells",
                    size: .sm
                )
            } else if shown.isEmpty {
                MonarchEmptyState(
                    labels.filteredTitle ?? MonarchPreviewRowsStrings.filteredTitle,
                    kind: .filtered,
                    size: .sm,
                    onClearFilters: { filter.wrappedValue = .all }
                )
            } else {
                LazyVStack(alignment: .leading, spacing: MonarchPreviewRowsTokens.rowSpacing) {
                    ForEach(shown.prefix(visible)) { row in
                        MonarchPreviewRowsCard(row: row, columns: columns, labels: labels)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(caption)
                if visible < shown.count {
                    MonarchLoadMore(
                        hasMore: true,
                        loadedCount: visible,
                        totalCount: shown.count,
                        onLoadMore: { pages += 1 }
                    )
                }
            }
        }
        .onChange(of: filter.wrappedValue) { pages = 1 }
    }
}

/// One row of ``MonarchPreviewRows``: the row number, then its result, its values and its issues as labelled
/// cells in three columns, as on the web. Rows are told apart by a hairline between them.
private struct MonarchPreviewRowsCard: View {
    let row: MonarchPreviewRow
    let columns: [MonarchPreviewRowsColumn]
    let labels: MonarchPreviewRowsLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        let status = row.resolvedStatus
        let count = dynamicTypeSize.isAccessibilitySize ? 1 : MonarchPreviewRowsTokens.gridColumns
        VStack(alignment: .leading, spacing: MonarchPreviewRowsTokens.partSpacing) {
            Text("\(row.row)")
                .monarchTextStyle(.label)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchPreviewRowsTokens.muted])
            LazyVGrid(
                columns: Array(
                    repeating: GridItem(
                        .flexible(),
                        spacing: MonarchPreviewRowsTokens.pairSpacing,
                        alignment: .topLeading
                    ),
                    count: count
                ),
                alignment: .leading,
                spacing: MonarchPreviewRowsTokens.partSpacing
            ) {
                cell(labels.result ?? MonarchPreviewRowsStrings.result) {
                    MonarchStatusBadge(
                        MonarchStatusDefinition(
                            MonarchPreviewRowsTokens.statusName(status, labels: labels),
                            tone: MonarchPreviewRowsTokens.tone(status),
                            systemImage: MonarchPreviewRowsTokens.symbol(status)
                        ),
                        size: .sm
                    )
                }
                ForEach(columns) { column in
                    valueCell(column)
                }
                cell(labels.issues ?? MonarchPreviewRowsStrings.issues) { issues }
            }
        }
        .padding(.vertical, MonarchPreviewRowsTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(theme.colors[role: MonarchPreviewRowsTokens.border])
                .frame(height: MonarchShapes.borderWidth)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchPreviewRowsTokens.rowTitle(row.row, labels: labels))
        .accessibilityValue(MonarchPreviewRowsTokens.spokenValue(row, columns: columns, labels: labels))
    }

    private func cell<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: MonarchPreviewRowsTokens.issueSpacing) {
            Text(title)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchPreviewRowsTokens.muted])
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func valueCell(_ column: MonarchPreviewRowsColumn) -> some View {
        let value = MonarchPreviewRowsTokens.value(row.values[column.id])
        return cell(column.label) {
            Text(value ?? MonarchPreviewRowsTokens.emptyMark)
                .monarchTextStyle(.label)
                .lineLimit(1)
                .minimumScaleFactor(0.65)
                .foregroundStyle(
                    theme.colors[role: value == nil ? MonarchPreviewRowsTokens.muted : MonarchPreviewRowsTokens.text]
                )
        }
    }

    @ViewBuilder private var issues: some View {
        VStack(alignment: .leading, spacing: MonarchPreviewRowsTokens.issueSpacing) {
            ForEach(Array(row.errors.enumerated()), id: \.offset) { _, issue in
                issueLine(issue, isError: true)
            }
            ForEach(Array(row.warnings.enumerated()), id: \.offset) { _, issue in
                issueLine(issue, isError: false)
            }
            if let note = row.note, !note.isEmpty {
                Text(note)
                    .monarchTextStyle(.caption)
                    .underline()
                    .foregroundStyle(theme.colors[role: MonarchPreviewRowsTokens.issueRole(isError: true)])
            }
        }
    }

    private func issueLine(_ issue: MonarchPreviewRowIssue, isError: Bool) -> some View {
        var text = AttributedString(MonarchPreviewRowsTokens.issueText(issue))
        if let field = issue.field, !field.isEmpty,
            let range = text.range(of: field + MonarchPreviewRowsTokens.fieldSeparator)
        {
            text[range].font = .caption.weight(.semibold)
        }
        return Text(text)
            .monarchTextStyle(.caption)
            .fixedSize(horizontal: false, vertical: true)
            .foregroundStyle(theme.colors[role: MonarchPreviewRowsTokens.issueRole(isError: isError)])
    }
}

#if DEBUG
    #Preview("MonarchPreviewRows") {
        MonarchPreviewMatrix {
            MonarchPreviewRows(
                "Preview of contacts.csv",
                rows: [
                    MonarchPreviewRow(row: 1, values: ["name": "Ada Lovelace", "email": "ada@example.com"]),
                    MonarchPreviewRow(
                        row: 2,
                        values: ["name": "Alan Turing", "email": "alan@"],
                        errors: [MonarchPreviewRowIssue("Not a valid address", field: "Email")]
                    ),
                    MonarchPreviewRow(
                        row: 3,
                        values: ["name": "Grace Hopper"],
                        warnings: [MonarchPreviewRowIssue("Missing email", field: "Email")]
                    ),
                    MonarchPreviewRow(
                        row: 4,
                        values: ["name": "Ada Lovelace", "email": "ada@example.com"],
                        status: .duplicate,
                        note: "Matches an existing contact"
                    ),
                ],
                columns: [
                    MonarchPreviewRowsColumn("Name", id: "name"),
                    MonarchPreviewRowsColumn("Email", id: "email"),
                ]
            )
        }
    }
#endif
