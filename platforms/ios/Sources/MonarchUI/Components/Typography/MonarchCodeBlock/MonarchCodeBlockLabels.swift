/// Replacements for the built-in strings of ``MonarchCodeBlock``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCodeBlockLabels: Sendable {
    /// The title of the copy button. Built in: "Copy".
    public var copy: String?
    /// The title of the copy button for a short time after a copy. Built in: "Copied".
    public var copied: String?
    /// Announced by VoiceOver after a copy. Built in: "Copied to clipboard".
    public var copiedAnnouncement: String?
    /// The title of the wrap toggle. Built in: "Wrap lines".
    public var wrap: String?
    /// Spoken before an added line of a diff. Built in: "Added".
    public var added: String?
    /// Spoken before a removed line of a diff. Built in: "Removed".
    public var removed: String?
    /// Names the code when there is no title: the spoken name of the code
    /// region and the target of the copy button. Receives the language's
    /// display name, or `nil` when there is none. Built in: "<Language>
    /// code", or "Code".
    public var codeName: (@Sendable (String?) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        copy: String? = nil,
        copied: String? = nil,
        copiedAnnouncement: String? = nil,
        wrap: String? = nil,
        added: String? = nil,
        removed: String? = nil,
        codeName: (@Sendable (String?) -> String)? = nil
    ) {
        self.copy = copy
        self.copied = copied
        self.copiedAnnouncement = copiedAnnouncement
        self.wrap = wrap
        self.added = added
        self.removed = removed
        self.codeName = codeName
    }

    /// The title of the wrap toggle.
    var resolvedWrap: String { wrap ?? MonarchCodeBlockStrings.wrap }

    /// The word spoken before an added line.
    var resolvedAdded: String { added ?? MonarchCodeBlockStrings.added }

    /// The word spoken before a removed line.
    var resolvedRemoved: String { removed ?? MonarchCodeBlockStrings.removed }

    /// The name of code in a language, given its display name.
    func resolvedCodeName(_ languageName: String?) -> String {
        if let codeName { return codeName(languageName) }
        guard let languageName else { return MonarchCodeBlockStrings.code }
        return MonarchCodeBlockStrings.languageCode(languageName)
    }
}
