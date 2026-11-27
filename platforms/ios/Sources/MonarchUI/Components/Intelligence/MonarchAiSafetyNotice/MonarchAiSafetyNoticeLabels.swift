/// Replacements for the built-in strings of ``MonarchAiSafetyNotice``. Every
/// value is optional; leave one out to keep the built-in English text.
public struct MonarchAiSafetyNoticeLabels: Sendable {
    /// The title of a kind, such as "This request wasn’t answered".
    public var title: @Sendable (_ kind: MonarchAiSafetyNoticeKind) -> String
    /// The explanation of a kind.
    public var message: @Sendable (_ kind: MonarchAiSafetyNoticeKind) -> String
    /// The label before the reason: "Reason:".
    public var reason: String
    /// The title of the button that puts the prompt back in the composer.
    public var editPrompt: String
    /// The title of the link to the content guidelines.
    public var learnMore: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - title: Replaces the title of each kind.
    ///   - message: Replaces the explanation of each kind.
    ///   - reason: Replaces "Reason:".
    ///   - editPrompt: Replaces "Edit prompt".
    ///   - learnMore: Replaces "Content guidelines".
    public init(
        title: (@Sendable (_ kind: MonarchAiSafetyNoticeKind) -> String)? = nil,
        message: (@Sendable (_ kind: MonarchAiSafetyNoticeKind) -> String)? = nil,
        reason: String? = nil,
        editPrompt: String? = nil,
        learnMore: String? = nil
    ) {
        self.title = title ?? { MonarchAiSafetyNoticeStrings.title($0) }
        self.message = message ?? { MonarchAiSafetyNoticeStrings.message($0) }
        self.reason = reason ?? MonarchAiSafetyNoticeStrings.reason
        self.editPrompt = editPrompt ?? MonarchAiSafetyNoticeStrings.editPrompt
        self.learnMore = learnMore ?? MonarchAiSafetyNoticeStrings.learnMore
    }
}
