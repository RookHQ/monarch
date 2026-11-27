import Charts
import MonarchTokens
import SwiftUI

/// The Swift Charts plot of a line chart, for one kind of x value (dates or
/// categories).
struct MonarchLineChartPlot<X: Plottable & Hashable>: View {
    let xs: [X]
    let xName: String
    let valueName: String
    let series: [MonarchLineChartTokens.Series]
    let referenceLines: [MonarchLineChartReferenceLine]
    let variant: MonarchLineChartVariant
    let curve: MonarchLineChartCurve
    let showsPoints: Bool
    let includesZero: Bool
    let valueDomain: ClosedRange<Double>?
    let xLabels: [String]
    let noValue: String
    let active: Int?
    let formatValue: ((Double) -> String)?
    let indexOf: (X) -> Int?
    let setActive: (Int?) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        scaled(
            Chart {
                ForEach(series) { item in
                    seriesMarks(item)
                }
                ForEach(Array(referenceLines.enumerated()), id: \.offset) { _, line in
                    referenceMark(line)
                }
                if let active, xs.indices.contains(active) {
                    crosshair(at: active)
                }
            }
        )
        .chartLegend(.hidden)
        // As on the web: the value axis on the leading edge with short numbers (2K), horizontal rules only,
        // and the dates as "Mar 3".
        .chartYAxis {
            AxisMarks(position: .leading) { value in
                AxisGridLine().foregroundStyle(theme.colors.borderSubtle)
                AxisValueLabel {
                    if let number = value.as(Double.self) {
                        Text(
                            MonarchFormats.capitalisedCompact(
                                number.formatted(.number.notation(.compactName).locale(locale))
                            )
                        )
                    }
                }
            }
        }
        .chartXAxis {
            AxisMarks(values: dayMarks) { value in
                AxisValueLabel(collisionResolution: .greedy) {
                    if let date = value.as(Date.self) {
                        Text(MonarchLineChartTokens.dayLabel(date, locale: locale))
                    } else if let text = value.as(String.self) {
                        Text(text)
                    }
                }
            }
        }
        .chartXSelection(
            value: Binding<X?>(
                get: { active.flatMap { xs.indices.contains($0) ? xs[$0] : nil } },
                set: { value in setActive(value.flatMap(indexOf)) }
            )
        )
    }

    /// Every second day and the last one, so the axis starts and ends on a label as on the web; labels that
    /// would collide are left out rather than cut short.
    private var dayMarks: [X] {
        xs.enumerated().filter { $0.offset % 2 == 0 || $0.offset == xs.count - 1 }.map(\.element)
    }

    @ViewBuilder private func scaled(_ chart: some View) -> some View {
        if let valueDomain {
            chart.chartYScale(domain: valueDomain)
        } else {
            chart.chartYScale(domain: AutomaticScaleDomain.automatic(includesZero: includesZero))
        }
    }

    private var interpolation: InterpolationMethod {
        switch curve {
        case .linear: .linear
        case .monotone: .monotone
        }
    }

    private var baseline: Double {
        MonarchLineChartTokens.areaBaseline(
            series.flatMap { $0.values.compactMap { $0 } },
            domain: valueDomain,
            includesZero: includesZero
        )
    }

    @ChartContentBuilder private func seriesMarks(_ item: MonarchLineChartTokens.Series) -> some ChartContent {
        let color = theme.colors[role: item.role]
        let runs = MonarchLineChartTokens.runs(item.values)
        let stroke = StrokeStyle(
            lineWidth: MonarchLineChartTokens.lineWidth,
            lineCap: .round,
            lineJoin: .round,
            dash: item.isDashed ? MonarchLineChartTokens.dash : []
        )
        ForEach(Array(runs.enumerated()), id: \.offset) { run, indices in
            ForEach(indices, id: \.self) { index in
                if variant == .area {
                    AreaMark(
                        x: .value(xName, xs[index]),
                        yStart: .value(valueName, baseline),
                        yEnd: .value(valueName, item.values[index] ?? baseline),
                        series: .value("Series", "\(item.key)#\(run)")
                    )
                    .foregroundStyle(color.opacity(MonarchLineChartTokens.areaFillOpacity))
                    .interpolationMethod(interpolation)
                }
                LineMark(
                    x: .value(xName, xs[index]),
                    y: .value(valueName, item.values[index] ?? baseline),
                    series: .value("Series", "\(item.key)#\(run)")
                )
                .foregroundStyle(color)
                .lineStyle(stroke)
                .interpolationMethod(interpolation)
            }
        }
        ForEach(MonarchLineChartTokens.pointIndices(item.values, showsPoints: showsPoints), id: \.self) { index in
            PointMark(x: .value(xName, xs[index]), y: .value(valueName, item.values[index] ?? baseline))
                .foregroundStyle(color)
                .symbolSize(CGSize(width: MonarchLineChartTokens.pointSide, height: MonarchLineChartTokens.pointSide))
        }
    }

    private func referenceMark(_ line: MonarchLineChartReferenceLine) -> some ChartContent {
        RuleMark(y: .value(valueName, line.value))
            .foregroundStyle(theme.colors[role: MonarchLineChartTokens.referenceRole])
            .lineStyle(
                StrokeStyle(lineWidth: MonarchLineChartTokens.ruleWidth, dash: MonarchLineChartTokens.referenceDash)
            )
            .annotation(position: .top, alignment: .trailing, spacing: MonarchTokens.Space.space1) {
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
                MonarchLineChartTooltip(
                    title: xLabels.indices.contains(index) ? xLabels[index] : "",
                    rows: MonarchLineChartTokens.tooltipRows(
                        series,
                        at: index,
                        noValue: noValue,
                        locale: locale,
                        formatValue: formatValue
                    )
                )
            }
        ForEach(series) { item in
            if let value = item.values[index] {
                PointMark(x: .value(xName, xs[index]), y: .value(valueName, value))
                    .foregroundStyle(theme.colors[role: item.role])
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

/// The values of every visible series at the inspected x, drawn over the
/// chart: the shared chart tooltip with line markers.
struct MonarchLineChartTooltip: View {
    let title: String
    let rows: [MonarchChartTooltip.Row]

    var body: some View {
        MonarchChartTooltip(title: title, rows: rows, footer: nil, marker: .line)
    }
}
