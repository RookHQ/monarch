import MonarchTokens
import SwiftUI

/// One person and, when their team is open, their reports in a row beneath,
/// joined by lines. The lines are laid-out rectangles, so they follow right
/// to left and Dynamic Type with the cards.
struct MonarchOrgChartBranch: View {
    let node: MonarchOrgChartNode
    let context: MonarchOrgChartContext

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var cardWidth = MonarchOrgChartTokens.cardWidth

    var body: some View {
        let reports = context.tree.reports(of: node.id)
        let hasReports = context.tree.hasReports(node)
        let isExpanded = context.isExpanded(node)
        VStack(spacing: 0) {
            card
            if hasReports {
                line
                MonarchOrgChartChip(node: node, context: context)
                    .background {
                        // The line runs behind the chip: always above it, and below it to the team when open.
                        VStack(spacing: 0) {
                            Rectangle()
                            Rectangle().opacity(isExpanded && !reports.isEmpty ? 1 : 0)
                        }
                        .foregroundStyle(theme.colors[role: MonarchOrgChartTokens.lineColor])
                        .frame(width: MonarchOrgChartTokens.lineWidth)
                        .accessibilityHidden(true)
                    }
                if isExpanded && !reports.isEmpty {
                    line
                    HStack(alignment: .top, spacing: 0) {
                        ForEach(Array(reports.enumerated()), id: \.element.id) { index, report in
                            VStack(spacing: 0) {
                                connector(index: index, count: reports.count)
                                MonarchOrgChartBranch(node: report, context: context)
                                    .padding(.horizontal, MonarchOrgChartTokens.siblingGap / 2)
                            }
                        }
                    }
                }
            }
        }
    }

    private var card: some View {
        MonarchOrgChartCard(node: node, context: context)
            .frame(width: cardWidth)
            .anchorPreference(key: MonarchOrgChartCardFrames.self, value: .bounds) { [node.id: $0] }
            .id(node.id)
    }

    private var line: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchOrgChartTokens.lineColor])
            .frame(width: MonarchOrgChartTokens.lineWidth, height: MonarchOrgChartTokens.connectorLength)
            .accessibilityHidden(true)
    }

    /// The top of a report's column: half of the horizontal line on each
    /// side (not past the first and last reports) and a stub down to the card.
    private func connector(index: Int, count: Int) -> some View {
        let halves = MonarchOrgChartTokens.connectorHalves(index: index, count: count)
        let color = theme.colors[role: MonarchOrgChartTokens.lineColor]
        return ZStack(alignment: .top) {
            HStack(spacing: 0) {
                Rectangle().fill(color).opacity(halves.leading ? 1 : 0)
                Rectangle().fill(color).opacity(halves.trailing ? 1 : 0)
            }
            .frame(height: MonarchOrgChartTokens.lineWidth)
            Rectangle()
                .fill(color)
                .frame(width: MonarchOrgChartTokens.lineWidth)
        }
        .frame(height: MonarchOrgChartTokens.connectorLength)
        .accessibilityHidden(true)
    }
}

/// The chip under a manager's card: a chevron and the number of reports,
/// or a spinner while the team loads. A pointer and touch shortcut; VoiceOver
/// opens the team with the card's action, so the chip is hidden from it.
struct MonarchOrgChartChip: View {
    let node: MonarchOrgChartNode
    let context: MonarchOrgChartContext

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let isLoading = context.isLoading(node)
        Button {
            context.toggle(node)
        } label: {
            HStack(spacing: MonarchOrgChartTokens.chipSpacing) {
                if isLoading {
                    ProgressView().controlSize(.mini)
                } else {
                    MonarchIcon(
                        systemName: MonarchOrgChartTokens.chevronSymbol(isExpanded: context.isExpanded(node)),
                        size: .sm
                    )
                    .scaleEffect(0.75)
                    .frame(width: 12, height: 12)
                }
                Text(context.tree.reportCount(node), format: .number)
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
            }
            .foregroundStyle(theme.colors[role: MonarchOrgChartTokens.chipText(isEnabled: isEnabled)])
            .padding(.horizontal, MonarchTokens.Space.space1_5)
            .padding(.vertical, MonarchTokens.Space.space0_5)
            .background(theme.colors[role: MonarchOrgChartTokens.chipFill], in: Capsule())
            .overlay(
                Capsule().strokeBorder(
                    theme.colors[role: MonarchOrgChartTokens.chipBorder],
                    lineWidth: MonarchOrgChartTokens.lineWidth
                )
            )
            .overlay {
                // The touch target stays 44 points; only the drawn chip is small, as on the web.
                Color.clear
                    .frame(
                        width: MonarchOrgChartTokens.chipTarget(zoom: context.zoom),
                        height: MonarchOrgChartTokens.chipTarget(zoom: context.zoom)
                    )
                    .contentShape(Rectangle())
            }
        }
        .buttonStyle(.plain)
        .disabled(isLoading)
        .accessibilityHidden(true)
    }
}
