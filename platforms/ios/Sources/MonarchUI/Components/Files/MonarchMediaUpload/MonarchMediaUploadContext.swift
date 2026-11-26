/// Passed to the upload closure of ``MonarchMediaUpload`` with the file.
///
/// Cancelling the upload cancels the task the closure runs in; check
/// `Task.isCancelled` or use cancellable APIs such as `URLSession`'s async
/// methods.
public struct MonarchMediaUploadContext: Sendable {
    /// Whether the file is an image or a video.
    public let kind: MonarchMediaUploadKind
    private let progress: @Sendable (Double) -> Void

    /// A context. Monarch creates it; create one yourself only in tests.
    ///
    /// - Parameters:
    ///   - kind: Whether the file is an image or a video.
    ///   - progress: Receives each progress report, from 0 to 1.
    public init(kind: MonarchMediaUploadKind, progress: @escaping @Sendable (Double) -> Void) {
        self.kind = kind
        self.progress = progress
    }

    /// Reports how much is sent, from 0 to 1. Without reports the bar is
    /// indeterminate. It can be called from any thread.
    public func reportProgress(_ fraction: Double) {
        progress(fraction)
    }
}
