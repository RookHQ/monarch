import MonarchTokens
import SwiftUI

/// The list layout: every visible person as a full-width card, indented by level, with the team chip under the
/// card and a rail line beside each open team, as on the web. The phone and accessibility-size form of the chart;
/// it reads in the same order.
struct MonarchOrgChartList: View {
    let context: MonarchOrgChartContext

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var indent = MonarchOrgChartTokens.indent

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 0) {
            ForEach(context.tree.visibleRows(expanded: context.expanded), id: \.node.id) { row in
                let steps = MonarchOrgChartTokens.indentSteps(level: row.level)
                VStack(alignment: .leading, spacing: 0) {
                    MonarchOrgChartCard(node: row.node, context: context)
                    if context.tree.hasReports(row.node) {
                        // On the card's bottom edge, 12 points in from the leading side, as on the web.
                        MonarchOrgChartChip(node: row.node, context: context)
                            .padding(.leading, MonarchTokens.Space.space3)
                            .padding(.top, -MonarchTokens.Space.space2_5)
                    }
                }
                .padding(.leading, indent * CGFloat(steps))
                .padding(.vertical, MonarchOrgChartTokens.listRowSpacing / 2)
                .background(alignment: .leading) { rails(steps: steps) }
                .id(row.node.id)
            }
        }
    }

    /// One rail line for each open team above the row, at the position of the chip of that team's manager.
    private func rails(steps: Int) -> some View {
        ZStack(alignment: .leading) {
            ForEach(0..<steps, id: \.self) { step in
                Rectangle()
                    .fill(theme.colors[role: MonarchOrgChartTokens.lineColor])
                    .frame(width: MonarchOrgChartTokens.lineWidth)
                    .padding(.leading, indent * CGFloat(step) + indent / 2)
            }
        }
        .frame(maxHeight: .infinity)
        .accessibilityHidden(true)
    }
}
