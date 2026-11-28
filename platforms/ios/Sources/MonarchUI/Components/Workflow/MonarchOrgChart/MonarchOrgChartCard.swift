import SwiftUI

/// A person's card: the content on its surface, with a dashed border for an
/// open position or a temporary line, a halo when focused and an accent
/// fill while a dragged person is over it. Used by the chart and the list.
struct MonarchOrgChartCard: View {
    let node: MonarchOrgChartNode
    let context: MonarchOrgChartContext

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchOrgChartTokens.cardAppearance(
            kind: node.kind,
            isDashed: node.isDashed,
            isFocused: node.id == context.focusID,
            isDropTarget: node.id == context.dropTargetID,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchOrgChartTokens.cardRadius, style: .continuous)
        MonarchOrgChartCardContent(node: node, appearance: appearance)
            .padding(MonarchOrgChartTokens.cardPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .background(theme.colors[role: appearance.fill], in: shape)
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    style: StrokeStyle(
                        lineWidth: MonarchOrgChartTokens.lineWidth,
                        dash: appearance.isDashed ? MonarchOrgChartTokens.borderDash : []
                    )
                )
            )
            .background {
                if let halo = appearance.halo {
                    shape.stroke(theme.colors[role: halo], lineWidth: MonarchOrgChartTokens.haloWidth * 2)
                }
            }
            .modifier(MonarchOrgChartNodeBehavior(node: node, context: context))
    }
}
