import Foundation

/// Replacements for the built-in strings of ``MonarchAiMessageActions``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiMessageActionsLabels: Sendable {
    /// The name of the row. Built in: "Message actions".
    public var toolbar: String?
    /// The copy button. Built in: "Copy".
    public var copy: String?
    /// The copy button after a copy. Built in: "Copied".
    public var copied: String?
    /// The regenerate button. Built in: "Regenerate".
    public var regenerate: String?
    /// The edit button. Built in: "Edit".
    public var edit: String?
    /// The thumbs up. Built in: "Good response".
    public var good: String?
    /// The thumbs down. Built in: "Bad response".
    public var bad: String?
    /// The share button. Built in: "Share".
    public var share: String?
    /// The read aloud button. Built in: "Read aloud".
    public var readAloud: String?
    /// The read aloud button while reading. Built in: "Stop reading".
    public var stopReading: String?
    /// The menu with the actions that do not fit and the extra actions. Built in: "More actions".
    public var more: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - toolbar: The name of the row.
    ///   - copy: The copy button.
    ///   - copied: The copy button after a copy.
    ///   - regenerate: The regenerate button.
    ///   - edit: The edit button.
    ///   - good: The thumbs up.
    ///   - bad: The thumbs down.
    ///   - share: The share button.
    ///   - readAloud: The read aloud button.
    ///   - stopReading: The read aloud button while reading.
    ///   - more: The menu with further actions.
    public init(
        toolbar: String? = nil,
        copy: String? = nil,
        copied: String? = nil,
        regenerate: String? = nil,
        edit: String? = nil,
        good: String? = nil,
        bad: String? = nil,
        share: String? = nil,
        readAloud: String? = nil,
        stopReading: String? = nil,
        more: String? = nil
    ) {
        self.toolbar = toolbar
        self.copy = copy
        self.copied = copied
        self.regenerate = regenerate
        self.edit = edit
        self.good = good
        self.bad = bad
        self.share = share
        self.readAloud = readAloud
        self.stopReading = stopReading
        self.more = more
    }
}
