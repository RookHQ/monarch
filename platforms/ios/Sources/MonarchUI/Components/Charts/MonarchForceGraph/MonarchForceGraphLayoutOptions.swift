/// Tunes the force layout of a ``MonarchForceGraph``. The layout is
/// deterministic: the same nodes, links and options give the same positions.
public struct MonarchForceGraphLayoutOptions: Sendable, Hashable {
    /// The rest length of a link, in layout units. 90 by default.
    public var linkDistance: Double
    /// The repulsion between every pair of nodes; negative pushes apart. -260 by default.
    public var charge: Double
    /// The least distance between node centres. 40 by default.
    public var collide: Double
    /// The pull of every node towards the centre, 0 to 1. 0.04 by default.
    public var gravity: Double
    /// The simulation steps. `nil` for 300 up to 300 nodes, fewer above.
    public var ticks: Int?

    /// Layout options. Every parameter has the default of the web component.
    public init(
        linkDistance: Double = 90,
        charge: Double = -260,
        collide: Double = 40,
        gravity: Double = 0.04,
        ticks: Int? = nil
    ) {
        self.linkDistance = linkDistance
        self.charge = charge
        self.collide = collide
        self.gravity = gravity
        self.ticks = ticks
    }
}
