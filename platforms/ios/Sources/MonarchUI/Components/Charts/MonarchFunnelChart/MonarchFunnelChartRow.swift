import Charts
import SwiftUI

/// One stage of a funnel: the step line, the label and value, and the bar.
struct MonarchFunnelChartRow: View {
    let stage: MonarchFunnelChartStage
    let value: String
    let share: String?
    let step: String?
    let span: ClosedRange<Double>
    let scale: ClosedRange<Double>

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var barHeight = MonarchFunnelChartTokens.barHeight

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchFunnelChartTokens.rowSpacing) {
            if let step {
                HStack(alignment: .firstTextBaseline, spacing: MonarchFunnelChartTokens.stepSpacing) {
                    MonarchIcon(systemName: MonarchFunnelChartTokens.stepSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textSubtle)
                    Text(step)
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchFunnelChartTokens.headSpacing) {
                    label
                    Spacer(minLength: 0)
                    valueText
                }
                VStack(alignment: .leading, spacing: MonarchFunnelChartTokens.rowSpacing) {
                    label
                    valueText
                }
            }
            bar
        }
        .padding(.vertical, MonarchFunnelChartTokens.verticalPadding)
        .padding(.horizontal, MonarchFunnelChartTokens.horizontalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
    }

    private var label: some View {
        Text(stage.label)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors.textDefault)
            .multilineTextAlignment(.leading)
    }

    private var valueText: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFunnelChartTokens.shareSpacing) {
            Text(value)
                .monarchTextStyle(.label, weight: .semibold)
                .foregroundStyle(theme.colors.textDefault)
            if let share {
                Text(share)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .monospacedDigit()
    }

    private var bar: some View {
        MonarchFunnelChartBar(span: span, scale: scale, role: MonarchChartLegendTokens.role(stage.color ?? .data1))
            .frame(height: barHeight)
    }
}

/// The bar of one stage, as a Swift Charts bar mark on the scale shared by every stage.
struct MonarchFunnelChartBar: View {
    let span: ClosedRange<Double>
    let scale: ClosedRange<Double>
    let role: MonarchColorRole

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Chart {
            BarMark(
                xStart: .value("Start", span.lowerBound),
                xEnd: .value("End", span.upperBound),
                y: .value("Stage", ""),
                height: .ratio(1)
            )
            .foregroundStyle(theme.colors.soft(role, by: MonarchBarChartTokens.softening))
            .cornerRadius(MonarchFunnelChartTokens.barRadius)
        }
        .chartXScale(domain: scale)
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .chartLegend(.hidden)
        .accessibilityHidden(true)
    }
}

/// The pressed state of a stage that can be selected.
struct MonarchFunnelChartPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchFunnelChartPressBody(configuration: configuration)
    }
}

/// Reads the theme, which a style value cannot do itself.
private struct MonarchFunnelChartPressBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        configuration.label
            .background {
                if configuration.isPressed {
                    MonarchShapes.mark.fill(theme.colors.bgMuted)
                }
            }
            .contentShape(Rectangle())
    }
}
