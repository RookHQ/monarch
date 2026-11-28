import Foundation

/// What ``MonarchVirtualMessageList`` reads from each message. Your message
/// type can carry anything else.
public protocol MonarchVirtualMessageListItem: Identifiable {
    /// When the message was sent. Places day separators and groups messages.
    var time: Date { get }
    /// The author's id, for grouping consecutive messages. `nil` never groups.
    var authorID: String? { get }
}

extension MonarchVirtualMessageListItem {
    /// No author id by default, so messages are never grouped.
    public var authorID: String? { nil }
}
