/// What ``MonarchAiFeedback`` hands to `onSubmit`. Send it to the product's
/// feedback service with the message id.
public struct MonarchAiFeedbackSubmission: Sendable, Hashable, Codable {
    /// The rating.
    public var value: MonarchAiMessageFeedback
    /// The ids of the chosen reasons, in the order they are listed.
    public var reasons: [String]
    /// The optional comment, trimmed. Empty when there is none.
    public var comment: String

    /// A submission.
    ///
    /// - Parameters:
    ///   - value: The rating.
    ///   - reasons: The ids of the chosen reasons, in listed order.
    ///   - comment: The trimmed comment.
    public init(value: MonarchAiMessageFeedback, reasons: [String] = [], comment: String = "") {
        self.value = value
        self.reasons = reasons
        self.comment = comment
    }
}
