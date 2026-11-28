import SwiftUI

/// One legend entry of a stacked proportion bar.
struct MonarchStackedProportionBarEntry: Equatable {
    let key: String
    let label: String
    let value: String?
    let color: MonarchChartColor
    let role: MonarchColorRole
    let isRemainder: Bool
}

/// The legend of a bar whose colours are not palette slots: status tones,
/// or a remainder drawn as empty track. It reads like ``MonarchChartLegend``.
struct MonarchStackedProportionBarLegend: View {
    let label: String
    let entries: [MonarchStackedProportionBarEntry]

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchFlowLayout(spacing: MonarchChartLegendTokens.entrySpacing) {
            ForEach(entries, id: \.key) { entry in
                HStack(spacing: MonarchChartLegendTokens.contentSpacing) {
                    MonarchChartLegendSwatch(marker: .square, role: entry.role, isDashed: entry.isRemainder)
                    Text(entry.label)
                        .foregroundStyle(theme.colors.textDefault)
                        .multilineTextAlignment(.leading)
                    if let value = entry.value, !value.isEmpty {
                        Text(value)
                            .monospacedDigit()
                            .foregroundStyle(theme.colors.textDefault)
                    }
                }
                .monarchTextStyle(MonarchChartLegendTokens.textStyle)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(MonarchChartLegendTokens.accessibilityLabel(label: entry.label, value: entry.value))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }
}
