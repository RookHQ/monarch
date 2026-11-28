/// How ``MonarchAiRelatedEntities`` lays out the entities.
public enum MonarchAiRelatedEntitiesVariant: Sendable, Hashable, CaseIterable {
    /// A ranked list, the default.
    case list
    /// A radial diagram with the centre entity in the middle. Falls back to
    /// the list when there are too many entities or at accessibility text sizes.
    case graph
}
