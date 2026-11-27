/// One pinned destination in a ``MonarchFavoritesList``.
///
/// The app resolves names before passing items in, and opens the
/// destination itself when the list reports a selection.
public struct MonarchFavoriteItem: Identifiable, Sendable, Hashable {
    /// A stable, unique key, such as `project:42`.
    public let id: String
    /// The visible name, already localised.
    public let title: String
    /// A decorative SF Symbol before the name.
    public let systemImage: String?
    /// Whether this is the screen the person is on.
    public let isCurrent: Bool

    /// A favorite.
    ///
    /// - Parameters:
    ///   - title: The visible name, already localised.
    ///   - id: A stable, unique key.
    ///   - systemImage: A decorative SF Symbol before the name.
    ///   - isCurrent: Whether this is the screen the person is on.
    public init(_ title: String, id: String, systemImage: String? = nil, isCurrent: Bool = false) {
        self.title = title
        self.id = id
        self.systemImage = systemImage
        self.isCurrent = isCurrent
    }
}
