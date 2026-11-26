import MonarchTokens
import SwiftUI

/// The matrix of a heatmap: a grid of rounded cells with the row labels on the
/// leading side and the column labels on top. Plain SwiftUI, so the cells stay
/// square and the labels keep their room at any width.
struct MonarchHeatmapPlot: View {
    let rows: [MonarchHeatmapAxisItem]
    let columns: [MonarchHeatmapAxisItem]
    let map: [String: MonarchHeatmapCell]
    let scale: MonarchHeatmapTokens.Scale
    let color: MonarchChartColor
    let showsValues: Bool
    let active: (row: Int, column: Int)?
    let valueText: (MonarchHeatmapCell) -> String
    let onTap: (Int, Int) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var minimumValueCell = MonarchHeatmapTokens.minimumValueCell
    @State private var gridWidth: CGFloat = 0
    @State private var labelWidth: CGFloat = 0

    /// One cell to draw.
    private struct Mark: Identifiable {
        let row: Int
        let column: Int
        let rowKey: String
        let columnKey: String
        let cell: MonarchHeatmapCell
        let level: Int?
        let isActive: Bool
        var id: String { MonarchHeatmapTokens.key(row: rowKey, column: columnKey) }
    }

    private var marks: [Mark] {
        rows.enumerated().flatMap { r, row in
            columns.enumerated().map { c, column in
                let cell = MonarchHeatmapTokens.cell(map, row: row.key, column: column.key)
                return Mark(
                    row: r,
                    column: c,
                    rowKey: row.key,
                    columnKey: column.key,
                    cell: cell,
                    level: scale.level(cell.value),
                    isActive: active?.row == r && active?.column == c
                )
            }
        }
    }

    var body: some View {
        // The top step is the data colour at full strength, as on the web.
        let base = theme.colors.tokens[keyPath: MonarchChartLegendTokens.role(color)]
        let muted = theme.colors.tokens.bgMuted
        let gap = MonarchTokens.Space.space0_5
        let side =
            columns.isEmpty
            ? 0 : max(0, (gridWidth - labelWidth - gap * CGFloat(columns.count)) / CGFloat(columns.count))
        Grid(horizontalSpacing: gap, verticalSpacing: gap) {
            GridRow {
                Color.clear.frame(width: 0, height: 0)
                    .gridCellUnsizedAxes([.horizontal, .vertical])
                ForEach(Array(columns.enumerated()), id: \.offset) { _, column in
                    axisLabel(column.label)
                }
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { r, row in
                GridRow {
                    axisLabel(row.label)
                        .fixedSize()
                        .gridColumnAlignment(.trailing)
                        .onGeometryChange(for: CGFloat.self) {
                            $0.size.width
                        } action: { width in
                            if r == 0 || width > labelWidth {
                                labelWidth = max(labelWidth, width + MonarchTokens.Space.space1)
                            }
                        }
                    ForEach(Array(columns.enumerated()), id: \.offset) { c, column in
                        let cell = MonarchHeatmapTokens.cell(map, row: row.key, column: column.key)
                        let level = scale.level(cell.value)
                        let isActive = active?.row == r && active?.column == c
                        cellView(cell, level: level, isActive: isActive, base: base, muted: muted, side: side)
                            .onTapGesture { onTap(r, c) }
                    }
                }
            }
        }
        .frame(
            maxWidth: MonarchHeatmapTokens.maximumPlotWidth(columns: columns.count) + labelWidth + 80,
            alignment: .leading
        )
        .onGeometryChange(for: CGFloat.self) {
            $0.size.width
        } action: {
            gridWidth = $0
        }
    }

    private func cellView(
        _ cell: MonarchHeatmapCell,
        level: Int?,
        isActive: Bool,
        base: MonarchRGBA,
        muted: MonarchRGBA,
        side: CGFloat
    ) -> some View {
        let fill = MonarchHeatmapTokens.fill(level: level, base: base, muted: muted)
        let shape = RoundedRectangle(cornerRadius: MonarchHeatmapTokens.cornerRadius, style: .continuous)
        return
            shape
            .fill(fill.map { Color(monarch: $0) } ?? theme.colors.bgSurface)
            .overlay {
                if isActive || fill == nil {
                    shape.strokeBorder(
                        theme.colors[role: isActive ? \.textDefault : \.borderSubtle],
                        lineWidth: MonarchShapes.borderWidth * (isActive ? 2 : 1)
                    )
                }
            }
            .overlay {
                if showsValues, level != nil, side >= minimumValueCell {
                    value(cell, level: level)
                        .frame(maxWidth: side * MonarchHeatmapTokens.innerRatio)
                }
            }
            .aspectRatio(1 / MonarchHeatmapTokens.cellRatio, contentMode: .fit)
            .frame(maxWidth: MonarchHeatmapTokens.maximumCellSize)
            .contentShape(Rectangle())
    }

    private func axisLabel(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }

    private func value(_ cell: MonarchHeatmapCell, level: Int?) -> some View {
        Text(valueText(cell))
            .monarchTextStyle(.caption)
            .monospacedDigit()
            .lineLimit(1)
            .minimumScaleFactor(MonarchHeatmapTokens.valueMinimumScale)
            .foregroundStyle(theme.colors.textDefault)
            .padding(.horizontal, MonarchHeatmapTokens.chipPadding)
            .background {
                if MonarchHeatmapTokens.usesChip(level: level) {
                    MonarchShapes.mark.fill(theme.colors.bgSurface)
                }
            }
    }
}
