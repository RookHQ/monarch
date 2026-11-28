import SwiftUI

/// A network of linked nodes, placed by a small built-in force simulation:
/// groups in data colours, link kinds with dashes, drag, pan and zoom.
///
/// ```swift
/// MonarchForceGraph(
///     "Linked notes",
///     nodes: [
///         MonarchForceGraphNode("Roadmap", group: "plans"),
///         MonarchForceGraphNode("Pricing", group: "plans"),
///         MonarchForceGraphNode("Interviews", group: "research"),
///     ],
///     edges: [
///         MonarchForceGraphEdge("Roadmap", "Pricing"),
///         MonarchForceGraphEdge("Roadmap", "Interviews"),
///     ]
/// )
/// ```
///
/// The graph sits in a ``MonarchChartFrame`` with loading, empty and error
/// states, a legend of groups and "View as table", which lists every node
/// with its group and links. The layout is deterministic and computed once
/// per set of nodes, links and options. Drag a node to move it, drag the
/// background to pan, pinch or use the buttons to zoom, and Reset view to
/// undo all three. Tapping a node highlights it and its neighbours, writes
/// its details under the canvas and calls `onSelect`.
///
/// Accessibility: the canvas is one adjustable element named by the
/// generated summary (nodes, links, groups and the most connected node).
/// Swiping up and down moves through the nodes in label order, reading each
/// one's group and links; with `onSelect`, a double tap selects it. It
/// carries an accessibility chart descriptor of each node's number of links.
/// The zoom buttons are labelled; the table is the full list alternative.
public struct MonarchForceGraph: View {
    private let title: String
    private let nodes: [MonarchForceGraphNode]
    private let edges: [MonarchForceGraphEdge]
    private let groups: [MonarchForceGraphGroup]?
    private let edgeKinds: [MonarchForceGraphEdgeKind]
    private let description: String?
    private let status: MonarchChartFrameStatus
    private let height: CGFloat
    private let showsLabels: Bool
    private let selectedID: String?
    private let summary: String?
    private let emptyMessage: String?
    private let footer: String?
    private let layoutOptions: MonarchForceGraphLayoutOptions
    private let labels: MonarchForceGraphLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchForceGraphNode) -> Void)?

    @State private var active: String?
    @State private var zoom: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var moved: [String: CGPoint] = [:]
    @State private var cache = MonarchForceGraphLayoutCache()
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A force graph.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - nodes: The nodes.
    ///   - edges: The links. Links to unknown nodes and to the same node are ignored.
    ///   - groups: The groups in legend order. The distinct group keys of the nodes, labelled by key, when `nil`.
    ///   - edgeKinds: The kinds of link, listed in a second legend.
    ///   - description: One line under the title.
    ///   - status: Whether the graph, a placeholder, an empty state or an error shows.
    ///     A ready chart without nodes shows the empty state.
    ///   - height: The height of the canvas. 420 points by default.
    ///   - showsLabels: Whether node labels are drawn.
    ///   - selectedID: The id of a node drawn as selected, such as the open document.
    ///   - summary: Replaces the generated summary that VoiceOver reads for the graph.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart.
    ///   - layoutOptions: Tunes the force layout.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Runs with a node when it is tapped or selected with VoiceOver, for example to open it.
    public init(
        _ title: String,
        nodes: [MonarchForceGraphNode],
        edges: [MonarchForceGraphEdge],
        groups: [MonarchForceGraphGroup]? = nil,
        edgeKinds: [MonarchForceGraphEdgeKind] = [],
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        height: CGFloat? = nil,
        showsLabels: Bool = true,
        selectedID: String? = nil,
        summary: String? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        layoutOptions: MonarchForceGraphLayoutOptions = MonarchForceGraphLayoutOptions(),
        labels: MonarchForceGraphLabels = MonarchForceGraphLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchForceGraphNode) -> Void)? = nil
    ) {
        self.title = title
        self.nodes = nodes
        self.edges = edges
        self.groups = groups
        self.edgeKinds = edgeKinds
        self.description = description
        self.status = status
        self.height = height ?? MonarchForceGraphTokens.defaultHeight
        self.showsLabels = showsLabels
        self.selectedID = selectedID
        self.summary = summary
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.layoutOptions = layoutOptions
        self.labels = labels
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchForceGraphTokens.status(status, nodeCount: nodes.count)
        let graph = MonarchForceGraphTokens.graph(nodes: nodes, edges: edges, groups: groups)
        let legend = MonarchForceGraphTokens.legendGroups(graph)
        MonarchChartFrame(
            title,
            description: description,
            status: resolved,
            table: MonarchForceGraphTokens.table(
                graph: graph,
                kinds: edgeKinds,
                noGroup: noGroup,
                labels: labels
            ),
            legend: legend.count > 1
                ? legend.map {
                    MonarchChartLegendItem($0.key, label: $0.label, color: graph.groupColor[$0.key] ?? .neutral)
                } : [],
            legendMarker: .dot,
            emptyMessage: emptyMessage,
            footer: footer,
            height: height,
            contentAccessibility: .contain,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            chart(graph: graph)
        }
    }

    private var noGroup: String {
        labels.noGroup ?? MonarchForceGraphStrings.noGroup
    }

    private func number(_ value: Int) -> String {
        value.formatted(.number.locale(locale))
    }

    private func connections(_ value: Int) -> String {
        MonarchForceGraphTokens.count(
            value,
            text: number(value),
            one: labels.connectionCountOne ?? MonarchForceGraphStrings.connectionCountOne,
            many: labels.connectionCount ?? MonarchForceGraphStrings.connectionCount
        )
    }

    private func layout(graph: MonarchForceGraphTokens.Graph) -> [String: CGPoint] {
        let ids = nodes.map(\.id).filter { graph.byID[$0] != nil }
        let key = MonarchForceGraphTokens.layoutKey(ids: ids, links: graph.links, options: layoutOptions)
        let base = cache.layout(for: key) {
            MonarchForceGraphLayout.layout(
                ids: ids,
                links: graph.links.map { ($0.source, $0.target) },
                options: layoutOptions
            )
        }
        return base.merging(moved.filter { base[$0.key] != nil }) { _, new in new }
    }

    private func describe(_ id: String, graph: MonarchForceGraphTokens.Graph) -> String {
        guard let node = graph.byID[id] else { return "" }
        return MonarchForceGraphTokens.describe(
            label: node.label,
            group: MonarchForceGraphTokens.groupName(node, graph: graph, noGroup: noGroup),
            connections: connections(graph.degree(id)),
            names: MonarchForceGraphTokens.linkNames(id, graph: graph, kinds: edgeKinds),
            labels: labels
        )
    }

    private func generatedSummary(graph: MonarchForceGraphTokens.Graph) -> String {
        let groupCount = MonarchForceGraphTokens.legendGroups(graph).count
        let hub = MonarchForceGraphTokens.hub(graph)
        return MonarchForceGraphTokens.summary(
            nodes: MonarchForceGraphTokens.count(
                graph.byID.count,
                text: number(graph.byID.count),
                one: labels.nodeCountOne ?? MonarchForceGraphStrings.nodeCountOne,
                many: labels.nodeCount ?? MonarchForceGraphStrings.nodeCount
            ),
            links: MonarchForceGraphTokens.count(
                graph.links.count,
                text: number(graph.links.count),
                one: labels.linkCountOne ?? MonarchForceGraphStrings.linkCountOne,
                many: labels.linkCount ?? MonarchForceGraphStrings.linkCount
            ),
            groups: groupCount == 0
                ? nil
                : MonarchForceGraphTokens.count(
                    groupCount,
                    text: number(groupCount),
                    one: labels.groupCountOne ?? MonarchForceGraphStrings.groupCountOne,
                    many: labels.groupCount ?? MonarchForceGraphStrings.groupCount
                ),
            hub: hub.flatMap { hub in
                graph.byID[hub.id].map { ($0.label, connections(hub.degree)) }
            },
            labels: labels
        )
    }

    private func chart(graph: MonarchForceGraphTokens.Graph) -> some View {
        let positions = layout(graph: graph)
        let current = active.flatMap { graph.byID[$0] == nil ? nil : $0 }
        let spoken = summary ?? generatedSummary(graph: graph)
        let kinds = MonarchForceGraphTokens.legendKinds(edgeKinds, graph: graph)
        return VStack(alignment: .leading, spacing: MonarchForceGraphTokens.sectionSpacing) {
            ZStack(alignment: .topTrailing) {
                canvas(graph: graph, positions: positions, current: current, spoken: spoken)
                controls
            }
            .frame(height: height)
            .background(theme.colors.bgSubtle, in: MonarchShapes.control)
            .clipShape(MonarchShapes.control)
            if let current, let node = graph.byID[current] {
                details(node, graph: graph)
            }
            if !kinds.isEmpty {
                MonarchChartLegend(
                    kinds.map {
                        MonarchChartLegendItem(
                            $0.key,
                            label: $0.label,
                            color: $0.color ?? .neutral,
                            isDashed: $0.isDashed
                        )
                    },
                    marker: .line,
                    legendLabel: labels.edgeLegend ?? MonarchForceGraphStrings.edgeLegend
                )
            }
        }
    }

    private func canvas(
        graph: MonarchForceGraphTokens.Graph,
        positions: [String: CGPoint],
        current: String?,
        spoken: String
    ) -> some View {
        GeometryReader { geometry in
            MonarchForceGraphCanvas(
                nodes: nodes.filter { graph.byID[$0.id] != nil },
                graph: graph,
                kinds: edgeKinds,
                positions: positions,
                viewport: MonarchForceGraphTokens.viewport(
                    layout: positions,
                    size: geometry.size,
                    showsLabels: showsLabels,
                    zoom: zoom,
                    offset: offset
                ),
                active: current,
                selectedID: selectedID,
                showsLabels: showsLabels,
                onTap: { id in tap(id, graph: graph) },
                onMove: { id, point in moved[id] = point },
                onPan: { offset = $0 },
                onZoom: { zoom = $0 }
            )
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spoken)
        .accessibilityValue(current.map { describe($0, graph: graph) } ?? "")
        .accessibilityAddTraits(onSelect == nil ? [] : .isButton)
        .accessibilityAdjustableAction { direction in
            active = MonarchForceGraphTokens.adjusted(
                current,
                forward: direction == .increment,
                order: graph.order
            )
        }
        .accessibilityAction {
            if let current { tap(current, graph: graph) }
        }
        .accessibilityChartDescriptor(
            MonarchChartFrameDescriptor(
                title: title,
                summary: spoken,
                table: MonarchForceGraphTokens.audioGraphTable(graph: graph, labels: labels)
            )
        )
    }

    private var controls: some View {
        HStack(spacing: MonarchForceGraphTokens.controlSpacing) {
            zoomButton(
                labels.zoomIn ?? MonarchForceGraphStrings.zoomIn,
                systemImage: MonarchForceGraphTokens.zoomInSymbol,
                isEnabled: zoom < MonarchForceGraphTokens.zoomRange.upperBound
            ) {
                zoom = MonarchForceGraphTokens.zoomed(zoom, by: MonarchForceGraphTokens.zoomStep)
            }
            zoomButton(
                labels.zoomOut ?? MonarchForceGraphStrings.zoomOut,
                systemImage: MonarchForceGraphTokens.zoomOutSymbol,
                isEnabled: zoom > MonarchForceGraphTokens.zoomRange.lowerBound
            ) {
                zoom = MonarchForceGraphTokens.zoomed(zoom, by: 1 / MonarchForceGraphTokens.zoomStep)
            }
            MonarchButton(labels.resetView ?? MonarchForceGraphStrings.resetView, variant: .secondary, size: .sm) {
                zoom = 1
                offset = .zero
                moved = [:]
            }
        }
        .padding(MonarchForceGraphTokens.controlInset)
    }

    private func zoomButton(
        _ title: String,
        systemImage: String,
        isEnabled: Bool,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage).labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.secondary, size: .sm))
        .disabled(!isEnabled)
    }

    private func details(_ node: MonarchForceGraphNode, graph: MonarchForceGraphTokens.Graph) -> some View {
        VStack(alignment: .leading, spacing: MonarchForceGraphTokens.controlSpacing) {
            Text(node.label)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            ForEach(
                MonarchForceGraphTokens.details(
                    node,
                    graph: graph,
                    connections: number(graph.degree(node.id)),
                    noGroup: noGroup,
                    labels: labels
                ),
                id: \.self
            ) { line in
                Text(line)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        // VoiceOver reads the same details as the canvas's value.
        .accessibilityHidden(true)
    }

    private func tap(_ id: String?, graph: MonarchForceGraphTokens.Graph) {
        active = id
        guard let id, let node = graph.byID[id], let onSelect else { return }
        onSelect(node)
    }
}

/// Keeps the last layout, so the simulation runs once per set of nodes,
/// links and options rather than on every view update.
@MainActor
final class MonarchForceGraphLayoutCache {
    private var key: String?
    private var points: [String: CGPoint] = [:]

    /// The positions for a key, computed only when the key changes.
    func layout(for key: String, compute: () -> [String: CGPoint]) -> [String: CGPoint] {
        if key != self.key {
            points = compute()
            self.key = key
        }
        return points
    }
}

#if DEBUG
    #Preview("MonarchForceGraph") {
        MonarchPreviewMatrix {
            MonarchForceGraph(
                "Linked notes",
                nodes: [
                    MonarchForceGraphNode("Roadmap", group: "plans"),
                    MonarchForceGraphNode("Pricing", group: "plans"),
                    MonarchForceGraphNode("Interviews", group: "research"),
                    MonarchForceGraphNode("Inbox"),
                ],
                edges: [
                    MonarchForceGraphEdge("Roadmap", "Pricing"),
                    MonarchForceGraphEdge("Roadmap", "Interviews", kind: "cites"),
                ],
                edgeKinds: [MonarchForceGraphEdgeKind("cites", label: "Cites", isDashed: true)],
                height: 240
            )
        }
    }
#endif
