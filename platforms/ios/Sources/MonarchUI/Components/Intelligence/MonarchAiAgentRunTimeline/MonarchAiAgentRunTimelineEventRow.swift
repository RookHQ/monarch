import SwiftUI

/// One event of the timeline: a disclosure whose header is the kind, the
/// name, the duration and the bar, and whose content is the facts, the
/// detail and "Show in conversation".
struct MonarchAiAgentRunTimelineEventRow: View {
    let row: MonarchAiAgentRunTimelineTokens.Row
    let totals: MonarchAiAgentRunTimelineTokens.Totals
    let currency: String
    let labels: MonarchAiAgentRunTimelineLabels
    @Binding var isExpanded: Bool
    let onOpenPart: (@MainActor (MonarchAiRunEvent) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        DisclosureGroup(isExpanded: $isExpanded) {
            panel
        } label: {
            header
        }
        .disclosureGroupStyle(.monarch)
        .environment(\.monarchDisclosureChevron, .leading())
    }

    private var header: some View {
        let kind = row.event.kind
        return VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.factSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiAgentRunTimelineTokens.innerSpacing) {
                Text(MonarchAiAgentRunTimelineTokens.kindName(kind, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiAgentRunTimelineTokens.kindText(kind)])
                    .padding(.horizontal, MonarchAiAgentRunTimelineTokens.kindPadding)
                    .background(
                        RoundedRectangle(cornerRadius: MonarchAiAgentRunTimelineTokens.radius, style: .continuous)
                            .fill(theme.colors[role: MonarchAiAgentRunTimelineTokens.kindFill(kind)])
                    )
                Text(row.event.name)
                    .monarchTextStyle(.code)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(MonarchAiAgentRunTimelineTokens.rowDuration(row, locale: locale, labels: labels))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
            }
            MonarchAiAgentRunTimelineBar(
                offset: MonarchAiAgentRunTimelineTokens.offsetFraction(row, totals: totals),
                width: MonarchAiAgentRunTimelineTokens.widthFraction(row, totals: totals),
                fill: MonarchAiAgentRunTimelineTokens.barFill(kind),
                isRunning: row.isRunning
            )
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAiAgentRunTimelineTokens.rowLabel(row, locale: locale, labels: labels))
    }

    private var panel: some View {
        let facts = MonarchAiAgentRunTimelineTokens.facts(
            row,
            totals: totals,
            currency: currency,
            locale: locale,
            labels: labels,
            includesUnknown: false
        )
        return VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.innerSpacing) {
            MonarchFlowLayout(spacing: MonarchAiAgentRunTimelineTokens.spacing, alignment: .top) {
                ForEach(facts.indices, id: \.self) { index in
                    VStack(alignment: .leading, spacing: MonarchAiAgentRunTimelineTokens.factSpacing) {
                        Text(facts[index].name)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                        Text(facts[index].value)
                            .monarchTextStyle(.body)
                            .monospacedDigit()
                            .foregroundStyle(theme.colors.textDefault)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(MonarchAiAgentRunTimelineTokens.fact(facts[index].name, facts[index].value))
                }
            }
            if let detail = row.event.detail, !detail.isEmpty {
                Text(detail)
                    .monarchTextStyle(.code)
                    .foregroundStyle(theme.colors.textDefault)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if MonarchAiAgentRunTimelineTokens.canOpenPart(row.event, hasHandler: onOpenPart != nil), let onOpenPart {
                let event = row.event
                MonarchButton(
                    labels.openPart ?? MonarchAiAgentRunTimelineStrings.openPart,
                    variant: .secondary,
                    size: .sm
                ) {
                    onOpenPart(event)
                }
            }
        }
    }
}

/// The track and the bar of one event. The bar is placed with leading
/// padding, so it follows the layout direction. Hidden from VoiceOver: the
/// header says the same in words.
struct MonarchAiAgentRunTimelineBar: View {
    let offset: CGFloat
    let width: CGFloat
    let fill: MonarchColorRole
    let isRunning: Bool

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var height = MonarchAiAgentRunTimelineTokens.trackHeight

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchAiAgentRunTimelineTokens.radius, style: .continuous)
        GeometryReader { proxy in
            let total = proxy.size.width
            let barWidth = max(MonarchAiAgentRunTimelineTokens.minimumBarWidth, total * width)
            shape
                .fill(barStyle)
                .frame(width: min(barWidth, total))
                .padding(.leading, min(total * offset, max(0, total - barWidth)))
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
        .frame(height: height)
        .background(shape.fill(theme.colors[role: MonarchAiAgentRunTimelineTokens.track]))
        .accessibilityHidden(true)
    }

    private var barStyle: AnyShapeStyle {
        let color = theme.colors.soft(fill, by: MonarchBarChartTokens.softening)
        guard isRunning else { return AnyShapeStyle(color) }
        // A running event fades into the thinking colour toward its open end.
        return AnyShapeStyle(
            LinearGradient(
                colors: [color, theme.colors[role: MonarchAiAgentRunTimelineTokens.running]],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
    }
}
