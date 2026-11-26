/// One question of a ``MonarchFaq`` with its answer.
public struct MonarchFaqItem: Sendable, Hashable {
    /// The question.
    public let question: String
    /// The answer, shown when the question is expanded.
    public let answer: String

    /// A question and its answer.
    ///
    /// - Parameters:
    ///   - question: The question, already localised.
    ///   - answer: The answer, already localised.
    public init(_ question: String, answer: String) {
        self.question = question
        self.answer = answer
    }
}
