import Foundation

/// One entity the assistant found related to the current content.
public struct MonarchAiRelatedEntity: Identifiable, Sendable, Hashable {
    /// A stable id, used by relations.
    public var id: String
    /// The name shown, such as "Jordan Lee". Already localised.
    public var label: String
    /// What it is.
    public var kind: MonarchAiRelatedEntityKind
    /// How strongly it relates, from 0 to 1. Sorts the list and sizes graph nodes.
    public var weight: Double?
    /// A picture for the avatar, loaded with `AsyncImage`. Initials or the kind's symbol otherwise.
    public var imageURL: URL?

    /// A related entity.
    ///
    /// - Parameters:
    ///   - id: A stable id, used by relations.
    ///   - label: The name shown.
    ///   - kind: What it is.
    ///   - weight: How strongly it relates, from 0 to 1.
    ///   - imageURL: A picture for the avatar.
    public init(
        id: String,
        label: String,
        kind: MonarchAiRelatedEntityKind,
        weight: Double? = nil,
        imageURL: URL? = nil
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.weight = weight
        self.imageURL = imageURL
    }
}
