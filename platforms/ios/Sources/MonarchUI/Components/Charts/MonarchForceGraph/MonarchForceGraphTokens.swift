import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchForceGraph``.
enum MonarchForceGraphTokens {
    /// The canvas height by default: 420 points.
    static let defaultHeight: CGFloat = MonarchTokens.Space.space20 * 5 + MonarchTokens.Space.space5
    /// The margin kept between the graph and the canvas edges when it is fitted.
    static let padding: CGFloat = MonarchTokens.Space.space8 + MonarchTokens.Space.space1
    /// The room kept under the lowest node for its label when it is fitted.
    static let labelAllowance: CGFloat = MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5
    /// The radius of a node.
    static let nodeRadius: CGFloat = MonarchTokens.Space.space2 + MonarchTokens.Space.px
    /// The gap between a node and its label.
    static let labelGap: CGFloat = MonarchTokens.Space.space1
    /// The widest a node label is drawn before it is truncated.
    static let labelMaxWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The outline of a node against the canvas.
    static let nodeStroke: CGFloat = MonarchTokens.Space.space0_5
    /// The outline of the selected or inspected node.
    static let ringStroke: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The outline of a node without links.
    static let orphanStroke: CGFloat = MonarchTokens.Space.px * 1.5
    /// The dash of a node without links.
    static let orphanDash: [CGFloat] = [
        MonarchTokens.Space.space0_5 + MonarchTokens.Space.px, MonarchTokens.Space.space0_5,
    ]
    /// The width of a link.
    static let edgeWidth: CGFloat = MonarchTokens.Space.px * 1.5
    /// The width of a link of the inspected node.
    static let highlightedEdgeWidth: CGFloat = MonarchTokens.Space.px * 2.5
    /// The dash of a dashed link.
    static let edgeDash: [CGFloat] = [
        MonarchTokens.Space.space1_5, MonarchTokens.Space.space1 + MonarchTokens.Space.px,
    ]
    /// The opacity of a link at rest.
    static let edgeOpacity: Double = 0.7
    /// The opacity of a link that is not linked to the inspected node.
    static let dimmedEdgeOpacity: Double = 0.2
    /// The opacity of a node that is not the inspected node or one of its neighbours.
    static let dimmedNodeOpacity: Double = 0.35
    /// The radius around a node that a touch hits: half the 44-point target.
    static let hitRadius: CGFloat = MonarchAccessibility.minimumHitTarget / 2
    /// How far a touch moves before it is a drag rather than a tap.
    static let tapSlop: CGFloat = MonarchTokens.Space.space1
    /// The smallest and largest zoom.
    static let zoomRange: ClosedRange<CGFloat> = 0.35...3
    /// The zoom step of the buttons.
    static let zoomStep: CGFloat = 1.25
    /// The largest scale of the fitted layout.
    static let maximumFit: CGFloat = 1.5
    /// The gap between the zoom controls.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space1
    /// The inset of the zoom controls from the canvas corner.
    static let controlInset: CGFloat = MonarchTokens.Space.space2
    /// The gap between the canvas, the details and the link legend.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbols of the zoom buttons.
    static let zoomInSymbol = "plus"
    /// The SF Symbol of the zoom-out button.
    static let zoomOutSymbol = "minus"

    /// The graph as the view uses it: valid links, neighbours, colours and order.
    struct Graph {
        /// The nodes by id.
        let byID: [String: MonarchForceGraphNode]
        /// The links whose ends both exist and differ.
        let links: [MonarchForceGraphEdge]
        /// Each node's neighbours, with the kind of the link.
        let neighbours: [String: [(id: String, kind: String?)]]
        /// Node ids by label, for VoiceOver and the table.
        let order: [String]
        /// The groups in legend order: given, or the distinct group keys of the nodes.
        let groups: [MonarchForceGraphGroup]
        /// The colour slot of each group.
        let groupColor: [String: MonarchChartColor]

        /// The number of links of a node.
        func degree(_ id: String) -> Int {
            neighbours[id]?.count ?? 0
        }
    }

    /// Builds the graph. Pure, so it is tested without a view.
    static func graph(
        nodes: [MonarchForceGraphNode],
        edges: [MonarchForceGraphEdge],
        groups given: [MonarchForceGraphGroup]?
    ) -> Graph {
        var byID: [String: MonarchForceGraphNode] = [:]
        for node in nodes where byID[node.id] == nil {
            byID[node.id] = node
        }
        let links = edges.filter { byID[$0.source] != nil && byID[$0.target] != nil && $0.source != $0.target }
        var neighbours: [String: [(id: String, kind: String?)]] = [:]
        for id in byID.keys { neighbours[id] = [] }
        for link in links {
            neighbours[link.source, default: []].append((link.target, link.kind))
            neighbours[link.target, default: []].append((link.source, link.kind))
        }
        let groups: [MonarchForceGraphGroup]
        if let given {
            groups = given
        } else {
            var keys: [String] = []
            for node in nodes {
                if let group = node.group, !keys.contains(group) { keys.append(group) }
            }
            groups = keys.map { MonarchForceGraphGroup($0, label: $0) }
        }
        var groupColor: [String: MonarchChartColor] = [:]
        for (index, group) in groups.enumerated() {
            groupColor[group.key] = MonarchChartLegendTokens.color(group.color, index: index)
        }
        let order = byID.values
            .sorted {
                let order = $0.label.localizedStandardCompare($1.label)
                return order == .orderedSame ? $0.id < $1.id : order == .orderedAscending
            }
            .map(\.id)
        return Graph(
            byID: byID,
            links: links,
            neighbours: neighbours,
            order: order,
            groups: groups,
            groupColor: groupColor
        )
    }

    /// The colour of a node: neutral without links or without a group, else its group's.
    static func nodeColor(_ node: MonarchForceGraphNode, graph: Graph) -> MonarchChartColor {
        guard graph.degree(node.id) > 0, let group = node.group else { return .neutral }
        return graph.groupColor[group] ?? .neutral
    }

    /// The node's group as written: its group's label, the key, or "No group".
    static func groupName(_ node: MonarchForceGraphNode, graph: Graph, noGroup: String) -> String {
        guard let key = node.group else { return noGroup }
        return graph.groups.first { $0.key == key }?.label ?? key
    }

    /// The node's links as written: each neighbour, with the kind in brackets.
    static func linkNames(_ id: String, graph: Graph, kinds: [MonarchForceGraphEdgeKind]) -> [String] {
        (graph.neighbours[id] ?? []).map { link in
            let name = graph.byID[link.id]?.label ?? link.id
            guard let kind = link.kind, let label = kinds.first(where: { $0.key == kind })?.label else { return name }
            return "\(name) (\(label))"
        }
    }

    /// The groups shown in the legend: those used by a node.
    static func legendGroups(_ graph: Graph) -> [MonarchForceGraphGroup] {
        let used = Set(graph.byID.values.compactMap(\.group))
        return graph.groups.filter { used.contains($0.key) }
    }

    /// The kinds shown in the link legend: those used by a link.
    static func legendKinds(_ kinds: [MonarchForceGraphEdgeKind], graph: Graph) -> [MonarchForceGraphEdgeKind] {
        let used = Set(graph.links.compactMap(\.kind))
        return kinds.filter { used.contains($0.key) }
    }

    /// The most connected node and its number of links, the first by label on a tie.
    static func hub(_ graph: Graph) -> (id: String, degree: Int)? {
        var best: (id: String, degree: Int)?
        for id in graph.order where graph.degree(id) > (best?.degree ?? 0) {
            best = (id, graph.degree(id))
        }
        return best
    }

    /// The status shown: empty when the chart is ready but has no nodes.
    static func status(_ status: MonarchChartFrameStatus, nodeCount: Int) -> MonarchChartFrameStatus {
        status == .ready && nodeCount == 0 ? .empty : status
    }

    /// The node after a VoiceOver adjustment, in label order. The first
    /// swipe goes to the first node (or the last, backwards); the ends stop.
    static func adjusted(_ current: String?, forward: Bool, order: [String]) -> String? {
        guard !order.isEmpty else { return nil }
        guard let current, let index = order.firstIndex(of: current) else {
            return forward ? order.first : order.last
        }
        return order[min(order.count - 1, max(0, index + (forward ? 1 : -1)))]
    }

    /// The ring around a node: default text while inspected, the accent while
    /// selected, none otherwise. Inspection wins, so both are never drawn at once.
    static func ringRole(isActive: Bool, isSelected: Bool) -> MonarchColorRole? {
        if isActive { return \.textDefault }
        if isSelected { return \.accentSolid }
        return nil
    }

    /// The node under a touch: the nearest within the hit radius.
    static func hit(_ point: CGPoint, positions: [String: CGPoint], radius: CGFloat = hitRadius) -> String? {
        var best: (id: String, distance: CGFloat)?
        for (id, position) in positions {
            let distance = hypot(position.x - point.x, position.y - point.y)
            if distance <= radius, distance < (best?.distance ?? .infinity) {
                best = (id, distance)
            }
        }
        return best?.id
    }

    /// The zoom after a step, kept in range.
    static func zoomed(_ zoom: CGFloat, by factor: CGFloat) -> CGFloat {
        min(zoomRange.upperBound, max(zoomRange.lowerBound, zoom * factor))
    }

    /// Maps layout positions to the canvas and back: the layout is fitted
    /// into the canvas, then zoomed about the centre and panned.
    struct Viewport: Equatable {
        /// The size of the canvas.
        let size: CGSize
        /// The centre of the layout's bounds.
        let center: CGPoint
        /// The scale that fits the layout into the canvas.
        let fit: CGFloat
        /// The zoom chosen by the person.
        let zoom: CGFloat
        /// The pan chosen by the person.
        let offset: CGSize

        /// A canvas position for a layout position.
        func toScreen(_ point: CGPoint) -> CGPoint {
            CGPoint(
                x: size.width / 2 + (point.x - center.x) * fit * zoom + offset.width,
                y: size.height / 2 + (point.y - center.y) * fit * zoom + offset.height
            )
        }

        /// A layout position for a canvas position.
        func fromScreen(_ point: CGPoint) -> CGPoint {
            CGPoint(
                x: (point.x - size.width / 2 - offset.width) / (fit * zoom) + center.x,
                y: (point.y - size.height / 2 - offset.height) / (fit * zoom) + center.y
            )
        }
    }

    /// The viewport that fits the layout into a canvas, before zoom and pan.
    static func viewport(
        layout: [String: CGPoint],
        size: CGSize,
        showsLabels: Bool,
        zoom: CGFloat,
        offset: CGSize
    ) -> Viewport {
        let xs = layout.values.map(\.x)
        let ys = layout.values.map(\.y)
        let minX = xs.min() ?? 0
        let maxX = xs.max() ?? 0
        let minY = ys.min() ?? 0
        let maxY = ys.max() ?? 0
        let fit = min(
            (size.width - 2 * padding) / max(1, maxX - minX),
            (size.height - 2 * padding - (showsLabels ? labelAllowance : 0)) / max(1, maxY - minY),
            maximumFit
        )
        return Viewport(
            size: size,
            center: CGPoint(x: (minX + maxX) / 2, y: (minY + maxY) / 2),
            fit: max(0.01, fit),
            zoom: zoom,
            offset: offset
        )
    }

    /// A stable key for a layout: equal for the same nodes, links and options.
    static func layoutKey(ids: [String], links: [MonarchForceGraphEdge], options: MonarchForceGraphLayoutOptions)
        -> String
    {
        var key = ids.joined(separator: "\u{1}") + "\u{2}"
        for link in links { key += link.source + "\u{1}" + link.target + "\u{2}" }
        return key
            + "\(options.linkDistance)|\(options.charge)|\(options.collide)|\(options.gravity)|\(options.ticks ?? -1)"
    }

    /// A count with its noun: the "one" format for 1, else the "many" format.
    static func count(_ value: Int, text: String, one: String, many: String) -> String {
        MonarchForceGraphStrings.fill(value == 1 ? one : many, text)
    }

    /// The generated summary: nodes, links, groups and the most connected node.
    /// The counts arrive written with their nouns, such as "12 nodes".
    static func summary(
        nodes: String,
        links: String,
        groups: String?,
        hub: (label: String, connections: String)?,
        labels: MonarchForceGraphLabels
    ) -> String {
        var text = MonarchForceGraphStrings.fill(labels.size ?? MonarchForceGraphStrings.size, nodes, links)
        if let groups {
            text += " " + MonarchForceGraphStrings.fill(labels.inGroups ?? MonarchForceGraphStrings.inGroups, groups)
        }
        text += "."
        if let hub {
            text +=
                " "
                + MonarchForceGraphStrings.fill(
                    labels.mostConnected ?? MonarchForceGraphStrings.mostConnected,
                    hub.label,
                    hub.connections
                )
        }
        return text
    }

    /// The lines written under the canvas for the inspected node: its group,
    /// its number of links and its description.
    static func details(
        _ node: MonarchForceGraphNode,
        graph: Graph,
        connections: String,
        noGroup: String,
        labels: MonarchForceGraphLabels
    ) -> [String] {
        let format = labels.detail ?? MonarchForceGraphStrings.detail
        var lines = [
            MonarchForceGraphStrings.fill(
                format,
                labels.group ?? MonarchForceGraphStrings.group,
                groupName(node, graph: graph, noGroup: noGroup)
            ),
            MonarchForceGraphStrings.fill(
                format,
                labels.connections ?? MonarchForceGraphStrings.connections,
                connections
            ),
        ]
        if let description = node.description, !description.isEmpty { lines.append(description) }
        return lines
    }

    /// What VoiceOver reads for a node: its label, its group, its number of links and their names.
    static func describe(
        label: String,
        group: String,
        connections: String,
        names: [String],
        labels: MonarchForceGraphLabels
    ) -> String {
        let head = MonarchForceGraphStrings.fill(
            labels.describe ?? MonarchForceGraphStrings.describe,
            label,
            group,
            connections
        )
        return names.isEmpty ? head : head + ": " + names.joined(separator: ", ")
    }

    /// The data table: one row per node, in label order, with its group and its links.
    static func table(
        graph: Graph,
        kinds: [MonarchForceGraphEdgeKind],
        noGroup: String,
        labels: MonarchForceGraphLabels
    ) -> MonarchChartTable {
        let nodes = graph.order.compactMap { graph.byID[$0] }
        return MonarchChartTable(
            categoryTitle: labels.node ?? MonarchForceGraphStrings.node,
            categories: nodes.map(\.label),
            series: [
                MonarchChartTableSeries(
                    labels.group ?? MonarchForceGraphStrings.group,
                    values: nodes.map { _ in 0 },
                    formattedValues: nodes.map { groupName($0, graph: graph, noGroup: noGroup) }
                ),
                MonarchChartTableSeries(
                    labels.connections ?? MonarchForceGraphStrings.connections,
                    values: nodes.map { Double(graph.degree($0.id)) },
                    formattedValues: nodes.map {
                        let names = linkNames($0.id, graph: graph, kinds: kinds)
                        return names.isEmpty ? MonarchChartFrameTokens.gapText : names.joined(separator: ", ")
                    }
                ),
            ]
        )
    }

    /// The table given to VoiceOver as an audio graph: each node's number of links.
    static func audioGraphTable(graph: Graph, labels: MonarchForceGraphLabels) -> MonarchChartTable {
        let nodes = graph.order.compactMap { graph.byID[$0] }
        let title = labels.connections ?? MonarchForceGraphStrings.connections
        return MonarchChartTable(
            categoryTitle: labels.node ?? MonarchForceGraphStrings.node,
            categories: nodes.map(\.label),
            series: [MonarchChartTableSeries(title, values: nodes.map { Double(graph.degree($0.id)) })],
            valueTitle: title
        )
    }
}
