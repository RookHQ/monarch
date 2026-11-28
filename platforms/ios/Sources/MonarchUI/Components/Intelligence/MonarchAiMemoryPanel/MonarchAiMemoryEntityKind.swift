/// What a remembered entity is. ``MonarchAiMemoryPanel`` lists people
/// first, then organizations, projects and other.
@_spi(Experimental) public enum MonarchAiMemoryEntityKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A person.
    case person
    /// A company or another organization.
    case organization
    /// A project.
    case project
    /// Anything else.
    case other
}
