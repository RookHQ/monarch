/// Replacements for the built-in strings of ``MonarchForceGraph``.
/// A property that is `nil` keeps the built-in English string.
///
/// Sentences with values are formats: `%1$@`, `%2$@`… stand for the values
/// named in each property's description, already formatted.
public struct MonarchForceGraphLabels: Sendable, Hashable {
    /// The strings of the chart frame: the table toggle, loading, empty, error and the legend's name.
    public var frame: MonarchChartFrameLabels
    /// The zoom-in button. Built in: "Zoom in".
    public var zoomIn: String?
    /// The zoom-out button. Built in: "Zoom out".
    public var zoomOut: String?
    /// The reset button. Built in: "Reset view".
    public var resetView: String?
    /// The table heading of the nodes. Built in: "Node".
    public var node: String?
    /// The table heading of the groups, and the group line of a selected node. Built in: "Group".
    public var group: String?
    /// The table heading of the links, and the links line of a selected node. Built in: "Connections".
    public var connections: String?
    /// The group of a node without one. Built in: "No group".
    public var noGroup: String?
    /// The name of the legend of link kinds. Built in: "Link types".
    public var edgeLegend: String?
    /// One node: `%1$@` the count. Built in: "%1$@ node".
    public var nodeCountOne: String?
    /// Several nodes: `%1$@` the count. Built in: "%1$@ nodes".
    public var nodeCount: String?
    /// One link: `%1$@` the count. Built in: "%1$@ link".
    public var linkCountOne: String?
    /// Several links: `%1$@` the count. Built in: "%1$@ links".
    public var linkCount: String?
    /// One group: `%1$@` the count. Built in: "%1$@ group".
    public var groupCountOne: String?
    /// Several groups: `%1$@` the count. Built in: "%1$@ groups".
    public var groupCount: String?
    /// One connection: `%1$@` the count. Built in: "%1$@ connection".
    public var connectionCountOne: String?
    /// Several connections: `%1$@` the count. Built in: "%1$@ connections".
    public var connectionCount: String?
    /// The size in the summary: `%1$@` the nodes, `%2$@` the links, with their nouns. Built in: "%1$@ and %2$@".
    public var size: String?
    /// The groups in the summary: `%1$@` the groups with their noun. Built in: "in %1$@".
    public var inGroups: String?
    /// The hub in the summary: `%1$@` its label, `%2$@` its connections. Built in: "Most connected: %1$@ (%2$@)."
    public var mostConnected: String?
    /// What VoiceOver reads for a node: `%1$@` its label, `%2$@` its group, `%3$@` its connections.
    /// The linked nodes follow. Built in: "%1$@, %2$@. %3$@".
    public var describe: String?

    /// A detail line under the canvas: `%1$@` the name, `%2$@` the value. Built in: "%1$@: %2$@".
    public var detail: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        zoomIn: String? = nil,
        zoomOut: String? = nil,
        resetView: String? = nil,
        node: String? = nil,
        group: String? = nil,
        connections: String? = nil,
        noGroup: String? = nil,
        edgeLegend: String? = nil,
        nodeCountOne: String? = nil,
        nodeCount: String? = nil,
        linkCountOne: String? = nil,
        linkCount: String? = nil,
        groupCountOne: String? = nil,
        groupCount: String? = nil,
        connectionCountOne: String? = nil,
        connectionCount: String? = nil,
        size: String? = nil,
        inGroups: String? = nil,
        mostConnected: String? = nil,
        describe: String? = nil,
        detail: String? = nil
    ) {
        self.frame = frame
        self.zoomIn = zoomIn
        self.zoomOut = zoomOut
        self.resetView = resetView
        self.node = node
        self.group = group
        self.connections = connections
        self.noGroup = noGroup
        self.edgeLegend = edgeLegend
        self.nodeCountOne = nodeCountOne
        self.nodeCount = nodeCount
        self.linkCountOne = linkCountOne
        self.linkCount = linkCount
        self.groupCountOne = groupCountOne
        self.groupCount = groupCount
        self.connectionCountOne = connectionCountOne
        self.connectionCount = connectionCount
        self.size = size
        self.inGroups = inGroups
        self.mostConnected = mostConnected
        self.describe = describe
        self.detail = detail
    }
}
