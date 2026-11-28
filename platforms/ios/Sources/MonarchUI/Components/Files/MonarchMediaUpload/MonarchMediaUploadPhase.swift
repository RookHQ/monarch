/// Where an upload of ``MonarchMediaUpload`` stands.
enum MonarchMediaUploadPhase: Equatable, Sendable {
    /// Nothing is happening.
    case idle
    /// A video's length is being read.
    case checking(String)
    /// The file is being sent. The fraction is `nil` until the first report.
    case uploading(String, Double?)

    /// Whether a check or an upload is running.
    var isBusy: Bool { self != .idle }
}
