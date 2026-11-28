/// Replacements for the built-in strings of ``MonarchDownloadReportButton``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDownloadReportButtonLabels: Sendable {
    /// The title with several formats. Built in: "Download".
    public var label: String?
    /// The title with one format. Receives the format. Built in: "Download CSV".
    public var single: (@Sendable (String) -> String)?
    /// Announced and read while the file is prepared. Receives the format.
    /// Built in: "Preparing CSV…".
    public var preparing: (@Sendable (String) -> String)?
    /// Announced when the file is saved. Receives the format. Built in:
    /// "CSV downloaded".
    public var downloaded: (@Sendable (String) -> String)?
    /// Shown when the file is prepared in the background. Built in: "The
    /// file is being prepared. You can leave this screen; it will be ready to
    /// download shortly."
    public var queued: String?
    /// Shown when the download failed. Receives the format. Built in: "The
    /// CSV download failed."
    public var error: (@Sendable (String) -> String)?
    /// The name of the progress bar. Built in: "Download progress".
    public var progress: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        single: (@Sendable (String) -> String)? = nil,
        preparing: (@Sendable (String) -> String)? = nil,
        downloaded: (@Sendable (String) -> String)? = nil,
        queued: String? = nil,
        error: (@Sendable (String) -> String)? = nil,
        progress: String? = nil
    ) {
        self.label = label
        self.single = single
        self.preparing = preparing
        self.downloaded = downloaded
        self.queued = queued
        self.error = error
        self.progress = progress
    }
}
