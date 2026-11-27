import SwiftUI

/// The data table of a chart frame, shown in place of the chart.
struct MonarchChartFrameTableView: View {
    let table: MonarchChartTable
    let noValue: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ScrollView(.horizontal) {
            Grid(
                alignment: .leading,
                horizontalSpacing: MonarchChartFrameTokens.columnSpacing,
                verticalSpacing: MonarchChartFrameTokens.rowSpacing
            ) {
                GridRow {
                    Text(table.categoryTitle)
                    ForEach(Array(table.series.enumerated()), id: \.offset) { _, series in
                        Text(series.name).gridColumnAlignment(.trailing)
                    }
                }
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                ForEach(Array(table.categories.enumerated()), id: \.offset) { row, category in
                    MonarchDivider().gridCellUnsizedAxes(.horizontal)
                    GridRow {
                        Text(category).foregroundStyle(theme.colors.textDefault)
                        ForEach(Array(table.series.enumerated()), id: \.offset) { _, series in
                            Text(MonarchChartFrameTokens.cellText(series, at: row) ?? MonarchChartFrameTokens.gapText)
                                .monospacedDigit()
                                .foregroundStyle(theme.colors.textDefault)
                        }
                    }
                    .monarchTextStyle(.body)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(
                        MonarchChartFrameTokens.rowAccessibilityLabel(table, row: row, noValue: noValue)
                    )
                }
            }
        }
    }
}
