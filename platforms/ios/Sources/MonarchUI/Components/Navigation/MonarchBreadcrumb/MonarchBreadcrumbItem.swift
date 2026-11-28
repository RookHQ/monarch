/// One level of a ``MonarchBreadcrumb``.
///
/// ```swift
/// MonarchBreadcrumbItem("Projects", id: "projects")
/// MonarchBreadcrumbItem("Archive", isLinked: false)
/// ```
public struct MonarchBreadcrumbItem: Identifiable, Hashable, Sendable {
    /// The identity of the level, given back by the trail's action.
    public let id: String
    /// The label, already localised.
    public let title: String
    /// Whether the level has a screen to go to. A level without one is shown, not linked.
    public let isLinked: Bool

    /// A level of the trail.
    ///
    /// - Parameters:
    ///   - title: The label, already localised.
    ///   - id: The identity of the level. The default is the title.
    ///   - isLinked: Whether the level has a screen to go to. A level without one is shown as
    ///     text and is disabled in the collapsed menu. The last level is never linked.
    public init(_ title: String, id: String? = nil, isLinked: Bool = true) {
        self.title = title
        self.id = id ?? title
        self.isLinked = isLinked
    }
}
