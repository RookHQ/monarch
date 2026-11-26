import MonarchTokens
import SwiftUI

/// The values at the inspected x, drawn over a line, area or burndown chart: a title, one row per series with its marker, and an
/// optional footer such as the total.
struct MonarchChartTooltip: View {
    /// One row: the series marker, its name and its value.
    struct Row: Equatable {
        let key: String
        let label: String
        let value: String
        let role: MonarchColorRole
        let isDashed: Bool
    }

    let title: String
    let rows: [Row]
    let footer: String?
    let marker: MonarchChartLegendMarker

    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the marker, the name and the value in a row.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space2

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: Self.rowSpacing) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
            ForEach(rows, id: \.key) { row in
                HStack(spacing: Self.columnSpacing) {
                    MonarchChartLegendSwatch(marker: marker, role: row.role, isDashed: row.isDashed)
                    Text(row.label).foregroundStyle(theme.colors.textMuted)
                    Text(row.value)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                }
                .monarchTextStyle(.caption)
            }
            if let footer, !footer.isEmpty {
                Text(footer)
                    .monarchTextStyle(.caption, weight: .semibold)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textDefault)
            }
        }
        .padding(Self.padding)
        .monarchElevation(.md, fill: \.bgRaised, in: MonarchShapes.control)
        // Visual only: VoiceOver reads the values through the chart descriptor and the data table.
        .accessibilityHidden(true)
    }
}
