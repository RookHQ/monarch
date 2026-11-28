/// Replacements for the built-in strings of ``MonarchFileUpload``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchFileUploadLabels: Sendable {
    /// The main text. Built in: "Choose files to upload".
    public var title: String?
    /// The document picker button. Built in: "Browse files".
    public var browse: String?
    /// The photo picker button. Built in: "Choose photos".
    public var photos: String?
    /// Shown while files are dragged over the area. Built in: "Drop to upload".
    public var dropHere: String?
    /// The message for a file of a type that is not allowed, from its name.
    public var invalidType: (@Sendable (String) -> String)?
    /// The message for a file over the size limit, from its name and the formatted limit.
    public var tooLarge: (@Sendable (String, String) -> String)?
    /// The message for a file over the count limit, from its name and the limit.
    public var tooMany: (@Sendable (String, Int) -> String)?
    /// The generated hint, from the formatted size limit and the count limit, each when set.
    public var limits: (@Sendable (String?, Int?) -> String)?
    /// The hint once the count limit is reached, from the limit.
    public var limitReached: (@Sendable (Int) -> String)?
    /// Announced after files are accepted, from their number.
    public var added: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        browse: String? = nil,
        photos: String? = nil,
        dropHere: String? = nil,
        invalidType: (@Sendable (String) -> String)? = nil,
        tooLarge: (@Sendable (String, String) -> String)? = nil,
        tooMany: (@Sendable (String, Int) -> String)? = nil,
        limits: (@Sendable (String?, Int?) -> String)? = nil,
        limitReached: (@Sendable (Int) -> String)? = nil,
        added: (@Sendable (Int) -> String)? = nil
    ) {
        self.title = title
        self.browse = browse
        self.photos = photos
        self.dropHere = dropHere
        self.invalidType = invalidType
        self.tooLarge = tooLarge
        self.tooMany = tooMany
        self.limits = limits
        self.limitReached = limitReached
        self.added = added
    }
}
