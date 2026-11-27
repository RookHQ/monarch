/// Built-in strings of ``MonarchFileUpload``, from the `MonarchFileUpload` strings table.
enum MonarchFileUploadStrings {
    private static let table = "MonarchFileUpload"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.fileUpload.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The main text.
    static var title: String { string("title") }
    /// The document picker button.
    static var browse: String { string("browse") }
    /// The photo picker button.
    static var photos: String { string("photos") }
    /// Shown while files are dragged over the area.
    static var dropHere: String { string("dropHere") }
    /// A file of a type that is not allowed.
    static func invalidType(_ name: String) -> String { string("invalidType", name) }
    /// A file over the size limit.
    static func tooLarge(_ name: String, _ limit: String) -> String { string("tooLarge", name, limit) }
    /// A file over the count limit.
    static func tooMany(_ name: String, _ limit: Int) -> String {
        limit == 1 ? string("tooMany.one", name) : string("tooMany.other", name, limit)
    }
    /// The size part of the generated hint.
    static func limitSize(_ size: String) -> String { string("limitSize", size) }
    /// The count part of the generated hint.
    static func limitCount(_ count: Int) -> String {
        count == 1 ? string("limitCount.one") : string("limitCount.other", count)
    }
    /// The hint once the count limit is reached.
    static func limitReached(_ count: Int) -> String {
        count == 1 ? string("limitReached.one") : string("limitReached.other", count)
    }
    /// Announced after files are accepted.
    static func added(_ count: Int) -> String {
        count == 1 ? string("added.one") : string("added.other", count)
    }
}
