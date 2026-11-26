/// One model a person can pick in a ``MonarchAiModelPicker``.
public struct MonarchAiModelOption: Identifiable, Sendable, Hashable {
    /// A stable id, stored in the selection.
    public var id: String
    /// The provider or family the model is grouped under. Groups keep the order they first appear in.
    public var provider: String
    /// The display name.
    public var name: String
    /// One short line, for example "Best for long documents".
    public var description: String?
    /// The capability badges.
    public var capabilities: [MonarchAiModelCapability]
    /// The context window in tokens, shown compactly ("200K context").
    public var contextTokens: Int?
    /// The relative speed.
    public var speed: MonarchAiModelSpeed?
    /// The relative cost from 1 (lowest) to 3 (highest), shown as $, $$ or $$$. Other values are clamped.
    public var costTier: Int?
    /// Whether the model runs on the workspace's own provider key.
    public var requiresKey: Bool
    /// Whether that key is set up. A model that needs a missing key cannot be picked.
    public var isKeyConfigured: Bool
    /// Not available now, for example not in the plan.
    public var isDisabled: Bool

    /// A model option.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - provider: The provider or family it is grouped under.
    ///   - name: The display name.
    ///   - description: One short line.
    ///   - capabilities: The capability badges.
    ///   - contextTokens: The context window in tokens.
    ///   - speed: The relative speed.
    ///   - costTier: The relative cost from 1 to 3.
    ///   - requiresKey: Whether it runs on the workspace's own provider key.
    ///   - isKeyConfigured: Whether that key is set up.
    ///   - isDisabled: Not available now.
    public init(
        id: String,
        provider: String,
        name: String,
        description: String? = nil,
        capabilities: [MonarchAiModelCapability] = [],
        contextTokens: Int? = nil,
        speed: MonarchAiModelSpeed? = nil,
        costTier: Int? = nil,
        requiresKey: Bool = false,
        isKeyConfigured: Bool = false,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.provider = provider
        self.name = name
        self.description = description
        self.capabilities = capabilities
        self.contextTokens = contextTokens
        self.speed = speed
        self.costTier = costTier
        self.requiresKey = requiresKey
        self.isKeyConfigured = isKeyConfigured
        self.isDisabled = isDisabled
    }
}
