import Accessibility
import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let notes = [
    MonarchForceGraphNode("Roadmap", group: "plans", description: "Product space"),
    MonarchForceGraphNode("Pricing", group: "plans"),
    MonarchForceGraphNode("Interviews", group: "research"),
    MonarchForceGraphNode("Survey", group: "research"),
    MonarchForceGraphNode("Inbox"),
]
private let links = [
    MonarchForceGraphEdge("Roadmap", "Pricing"),
    MonarchForceGraphEdge("Roadmap", "Interviews", kind: "cites"),
    MonarchForceGraphEdge("Interviews", "Survey", kind: "cites"),
    MonarchForceGraphEdge("Roadmap", "Missing"),
    MonarchForceGraphEdge("Pricing", "Pricing"),
]
private let kinds = [
    MonarchForceGraphEdgeKind("cites", label: "Cites", color: .data4, isDashed: true),
    MonarchForceGraphEdgeKind("unused", label: "Unused"),
]
private let groups = [
    MonarchForceGraphGroup("plans", label: "Plans"),
    MonarchForceGraphGroup("research", label: "Research", color: .data3),
]

enum MonarchForceGraphFixtures: ComponentFixtures {
    static let id = "force-graph"
    static let fixtures = [
        Fixture("groups, link kinds, an orphan and a selected node") {
            MonarchForceGraph(
                "Linked notes",
                nodes: notes,
                edges: links,
                groups: groups,
                edgeKinds: kinds,
                description: "Notes in this space",
                height: 280,
                selectedID: "Pricing",
                footer: "Updated just now.",
                onSelect: { _ in }
            )
        },
        Fixture("groups from keys, no labels, tuned layout") {
            MonarchForceGraph(
                "Services",
                nodes: (0..<12).map { MonarchForceGraphNode("Service \($0)", group: $0 < 6 ? "api" : "web") },
                edges: (1..<12).map { MonarchForceGraphEdge("Service 0", "Service \($0)") },
                height: 200,
                showsLabels: false,
                layoutOptions: MonarchForceGraphLayoutOptions(linkDistance: 60, ticks: 80)
            )
        },
        Fixture("long labels, single node") {
            MonarchForceGraph(
                "A graph of one long-named note with no links to anything else in the workspace",
                nodes: [MonarchForceGraphNode("Quarterly planning notes for the platform and infrastructure teams")],
                edges: [],
                height: 160
            )
        },
        Fixture("empty, loading, error") {
            VStack {
                MonarchForceGraph(
                    "Linked notes",
                    nodes: [],
                    edges: [],
                    emptyMessage: "Link two notes to see them here."
                )
                MonarchForceGraph("Linked notes", nodes: notes, edges: links, status: .loading, height: 120)
                MonarchForceGraph("Linked notes", nodes: notes, edges: links, status: .error, height: 120, onRetry: {})
            }
        },
        Fixture("data table") {
            let graph = MonarchForceGraphTokens.graph(nodes: notes, edges: links, groups: groups)
            MonarchChartFrameTableView(
                table: MonarchForceGraphTokens.table(
                    graph: graph,
                    kinds: kinds,
                    noGroup: "No group",
                    labels: MonarchForceGraphLabels()
                ),
                noValue: "No value"
            )
        },
    ]
}

@Suite("MonarchForceGraph")
struct MonarchForceGraphTests {
    private let graph = MonarchForceGraphTokens.graph(nodes: notes, edges: links, groups: groups)

    @Test("Links to unknown nodes and to the same node are dropped; neighbours go both ways")
    func graphModel() {
        #expect(graph.links.count == 3)
        #expect(graph.degree("Roadmap") == 2)
        #expect(graph.degree("Interviews") == 2)
        #expect(graph.degree("Inbox") == 0)
        #expect(graph.neighbours["Survey"]?.map(\.id) == ["Interviews"])
        #expect(graph.order == ["Inbox", "Interviews", "Pricing", "Roadmap", "Survey"])
    }

    @Test("Group colours come from the given slot or the group's position; orphans and ungrouped nodes are neutral")
    func colours() {
        #expect(graph.groupColor["plans"] == .data1)
        #expect(graph.groupColor["research"] == .data3)
        #expect(MonarchForceGraphTokens.nodeColor(notes[0], graph: graph) == .data1)
        #expect(MonarchForceGraphTokens.nodeColor(notes[3], graph: graph) == .data3)
        #expect(MonarchForceGraphTokens.nodeColor(notes[4], graph: graph) == .neutral)
        let derived = MonarchForceGraphTokens.graph(nodes: notes, edges: links, groups: nil)
        #expect(derived.groups.map(\.label) == ["plans", "research"])
        #expect(derived.groupColor["research"] == .data2)
    }

    @Test("Rings: default text while inspected, the accent while selected")
    func rings() {
        #expect(MonarchForceGraphTokens.ringRole(isActive: true, isSelected: true) == \.textDefault)
        #expect(MonarchForceGraphTokens.ringRole(isActive: false, isSelected: true) == \.accentSolid)
        #expect(MonarchForceGraphTokens.ringRole(isActive: false, isSelected: false) == nil)
    }

    @Test("Legends list only the groups and link kinds in use")
    func legends() {
        #expect(MonarchForceGraphTokens.legendGroups(graph).map(\.key) == ["plans", "research"])
        #expect(MonarchForceGraphTokens.legendKinds(kinds, graph: graph).map(\.key) == ["cites"])
    }

    @Test("Names: group label, no group, links with their kinds")
    func names() {
        #expect(MonarchForceGraphTokens.groupName(notes[2], graph: graph, noGroup: "No group") == "Research")
        #expect(MonarchForceGraphTokens.groupName(notes[4], graph: graph, noGroup: "No group") == "No group")
        #expect(
            MonarchForceGraphTokens.linkNames("Roadmap", graph: graph, kinds: kinds) == [
                "Pricing", "Interviews (Cites)",
            ]
        )
    }

    @Test("The summary names the size, the groups and the most connected node")
    func summary() {
        let hub = MonarchForceGraphTokens.hub(graph)
        #expect(hub?.id == "Interviews")
        #expect(hub?.degree == 2)
        let text = MonarchForceGraphTokens.summary(
            nodes: "5 nodes",
            links: "3 links",
            groups: "2 groups",
            hub: ("Interviews", "2 connections"),
            labels: MonarchForceGraphLabels()
        )
        #expect(text == "5 nodes and 3 links in 2 groups. Most connected: Interviews (2 connections).")
        #expect(
            MonarchForceGraphTokens.summary(
                nodes: "1 node",
                links: "0 links",
                groups: nil,
                hub: nil,
                labels: MonarchForceGraphLabels()
            )
                == "1 node and 0 links."
        )
        #expect(MonarchForceGraphTokens.count(1, text: "1", one: "%1$@ node", many: "%1$@ nodes") == "1 node")
        #expect(MonarchForceGraphTokens.count(4, text: "4", one: "%1$@ node", many: "%1$@ nodes") == "4 nodes")
    }

    @Test("A node is read as its label, group and links")
    func describe() {
        let labels = MonarchForceGraphLabels()
        #expect(
            MonarchForceGraphTokens.describe(
                label: "Roadmap",
                group: "Plans",
                connections: "2 connections",
                names: ["Pricing", "Interviews (Cites)"],
                labels: labels
            ) == "Roadmap, Plans. 2 connections: Pricing, Interviews (Cites)"
        )
        #expect(
            MonarchForceGraphTokens.describe(
                label: "Inbox",
                group: "No group",
                connections: "0 connections",
                names: [],
                labels: labels
            )
                == "Inbox, No group. 0 connections"
        )
    }

    @Test("Details under the canvas list the group, the links and the description")
    func details() {
        let lines = MonarchForceGraphTokens.details(
            notes[0],
            graph: graph,
            connections: "2",
            noGroup: "No group",
            labels: MonarchForceGraphLabels()
        )
        #expect(lines == ["Group: Plans", "Connections: 2", "Product space"])
    }

    @Test("The table lists every node by label with its group and links")
    func table() {
        let table = MonarchForceGraphTokens.table(
            graph: graph,
            kinds: kinds,
            noGroup: "No group",
            labels: MonarchForceGraphLabels()
        )
        #expect(table.categoryTitle == "Node")
        #expect(table.categories == ["Inbox", "Interviews", "Pricing", "Roadmap", "Survey"])
        #expect(table.series.map(\.name) == ["Group", "Connections"])
        #expect(table.series[0].formattedValues?.first == "No group")
        #expect(table.series[1].formattedValues?.first == MonarchChartFrameTokens.gapText)
        #expect(table.series[1].formattedValues?[3] == "Pricing, Interviews (Cites)")
        #expect(
            MonarchChartFrameTokens.rowAccessibilityLabel(table, row: 0, noValue: "No value")
                == "Inbox, Group No group, Connections –"
        )
    }

    @Test("VoiceOver's chart descriptor has each node's number of links")
    @MainActor
    func descriptor() {
        let chart = MonarchChartFrameDescriptor(
            title: "Notes",
            summary: "5 nodes",
            table: MonarchForceGraphTokens.audioGraphTable(graph: graph, labels: MonarchForceGraphLabels())
        )
        .makeChartDescriptor()
        #expect(chart.series.count == 1)
        #expect(chart.series[0].dataPoints.map(\.label) == ["0", "2", "1", "2", "1"])
        #expect(chart.yAxis?.title == "Connections")
    }

    @Test("VoiceOver adjustment moves through the nodes in label order and stops at the ends")
    func adjust() {
        let order = graph.order
        #expect(MonarchForceGraphTokens.adjusted(nil, forward: true, order: order) == "Inbox")
        #expect(MonarchForceGraphTokens.adjusted(nil, forward: false, order: order) == "Survey")
        #expect(MonarchForceGraphTokens.adjusted("Inbox", forward: true, order: order) == "Interviews")
        #expect(MonarchForceGraphTokens.adjusted("Survey", forward: true, order: order) == "Survey")
        #expect(MonarchForceGraphTokens.adjusted("Inbox", forward: false, order: order) == "Inbox")
        #expect(MonarchForceGraphTokens.adjusted("Gone", forward: true, order: order) == "Inbox")
        #expect(MonarchForceGraphTokens.adjusted(nil, forward: true, order: []) == nil)
    }

    @Test("The layout is deterministic, centred, and keeps linked nodes closer than unlinked ones")
    func layout() {
        let ids = notes.map(\.id)
        let pairs = graph.links.map { ($0.source, $0.target) }
        let first = MonarchForceGraphLayout.layout(ids: ids, links: pairs, options: MonarchForceGraphLayoutOptions())
        let second = MonarchForceGraphLayout.layout(ids: ids, links: pairs, options: MonarchForceGraphLayoutOptions())
        #expect(first == second)
        #expect(first.count == 5)
        let meanX = first.values.map(\.x).reduce(0, +) / 5
        let meanY = first.values.map(\.y).reduce(0, +) / 5
        #expect(abs(meanX) < 0.1 && abs(meanY) < 0.1)
        func distance(_ a: String, _ b: String) -> CGFloat {
            guard let p = first[a], let q = first[b] else { return .infinity }
            return hypot(p.x - q.x, p.y - q.y)
        }
        #expect(distance("Roadmap", "Pricing") < distance("Pricing", "Survey"))
        for a in ids {
            for b in ids where a < b {
                #expect(distance(a, b) > 20, "\(a) and \(b) overlap")
            }
        }
    }

    @Test("Large graphs use the approximate forces and still place every node apart")
    func largeLayout() {
        let ids = (0..<320).map { "n\($0)" }
        let pairs = (1..<320).map { ("n\($0 / 2)", "n\($0)") }
        #expect(MonarchForceGraphLayout.defaultTicks(320) == 281)
        #expect(MonarchForceGraphLayout.defaultTicks(10) == 300)
        let layout = MonarchForceGraphLayout.layout(
            ids: ids,
            links: pairs,
            options: MonarchForceGraphLayoutOptions(ticks: 40)
        )
        #expect(layout.count == 320)
        #expect(layout.values.allSatisfy { $0.x.isFinite && $0.y.isFinite })
        #expect(Set(layout.values.map { "\($0.x),\($0.y)" }).count == 320)
    }

    @Test("The viewport fits the layout, zooms about the centre and round-trips")
    func viewport() {
        let layout = ["a": CGPoint(x: -100, y: 0), "b": CGPoint(x: 100, y: 50)]
        let fitted = MonarchForceGraphTokens.viewport(
            layout: layout,
            size: CGSize(width: 290, height: 400),
            showsLabels: false,
            zoom: 1,
            offset: .zero
        )
        #expect(fitted.fit == 1.09)
        #expect(fitted.toScreen(CGPoint(x: 0, y: 25)) == CGPoint(x: 145, y: 200))
        let zoomed = MonarchForceGraphTokens.Viewport(
            size: fitted.size,
            center: fitted.center,
            fit: fitted.fit,
            zoom: 2,
            offset: CGSize(width: 10, height: -5)
        )
        let point = CGPoint(x: 40, y: -12)
        let back = zoomed.fromScreen(zoomed.toScreen(point))
        #expect(abs(back.x - point.x) < 0.0001 && abs(back.y - point.y) < 0.0001)
        let single = MonarchForceGraphTokens.viewport(
            layout: ["a": .zero],
            size: CGSize(width: 300, height: 300),
            showsLabels: true,
            zoom: 1,
            offset: .zero
        )
        #expect(single.fit == MonarchForceGraphTokens.maximumFit)
    }

    @Test("Zoom stays between 0.35 and 3; a touch hits the nearest node within 22 points")
    func interaction() {
        #expect(MonarchForceGraphTokens.zoomed(2.8, by: 1.25) == 3)
        #expect(MonarchForceGraphTokens.zoomed(0.4, by: 0.8) == 0.35)
        #expect(MonarchForceGraphTokens.zoomed(1, by: 1.25) == 1.25)
        let positions = ["a": CGPoint(x: 0, y: 0), "b": CGPoint(x: 30, y: 0)]
        #expect(MonarchForceGraphTokens.hit(CGPoint(x: 10, y: 0), positions: positions) == "a")
        #expect(MonarchForceGraphTokens.hit(CGPoint(x: 20, y: 0), positions: positions) == "b")
        #expect(MonarchForceGraphTokens.hit(CGPoint(x: 0, y: 30), positions: positions) == nil)
        #expect(MonarchForceGraphTokens.hitRadius * 2 >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("A ready graph without nodes is empty; the layout key changes with the content")
    func statusAndKey() {
        #expect(MonarchForceGraphTokens.status(.ready, nodeCount: 0) == .empty)
        #expect(MonarchForceGraphTokens.status(.ready, nodeCount: 3) == .ready)
        #expect(MonarchForceGraphTokens.status(.error, nodeCount: 0) == .error)
        let options = MonarchForceGraphLayoutOptions()
        let key = MonarchForceGraphTokens.layoutKey(
            ids: ["a", "b"],
            links: [MonarchForceGraphEdge("a", "b")],
            options: options
        )
        #expect(
            key
                == MonarchForceGraphTokens.layoutKey(
                    ids: ["a", "b"],
                    links: [MonarchForceGraphEdge("a", "b")],
                    options: options
                )
        )
        #expect(key != MonarchForceGraphTokens.layoutKey(ids: ["a", "b"], links: [], options: options))
        #expect(
            key
                != MonarchForceGraphTokens.layoutKey(
                    ids: ["a", "b"],
                    links: [MonarchForceGraphEdge("a", "b")],
                    options: MonarchForceGraphLayoutOptions(linkDistance: 50)
                )
        )
    }

    @Test("The layout cache computes once per key")
    @MainActor
    func cache() {
        let cache = MonarchForceGraphLayoutCache()
        var runs = 0
        _ = cache.layout(for: "k") {
            runs += 1
            return ["a": .zero]
        }
        _ = cache.layout(for: "k") {
            runs += 1
            return ["a": .zero]
        }
        #expect(runs == 1)
        _ = cache.layout(for: "other") {
            runs += 1
            return [:]
        }
        #expect(runs == 2)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchForceGraphStrings.zoomIn == "Zoom in")
        #expect(MonarchForceGraphStrings.zoomOut == "Zoom out")
        #expect(MonarchForceGraphStrings.resetView == "Reset view")
        #expect(MonarchForceGraphStrings.node == "Node")
        #expect(MonarchForceGraphStrings.group == "Group")
        #expect(MonarchForceGraphStrings.connections == "Connections")
        #expect(MonarchForceGraphStrings.noGroup == "No group")
        #expect(MonarchForceGraphStrings.edgeLegend == "Link types")
        for format in [
            MonarchForceGraphStrings.nodeCountOne, MonarchForceGraphStrings.nodeCount,
            MonarchForceGraphStrings.linkCountOne, MonarchForceGraphStrings.linkCount,
            MonarchForceGraphStrings.groupCountOne, MonarchForceGraphStrings.groupCount,
            MonarchForceGraphStrings.connectionCountOne, MonarchForceGraphStrings.connectionCount,
            MonarchForceGraphStrings.size, MonarchForceGraphStrings.inGroups,
            MonarchForceGraphStrings.mostConnected, MonarchForceGraphStrings.describe, MonarchForceGraphStrings.detail,
        ] {
            #expect(!format.hasPrefix("monarch."))
            #expect(format.contains("%1$@"))
        }
    }

    @Test("The graph keeps its canvas height")
    @MainActor
    func keepsHeight() {
        let size = Host.layout(MonarchForceGraph("Notes", nodes: notes, edges: links, height: 300))
        #expect(size.height >= 300)
    }
}
