import Foundation

/// A remembered fact as ``MonarchAiMemoryPanel`` lists it: always about an
/// entity, with the time it was last changed.
@_spi(Experimental) public struct MonarchAiMemoryItem: Identifiable, Sendable, Hashable, Codable {
    /// The memory's id in your memory store.
    public var id: String
    /// The remembered fact, in plain language.
    public var text: String
    /// Who or what the memory is about.
    public var entity: MonarchAiMemoryEntity
    /// When the memory was saved or last changed.
    public var updatedAt: Date

    /// A memory.
    ///
    /// - Parameters:
    ///   - id: The memory's id in your memory store.
    ///   - text: The remembered fact.
    ///   - entity: Who or what the memory is about.
    ///   - updatedAt: When the memory was saved or last changed.
    public init(id: String, text: String, entity: MonarchAiMemoryEntity, updatedAt: Date) {
        self.id = id
        self.text = text
        self.entity = entity
        self.updatedAt = updatedAt
    }

    /// Applies one memory event of a conversation to a list, as the web's
    /// `applyAiMemoryEvent` does: `write` updates the memory with that id
    /// (text and time) or adds it at the end; `forget` removes it; `read`
    /// changes nothing.
    ///
    /// - Parameters:
    ///   - memories: The list.
    ///   - event: The memory part a reply streamed.
    ///   - now: The time a written memory gets as `updatedAt`.
    ///   - defaultEntityName: The name of the `other` group for a new memory without a scope.
    ///   - entity: Who or what a new memory is about. By default an `other`
    ///     entity named by the event's scope, or `defaultEntityName`.
    /// - Returns: The updated list; the same list when nothing changed.
    public static func apply(
        _ memories: [MonarchAiMemoryItem],
        event: MonarchAiMemoryPart,
        now: Date,
        defaultEntityName: String = "General",
        entity: (@Sendable (MonarchAiMemoryPart) -> MonarchAiMemoryEntity)? = nil
    ) -> [MonarchAiMemoryItem] {
        let index = memories.firstIndex { $0.id == event.id }
        switch event.action {
        case .read:
            return memories
        case .forget:
            guard let index else { return memories }
            var result = memories
            result.remove(at: index)
            return result
        case .write:
            if let index {
                guard memories[index].text != event.text else { return memories }
                var result = memories
                result[index].text = event.text
                result[index].updatedAt = now
                return result
            }
            let about = entity?(event) ?? MonarchAiMemoryEntity(kind: .other, name: event.scope ?? defaultEntityName)
            return memories + [MonarchAiMemoryItem(id: event.id, text: event.text, entity: about, updatedAt: now)]
        }
    }
}
