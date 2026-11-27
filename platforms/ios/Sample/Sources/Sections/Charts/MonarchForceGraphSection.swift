import MonarchUI
import SwiftUI

struct MonarchForceGraphSection: CataloguePage {
    static let title = "ForceGraph"

    @State private var showsLabels = true
    @State private var status = MonarchChartFrameStatus.ready
    @State private var selected: String?

    private let nodes = [
        MonarchForceGraphNode("Roadmap", group: "plans", description: "Product space"),
        MonarchForceGraphNode("Pricing", group: "plans"),
        MonarchForceGraphNode("Launch plan", group: "plans"),
        MonarchForceGraphNode("Interviews", group: "research"),
        MonarchForceGraphNode("Survey", group: "research"),
        MonarchForceGraphNode("Personas", group: "research"),
        MonarchForceGraphNode("Design review", group: "design"),
        MonarchForceGraphNode("Style guide", group: "design"),
        MonarchForceGraphNode("Inbox"),
    ]

    private let edges = [
        MonarchForceGraphEdge("Roadmap", "Pricing"),
        MonarchForceGraphEdge("Roadmap", "Launch plan"),
        MonarchForceGraphEdge("Roadmap", "Interviews", kind: "cites"),
        MonarchForceGraphEdge("Interviews", "Survey", kind: "cites"),
        MonarchForceGraphEdge("Interviews", "Personas"),
        MonarchForceGraphEdge("Personas", "Design review", kind: "cites"),
        MonarchForceGraphEdge("Design review", "Style guide"),
        MonarchForceGraphEdge("Launch plan", "Design review"),
    ]

    var body: some View {
        Toggle("Node labels", isOn: $showsLabels)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchForceGraph(
            "Linked notes",
            nodes: nodes,
            edges: edges,
            groups: [
                MonarchForceGraphGroup("plans", label: "Plans"),
                MonarchForceGraphGroup("research", label: "Research"),
                MonarchForceGraphGroup("design", label: "Design"),
            ],
            edgeKinds: [MonarchForceGraphEdgeKind("cites", label: "Cites", color: .data4, isDashed: true)],
            description: "Notes in the product space",
            status: status,
            height: 360,
            showsLabels: showsLabels,
            selectedID: selected,
            emptyMessage: "Link two notes to see them here.",
            onRetry: { status = .ready },
            onSelect: { selected = $0.id }
        )
        CatalogueCaption("Drag a node to move it, drag the background to pan, pinch to zoom.")
    }
}
