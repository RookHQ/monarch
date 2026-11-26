/// Token counts for one turn, and its cost in millionths of the billing currency.
public struct MonarchAiUsage: Sendable, Hashable, Codable {
    /// Every token the model read for this turn, including any served from or written to a prompt cache.
    public var inputTokens: Int
    /// Every token the model wrote, including reasoning tokens.
    public var outputTokens: Int
    /// The cost in millionths of the billing currency.
    public var costMicros: Int?
    /// How many of `inputTokens` were read from a prompt cache.
    public var cacheReadTokens: Int?
    /// How many of `inputTokens` were written to a prompt cache.
    public var cacheWriteTokens: Int?
    /// How many of `outputTokens` were reasoning the person does not see.
    public var reasoningTokens: Int?

    /// Usage of one turn.
    ///
    /// - Parameters:
    ///   - inputTokens: Every token the model read.
    ///   - outputTokens: Every token the model wrote.
    ///   - costMicros: The cost in millionths of the billing currency.
    ///   - cacheReadTokens: How many input tokens were read from a prompt cache.
    ///   - cacheWriteTokens: How many input tokens were written to a prompt cache.
    ///   - reasoningTokens: How many output tokens were reasoning.
    public init(
        inputTokens: Int,
        outputTokens: Int,
        costMicros: Int? = nil,
        cacheReadTokens: Int? = nil,
        cacheWriteTokens: Int? = nil,
        reasoningTokens: Int? = nil
    ) {
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.costMicros = costMicros
        self.cacheReadTokens = cacheReadTokens
        self.cacheWriteTokens = cacheWriteTokens
        self.reasoningTokens = reasoningTokens
    }
}
