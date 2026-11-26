import SwiftUI

/// A table the model produced, shown as a real table: column types inferred
/// (numbers sit at the trailing edge and sort by value, ISO dates sort by
/// time), sortable headers, Copy as CSV and an optional Open as artifact.
/// Rows can stream in, and a wide table scrolls sideways inside its border.
///
/// ```swift
/// MonarchAiTableAnswer(
///     rows,
///     title: "Revenue by region",
///     columns: [
///         MonarchAiTableColumn("region", label: "Region"),
///         MonarchAiTableColumn("revenue", label: "Revenue"),
///     ],
///     isStreaming: message.isStreaming
/// )
/// ```
///
/// Rows are `[String: MonarchAiJSON]`, the JSON type of the conversation
/// model, so a tool's output can be passed as it is. A Swift dictionary has
/// no key order: pass `columns` to choose the order and the headers; without
/// them every key is a column, in alphabetical order. Sorting and the CSV
/// always use every row; only the first `visibleRows` are drawn until the
/// person asks for the rest.
///
/// Accessibility: the provenance label, title and row count are read as one
/// element. Each sortable header is a button with the header trait, read as
/// its name with "Sorted ascending" or "Sorted descending" as its value, and
/// a sort is announced. Each row is one element read as every header with
/// its value ("Region: North, Revenue: 1,200"), and an empty cell is read as
/// "Empty". The end of streaming is announced as "Table complete.". The sort
/// icons are decorative; the direction is the spoken value.
public struct MonarchAiTableAnswer: View {
    private let rows: [[String: MonarchAiJSON]]
    private let title: String?
    private let columns: [MonarchAiTableColumn]?
    private let isStreaming: Bool
    private let isSortable: Bool
    private let isCopyable: Bool
    private let visibleRows: Int
    private let labels: MonarchAiTableAnswerLabels
    private let onOpenAsArtifact: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var sort: MonarchAiTableAnswerTokens.Sort?
    @State private var isShowingAll = false

    /// A table answer.
    ///
    /// - Parameters:
    ///   - rows: The rows, usually the parsed output of the model or a tool.
    ///   - title: The title shown under the provenance label, already localised.
    ///   - columns: The order, headers, types and alignment of the columns. `nil` shows every key, inferred.
    ///   - isStreaming: Whether rows are still arriving: shows a placeholder row.
    ///   - isSortable: Whether the headers sort the table.
    ///   - isCopyable: Whether Copy as CSV is shown.
    ///   - visibleRows: How many rows are drawn before Show all rows. Sorting and the CSV use every row.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenAsArtifact: Shows Open as artifact, for example to open the table in an artifact canvas.
    public init(
        _ rows: [[String: MonarchAiJSON]],
        title: String? = nil,
        columns: [MonarchAiTableColumn]? = nil,
        isStreaming: Bool = false,
        isSortable: Bool = true,
        isCopyable: Bool = true,
        visibleRows: Int = 200,
        labels: MonarchAiTableAnswerLabels = MonarchAiTableAnswerLabels(),
        onOpenAsArtifact: (@MainActor () -> Void)? = nil
    ) {
        self.rows = rows
        self.title = title
        self.columns = columns
        self.isStreaming = isStreaming
        self.isSortable = isSortable
        self.isCopyable = isCopyable
        self.visibleRows = visibleRows
        self.labels = labels
        self.onOpenAsArtifact = onOpenAsArtifact
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchAiTableAnswerTokens.resolveColumns(columns, rows: rows)
        let order = MonarchAiTableAnswerTokens.order(rows, by: sort, columns: resolved, locale: locale)
        let shownCount = MonarchAiTableAnswerTokens.shownCount(
            total: order.count,
            visibleRows: visibleRows,
            isShowingAll: isShowingAll
        )
        VStack(alignment: .leading, spacing: MonarchAiTableAnswerTokens.sectionSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: MonarchAiTableAnswerTokens.actionSpacing) {
                    titles.frame(maxWidth: .infinity, alignment: .leading)
                    actions(resolved, order: order)
                }
                VStack(alignment: .leading, spacing: MonarchAiTableAnswerTokens.actionSpacing) {
                    titles
                    actions(resolved, order: order)
                }
            }
            table(resolved, shown: Array(order.prefix(shownCount)))
            if shownCount < order.count {
                MonarchButton(
                    labels.showAllRows?(order.count) ?? MonarchAiTableAnswerStrings.showAllRows(order.count),
                    size: .sm
                ) {
                    isShowingAll = true
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .onChange(of: isStreaming) { wasStreaming, streaming in
            if wasStreaming && !streaming {
                AccessibilityNotification.Announcement(labels.complete ?? MonarchAiTableAnswerStrings.complete).post()
            }
        }
    }

    private var titles: some View {
        VStack(alignment: .leading, spacing: MonarchAiTableAnswerTokens.titleSpacing) {
            HStack(spacing: MonarchAiTableAnswerTokens.eyebrowSpacing) {
                MonarchAiBotGlyph(size: .sm)
                Text(labels.aiTable ?? MonarchAiTableAnswerStrings.aiTable)
                    .monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: MonarchAiTableAnswerTokens.eyebrowColor])
            if let title, !title.isEmpty {
                Text(title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
            }
            Text(labels.rowCount?(rows.count) ?? MonarchAiTableAnswerStrings.rowCount(rows.count))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchAiTableAnswerTokens.supportingColor])
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private func actions(
        _ resolved: [MonarchAiTableAnswerTokens.ResolvedColumn],
        order: [Int]
    ) -> some View {
        if isCopyable || onOpenAsArtifact != nil {
            HStack(spacing: MonarchAiTableAnswerTokens.actionSpacing) {
                if isCopyable {
                    MonarchCopyButton(
                        labels.copyCSV ?? MonarchAiTableAnswerStrings.copyCSV,
                        value: rows.isEmpty
                            ? "" : MonarchAiTableAnswerTokens.csv(order.map { rows[$0] }, columns: resolved),
                        variant: .ghost,
                        size: .sm,
                        copiedTitle: labels.copied ?? MonarchAiTableAnswerStrings.copied
                    )
                    .disabled(rows.isEmpty)
                }
                if let onOpenAsArtifact {
                    MonarchButton(
                        labels.openAsArtifact ?? MonarchAiTableAnswerStrings.openAsArtifact,
                        variant: .secondary,
                        size: .sm,
                        action: onOpenAsArtifact
                    )
                    .disabled(isStreaming)
                }
            }
        }
    }

    private func table(_ resolved: [MonarchAiTableAnswerTokens.ResolvedColumn], shown: [Int]) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchAiTableAnswerTokens.cornerRadius, style: .continuous)
        let emptyLabel = labels.empty ?? MonarchAiTableAnswerStrings.empty
        let span = max(1, resolved.count)
        let grid = Grid(
            alignment: .leading,
            horizontalSpacing: MonarchAiTableAnswerTokens.columnSpacing,
            verticalSpacing: 0
        ) {
            GridRow {
                ForEach(resolved) { column in
                    header(column)
                }
            }
            separator
            ForEach(shown, id: \.self) { index in
                GridRow {
                    ForEach(Array(resolved.enumerated()), id: \.element.id) { position, column in
                        cell(
                            rows[index],
                            column: column,
                            position: position,
                            resolved: resolved,
                            emptyLabel: emptyLabel
                        )
                    }
                }
                if index != shown.last || isStreaming {
                    separator
                }
            }
            if isStreaming {
                Text(labels.loadingRows ?? MonarchAiTableAnswerStrings.loadingRows)
                    .monarchTextStyle(.body)
                    .redacted(reason: .placeholder)
                    .padding(.vertical, MonarchAiTableAnswerTokens.cellVerticalPadding)
                    .gridCellColumns(span)
                    .accessibilityLabel(labels.loadingRows ?? MonarchAiTableAnswerStrings.loadingRows)
            } else if rows.isEmpty {
                Text(labels.noRows ?? MonarchAiTableAnswerStrings.noRows)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchAiTableAnswerTokens.supportingColor])
                    .padding(.vertical, MonarchAiTableAnswerTokens.cellVerticalPadding)
                    .gridCellColumns(span)
            }
        }
        .padding(.horizontal, MonarchAiTableAnswerTokens.tablePadding)
        // The whole table at the reading size when it fits; one text step smaller before it scrolls sideways.
        return ViewThatFits(in: .horizontal) {
            grid
            grid.dynamicTypeSize(.xSmall)
            ScrollView(.horizontal) { grid }.scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        }
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchAiTableAnswerTokens.border],
                lineWidth: MonarchAiTableAnswerTokens.borderWidth
            )
        )
        .clipShape(shape)
    }

    private var separator: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchAiTableAnswerTokens.border])
            .frame(height: MonarchAiTableAnswerTokens.borderWidth)
            .gridCellUnsizedAxes(.horizontal)
            .accessibilityHidden(true)
    }

    @ViewBuilder private func header(_ column: MonarchAiTableAnswerTokens.ResolvedColumn) -> some View {
        let ascending = sort?.key == column.key ? sort?.ascending : nil
        let alignment: HorizontalAlignment = column.alignment == .trailing ? .trailing : .leading
        if isSortable {
            Button {
                let next = MonarchAiTableAnswerTokens.toggled(sort, key: column.key)
                sort = next
                AccessibilityNotification.Announcement(
                    labels.sorted?(column.label, next.ascending)
                        ?? MonarchAiTableAnswerStrings.sorted(column.label, ascending: next.ascending)
                ).post()
            } label: {
                HStack(spacing: MonarchAiTableAnswerTokens.sortIconSpacing) {
                    Text(column.label).monarchTextStyle(.label)
                    MonarchIcon(systemName: MonarchAiTableAnswerTokens.sortSymbol(ascending: ascending), size: .sm)
                        .foregroundStyle(ascending == nil ? theme.colors.textSubtle : theme.colors.textDefault)
                }
                .foregroundStyle(theme.colors[role: MonarchAiTableAnswerTokens.headerColor])
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .gridColumnAlignment(alignment)
            .accessibilityLabel(column.label)
            .accessibilityValue(
                ascending.map { labels.sortValue?($0) ?? MonarchAiTableAnswerStrings.sortValue(ascending: $0) } ?? ""
            )
            .accessibilityAddTraits(.isHeader)
        } else {
            Text(column.label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchAiTableAnswerTokens.headerColor])
                .padding(.vertical, MonarchAiTableAnswerTokens.cellVerticalPadding)
                .gridColumnAlignment(alignment)
                .accessibilityAddTraits(.isHeader)
        }
    }

    @ViewBuilder private func cell(
        _ row: [String: MonarchAiJSON],
        column: MonarchAiTableAnswerTokens.ResolvedColumn,
        position: Int,
        resolved: [MonarchAiTableAnswerTokens.ResolvedColumn],
        emptyLabel: String
    ) -> some View {
        let value = row[column.key]
        let isBlank = MonarchAiTableAnswerTokens.isBlank(value)
        let text = Text(
            isBlank
                ? MonarchAiTableAnswerTokens.emptyMark
                : MonarchAiTableAnswerTokens.display(value, type: column.type, locale: locale)
        )
        .monarchTextStyle(.body)
        .monospacedDigit()
        .foregroundStyle(isBlank ? theme.colors.textSubtle : theme.colors.textDefault)
        .padding(.vertical, MonarchAiTableAnswerTokens.cellVerticalPadding)
        // The first cell speaks for the whole row; the others are read through it.
        if position == 0 {
            text
                .accessibilityLabel(
                    MonarchAiTableAnswerTokens.rowLabel(row, columns: resolved, locale: locale, emptyLabel: emptyLabel)
                )
        } else {
            text.accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiTableAnswer") {
        MonarchPreviewMatrix {
            MonarchAiTableAnswer(
                [
                    ["region": "North", "revenue": 1200, "updated": "2024-10-14"],
                    ["region": "South", "revenue": 980.5, "updated": "2024-09-30"],
                    ["region": "East", "revenue": nil, "updated": "2024-10-01"],
                ],
                title: "Revenue by region",
                columns: [
                    MonarchAiTableColumn("region", label: "Region"),
                    MonarchAiTableColumn("revenue", label: "Revenue"),
                    MonarchAiTableColumn("updated", label: "Updated"),
                ],
                isStreaming: true,
                onOpenAsArtifact: {}
            )
        }
    }
#endif
