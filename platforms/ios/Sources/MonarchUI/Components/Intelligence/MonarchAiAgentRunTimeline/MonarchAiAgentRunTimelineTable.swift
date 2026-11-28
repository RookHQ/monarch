import SwiftUI

/// The table view of the run: a `Grid` that scrolls sideways. VoiceOver
/// reads one element per row with every value, instead of cell by cell.
struct MonarchAiAgentRunTimelineTable: View {
    let rows: [MonarchAiAgentRunTimelineTokens.Row]
    let totals: MonarchAiAgentRunTimelineTokens.Totals
    let currency: String
    let labels: MonarchAiAgentRunTimelineLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let columns = columnNames
        ScrollView(.horizontal) {
            Grid(alignment: .leading, horizontalSpacing: 0, verticalSpacing: 0) {
                GridRow {
                    ForEach(columns.indices, id: \.self) { index in
                        cell(columns[index], isHeader: true, isNumeric: index > 1)
                    }
                }
                ForEach(rows) { row in
                    Rectangle()
                        .fill(theme.colors.borderSubtle)
                        .frame(height: MonarchShapes.borderWidth)
                        .gridCellUnsizedAxes(.horizontal)
                    GridRow {
                        let values = values(row)
                        ForEach(values.indices, id: \.self) { index in
                            cell(values[index], isHeader: false, isNumeric: index > 1)
                        }
                    }
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityChildren {
            ForEach(rows) { row in
                Text(
                    MonarchAiAgentRunTimelineTokens.tableRowSummary(
                        row,
                        totals: totals,
                        currency: currency,
                        locale: locale,
                        labels: labels
                    )
                )
            }
        }
        .accessibilityLabel(labels.label ?? MonarchAiAgentRunTimelineStrings.label)
    }

    private var columnNames: [String] {
        var names = [
            labels.eventColumn ?? MonarchAiAgentRunTimelineStrings.eventColumn,
            labels.kindColumn ?? MonarchAiAgentRunTimelineStrings.kindColumn,
            labels.start ?? MonarchAiAgentRunTimelineStrings.start,
            labels.duration ?? MonarchAiAgentRunTimelineStrings.duration,
            labels.inputTokens ?? MonarchAiAgentRunTimelineStrings.inputTokens,
            labels.outputTokens ?? MonarchAiAgentRunTimelineStrings.outputTokens,
        ]
        if totals.hasCost { names.append(labels.cost ?? MonarchAiAgentRunTimelineStrings.cost) }
        return names
    }

    private func values(_ row: MonarchAiAgentRunTimelineTokens.Row) -> [String] {
        let facts = MonarchAiAgentRunTimelineTokens.facts(
            row,
            totals: totals,
            currency: currency,
            locale: locale,
            labels: labels,
            includesUnknown: true
        )
        return [row.event.name, MonarchAiAgentRunTimelineTokens.kindName(row.event.kind, labels: labels)]
            + facts.map(\.value)
    }

    private func cell(_ text: String, isHeader: Bool, isNumeric: Bool) -> some View {
        Text(text)
            .monarchTextStyle(isHeader ? .label : .body)
            .monospacedDigit()
            .foregroundStyle(isHeader ? theme.colors.textMuted : theme.colors.textDefault)
            .fixedSize()
            .gridColumnAlignment(isNumeric ? .trailing : .leading)
            .padding(.horizontal, MonarchAiAgentRunTimelineTokens.cellPadding)
            .padding(.vertical, MonarchAiAgentRunTimelineTokens.cellVerticalPadding)
    }
}
