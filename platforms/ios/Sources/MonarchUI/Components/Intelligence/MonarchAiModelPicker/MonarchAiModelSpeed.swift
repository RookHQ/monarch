/// The relative speed of an AI model, shown in ``MonarchAiModelPicker``.
public enum MonarchAiModelSpeed: String, Sendable, Hashable, Codable, CaseIterable {
    /// Answers quickly.
    case fast
    /// Neither fast nor slow.
    case balanced
    /// Takes longer to answer.
    case slow
}
