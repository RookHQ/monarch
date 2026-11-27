import Observation

/// Answers kept by key for the life of a view, observable so the view
/// redraws when one arrives: the regions of a country, the names of codes.
@MainActor
@Observable
final class MonarchKeyedCache<Key: Hashable, Value> {
    /// The answers by key.
    private(set) var answers: [Key: Value] = [:]

    init() {}

    /// Keeps an answer.
    func store(_ answer: Value, for key: Key) {
        answers[key] = answer
    }

    /// The answer for a key, or `nil`.
    subscript(key: Key) -> Value? { answers[key] }
}
