import SwiftUI

/// The drawing of a force graph: links, nodes and labels on a SwiftUI
/// `Canvas`, with drag to move a node or pan, and pinch to zoom.
///
/// A graph has no reading direction, so the canvas is not mirrored in
/// right-to-left layouts; labels are laid out as text and read correctly.
struct MonarchForceGraphCanvas: View {
    let nodes: [MonarchForceGraphNode]
    let graph: MonarchForceGraphTokens.Graph
    let kinds: [MonarchForceGraphEdgeKind]
    let positions: [String: CGPoint]
    let viewport: MonarchForceGraphTokens.Viewport
    let active: String?
    let selectedID: String?
    let showsLabels: Bool
    let onTap: (String?) -> Void
    let onMove: (String, CGPoint) -> Void
    let onPan: (CGSize) -> Void
    let onZoom: (CGFloat) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var drag: DragStart?
    @State private var panStart: CGSize?
    @State private var zoomStart: CGFloat?

    /// Where a drag began: on a node, or on the background.
    struct DragStart: Equatable {
        let node: String?
    }

    var body: some View {
        let lit = litNodes
        Canvas { context, canvasSize in
            drawEdges(in: &context)
            for node in nodes {
                guard let point = screen(node.id) else { continue }
                var layer = context
                if active != nil, !lit.contains(node.id) {
                    layer.opacity = MonarchForceGraphTokens.dimmedNodeOpacity
                }
                drawNode(node, at: point, in: &layer)
                if showsLabels, let label = layer.resolveSymbol(id: node.id) {
                    // Kept inside the canvas, so a node at the edge keeps its whole name.
                    let half = label.size.width / 2 + MonarchForceGraphTokens.labelGap
                    let at = CGPoint(
                        x: min(max(point.x, half), max(half, canvasSize.width - half)),
                        y: point.y + MonarchForceGraphTokens.nodeRadius + MonarchForceGraphTokens.labelGap
                    )
                    // A halo in the canvas colour keeps a link from striking through the name.
                    if let halo = layer.resolveSymbol(id: "halo-" + node.id) {
                        for (dx, dy) in [(-1.5, 0.0), (1.5, 0.0), (0.0, -1.5), (0.0, 1.5)] {
                            layer.draw(halo, at: CGPoint(x: at.x + dx, y: at.y + dy), anchor: .top)
                        }
                    }
                    layer.draw(label, at: at, anchor: .top)
                }
            }
        } symbols: {
            if showsLabels {
                ForEach(nodes) { node in
                    Text(node.label)
                        .monarchTextStyle(.caption)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .foregroundStyle(theme.colors.textDefault)
                        .frame(maxWidth: MonarchForceGraphTokens.labelMaxWidth)
                        .fixedSize(horizontal: false, vertical: true)
                        .tag(node.id)
                    Text(node.label)
                        .monarchTextStyle(.caption)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .foregroundStyle(theme.colors.bgSubtle)
                        .frame(maxWidth: MonarchForceGraphTokens.labelMaxWidth)
                        .fixedSize(horizontal: false, vertical: true)
                        .tag("halo-" + node.id)
                }
            }
        }
        .contentShape(Rectangle())
        .gesture(dragGesture.simultaneously(with: zoomGesture))
    }

    private func screen(_ id: String) -> CGPoint? {
        positions[id].map(viewport.toScreen)
    }

    /// The inspected node and its neighbours.
    private var litNodes: Set<String> {
        guard let active else { return [] }
        return Set([active] + (graph.neighbours[active] ?? []).map(\.id))
    }

    private func drawEdges(in context: inout GraphicsContext) {
        for link in graph.links {
            guard let a = screen(link.source), let b = screen(link.target) else { continue }
            let kind = kinds.first { $0.key == link.kind }
            let isLit = active != nil && (link.source == active || link.target == active)
            var path = Path()
            path.move(to: a)
            path.addLine(to: b)
            var layer = context
            layer.opacity =
                active == nil
                ? MonarchForceGraphTokens.edgeOpacity
                : isLit ? 1 : MonarchForceGraphTokens.dimmedEdgeOpacity
            layer.stroke(
                path,
                with: .color(theme.colors[role: MonarchChartLegendTokens.role(kind?.color ?? .neutral)]),
                style: StrokeStyle(
                    lineWidth: isLit ? MonarchForceGraphTokens.highlightedEdgeWidth : MonarchForceGraphTokens.edgeWidth,
                    lineCap: .round,
                    dash: kind?.isDashed == true ? MonarchForceGraphTokens.edgeDash : []
                )
            )
        }
    }

    private func drawNode(_ node: MonarchForceGraphNode, at point: CGPoint, in context: inout GraphicsContext) {
        let radius = MonarchForceGraphTokens.nodeRadius
        let circle = Path(
            ellipseIn: CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2)
        )
        let isOrphan = graph.degree(node.id) == 0
        if isOrphan {
            context.fill(circle, with: .color(theme.colors.bgSurface))
            context.stroke(
                circle,
                with: .color(theme.colors.textSubtle),
                style: StrokeStyle(
                    lineWidth: MonarchForceGraphTokens.orphanStroke,
                    dash: MonarchForceGraphTokens.orphanDash
                )
            )
        } else {
            let color = MonarchForceGraphTokens.nodeColor(node, graph: graph)
            context.fill(circle, with: .color(theme.colors[role: MonarchChartLegendTokens.role(color)]))
            context.stroke(
                circle,
                with: .color(theme.colors.bgSurface),
                lineWidth: MonarchForceGraphTokens.nodeStroke
            )
        }
        if let ring = MonarchForceGraphTokens.ringRole(isActive: node.id == active, isSelected: node.id == selectedID) {
            context.stroke(
                circle,
                with: .color(theme.colors[role: ring]),
                lineWidth: MonarchForceGraphTokens.ringStroke
            )
        }
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                let start =
                    drag
                    ?? DragStart(node: MonarchForceGraphTokens.hit(value.startLocation, positions: screenPositions))
                if drag == nil { drag = start }
                guard hypot(value.translation.width, value.translation.height) >= MonarchForceGraphTokens.tapSlop else {
                    return
                }
                if let node = start.node {
                    onMove(node, viewport.fromScreen(value.location))
                } else {
                    let origin = panStart ?? viewport.offset
                    if panStart == nil { panStart = origin }
                    onPan(
                        CGSize(
                            width: origin.width + value.translation.width,
                            height: origin.height + value.translation.height
                        )
                    )
                }
            }
            .onEnded { value in
                let start = drag
                drag = nil
                panStart = nil
                guard hypot(value.translation.width, value.translation.height) < MonarchForceGraphTokens.tapSlop else {
                    return
                }
                onTap(start?.node ?? MonarchForceGraphTokens.hit(value.startLocation, positions: screenPositions))
            }
    }

    private var zoomGesture: some Gesture {
        MagnifyGesture()
            .onChanged { value in
                let start = zoomStart ?? viewport.zoom
                if zoomStart == nil { zoomStart = start }
                onZoom(MonarchForceGraphTokens.zoomed(start, by: value.magnification))
            }
            .onEnded { _ in zoomStart = nil }
    }

    private var screenPositions: [String: CGPoint] {
        positions.mapValues(viewport.toScreen)
    }
}
