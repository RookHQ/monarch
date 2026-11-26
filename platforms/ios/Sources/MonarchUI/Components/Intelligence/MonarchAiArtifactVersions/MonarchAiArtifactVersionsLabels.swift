/// Replacements for the built-in strings of ``MonarchAiArtifactVersions``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiArtifactVersionsLabels: Sendable {
    /// The spoken name of the group. Built in: "Versions".
    public var label: String?
    /// The position. Receives the version number and the count. Built in: "Version 3 of 5".
    public var position: (@Sendable (Int, Int) -> String)?
    /// The spoken name of the previous button. Built in: "Previous version".
    public var previous: String?
    /// The spoken name of the next button. Built in: "Next version".
    public var next: String?
    /// The spoken name of the version menu. Built in: "Choose a version".
    public var select: String?
    /// The short name of a version in the menu. Receives its number. Built in: "v3".
    public var versionTag: (@Sendable (Int) -> String)?
    /// The name of a version above the diff. Receives its number. Built in: "Version 3".
    public var versionName: (@Sendable (Int) -> String)?
    /// The compare toggle. Built in: "Compare with previous".
    public var compare: String?
    /// Announced when the version changes. Receives the version number and the count. Built in: "Showing
    /// version 3 of 5".
    public var announce: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        position: (@Sendable (Int, Int) -> String)? = nil,
        previous: String? = nil,
        next: String? = nil,
        select: String? = nil,
        versionTag: (@Sendable (Int) -> String)? = nil,
        versionName: (@Sendable (Int) -> String)? = nil,
        compare: String? = nil,
        announce: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.label = label
        self.position = position
        self.previous = previous
        self.next = next
        self.select = select
        self.versionTag = versionTag
        self.versionName = versionName
        self.compare = compare
        self.announce = announce
    }
}
