/// Replacements for the built-in strings of ``MonarchAiFeedback``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiFeedbackLabels: Sendable {
    /// Names the thumbs and the sheet. Built in: "Rate this response".
    public var group: String?
    /// The thumbs up. Built in: "Good response".
    public var good: String?
    /// The thumbs down. Built in: "Bad response".
    public var bad: String?
    /// The question above the reasons after a thumbs down. Built in: "What went wrong?".
    public var reasonsDown: String?
    /// The question above the reasons after a thumbs up. Built in: "What went well?".
    public var reasonsUp: String?
    /// The label of the comment. Built in: "Tell us more (optional)".
    public var comment: String?
    /// The placeholder of the comment. Built in: "What would have made it better?".
    public var commentPlaceholder: String?
    /// The send button. Built in: "Send feedback".
    public var submit: String?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// Shown after sending. Built in: "Thanks for your feedback."
    public var thanks: String?
    /// Shown when sending failed. Built in: "Your feedback could not be sent. Try again."
    public var error: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - group: Names the thumbs and the sheet.
    ///   - good: The thumbs up.
    ///   - bad: The thumbs down.
    ///   - reasonsDown: The question after a thumbs down.
    ///   - reasonsUp: The question after a thumbs up.
    ///   - comment: The label of the comment.
    ///   - commentPlaceholder: The placeholder of the comment.
    ///   - submit: The send button.
    ///   - cancel: The cancel button.
    ///   - thanks: Shown after sending.
    ///   - error: Shown when sending failed.
    public init(
        group: String? = nil,
        good: String? = nil,
        bad: String? = nil,
        reasonsDown: String? = nil,
        reasonsUp: String? = nil,
        comment: String? = nil,
        commentPlaceholder: String? = nil,
        submit: String? = nil,
        cancel: String? = nil,
        thanks: String? = nil,
        error: String? = nil
    ) {
        self.group = group
        self.good = good
        self.bad = bad
        self.reasonsDown = reasonsDown
        self.reasonsUp = reasonsUp
        self.comment = comment
        self.commentPlaceholder = commentPlaceholder
        self.submit = submit
        self.cancel = cancel
        self.thanks = thanks
        self.error = error
    }
}
