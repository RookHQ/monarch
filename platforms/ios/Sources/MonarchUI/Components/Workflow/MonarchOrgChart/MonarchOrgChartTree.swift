/// The reporting structure of a ``MonarchOrgChart``, built once from the flat
/// nodes. Pure: every question the chart asks about the tree is answered here
/// and unit-tested.
struct MonarchOrgChartTree: Sendable {
    /// A visible node and its depth, 1 for top level.
    struct Row: Hashable, Sendable {
        let node: MonarchOrgChartNode
        let level: Int
    }

    /// What opening or closing a node does.
    enum ToggleDecision: Hashable, Sendable {
        /// Remove it from the expanded set.
        case collapse
        /// Add it to the expanded set.
        case expand
        /// Ask the app for the missing reports first, then expand.
        case load
        /// Nothing: a leaf, or a branch that is already loading.
        case none
    }

    let nodes: [MonarchOrgChartNode]
    let byID: [String: MonarchOrgChartNode]
    let roots: [MonarchOrgChartNode]
    let children: [String: [MonarchOrgChartNode]]

    init(_ nodes: [MonarchOrgChartNode]) {
        var byID: [String: MonarchOrgChartNode] = [:]
        var unique: [MonarchOrgChartNode] = []
        for node in nodes where byID[node.id] == nil {
            byID[node.id] = node
            unique.append(node)
        }
        var roots: [MonarchOrgChartNode] = []
        var children: [String: [MonarchOrgChartNode]] = [:]
        for node in unique {
            if let parent = node.parentID, parent != node.id, byID[parent] != nil {
                children[parent, default: []].append(node)
            } else {
                roots.append(node)
            }
        }
        self.nodes = unique
        self.byID = byID
        self.roots = roots
        self.children = children
    }

    /// The node's manager in the chart, or `nil` at the top level.
    func manager(of id: String) -> String? {
        guard let parent = byID[id]?.parentID, parent != id, byID[parent] != nil else { return nil }
        return parent
    }

    /// The managers above a node, nearest first. Stops at a cycle.
    func ancestors(of id: String) -> [String] {
        var out: [String] = []
        var seen: Set<String> = [id]
        var current = manager(of: id)
        while let parent = current, !seen.contains(parent) {
            out.append(parent)
            seen.insert(parent)
            current = manager(of: parent)
        }
        return out
    }

    /// The reports of a node that are in the chart.
    func reports(of id: String) -> [MonarchOrgChartNode] {
        children[id] ?? []
    }

    /// Whether the node has reports, loaded or announced by `childCount`.
    func hasReports(_ node: MonarchOrgChartNode) -> Bool {
        !reports(of: node.id).isEmpty || (node.childCount ?? 0) > 0
    }

    /// The number of direct reports shown on the chip and read by VoiceOver.
    func reportCount(_ node: MonarchOrgChartNode) -> Int {
        max(reports(of: node.id).count, node.childCount ?? 0)
    }

    /// Whether the node announces more reports than the chart has.
    func needsLoading(_ node: MonarchOrgChartNode) -> Bool {
        (node.childCount ?? 0) > reports(of: node.id).count
    }

    /// The expansion when the app gives none: the top level, and the level
    /// under it when there are three or fewer top-level nodes, plus the path
    /// to the focused node.
    func defaultExpanded(focusID: String?) -> Set<String> {
        var open = Set(roots.map(\.id))
        if roots.count <= MonarchOrgChartTokens.defaultSecondLevelRootLimit {
            for root in roots {
                open.formUnion(reports(of: root.id).map(\.id))
            }
        }
        if let focusID {
            open.formUnion(ancestors(of: focusID))
        }
        return open
    }

    /// The visible nodes in reading order with their depth. A cycle is shown once.
    func visibleRows(expanded: Set<String>) -> [Row] {
        var out: [Row] = []
        var seen: Set<String> = []
        func walk(_ list: [MonarchOrgChartNode], level: Int) {
            for node in list where !seen.contains(node.id) {
                seen.insert(node.id)
                out.append(Row(node: node, level: level))
                if expanded.contains(node.id) {
                    walk(reports(of: node.id), level: level + 1)
                }
            }
        }
        walk(roots, level: 1)
        return out
    }

    /// What opening or closing a node does now.
    func toggleDecision(
        _ node: MonarchOrgChartNode,
        expanded: Set<String>,
        loading: Set<String>,
        canLoad: Bool
    ) -> ToggleDecision {
        guard hasReports(node), !loading.contains(node.id) else { return .none }
        if expanded.contains(node.id) { return .collapse }
        return canLoad && needsLoading(node) ? .load : .expand
    }

    /// Whether `movingID` may be dropped on `targetID`: not on a vacancy, not
    /// on itself or anyone in its own team, and not on its current manager.
    func canMove(_ movingID: String, onto targetID: String) -> Bool {
        guard let moving = byID[movingID], let target = byID[targetID] else { return false }
        guard moving.kind == .person, target.kind == .person else { return false }
        if movingID == targetID || ancestors(of: targetID).contains(movingID) { return false }
        return manager(of: movingID) != targetID
    }

    /// Whether `movingID` may be dropped on the background to become top level.
    func canMoveToTopLevel(_ movingID: String) -> Bool {
        guard let moving = byID[movingID], moving.kind == .person else { return false }
        return manager(of: movingID) != nil
    }

    /// The names of the managers a node also reports to through dotted lines.
    func dottedManagers(of id: String, lines: [MonarchOrgChartDottedLine]) -> [String] {
        lines.filter { $0.from == id }.compactMap { byID[$0.to]?.name }
    }
}
