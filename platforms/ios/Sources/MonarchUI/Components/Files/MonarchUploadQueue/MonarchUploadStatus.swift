/// Where an upload is. Shared by ``MonarchUploadQueue`` and ``MonarchAttachmentChip``.
public enum MonarchUploadStatus: Sendable, Hashable, CaseIterable {
    /// Waiting to start.
    case queued
    /// Sending.
    case uploading
    /// Uploaded.
    case done
    /// The upload failed.
    case failed
    /// The upload was cancelled.
    case cancelled

    /// Whether the upload can still finish: queued or uploading.
    public var isActive: Bool { self == .queued || self == .uploading }
}
