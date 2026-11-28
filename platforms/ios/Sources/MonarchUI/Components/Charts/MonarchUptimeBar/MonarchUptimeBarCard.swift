import SwiftUI

/// The details of one period, shown in the popover of a tapped segment.
struct MonarchUptimeBarCard: View {
    let segment: MonarchUptimeBarSegment
    let status: MonarchUptimeBarStatus
    let digits: Int
    let labels: MonarchUptimeBarLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchUptimeBarTokens.cardSpacing) {
            Text(segment.label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            HStack(alignment: .firstTextBaseline, spacing: MonarchUptimeBarTokens.markerSpacing) {
                MonarchUptimeBarSwatch(status: status)
                Text(MonarchUptimeBarTokens.name(status, labels: labels))
                    .foregroundStyle(theme.colors.textDefault)
                if let uptime = segment.uptime, uptime.isFinite {
                    let percent = MonarchUptimeBarTokens.percent(uptime, digits: digits, locale: locale)
                    Spacer(minLength: MonarchUptimeBarTokens.cardSpacing)
                    Text(percent)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityLabel("\(labels.uptime ?? MonarchUptimeBarStrings.uptime) \(percent)")
                }
            }
            .accessibilityElement(children: .combine)
            ForEach(Array(segment.details.enumerated()), id: \.offset) { _, detail in
                HStack(alignment: .firstTextBaseline, spacing: MonarchUptimeBarTokens.cardSpacing) {
                    Text(detail.label).foregroundStyle(theme.colors.textMuted)
                    Spacer(minLength: MonarchUptimeBarTokens.cardSpacing)
                    Text(detail.value)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                }
                .accessibilityElement(children: .combine)
            }
            if let note = segment.note, !note.isEmpty {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .monarchTextStyle(.caption)
        .multilineTextAlignment(.leading)
        .padding(MonarchUptimeBarTokens.cardPadding)
        .fixedSize(horizontal: false, vertical: true)
    }
}

/// The colour mark of a state: filled, or outlined for no data.
struct MonarchUptimeBarSwatch: View {
    let status: MonarchUptimeBarStatus
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchChartLegendSwatch(
            marker: .square,
            role: status == .noData ? MonarchUptimeBarTokens.noDataBorderRole : MonarchUptimeBarTokens.role(status),
            isDashed: status == .noData,
            fill: status == .noData ? nil : MonarchUptimeBarTokens.fill(status, colors: theme.colors)
        )
    }
}

/// The names of the colours, once for a bar.
struct MonarchUptimeBarLegend: View {
    let labels: MonarchUptimeBarLabels

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchFlowLayout(spacing: MonarchUptimeBarTokens.legendSpacing) {
            ForEach(MonarchUptimeBarTokens.order, id: \.self) { status in
                HStack(spacing: MonarchUptimeBarTokens.markerSpacing) {
                    MonarchUptimeBarSwatch(status: status)
                    Text(MonarchUptimeBarTokens.name(status, labels: labels))
                        .foregroundStyle(theme.colors.textDefault)
                }
                .monarchTextStyle(.caption)
                .accessibilityElement(children: .combine)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.legend ?? MonarchUptimeBarStrings.legend)
    }
}
