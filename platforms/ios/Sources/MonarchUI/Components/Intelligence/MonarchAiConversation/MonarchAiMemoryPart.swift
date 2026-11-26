/// A memory the assistant used, saved or forgot while writing a reply. The
/// `memory` stream event carries the same value.
public struct MonarchAiMemoryPart: Identifiable, Sendable, Hashable, Codable {
    /// The memory's id in your memory store.
    public var id: String
    /// What happened to the memory.
    public var action: MonarchAiMemoryAction
    /// The remembered fact, in plain language.
    public var text: String
    /// Where the memory applies, for example `user`, `project` or a workspace name.
    public var scope: String?

    /// A memory.
    ///
    /// - Parameters:
    ///   - id: The memory's id in your memory store.
    ///   - action: What happened to the memory.
    ///   - text: The remembered fact.
    ///   - scope: Where the memory applies.
    public init(id: String, action: MonarchAiMemoryAction, text: String, scope: String? = nil) {
        self.id = id
        self.action = action
        self.text = text
        self.scope = scope
    }
}
