/// Replacements for the built-in strings of ``MonarchInboxRow``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchInboxRowLabels: Sendable {
    /// The spoken unread state. Receives the count, or `nil` for a plain
    /// unread mark. Built in: "Unread", "1 unread", "3 unread".
    public var unread: (@Sendable (Int?) -> String)?
    /// The compact age: "now", "5m", "3h", "2d", "4w". Receives the unit
    /// and the number. Built in: English abbreviations.
    public var age: (@Sendable (MonarchInboxRowAgeUnit, Int) -> String)?
    /// The mark-as-read swipe action. Built in: "Read".
    public var markRead: String?
    /// The mark-as-unread swipe action. Built in: "Unread".
    public var markUnread: String?
    /// The archive swipe action. Built in: "Archive".
    public var archive: String?
    /// The delete swipe action. Built in: "Delete".
    public var delete: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        unread: (@Sendable (Int?) -> String)? = nil,
        age: (@Sendable (MonarchInboxRowAgeUnit, Int) -> String)? = nil,
        markRead: String? = nil,
        markUnread: String? = nil,
        archive: String? = nil,
        delete: String? = nil
    ) {
        self.unread = unread
        self.age = age
        self.markRead = markRead
        self.markUnread = markUnread
        self.archive = archive
        self.delete = delete
    }
}
