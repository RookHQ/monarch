/// One suggested way to start a conversation.
public struct MonarchAiStarterPrompt: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// A short visible label, for example "Summarise a document", already localised.
    public let title: String
    /// The prompt that is sent, or placed in the input, when the starter is chosen. Shown under the title.
    public let prompt: String
    /// A decorative SF Symbol before the title. Never a sparkle.
    public let systemImage: String?

    /// A starter prompt.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: A short visible label, already localised.
    ///   - prompt: The prompt that is sent when the starter is chosen.
    ///   - systemImage: A decorative SF Symbol before the title.
    public init(id: String, title: String, prompt: String, systemImage: String? = nil) {
        self.id = id
        self.title = title
        self.prompt = prompt
        self.systemImage = systemImage
    }
}
