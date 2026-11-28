import Foundation

/// Built-in strings of the force graph, from the `MonarchForceGraph` strings table.
enum MonarchForceGraphStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.forceGraph.\(key)", table: "MonarchForceGraph")
    }

    /// The button that zooms in on a force graph.
    static var zoomIn: String { string("zoomIn") }
    /// The button that zooms out of a force graph.
    static var zoomOut: String { string("zoomOut") }
    /// The button that resets the zoom, the pan and moved nodes of a force graph.
    static var resetView: String { string("resetView") }
    /// The heading of the node column in a force graph's data table.
    static var node: String { string("node") }
    /// The heading of the group column in a force graph's data table, and the label of a selected node's group.
    static var group: String { string("group") }
    /// The heading of the links column in a force graph's data table, and the label of a selected node's number of links.
    static var connections: String { string("connections") }
    /// The group of a force graph node that has none.
    static var noGroup: String { string("noGroup") }
    /// The spoken name of the legend of a force graph's link kinds.
    static var edgeLegend: String { string("edgeLegend") }
    /// One node of a force graph.
    static var nodeCountOne: String { string("nodeCountOne") }
    /// Several nodes of a force graph.
    static var nodeCount: String { string("nodeCount") }
    /// One link of a force graph.
    static var linkCountOne: String { string("linkCountOne") }
    /// Several links of a force graph.
    static var linkCount: String { string("linkCount") }
    /// One group of a force graph.
    static var groupCountOne: String { string("groupCountOne") }
    /// Several groups of a force graph.
    static var groupCount: String { string("groupCount") }
    /// One link of a force graph node.
    static var connectionCountOne: String { string("connectionCountOne") }
    /// Several links of a force graph node.
    static var connectionCount: String { string("connectionCount") }
    /// The size of a force graph in its spoken summary.
    static var size: String { string("size") }
    /// Follows the size of a force graph in its summary.
    static var inGroups: String { string("inGroups") }
    /// The node with the most links, in a force graph's summary.
    static var mostConnected: String { string("mostConnected") }
    /// Spoken for a force graph node.
    static var describe: String { string("describe") }
    /// A detail line under the canvas.
    static var detail: String { string("detail") }

    /// Fills a format's `%@` placeholders.
    static func fill(_ format: String, _ arguments: String...) -> String {
        String(format: format, arguments: arguments)
    }
}
