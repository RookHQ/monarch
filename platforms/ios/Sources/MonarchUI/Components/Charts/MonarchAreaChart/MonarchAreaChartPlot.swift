import Charts
import MonarchTokens
import SwiftUI

/// The Swift Charts plot of an area chart, for one kind of x value (dates or
/// categories). The stack is computed beforehand, so every area is an
/// `AreaMark` between its own lower and upper edge.
struct MonarchAreaChartPlot<X: Plottable & Hashable>: View {
    let xs: [X]
    let xName: String
    let valueName: String
    let stack: MonarchAreaChartTokens.Stack
    let stacking: MonarchAreaChartStacking
    let referenceLines: [MonarchLineChartReferenceLine]
    let curve: MonarchLineChartCurve
    let includesZero: Bool
    let valueDomain: ClosedRange<Double>?
    let xLabels: [String]
    let active: Int?
    let tooltipRows: (Int) -> [MonarchChartTooltip.Row]
    let tooltipFooter: (Int) -> String?
    let formatValue: ((Double) -> String)?
    let indexOf: (X) -> Int?
    let setActive: (Int?) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        axes(
            scaled(
                Chart {
                    ForEach(stack.layers) { layer in
                        layerMarks(layer)
                    }
                    ForEach(Array(referenceLines.enumerated()), id: \.offset) { _, line in
                        referenceMark(line)
                    }
                    if let active, xs.indices.contains(active) {
                        crosshair(at: active)
                    }
                }
            )
        )
        .chartLegend(.hidden)
        .chartXSelection(
            value: Binding<X?>(
                get: { active.flatMap { xs.indices.contains($0) ? xs[$0] : nil } },
                set: { value in setActive(value.flatMap(indexOf)) }
            )
        )
    }

    @ViewBuilder private func scaled(_ chart: some View) -> some View {
        if let valueDomain {
            chart.chartYScale(domain: valueDomain)
        } else {
            chart.chartYScale(domain: AutomaticScaleDomain.automatic(includesZero: includesZero))
        }
    }

    @ViewBuilder private func axes(_ chart: some View) -> some View {
        // As the web draws them: the values on the leading side with faint grid lines, the x labels without grid
        // lines, and large numbers short ("1.5K").
        if stacking == .expand {
            chart
                .chartYAxis {
                    AxisMarks(position: .leading, values: MonarchAreaChartTokens.shareTicks) { value in
                        AxisGridLine().foregroundStyle(theme.colors.borderSubtle)
                        AxisValueLabel {
                            if let share = value.as(Double.self) {
                                Text(MonarchAreaChartTokens.tickText(share, locale: locale))
                            }
                        }
                        .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .chartXAxis { dateAxis }
        } else {
            chart
                .chartYAxis {
                    AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) { value in
                        AxisGridLine().foregroundStyle(theme.colors.borderSubtle)
                        AxisValueLabel {
                            if let number = value.as(Double.self) {
                                Text(compact(number))
                            }
                        }
                        .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .chartXAxis { dateAxis }
        }
    }

    /// The x labels as the web picks them: about five on a phone, the first and last always among them. The first
    /// label starts at its tick and the last ends at it, so neither is cut off at the edge of the plot.
    private var dateAxis: some AxisContent {
        let indices = MonarchAreaChartTokens.labelIndices(count: xs.count)
        return AxisMarks(values: indices.map { xs[$0] }) { value in
            let index = value.as(X.self).flatMap(indexOf)
            AxisValueLabel(
                anchor: MonarchAreaChartTokens.labelAnchor(index, count: xs.count),
                collisionResolution: .greedy
            ) {
                if let index, xLabels.indices.contains(index) { Text(xLabels[index]).fixedSize() }
            }
            .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func compact(_ number: Double) -> String {
        if let formatValue, abs(number) < 1000 { return formatValue(number) }
        return MonarchFormats.capitalisedCompact(
            number.formatted(.number.notation(.compactName).precision(.fractionLength(0...1)).locale(locale))
        )
    }

    private var interpolation: InterpolationMethod {
        switch curve {
        case .linear: .linear
        case .monotone: .monotone
        }
    }

    @ChartContentBuilder private func layerMarks(_ layer: MonarchAreaChartTokens.Layer) -> some ChartContent {
        let color = theme.colors[role: layer.series.role]
        let runs = MonarchAreaChartTokens.runs(layer)
        let stroke = StrokeStyle(
            lineWidth: MonarchLineChartTokens.lineWidth,
            lineCap: .round,
            lineJoin: .round,
            dash: layer.series.isDashed ? MonarchLineChartTokens.dash : []
        )
        ForEach(Array(runs.enumerated()), id: \.offset) { run, indices in
            ForEach(indices, id: \.self) { index in
                if let band = layer.bands[index] {
                    AreaMark(
                        x: .value(xName, xs[index]),
                        yStart: .value(valueName, band.low),
                        yEnd: .value(valueName, band.high),
                        series: .value("Series", "\(layer.series.key)#\(run)")
                    )
                    .foregroundStyle(
                        color.opacity(
                            MonarchAreaChartTokens.fillOpacity(stacking: stacking, isDashed: layer.series.isDashed)
                        )
                    )
                    .interpolationMethod(interpolation)
                    LineMark(
                        x: .value(xName, xs[index]),
                        y: .value(valueName, band.high),
                        series: .value("Series", "\(layer.series.key)#\(run)")
                    )
                    .foregroundStyle(color)
                    .lineStyle(stroke)
                    .interpolationMethod(interpolation)
                }
            }
        }
        ForEach(runs.filter { $0.count == 1 }.flatMap { $0 }, id: \.self) { index in
            if let band = layer.bands[index] {
                PointMark(x: .value(xName, xs[index]), y: .value(valueName, band.high))
                    .foregroundStyle(color)
                    .symbolSize(
                        CGSize(width: MonarchLineChartTokens.pointSide, height: MonarchLineChartTokens.pointSide)
                    )
            }
        }
    }

    private func referenceMark(_ line: MonarchLineChartReferenceLine) -> some ChartContent {
        RuleMark(y: .value(valueName, line.value))
            .foregroundStyle(theme.colors[role: MonarchLineChartTokens.referenceRole])
            .lineStyle(
                StrokeStyle(lineWidth: MonarchLineChartTokens.ruleWidth, dash: MonarchLineChartTokens.referenceDash)
            )
            .annotation(position: .top, alignment: .leading, spacing: MonarchTokens.Space.space1) {
                Text(MonarchLineChartTokens.referenceText(line, locale: locale, formatValue: formatValue))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchLineChartTokens.referenceRole])
            }
    }

    @ChartContentBuilder private func crosshair(at index: Int) -> some ChartContent {
        RuleMark(x: .value(xName, xs[index]))
            .foregroundStyle(theme.colors[role: MonarchLineChartTokens.crosshairRole])
            .lineStyle(StrokeStyle(lineWidth: MonarchLineChartTokens.ruleWidth))
            .annotation(
                position: .top,
                spacing: 0,
                overflowResolution: AnnotationOverflowResolution(x: .fit(to: .chart), y: .fit(to: .chart))
            ) {
                MonarchChartTooltip(
                    title: xLabels.indices.contains(index) ? xLabels[index] : "",
                    rows: tooltipRows(index),
                    footer: tooltipFooter(index),
                    marker: .square
                )
            }
        ForEach(stack.layers) { layer in
            if let band = layer.bands[index] {
                PointMark(x: .value(xName, xs[index]), y: .value(valueName, band.high))
                    .foregroundStyle(theme.colors[role: layer.series.role])
                    .symbolSize(
                        CGSize(
                            width: MonarchLineChartTokens.activePointSide,
                            height: MonarchLineChartTokens.activePointSide
                        )
                    )
            }
        }
    }
}
