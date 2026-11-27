import Foundation

/// One thing the agent did, shown in the log of ``MonarchAiComputerView``.
@_spi(Experimental) public struct MonarchAiComputerAction: Sendable, Hashable, Identifiable {
    /// A stable id; the value of the selection.
    public var id: String
    /// What kind of action it was; picks the icon.
    public var kind: MonarchAiComputerActionKind
    /// What happened, in plain words, such as "Clicked Sign in". Never put typed text here.
    public var label: String
    /// When it happened.
    public var date: Date
    /// Where on the screenshot it happened, in screenshot pixels.
    public var point: MonarchAiComputerPoint?
    /// More about it, such as the text typed or the address opened.
    public var detail: String?
    /// The action touched something private: its label and detail are never
    /// shown or announced, and the screenshot after it is hidden.
    public var isSensitive: Bool

    /// An action.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - kind: What kind of action it was.
    ///   - label: What happened, in plain words, already localised.
    ///   - date: When it happened.
    ///   - point: Where on the screenshot it happened.
    ///   - detail: More about it. Typed text shows only with `showsTypedText`.
    ///   - isSensitive: Whether the action touched something private.
    public init(
        id: String,
        kind: MonarchAiComputerActionKind,
        label: String,
        date: Date,
        point: MonarchAiComputerPoint? = nil,
        detail: String? = nil,
        isSensitive: Bool = false
    ) {
        self.id = id
        self.kind = kind
        self.label = label
        self.date = date
        self.point = point
        self.detail = detail
        self.isSensitive = isSensitive
    }
}
