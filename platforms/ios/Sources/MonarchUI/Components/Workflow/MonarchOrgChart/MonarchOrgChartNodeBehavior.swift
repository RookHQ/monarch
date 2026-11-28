import SwiftUI

/// Makes a card or a list row behave as one person: a button that selects
/// (when the chart has `onSelect`), one VoiceOver element with the node's
/// label and team state, actions to open or close the team and to move, a
/// Move item in the context menu, and drag and drop onto other cards.
struct MonarchOrgChartNodeBehavior: ViewModifier {
    let node: MonarchOrgChartNode
    let context: MonarchOrgChartContext

    func body(content: Content) -> some View {
        let tree = context.tree
        let isExpanded = context.isExpanded(node)
        let isLoading = context.isLoading(node)
        let toggleName = MonarchOrgChartTokens.toggleActionName(
            hasReports: tree.hasReports(node),
            isExpanded: isExpanded,
            isLoading: isLoading,
            labels: context.labels
        )
        let canMove = context.canMove(node)
        let moveName = context.labels.move ?? MonarchOrgChartStrings.move
        selectable(content)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                MonarchOrgChartTokens.accessibilityLabel(
                    node,
                    dottedManagers: tree.dottedManagers(of: node.id, lines: context.dottedLines),
                    labels: context.labels
                )
            )
            .accessibilityValue(
                MonarchOrgChartTokens.accessibilityValue(
                    reportCount: tree.reportCount(node),
                    hasReports: tree.hasReports(node),
                    isExpanded: isExpanded,
                    isLoading: isLoading,
                    labels: context.labels
                )
            )
            .accessibilityAddTraits(context.canSelect ? .isButton : [])
            .accessibilityAddTraits(node.id == context.focusID ? .isSelected : [])
            .accessibilityAction {
                if context.canSelect { context.select(node) }
            }
            .accessibilityActions {
                if let toggleName {
                    Button(toggleName) { context.toggle(node) }
                }
                if canMove {
                    Button(moveName) { context.requestMove(node, .unspecified) }
                }
            }
            .contextMenu {
                if canMove {
                    Button(moveName, systemImage: MonarchOrgChartTokens.moveSymbol) {
                        context.requestMove(node, .unspecified)
                    }
                }
            }
            .modifier(
                MonarchOrgChartDragSource(payload: MonarchOrgChartTokens.dragPayload(node.id), isEnabled: canMove)
            )
            .dropDestination(
                for: String.self,
                action: { payloads, _ in drop(payloads) },
                isTargeted: { isTargeted in
                    if context.canMove, node.kind == .person {
                        context.setDropTarget(node.id, isTargeted)
                    }
                }
            )
    }

    @ViewBuilder
    private func selectable(_ content: Content) -> some View {
        if context.canSelect {
            Button {
                context.select(node)
            } label: {
                content.contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        } else {
            content
        }
    }

    @MainActor
    private func drop(_ payloads: [String]) -> Bool {
        context.setDropTarget(node.id, false)
        guard context.canMove,
            let id = payloads.lazy.compactMap(MonarchOrgChartTokens.movingID(fromPayload:)).first,
            context.tree.canMove(id, onto: node.id),
            let moving = context.tree.byID[id]
        else { return false }
        context.requestMove(moving, .manager(node.id))
        return true
    }
}

/// Makes a card draggable only when the person can move.
struct MonarchOrgChartDragSource: ViewModifier {
    let payload: String
    let isEnabled: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content.draggable(payload)
        } else {
            content
        }
    }
}
