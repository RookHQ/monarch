/// A file answer of a ``MonarchFieldRenderer``. The app uploads ``file`` and may replace the answer with a
/// stored ``name`` and no file.
public struct MonarchFormFieldFile: Hashable, Sendable {
    /// The file name shown in the list.
    public var name: String
    /// The size in bytes, when known.
    public var size: Int64?
    /// The picked file, until the app has uploaded it.
    public var file: MonarchFileUploadFile?

    /// A file answer.
    ///
    /// - Parameters:
    ///   - name: The file name shown in the list.
    ///   - size: The size in bytes, when known.
    ///   - file: The picked file, until the app has uploaded it.
    public init(name: String, size: Int64? = nil, file: MonarchFileUploadFile? = nil) {
        self.name = name
        self.size = size
        self.file = file
    }
}
